package com.microservice.ecommerce.controller;

import com.microservice.common.security.AuthenticatedUser;
import com.microservice.ecommerce.dto.OrderDTO;
import com.microservice.ecommerce.model.OrderRequest;
import com.microservice.ecommerce.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /** A user can only place orders for themselves. The total is calculated on the server. */
    @PostMapping("/place/{userId}")
    public OrderDTO placeOrder(@PathVariable Long userId,
                               @RequestBody OrderRequest orderRequest,
                               @AuthenticationPrincipal AuthenticatedUser caller) {
        requireSelf(userId, caller, false);
        return orderService.placeOrder(userId, orderRequest.getProductQuantities());
    }

    @GetMapping("/all-orders")
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderDTO> getAllOrders() {
        return orderService.getAllOrders();
    }

    /** A user can only read their own orders (admins can read anyone's). */
    @GetMapping("/user/{userId}")
    public List<OrderDTO> getOrderByUser(@PathVariable Long userId,
                                         @AuthenticationPrincipal AuthenticatedUser caller) {
        requireSelf(userId, caller, true);
        return orderService.getOrderByUser(userId);
    }

    private void requireSelf(Long userId, AuthenticatedUser caller, boolean adminAllowed) {
        boolean self = caller.getUserId().equals(userId);
        if (!self && !(adminAllowed && caller.isAdmin())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own orders");
        }
    }
}
