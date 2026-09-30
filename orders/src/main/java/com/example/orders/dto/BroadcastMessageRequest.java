package com.example.orders.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BroadcastMessageRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title must not exceed 100 characters")
        String title,

        @NotBlank(message = "Message is required")
        @Size(max = 500, message = "Message must not exceed 500 characters")
        String message
) {
}
