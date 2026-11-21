package com.microservice.ecommerce.service;

import com.microservice.ecommerce.dto.OrderDTO;
import com.microservice.ecommerce.exception.BadRequestException;
import com.microservice.ecommerce.exception.ResourceNotFoundException;
import com.microservice.ecommerce.dto.OrderItemDTO;
import com.microservice.ecommerce.model.OrderItem;
import com.microservice.ecommerce.model.Orders;
import com.microservice.ecommerce.model.Product;
import com.microservice.ecommerce.model.User;
import com.microservice.ecommerce.repo.OrderRepository;
import com.microservice.ecommerce.repo.ProductRepository;
import com.microservice.ecommerce.repo.UserRepository;
import com.microservice.ecommerce.kafka.OrderEventProducer;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final int MAX_QUANTITY_PER_ITEM = 100;

    private final UserRepository userRepository;

    private final ProductRepository productRepository;

    private final OrderRepository orderRepository;

    private final OrderEventProducer orderEventProducer;

    public OrderService(UserRepository userRepository, ProductRepository productRepository, OrderRepository orderRepository, OrderEventProducer orderEventProducer) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderEventProducer = orderEventProducer;
    }

    /**
     * The total amount is calculated here from the prices stored in the database.
     * It is never taken from the client, so a user cannot change the price they pay.
     */
    public OrderDTO placeOrder(Long userId, Map<Long, Integer> productQuantities) {
        if (productQuantities == null || productQuantities.isEmpty()) {
            throw new BadRequestException("Order must contain at least one product");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Orders order = new Orders();
        order.setUser(user);
        order.setOrderDate(new Date());
        order.setStatus("PENDING");

        List<OrderItem> orderItems = new ArrayList<>();
        List<OrderItemDTO> orderItemDTOS = new ArrayList<>();
        double total = 0;

        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Integer quantity = entry.getValue();
            if (quantity == null || quantity <= 0 || quantity > MAX_QUANTITY_PER_ITEM) {
                throw new BadRequestException("Quantity must be between 1 and " + MAX_QUANTITY_PER_ITEM);
            }

            Product product = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + entry.getKey()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItems.add(orderItem);

            orderItemDTOS.add(new OrderItemDTO(product.getName(), product.getPrice(), quantity));
            total += product.getPrice() * quantity;
        }

        order.setTotalAmount(Math.round(total * 100.0) / 100.0);
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        List<Orders> ordersList = orderRepository.findByUser(user);
        return ordersList.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Called when PaymentProcessedEvent is received from Kafka.
     * Updates the order status based on payment result.
     */
    public void updateOrderStatus(Long orderId, String newStatus) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        String previousStatus = order.getStatus();
        order.setStatus(newStatus);
        orderRepository.save(order);

        // Publish order status updated event
        orderEventProducer.publishOrderStatusUpdated(orderId, previousStatus, newStatus);
    }
}
