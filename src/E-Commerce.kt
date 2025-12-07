import java.time.LocalDateTime

object IdGenerator {
    private var userId = 0
    private var addressId = 0
    private var categoryId = 0
    private var productId = 0
    private var cartItemId = 0
    private var cartId = 0
    private var orderId = 0
    private var orderItemId = 0
    private var transactionId = 0
    private var wishlistId = 0
    private var deliveryId = 0
    private var trackingId = 0
    private var reviewId = 0
    private var logId = 0
    private var couponId = 0
    private var notificationId = 0
    private var adminId = 0
    private var paymentId = 0
    private var partnerId = 0
    private var returnId = 0
    private var historyId = 0

    fun nextUserId() = ++userId
    fun nextAddressId() = ++addressId
    fun nextCategoryId() = ++categoryId
    fun nextProductId() = ++productId
    fun nextCartItemId() = ++cartItemId
    fun nextCartId() = ++cartId
    fun nextOrderId() = ++orderId
    fun nextOrderItemId() = ++orderItemId
    fun nextTransactionId() = ++transactionId
    fun nextWishlistId() = ++wishlistId
    fun nextDeliveryId() = ++deliveryId
    fun nextTrackingId() = ++trackingId
    fun nextReviewId() = ++reviewId
    fun nextLogId() = ++logId
    fun nextCouponId() = ++couponId
    fun nextNotificationId() = ++notificationId
    fun nextAdminId() = ++adminId
    fun nextPaymentId() = ++paymentId
    fun nextPartnerId() = ++partnerId
    fun nextReturnId() = ++returnId
    fun nextHistoryId() = ++historyId
}

@JvmInline
value class PhoneNumber(val value: String) {
    init {
        require(value.matches(Regex("\\d{10}"))) {
            "Phone No must be exactly 10 digits"
        }
    }
}

@JvmInline
value class EmailFormat(val value: String) {
    init {
        require(value.contains("@")) { "Email format incorrect" }
    }
}

object DateTimeUtil {
    fun now(): String = LocalDateTime.now().toString()
}
enum class CategoryType {
    ELECTRONICS, FASHION, GROCERY, FRUITS, VEGETABLES,
    BEAUTY, HOME_APPLIANCES, MOBILE_ACCESSORIES, SPORTS, BOOKS
}

enum class OrderStatus { PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED }
enum class DeliveryStatus { NOT_DISPATCHED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, FAILED }
enum class Ratings { Excellent, Good, Bad, Nice, Satisfied }
enum class ChangeType { ADDED, REMOVED, RETURNED }
enum class ReturnStatus { REQUESTED, APPROVED, REJECTED, REFUNDED }
enum class PaymentMethod { UPI, DEBIT_CARD, CREDIT_CARD, NET_BANKING, CASH_ON_DELIVERY }
enum class PaymentStatus { SUCCESS, FAILED, PENDING }
enum class LanguageList { ENGLISH, HINDI, MARATHI, TELlGU }

data class User(
    val userId: Int = IdGenerator.nextUserId(),
    val name: String,
    val address: Address,
    val email: EmailFormat,
    val phone: PhoneNumber,
    val city: String,
    val language: LanguageList,
    val password :String,
    var isBlocked : Boolean = false
)

data class Address(
    val addressId: Int = IdGenerator.nextAddressId(),
    val userId: Int,
    val houseNo: String,
    val street: String,
    val city: String,
    val pinCode: String,
    val state: String
)

data class Category(
    val categoryId: Int = IdGenerator.nextCategoryId(),
    val categoryName: CategoryType
)

data class Product(
    val productId: Int = IdGenerator.nextProductId(),
    val productName: String,
    val description: String,
    val price: Double,
    val quantityAvailable: Int,
    val categoryId: Int,
    var isApproved : Boolean = false
)

data class CartItem(
    val cartItemId: Int = IdGenerator.nextCartItemId(),
    val userId: Int,
    val productId: Int,
    val quantity: Int,
    val productAmount: Float
)

data class Cart(
    val cartId: Int = IdGenerator.nextCartId(),
    val userId: Int,
    val items: MutableList<CartItem> = mutableListOf(),
    val createdAt: String = DateTimeUtil.now()
)

data class Order(
    val orderId: Int = IdGenerator.nextOrderId(),
    val userId: Int,
    val totalAmount: Double,
    val orderStatus: OrderStatus
)

data class OrderItem(
    val orderItemId: Int = IdGenerator.nextOrderItemId(),
    val orderId: Int,
    val productId: Int,
    val itemName: String,
    val quantity: Int,
    val price: Double
)

data class ProductTransaction(
    val transactionId: Int = IdGenerator.nextTransactionId(),
    val orderId: Int,
    val paymentId: Int,
    val transactionDate: String = DateTimeUtil.now(),
    val transactionAmount: Double
)

