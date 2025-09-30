// src/main/java/com/graduation/integrationtests/models/OrderInputModel.java
package com.graduation.integrationtests.model;

import lombok.Builder;
import lombok.Data;

/**
 * Model representing order form input data for integration tests
 * This encapsulates all the data needed to fill the order creation form
 */
@Data
@Builder
public class OrderInputModel {

    // Customer Information
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerAddress;

    // Product Information
    private String productName;
    private String productPrice;
    private int quantity;

    /**
     * Factory method to create a valid sample order
     */
    public static OrderInputModel createValidSampleOrder() {
        return OrderInputModel.builder()
                .customerName("John Doe")
                .customerEmail("john.doe@example.com")
                .customerPhone("+845551234567")
                .customerAddress("123 Main St, New York, NY 10001")
                .productName("Premium Widget")
                .productPrice("100")
                .quantity(2)
                .build();
    }

    public static OrderInputModel createRandomCustomerNameSampleOrder() {

        String randomCustomerName = "Customer" + System.currentTimeMillis();
        return OrderInputModel.builder()
                .customerName(randomCustomerName)
                .customerEmail("john.doe@example.com")
                .customerPhone("+845551234567")
                .customerAddress("123 Main St, New York, NY 10001")
                .productName("Premium Widget")
                .productPrice("100")
                .quantity(2)
                .build();
    }

    /**
     * Factory method for testing edge cases
     */
    public static OrderInputModel createMinimalValidOrder() {
        return OrderInputModel.builder()
                .customerName("Test User")
                .customerEmail("test@example.com")
                .customerPhone("+15550000000")
                .customerAddress("Test Address")
                .productName("Test Product")
                .productPrice("100")
                .quantity(1)
                .build();
    }

    /**
     * Get total order amount
     */
    public double getTotalAmount() {
        return Double.parseDouble(productPrice) * quantity;
    }
}