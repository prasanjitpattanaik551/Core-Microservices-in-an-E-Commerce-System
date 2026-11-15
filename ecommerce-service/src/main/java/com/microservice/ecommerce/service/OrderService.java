package com.microservice.ecommerce.service;

import com.microservice.ecommerce.dto.OrderDTO;
import com.microservice.ecommerce.dto.OrderItemDTO;
import com.microservice.ecommerce.model.OrderItem;
import com.microservice.ecommerce.model.Orders;
import com.microservice.ecommerce.model.Product;
import com.microservice.ecommerce.model.User;
import com.microservice.ecommerce.repo.OrderRepository;
import com.microservice.ecommerce.repo.ProductRepository;
import com.microservice.ecommerce.repo.UserRepository;
import com.microservice.ecommerce.kafka.OrderEventProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderEventProducer orderEventProducer;

    public OrderDTO placeOrder(Long userId, Map<Long, Integer> productQuantities, double totalAmount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Orders order = new Orders();
        order.setUser(user);
        order.setOrderDate(new Date());
        order.setStatus("PENDING");
        order.setTotalAmount(totalAmount);

        List<OrderItem> orderItems = new ArrayList<>();
        List<OrderItemDTO> orderItemDTOS = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Product product = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("Product Not found"));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(entry.getValue());
            orderItems.add(orderItem);

            orderItemDTOS.add(new OrderItemDTO(product.getName(), product.getPrice(), entry.getValue()));
        }

        order.setOrderItems(orderItems);
        Orders saveOrder = orderRepository.save(order);

        // Publish OrderCreatedEvent to Kafka
        orderEventProducer.publishOrderCreated(saveOrder, user, productQuantities);

        return new OrderDTO(saveOrder.getId(), saveOrder.getTotalAmount(),
                saveOrder.getStatus(), saveOrder.getOrderDate(), orderItemDTOS);
    }

    public List<OrderDTO> getAllOrders() {
        List<Orders> orders = orderRepository.findAllOrdersWithUsers();
        return orders.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private OrderDTO convertToDTO(Orders orders) {
        List<OrderItemDTO> OrderItems = orders.getOrderItems().stream()
                .map(item -> new OrderItemDTO(
                        item.getProduct().getName(),
                        item.getProduct().getPrice(),
                        item.getQuantity())).collect(Collectors.toList());
        return new OrderDTO(
                orders.getId(),
                orders.getTotalAmount(),
                orders.getStatus(),
                orders.getOrderDate(),
                orders.getUser() != null ? orders.getUser().getName() : "Unknown",
                orders.getUser() != null ? orders.getUser().getEmail() : "Unknown",
                OrderItems
        );
    }

    public List<OrderDTO> getOrderByUser(Long userId) {
        Optional<User> userOp = userRepository.findById(userId);
        if (userOp.isEmpty()) {
            throw new RuntimeException("user not found");
        }
        User user = userOp.get();
        List<Orders> ordersList = orderRepository.findByUser(user);
        return ordersList.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Called when PaymentProcessedEvent is received from Kafka.
     * Updates the order status based on payment result.
     */
    public void updateOrderStatus(Long orderId, String newStatus) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        String previousStatus = order.getStatus();
        order.setStatus(newStatus);
        orderRepository.save(order);

        // Publish order status updated event
        orderEventProducer.publishOrderStatusUpdated(orderId, previousStatus, newStatus);
    }
}
