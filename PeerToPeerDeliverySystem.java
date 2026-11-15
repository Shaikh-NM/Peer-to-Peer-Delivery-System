import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class PeerToPeerDeliverySystem {

    private enum OrderStatus { CREATED, ASSIGNED, PICKED_UP, DELIVERED, CANCELED }
    private static final class Order {
        final String id;
        final String customerId;
        final String itemId;
        OrderStatus status = OrderStatus.CREATED;
        String driverId; 

        Order(String id, String customerId, String itemId) {
            this.id = id;
            this.customerId = customerId;
            this.itemId = itemId;
        }
    }

    private final ReentrantLock lock = new ReentrantLock();

    private final Map<String, Order> orders = new HashMap<>();
    private final LinkedHashSet<String> pendingOrders = new LinkedHashSet<>();
    private final Map<String, String> driverToOrder = new HashMap<>();


    private final Set<String> validUsers = new HashSet<>();
    private final Set<String> allDrivers = new HashSet<>();
    private final TreeSet<String> availableDrivers = new TreeSet<>();
    private final Set<String> validItems;
    public PeerToPeerDeliverySystem(int totalUsers, int totalDrivers, List<String> deliverableItemIds) {
        for (int i = 0; i < totalUsers; i++) validUsers.add("user-" + i);
        for (int i = 0; i < totalDrivers; i++) {
            String d = "dv-" + i;
            allDrivers.add(d);
            availableDrivers.add(d);
        }
        this.validItems = new HashSet<>(deliverableItemIds);
    }

    public String createOrder(String orderId, String customerId, String itemId) {
        if (orderId == null || customerId == null || itemId == null) return "INVALID";
        lock.lock();
        try {
            if (orders.containsKey(orderId)
                    || !validUsers.contains(customerId)
                    || !validItems.contains(itemId)) {
                return "INVALID";
            }
            Order order = new Order(orderId, customerId, itemId);
            orders.put(orderId, order);
            pendingOrders.add(orderId);
            assignPending();
            return order.status == OrderStatus.ASSIGNED ? "ACCEPTED" : "PENDING";
        } finally {
            lock.unlock();
        }
    }

    public String cancelOrder(String orderId) {
        if (orderId == null) return "ERROR";
        lock.lock();
        try {
            Order order = orders.get(orderId);
            if (order == null) return "ERROR";

            switch (order.status) {
                case CREATED:
                    pendingOrders.remove(orderId);
                    order.status = OrderStatus.CANCELED;
                    return OrderStatus.CANCELED.name();
                case ASSIGNED:
                    releaseDriver(order.driverId);
                    order.driverId = null;
                    order.status = OrderStatus.CANCELED;
                    assignPending(); 
                    return OrderStatus.CANCELED.name();
                default: 
                    return "ERROR";
            }
        } finally {
            lock.unlock();
        }
    }

    public String pickUpOrder(String driverId, String orderId) {
        if (driverId == null || orderId == null) return "ERROR";
        lock.lock();
        try {
            if (!allDrivers.contains(driverId)) return "ERROR";
            Order order = orders.get(orderId);
            if (order == null
                    || order.status != OrderStatus.ASSIGNED
                    || !driverId.equals(order.driverId)) {
                return "ERROR";
            }
            order.status = OrderStatus.PICKED_UP;
            return OrderStatus.PICKED_UP.name();
        } finally {
            lock.unlock();
        }
    }

    public String deliverOrder(String driverId, String orderId) {
        if (driverId == null || orderId == null) return "ERROR";
        lock.lock();
        try {
            if (!allDrivers.contains(driverId)) return "ERROR";
            Order order = orders.get(orderId);
            if (order == null
                    || order.status != OrderStatus.PICKED_UP
                    || !driverId.equals(order.driverId)) {
                return "ERROR";
            }
            order.status = OrderStatus.DELIVERED;
            releaseDriver(driverId);
            assignPending();
            return OrderStatus.DELIVERED.name();
        } finally {
            lock.unlock();
        }
    }

    public String getOrderStatus(String orderId) {
        if (orderId == null) return "ERROR";
        lock.lock();
        try {
            Order order = orders.get(orderId);
            return order == null ? "ERROR" : order.status.name();
        } finally {
            lock.unlock();
        }
    }

    public String getDriverStatus(String driverId) {
        if (driverId == null) return "ERROR";
        lock.lock();
        try {
            if (!allDrivers.contains(driverId)) return "ERROR";
            String orderId = driverToOrder.get(driverId);
            return orderId == null ? "AVAILABLE" : "BUSY:" + orderId;
        } finally {
            lock.unlock();
        }
    }


    private void releaseDriver(String driverId) {
        driverToOrder.remove(driverId);
        availableDrivers.add(driverId);
    }

    private void assignPending() {
        while (!pendingOrders.isEmpty() && !availableDrivers.isEmpty()) {
            Iterator<String> it = pendingOrders.iterator();
            String orderId = it.next();
            it.remove();

            String driverId = availableDrivers.pollFirst();
            Order order = orders.get(orderId);
            order.driverId = driverId;
            order.status = OrderStatus.ASSIGNED;
            driverToOrder.put(driverId, orderId);
        }
    }

    public static void main(String[] args) {
        PeerToPeerDeliverySystem s =
                new PeerToPeerDeliverySystem(3, 1, List.of("X", "Y"));
        System.out.println(s.createOrder("O1", "user-0", "X"));   // ACCEPTED
        System.out.println(s.pickUpOrder("dv-0", "O1"));          // PICKED_UP
        System.out.println(s.createOrder("O2", "user-1", "Y"));   // PENDING
        System.out.println(s.createOrder("O3", "user-2", "X"));   // PENDING
        System.out.println(s.getOrderStatus("O2"));               // CREATED
        System.out.println(s.cancelOrder("O3"));                  // CANCELED
        System.out.println(s.deliverOrder("dv-0", "O1"));         // DELIVERED
        System.out.println(s.getOrderStatus("O2"));               // ASSIGNED
        System.out.println(s.getDriverStatus("dv-0"));            // BUSY:O2
        System.out.println(s.pickUpOrder("dv-0", "O3"));          // ERROR
    }
}