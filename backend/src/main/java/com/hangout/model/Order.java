package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "orders")
public class Order {
    @Id
    private String id;
    
    private String userId;
    private String restaurantId;
    private String reservationId;
    private List<OrderItem> items;
    private Double subtotal;
    private Double tax;
    private Double total;
    private OrderStatus status = OrderStatus.PENDING;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @Data
    public static class OrderItem {
        private String menuItemId;
        private String name;
        private Integer quantity;
        private Double price;
    }
    
    public enum OrderStatus {
        PENDING, CONFIRMED, PREPARING, READY, DELIVERED, CANCELLED
    }
}
