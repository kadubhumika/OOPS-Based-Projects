

import java.io.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random


fun nowFormatted(): String =
    Instant.now().atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

fun sha256(text: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(text.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}


interface Persistable : Serializable

@JvmInline
value class Email(val value: String) : Serializable {
    init {
        require(value.contains("@")) { "Invalid email format." }
    }
}

@JvmInline
value class PhoneNo(val value: String) : Serializable {
    init {
        require(value.length == 10) { "Phone number must be 10 digits." }
    }
}

data class UserDetails(
    val username: String,
    val name: String,
    val city: String,
    val email: Email,
    val phone: PhoneNo,
    private var passwordHash: String
) : Persistable {
    fun checkPassword(password: String): Boolean = sha256(password) == passwordHash
    fun setPassword(newPassword: String) {
        passwordHash = sha256(newPassword)
    }

    override fun toString(): String = "User(username=$username, name=$name, city=$city, phone=${phone.value})"
}

enum class AccountType { SAVING, CURRENT }

abstract class BankAccount(
    val ownerUsername: String,
    val bankName: String,
    val accountType: AccountType
) : Persistable {


     var balance: BigDecimal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
    val accountNo: String = AccountNoGenerator.generate()

    abstract fun deposit(amount: BigDecimal): TransactionResult
    abstract fun withdraw(amount: BigDecimal): TransactionResult







    abstract fun accountSummary(): String
}

class SavingAccount(ownerUsername: String, bankName: String) : BankAccount(ownerUsername, bankName, AccountType.SAVING) {
    private val minBalance = BigDecimal(1000)
    override fun deposit(amount: BigDecimal): TransactionResult {
        if (amount <= BigDecimal.ZERO) return TransactionResult(false, "Invalid deposit amount", null)
        balance += amount
        return TransactionResult(true, "Deposited ₹$amount", null)
    }

    override fun withdraw(amount: BigDecimal): TransactionResult {
        if (amount <= BigDecimal.ZERO) return TransactionResult(false, "Invalid withdraw amount", null)
        if (balance - amount < minBalance) {
            return TransactionResult(false, "Minimum balance ₹$minBalance must be maintained", null)
        }
        balance -= amount
        return TransactionResult(true, "Withdrew ₹$amount", null)
    }

    fun applyMonthlyInterest(annualRatePercent: Double) {
        val monthlyRate = BigDecimal(annualRatePercent / 12 / 100).setScale(8, RoundingMode.HALF_UP)
        val interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP)
        if (interest > BigDecimal.ZERO) balance += interest
    }

    override fun accountSummary(): String =
        "SavingAccount(owner=$ownerUsername, no=$accountNo, balance=₹$balance)"
}

class CurrentAccount(ownerUsername: String, bankName: String) : BankAccount(ownerUsername, bankName, AccountType.CURRENT) {
    override fun deposit(amount: BigDecimal): TransactionResult {
        if (amount <= BigDecimal.ZERO) return TransactionResult(false, "Invalid deposit amount", null)
        balance += amount
        return TransactionResult(true, "Deposited ₹$amount", null)
    }

    override fun withdraw(amount: BigDecimal): TransactionResult {
        if (amount <= BigDecimal.ZERO) return TransactionResult(false, "Invalid withdraw amount", null)
        if (amount > balance) return TransactionResult(false, "Insufficient funds", null)
        balance -= amount
        return TransactionResult(true, "Withdrew ₹$amount", null)
    }

    override fun accountSummary(): String =
        "CurrentAccount(owner=$ownerUsername, no=$accountNo, balance=₹$balance)"
}

data class Transaction(
    val id: String,
    val fromAccount: String?,
    val toAccount: String?,
    val amount: BigDecimal,
    val type: TransactionType,
    val status: TransactionStatus,
    val timestamp: Long,
    val description: String?,
    val balanceAfter: BigDecimal?
) : Persistable

enum class TransactionStatus { PENDING, FAILED, COMPLETED }
enum class TransactionType { DEPOSIT, WITHDRAW, TRANSFER }

