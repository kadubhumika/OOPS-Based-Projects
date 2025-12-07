object UserRepository{
    val listUsers = mutableListOf<User>()

}


object AuthRepository{

    val saveTokens = mutableMapOf<String,Int>()
}
object ProductRepository{
    val productList  = mutableListOf<Product>()
}

object CartRepository{
    val cartItemList = mutableListOf<Cart>()


}
object OrderRepository{
    val ordersList = mutableListOf<Order>()
    val orderItemList = mutableListOf<OrderItem>()
}
object PaymentRepository {
    val payments = mutableListOf<Payment>()
}

object InventoryRepository {
    val stock = mutableMapOf<Int, Int>()  // productId → quantity
}
object InventoryLogRepository {
    val logs = mutableListOf<InventoryLog>()
}
object ReviewsRepository{
    val listReview = mutableListOf<Review>()
}

object NotificationRepository {
    val notifications = mutableListOf<Notification>()
}
object AddressRepository {
    val addresses = mutableListOf<Address>()
}
object AdminRepository {
    val admins = mutableListOf<AdminPanel>()
}
object WishlistRepository {
    val wishlists = mutableListOf<Wishlist>()
}



