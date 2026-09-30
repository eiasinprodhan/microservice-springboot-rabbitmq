package com.example.orders.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateOrderRequest(

        @NotBlank(message = "Product name is required")
        @Size(max = 100,
                message = "Product name must not exceed 100 characters")
        String productName,

        @NotNull(message = "Amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Amount must be greater than zero"
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Amount must have maximum 2 decimal places"
        )
        BigDecimal amount,

        @NotBlank(message = "Customer email is required")
        @Email(message = "Invalid customer email")
        @Size(max = 255)
        String customerEmail
) {
}