data class TransactionResult(val success: Boolean, val message: String, val transaction: Transaction?)


object AccountNoGenerator : Serializable {
    private val used = mutableSetOf<String>()
    private val rnd = Random(System.currentTimeMillis())

    fun generate(length: Int = 12): String {
        while (true) {
            val acc = buildString {
                repeat(length) { append(rnd.nextInt(0, 10)) }
            }
            if (acc !in used) {
                used.add(acc)
                return acc
            }
        }
    }
}

object TransactionManager : Persistable {
    private val transactions = mutableListOf<Transaction>()

    fun deposit(account: BankAccount, amount: BigDecimal, desc: String?): TransactionResult {
        val res = account.deposit(amount)
        val tx = Transaction(
            id = System.currentTimeMillis().toString(),
            fromAccount = null,
            toAccount = account.accountNo,
            amount = amount,
            type = TransactionType.DEPOSIT,
            status = if (res.success) TransactionStatus.COMPLETED else TransactionStatus.FAILED,
            timestamp = System.currentTimeMillis(),
            description = desc,
            balanceAfter = account.balance
        )
        transactions.add(tx)
        return TransactionResult(res.success, res.message, tx)
    }

    fun withdraw(account: BankAccount, amount: BigDecimal, desc: String?): TransactionResult {
        val res = account.withdraw(amount)
        val tx = Transaction(
            id = System.currentTimeMillis().toString(),
            fromAccount = account.accountNo,
            toAccount = null,
            amount = amount,
            type = TransactionType.WITHDRAW,
            status = if (res.success) TransactionStatus.COMPLETED else TransactionStatus.FAILED,
            timestamp = System.currentTimeMillis(),
            description = desc,
            balanceAfter = account.balance
        )
        transactions.add(tx)
        return TransactionResult(res.success, res.message, tx)
    }

    fun transfer(from: BankAccount, to: BankAccount, amount: BigDecimal, desc: String?): TransactionResult {
        val w = from.withdraw(amount)
        if (!w.success) return TransactionResult(false, w.message, null)
        val d = to.deposit(amount)
        if (!d.success) {
            // rollback if needed (simple)
            from.deposit(amount)
            return TransactionResult(false, d.message, null)
        }
        val tx = Transaction(
            id = System.currentTimeMillis().toString(),
            fromAccount = from.accountNo,
            toAccount = to.accountNo,
            amount = amount,
            type = TransactionType.TRANSFER,
            status = TransactionStatus.COMPLETED,
            timestamp = System.currentTimeMillis(),
            description = desc,
            balanceAfter = from.balance
        )
        transactions.add(tx)
        return TransactionResult(true, "Transferred ₹$amount from ${from.accountNo} to ${to.accountNo}", tx)
    }

    fun history(accountNo: String): List<Transaction> =
        transactions.filter { it.fromAccount == accountNo || it.toAccount == accountNo }


    fun loadState(saved: List<Transaction>) {
        transactions.clear()
        transactions.addAll(saved)
    }

    fun exportState(): List<Transaction> = transactions.toList()
}


object BankStorage {
    private const val USERS_FILE = "users.dat"
    private const val ACCOUNTS_FILE = "accounts.dat"
    private const val TRANSACTIONS_FILE = "transactions.dat"

    fun saveUsers(users: Map<String, UserDetails>) = writeObjectToFile(USERS_FILE, users)
    fun loadUsers(): Map<String, UserDetails> =
        (readObjectFromFile(USERS_FILE) as? Map<String, UserDetails>) ?: emptyMap()

    fun saveAccounts(accounts: Map<String, BankAccount>) = writeObjectToFile(ACCOUNTS_FILE, accounts)
    fun loadAccounts(): Map<String, BankAccount> =
        (readObjectFromFile(ACCOUNTS_FILE) as? Map<String, BankAccount>) ?: emptyMap()

    fun saveTransactions(trans: List<Transaction>) = writeObjectToFile(TRANSACTIONS_FILE, trans)
    fun loadTransactions(): List<Transaction> =
        (readObjectFromFile(TRANSACTIONS_FILE) as? List<Transaction>) ?: emptyList()

