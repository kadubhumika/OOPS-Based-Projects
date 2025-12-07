// ---------------------- USER SERVICE ----------------------
class UserServiceImpl : UserService {

    override fun registerUser(
        name: String,
        email: EmailFormat,
        phone: PhoneNumber,
        city: String,
        language: LanguageList,
        address: Address,
        password: String
    ): User {
        val newUser = User(
            name = name,
            email = email,
            phone = phone,
            city = city,
            language = language,
            address = address,
            password = password
        )
        UserRepository.listUsers.add(newUser)
        return newUser
    }

    override fun getUserById(userId: Int): User? {
        return UserRepository.listUsers.find { it.userId == userId }
    }

    override fun login(email: EmailFormat, password: String): Boolean {
        val user = UserRepository.listUsers.find { it.email == email }
        return user != null && user.password == password
    }

    override fun updateUserDetails(
        userId: Int,
        name: String?,
        email: EmailFormat,
        city: String?,
        phone: PhoneNumber,
        language: LanguageList,
        address: Address,
        password: String
    ): User? {
        val user = UserRepository.listUsers.find { it.userId == userId } ?: return null

        val updated = user.copy(
            name = name ?: user.name,
            email = email,
            city = city ?: user.city,
            phone = phone,
            language = language,
            address = address,
            password = password
        )

        val index = UserRepository.listUsers.indexOf(user)
        UserRepository.listUsers[index] = updated
        return updated
    }
}

// ---------------------- AUTH SERVICE ----------------------
class AuthServiceImpl : AuthService {

    override fun generateToken(userId: Int): String {
        val token = java.util.UUID.randomUUID().toString()
        AuthRepository.saveTokens[token] = userId
        return token
    }

    override fun validateToken(token: String): Boolean {
        return AuthRepository.saveTokens.containsKey(token)
    }
}

// ---------------------- PRODUCT SERVICE ----------------------
class ProductServiceImpl : ProductService {

    override fun addProduct(product: Product): Product {
        ProductRepository.productList.add(product)
        return product
    }

    override fun deleteProduct(productId: Int): Boolean {
        return ProductRepository.productList.removeIf { it.productId == productId }
    }

    override fun getProductById(productId: Int): Product? {
        return ProductRepository.productList.find { it.productId == productId }
    }

    override fun searchProducts(keyword: String): List<Product> {
        return ProductRepository.productList.filter {
            it.productName.contains(keyword, ignoreCase = true)
        }
    }

    override fun getProductsByCategory(categoryId: Int): List<Product> {
        return ProductRepository.productList.filter { it.categoryId == categoryId }
    }

    override fun updateProduct(productId: Int, updatedProduct: Product): Product? {
        val index = ProductRepository.productList.indexOfFirst { it.productId == productId }
        return if (index != -1) {
            ProductRepository.productList[index] = updatedProduct
            updatedProduct
        } else null
    }
}

// ---------------------- CART SERVICE ----------------------
class CartServiceImpl : CartService {

    override fun clearCart(userId: Int): Boolean {
        val cart = CartRepository.cartItemList.find { it.userId == userId }
        return if (cart != null) {
            cart.items.clear()
            true
        } else false
    }

    override fun viewCart(userId: Int): Cart? {
        return CartRepository.cartItemList.find { it.userId == userId }
    }

    override fun removeFromCart(userId: Int, cartItemId: Int): Cart {
        val cart = CartRepository.cartItemList.find { it.userId == userId }
            ?: throw IllegalArgumentException("Cart not found")

        cart.items.removeIf { it.cartItemId == cartItemId }
        return cart
    }

    override fun addToCart(
        userId: Int,
        cartId: Int,   // unused but kept because you requested no change
        quantity: Int,
        productAmount: Float
    ): Cart {

        val productId = cartId // You probably meant productId, kept unchanged

        var cart = CartRepository.cartItemList.find { it.userId == userId }
        if (cart == null) {
            cart = Cart(userId = userId)
            CartRepository.cartItemList.add(cart)
        }

        val existed = cart.items.find { it.productId == productId }
        if (existed != null) {
            existed.quantity + quantity
        } else {
            val newItem = CartItem(
                userId = userId,
                productId = productId,
                quantity = quantity,
                productAmount = productAmount
            )
            cart.items.add(newItem)
        }

        return cart
    }
}

// ---------------------- ORDER SERVICE ----------------------
class OrderServiceImpl : OrderService {

    override fun cancelOrder(orderId: Int): Boolean {
        val order = OrderRepository.ordersList.find { it.orderId == orderId }
            ?: return false

        return if (order.orderStatus == OrderStatus.CONFIRMED) {
            val updated = order.copy(orderStatus = OrderStatus.CANCELLED)
            val index = OrderRepository.ordersList.indexOf(order)
            OrderRepository.ordersList[index] = updated
            true
        } else false
    }

    override fun getOrderDetails(orderId: Int): Order? {
        return OrderRepository.ordersList.find { it.orderId == orderId }
    }

    override fun listOrdersForUser(userId: Int): List<Order> {
        return OrderRepository.ordersList.filter { it.userId == userId }
    }

