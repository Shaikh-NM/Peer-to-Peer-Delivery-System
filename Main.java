// String createOrder(String orderId, String customerId, String itemId)
// String cancelOrder(String orderId)
// String pickUpOrder(String driverId, String orderId)
// String deliverOrder(String driverId, String orderId)
// String getOrderStatus(String orderId)
// String getDriverStatus(String driverId)

enum OrderStatus{
    CREATED,
    ASSIGNED,
    PICKED_UP,
    DELIVERED,
    CANCELED
}

class Order{
    String orderId;
}

// class Customer{}

enum DriverStatus{
    AVAILABLE,
    BUSY:<orderId>,
}
// class Driver{}

// no need to create driver-service, customer-service
class OrderService{

}
class DriverService{}


public class Main{
    public static void main(string[] args){

    }
}