    private fun writeObjectToFile(filename: String, obj: Any) {
        try {
            ObjectOutputStream(FileOutputStream(filename)).use { it.writeObject(obj) }
        } catch (e: Exception) {
            println("Failed to save $filename: ${e.message}")
        }
    }

    private fun readObjectFromFile(filename: String): Any? {
        val f = File(filename)
        if (!f.exists()) return null
        return try {
            ObjectInputStream(FileInputStream(f)).use { it.readObject() }
        } catch (e: Exception) {
            println("Failed to read $filename: ${e.message}")
            null
        }
    }
}

object BankSystem {

    private val users = mutableMapOf<String, UserDetails>()
    private val accounts = mutableMapOf<String, BankAccount>()

    private const val ADMIN_PASSWORD = "admin123"

    init {

        val loadedUsers = BankStorage.loadUsers()
        users.putAll(loadedUsers)

        val loadedAccounts = BankStorage.loadAccounts()
        accounts.putAll(loadedAccounts)

        val tx = BankStorage.loadTransactions()
        TransactionManager.loadState(tx)
    }

    fun persistAll() {
        BankStorage.saveUsers(users)
        BankStorage.saveAccounts(accounts)
        BankStorage.saveTransactions(TransactionManager.exportState())
    }

    fun registerUser(): UserDetails {
        print("Choose a username (unique): ")
        val username = readln().trim()
        if (username.isEmpty() || users.containsKey(username)) {
            println("Invalid or taken username.")
            return registerUser()
        }
        print("Full name: ")
        val name = readln().trim()
        print("City: ")
        val city = readln().trim()
        print("Email: ")
        val email = Email(readln().trim())
        print("Phone (10 digits): ")
        val phone = PhoneNo(readln().trim())
        print("Choose password: ")
        val pw = readln()
        val user = UserDetails(username, name, city, email, phone, sha256(pw))
        users[username] = user
        persistAll()
        println("User registered: $username")
        return user
    }

    fun login(): UserDetails? {
        print("Username: ")
        val uname = readln().trim()
        print("Password: ")
        val pw = readln()
        val user = users[uname]
        return if (user != null && user.checkPassword(pw)) user else null
    }

    fun createAccountForUser(username: String): BankAccount? {
        val user = users[username] ?: return null
        print("Choose account type (SAVING/CURRENT): ")
        val t = readln().trim().uppercase()
        val acc: BankAccount = when (t) {
            "SAVING" -> SavingAccount(username, "SBI")
            "CURRENT" -> CurrentAccount(username, "SBI")
            else -> {
                println("Invalid type. Defaulting to SAVING.")
                SavingAccount(username, "SBI")
            }
        }
        accounts[acc.accountNo] = acc
        persistAll()
        println("Created account ${acc.accountNo} for $username")
        return acc
    }

    fun findAccount(accNo: String): BankAccount? = accounts[accNo]

    fun findAccountsByUser(username: String): List<BankAccount> =
        accounts.values.filter { it.ownerUsername == username }

    fun applyMonthlyInterestToAllSavings(annualRatePercent: Double) {
        accounts.values.filterIsInstance<SavingAccount>()
            .forEach { it.applyMonthlyInterest(annualRatePercent) }
        persistAll()
    }

    fun adminLogin(): Boolean {
        print("Enter admin password: ")
        val pw = readln()
        return pw == ADMIN_PASSWORD
    }

    fun adminListAllAccounts() {
        println("--- ALL ACCOUNTS ---")
        accounts.values.forEach { println(it.accountSummary() + " (owner=${it.ownerUsername})") }
    }

    fun adminListAllUsers() {
        println("--- ALL USERS ---")
        users.values.forEach { println(it) }
    }

}