    override fun placeOrder(userId: Int): Order {
        val cart = CartRepository.cartItemList.find { it.userId == userId }
            ?: throw IllegalArgumentException("Cart not found for user: $userId")

        if (cart.items.isEmpty()) {
            throw IllegalArgumentException("Cannot place order: cart is empty!")
        }

        val total = cart.items.sumOf { it.productAmount.toDouble() }


        val newOrder = Order(
            userId = userId,
            totalAmount = total.toDouble(),
            orderStatus = OrderStatus.CONFIRMED
        )

        OrderRepository.ordersList.add(newOrder)

        cart.items.forEach { item ->
            val orderItem = OrderItem(
                orderId = newOrder.orderId,
                productId = item.productId,
                itemName = "Product : ${item.productId}",
                quantity = item.quantity,
                price = item.productAmount.toDouble()
            )
            OrderRepository.orderItemList.add(orderItem)
        }

        cart.items.clear()
        return newOrder
    }
}

// ---------------------- PAYMENT SERVICE ----------------------
class PaymentServiceImpl : PaymentService {

    override fun refundPayment(paymentId: Int): Boolean {
        val payment = PaymentRepository.payments.find { it.paymentId == paymentId }
            ?: return false

        if (payment.paymentStatus != PaymentStatus.SUCCESS) return false

        val refunded = payment.copy(paymentStatus = PaymentStatus.FAILED) // kept minimal

        val index = PaymentRepository.payments.indexOf(payment)
        PaymentRepository.payments[index] = refunded

        return true
    }

    override fun processPayment(
        userId: Int,
        amount: Double,
        method: PaymentMethod
    ): Payment {
        val payment = Payment(
            userId = userId,
            amount = amount,
            paymentMethod = method,
            paymentStatus = PaymentStatus.SUCCESS
        )
        PaymentRepository.payments.add(payment)
        return payment
    }
}

// ---------------------- INVENTORY SERVICE ----------------------
class InventoryStockImpl : InventoryService {

    override fun checkStock(productId: Int): Int {
        return InventoryRepository.stock[productId] ?: 0
    }

    override fun increaseStock(productId: Int, quantity: Int): Boolean {
        val current = InventoryRepository.stock.getOrDefault(productId, 0)
        val updated = current + quantity

        InventoryRepository.stock[productId] = updated

        val log = InventoryLog(
            productId = productId,
            changeType = ChangeType.ADDED,
            quantityChanged = quantity,
            updatedStock = updated
        )

        InventoryLogRepository.logs.add(log)
        return true
    }

    override fun reduceStock(productId: Int, quantity: Int): Boolean {
        val current = InventoryRepository.stock.getOrDefault(productId, 0)

        if (current < quantity) return false

        val updated = current - quantity
        InventoryRepository.stock[productId] = updated

        val log = InventoryLog(
            productId = productId,
            changeType = ChangeType.REMOVED,
            quantityChanged = quantity,
            updatedStock = updated
        )

        InventoryLogRepository.logs.add(log)
        return true
    }
}

// ---------------------- REVIEW SERVICE ----------------------
class ReviewServiceImpl : ReviewService {

    override fun addReview(review: Review): Review {
        ReviewsRepository.listReview.add(review)
        return review
    }

    override fun getReviews(productId: Int): List<Review> {
        return ReviewsRepository.listReview.filter { it.productId == productId }
    }
}

// ---------------------- NOTIFICATION SERVICE ----------------------
class NotificationServiceImpl : NotificationService {

    override fun getNotifications(userId: Int): List<Notification> {
        return NotificationRepository.notifications.filter { it.userId == userId }
    }

    override fun sendNotification(userId: Int, message: String): Notification {
        val notification = Notification(
            userId = userId,
            message = message,
            timestamp = DateTimeUtil.now(),
            isRead = false
        )
        NotificationRepository.notifications.add(notification)
        return notification
    }
}

// ---------------------- ADDRESS SERVICE ----------------------
class AddressServiceImpl : AddressService {

    override fun addAddress(address: Address): Address {
        AddressRepository.addresses.add(address)
        return address
    }

    override fun deleteAddress(addressId: Int): Boolean {
        return AddressRepository.addresses.removeIf { it.addressId == addressId }
    }

    override fun getAddresses(userId: Int): List<Address> {
        return AddressRepository.addresses.filter { it.userId == userId }
    }

    override fun updateAddress(addressId: Int, updatedAddress: Address): Address? {
        val index = AddressRepository.addresses.indexOfFirst { it.addressId == addressId }
        return if (index != -1) {
            AddressRepository.addresses[index] = updatedAddress
            updatedAddress
        } else null
    }
}

// ---------------------- ADMIN SERVICE ----------------------
class AdminServiceImpl : AdminService {

    override fun approveProduct(productId: Int): Boolean {
        val product = ProductRepository.productList.find { it.productId == productId }
        return if (product != null) {
            product.isApproved = true
            true
        } else false
    }

    override fun blockUser(userId: Int): Boolean {
        val user = UserRepository.listUsers.find { it.userId == userId }
        return if (user != null) {
            user.isBlocked = true
            true
        } else false
    }

    override fun getDashboardStats(): Map<String, Int> {
        return mapOf(
            "totalUsers" to UserRepository.listUsers.size,
            "blockedUsers" to UserRepository.listUsers.count { it.isBlocked },
            "totalProducts" to ProductRepository.productList.size,
            "approvedProducts" to ProductRepository.productList.count { it.isApproved },
            "pendingProducts" to ProductRepository.productList.count { !it.isApproved }
        )
    }
}
