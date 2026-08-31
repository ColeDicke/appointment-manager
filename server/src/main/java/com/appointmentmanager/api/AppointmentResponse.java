package com.appointmentmanager.api;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        String customerName,
        LocalDateTime startsAt,
        int durationMinutes) {
}