fun main() {
    println("Welcome to SBI Console - ${nowFormatted()}")
    loop@ while (true) {
        println()

        println("Main Menu: 1:Register  2:Login  3:Admin  4:Exit")
        println("Enter Your Choice : ")
        when (readln().trim()) {
            "1" -> {
                BankSystem.registerUser()
            }
            "2" -> {
                val user = BankSystem.login()
                if (user == null) {
                    println("Login failed.")
                } else {
                    userMenu(user)
                }
            }
            "3" -> {
                if (BankSystem.adminLogin()) {
                    adminMenu()
                } else {
                    println("Admin auth failed.")
                }
            }
            "4" -> {
                println("Goodbye.")
                BankSystem.persistAll()
                break@loop
            }
            else -> println("Invalid choice")
        }
    }
}

fun userMenu(user: UserDetails) {
    println("Welcome, ${user.name} (${user.username})")
    menu@ while (true) {
        println()
        println("User Menu: 1:Create Account 2:List Accounts 3:Deposit 4:Withdraw 5:Transfer 6:History 7:Logout")
        when (readln().trim()) {
            "1" -> BankSystem.createAccountForUser(user.username)
            "2" -> {
                val accs = BankSystem.findAccountsByUser(user.username)
                if (accs.isEmpty()) println("No accounts yet.")
                else accs.forEach { println(it.accountSummary()) }
            }
            "3" -> {
                print("Account No: "); val accNo = readln().trim()
                val acc = BankSystem.findAccount(accNo)
                if (acc == null || acc.ownerUsername != user.username) { println("Account not found or not yours."); continue@menu }
                print("Amount to deposit: "); val amt = readln().toBigDecimalOrNull() ?: continue@menu
                val res = TransactionManager.deposit(acc, amt, "Deposit by ${user.username}")
                println(res.message)
                BankSystem.persistAll()
            }
            "4" -> {
                print("Account No: "); val accNo = readln().trim()
                val acc = BankSystem.findAccount(accNo)
                if (acc == null || acc.ownerUsername != user.username) { println("Account not found or not yours."); continue@menu }
                print("Amount to withdraw: "); val amt = readln().toBigDecimalOrNull() ?: continue@menu
                val res = TransactionManager.withdraw(acc, amt, "Withdraw by ${user.username}")
                println(res.message)
                BankSystem.persistAll()
            }
            "5" -> {
                print("From Account No: "); val from = readln().trim()
                print("To Account No: "); val to = readln().trim()
                val fromAcc = BankSystem.findAccount(from)
                val toAcc = BankSystem.findAccount(to)
                if (fromAcc == null || fromAcc.ownerUsername != user.username) { println("Invalid from account."); continue@menu }
                if (toAcc == null) { println("Invalid to account."); continue@menu }
                print("Amount to transfer: "); val amt = readln().toBigDecimalOrNull() ?: continue@menu
                val res = TransactionManager.transfer(fromAcc, toAcc, amt, "Transfer by ${user.username}")
                println(res.message)
                BankSystem.persistAll()
            }
            "6" -> {
                print("Account No for history: "); val accNo = readln().trim()
                val hist = TransactionManager.history(accNo)
                if (hist.isEmpty()) println("No transactions.")
                else hist.forEach { t ->
                    println("${t.type} ${t.amount} ${t.status} at ${Instant.ofEpochMilli(t.timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))} desc=${t.description}")
                }
            }
            "7" -> {
                println("Logging out.")
                break@menu
            }
            else -> println("Invalid choice")
        }
    }
}

fun adminMenu() {
    println("Admin Menu")
    aLoop@ while (true) {
        println("1:List Users 2:List Accounts 3:Apply monthly interest to savings 4:Back")
        when (readln().trim()) {
            "1" -> BankSystem.adminListAllUsers()
            "2" -> BankSystem.adminListAllAccounts()
            "3" -> {
                print("Enter annual interest rate percent (e.g., 3.5): ")
                val r = readln().toDoubleOrNull()
                if (r == null) { println("Invalid rate"); continue@aLoop }
                BankSystem.applyMonthlyInterestToAllSavings(r)
                println("Applied monthly interest (annual $r%) to all saving accounts.")
            }
            "4" -> break@aLoop
            else -> println("Invalid")
        }
    }
}
