package com.appointmentmanager.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentRequest(
        @NotBlank(message = "Customer name is required.") String customerName,
        @NotNull(message = "A start date and time is required.") LocalDateTime startsAt,
        @NotNull(message = "Duration is required.") Integer durationMinutes) {
}