data class Wishlist(
    val wishlistId: Int = IdGenerator.nextWishlistId(),
    val userId: Int,
    val productIds: List<Int>
)

data class Delivery(
    val deliveryId: Int = IdGenerator.nextDeliveryId(),
    val orderId: Int,
    val deliveryPartner: String,
    val trackingId: Int = IdGenerator.nextTrackingId(),
    val expectedDate: String = DateTimeUtil.now(),
    val deliveryStatus: DeliveryStatus
)

data class Review(
    val reviewId: Int = IdGenerator.nextReviewId(),
    val userId: Int,
    val productId: Int,
    val rating: Ratings,
    val reviewText: String
)

data class InventoryLog(
    val logId: Int = IdGenerator.nextLogId(),
    val productId: Int,
    val changeType: ChangeType,
    val quantityChanged: Int,
    val updatedStock: Int,
    val timestamp: String = DateTimeUtil.now()
)

data class Coupon(
    val couponId: Int = IdGenerator.nextCouponId(),
    val code: String,
    val discountPercentage: Int,
    val expiryDate: String,
    val minOrderAmount: Double
)

data class Notification(
    val notificationId: Int = IdGenerator.nextNotificationId(),
    val userId: Int,
    val message: String,
    val timestamp: String = DateTimeUtil.now(),
    val isRead: Boolean
)

data class AdminPanel(
    val adminId: Int = IdGenerator.nextAdminId(),
    val name: String,
    val email: EmailFormat,
    val password: String
)

data class Payment(
    val userId: Int,
    val paymentId: Int = IdGenerator.nextPaymentId(),

    val amount: Double,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus
)

data class CourierPartner(
    val partnerId: Int = IdGenerator.nextPartnerId(),
    val name: String,
    val contactNumber: PhoneNumber
)

data class OrderHistory(
    val historyId: Int = IdGenerator.nextHistoryId(),
    val userId: Int,
    val orders: List<Order>
)

data class ReturnRequest(
    val returnId: Int = IdGenerator.nextReturnId(),
    val orderId: Int,
    val productId: Int,
    val reason: String,
    val status: ReturnStatus
)


interface UserService {
    fun registerUser(name: String, email: EmailFormat, phone: PhoneNumber, city: String,language: LanguageList,address: Address,password: String): User
    fun login(email: EmailFormat, password: String): Boolean
    fun updateUserDetails(userId: Int, name: String?, email: EmailFormat, city: String?,phone: PhoneNumber,language: LanguageList,address: Address,password: String): User?
    fun getUserById(userId: Int): User?
}

interface AuthService {
    fun generateToken(userId: Int): String
    fun validateToken(token: String): Boolean
}

interface ProductService {
    fun addProduct(product: Product): Product
    fun updateProduct(productId: Int, updatedProduct: Product): Product?
    fun deleteProduct(productId: Int): Boolean
    fun getProductById(productId: Int): Product?
    fun getProductsByCategory(categoryId: Int): List<Product>
    fun searchProducts(keyword: String): List<Product>
}

interface CartService {
    fun addToCart(userId: Int, cartId: Int, quantity: Int,productAmount : Float): Cart
    fun removeFromCart(userId: Int, cartItemId: Int): Cart
    fun viewCart(userId: Int): Cart?
    fun clearCart(userId: Int): Boolean
}

interface OrderService {
    fun placeOrder(userId: Int): Order
    fun cancelOrder(orderId: Int): Boolean
    fun getOrderDetails(orderId: Int): Order?
    fun listOrdersForUser(userId: Int): List<Order>
}

interface PaymentService {
    fun processPayment(userId: Int, amount: Double, method: PaymentMethod): Payment
    fun refundPayment(paymentId: Int): Boolean
}

interface InventoryService {
    fun checkStock(productId: Int): Int
    fun reduceStock(productId: Int, quantity: Int): Boolean
    fun increaseStock(productId: Int, quantity: Int): Boolean
}

interface ReviewService {
    fun addReview(review: Review): Review
    fun getReviews(productId: Int): List<Review>
}

interface NotificationService {
    fun sendNotification(userId: Int, message: String): Notification
    fun getNotifications(userId: Int): List<Notification>
}

interface AddressService {
    fun addAddress(address: Address): Address
    fun updateAddress(addressId: Int, updatedAddress: Address): Address?
    fun deleteAddress(addressId: Int): Boolean
    fun getAddresses(userId: Int): List<Address>
}

interface AdminService {
    fun approveProduct(productId: Int): Boolean
    fun blockUser(userId: Int): Boolean
    fun getDashboardStats(): Map<String, Int>
}
