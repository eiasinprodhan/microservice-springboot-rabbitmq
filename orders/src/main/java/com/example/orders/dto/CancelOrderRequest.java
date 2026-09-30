package com.example.orders.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(

        @NotBlank(message = "Cancellation reason is required")
        @Size(max = 255, message = "Cancellation reason must not exceed 255 characters")
        String reason
) {
}
