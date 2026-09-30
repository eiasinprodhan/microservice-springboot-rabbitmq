package com.example.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateNotificationRequest(

        UUID orderId,

        @NotBlank(message = "Customer email is required")
        @Email(message = "Invalid customer email")
        @Size(max = 255)
        String customerEmail,

        @NotBlank(message = "Message is required")
        @Size(max = 500, message = "Message must not exceed 500 characters")
        String message
) {
}
