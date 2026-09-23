package com.appointmentmanager.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentRepository appointments;

    public AppointmentController(AppointmentRepository appointments) {
        this.appointments = appointments;
    }

    @GetMapping
    public List<AppointmentResponse> list(
            @RequestAttribute(AuthenticationFilter.USER_ID_ATTRIBUTE) UUID userId,
            @RequestParam(required = false) String name) {
        String normalizedName = name == null || name.isBlank() ? null : normalizeName(name);
        return appointments.findAll(userId, normalizedName);
    }

    /** A compact, JDK-friendly format used by the existing JavaFX desktop client. */
    @GetMapping(value = "/export", produces = MediaType.TEXT_PLAIN_VALUE)
    public String export(@RequestAttribute(AuthenticationFilter.USER_ID_ATTRIBUTE) UUID userId) {
        return appointments.findAll(userId, null).stream()
                .map(this::toExportLine)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    @GetMapping(value = "/availability", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> availability(
            @RequestAttribute(AuthenticationFilter.USER_ID_ATTRIBUTE) UUID userId,
            @RequestParam LocalDate date,
            @RequestParam int durationMinutes,
            @RequestParam(required = false) UUID excludedId) {
        if (durationMinutes != 15 && durationMinutes != 30
                && durationMinutes != 45 && durationMinutes != 60) {
            return ResponseEntity.badRequest().body("Duration must be 15, 30, 45, or 60 minutes.");
        }
        if (excludedId != null && appointments.findById(excludedId, userId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String result = appointments.findAvailableStarts(date, durationMinutes, excludedId).stream()
                .map(LocalTime::toString)
                .collect(java.util.stream.Collectors.joining("\n"));
        return ResponseEntity.ok(result);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(
            @RequestAttribute(AuthenticationFilter.USER_ID_ATTRIBUTE) UUID userId,
            @Valid @RequestBody AppointmentRequest request) {
        String conflict = validate(request, null);
        if (conflict != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(conflict));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(appointments.insert(
                userId, normalizeName(request.customerName()), request.startsAt(), request.durationMinutes()));
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> createFromDesktop(
                                                @RequestAttribute(AuthenticationFilter.USER_ID_ATTRIBUTE) UUID userId,
                                                @RequestParam UUID id,
                                                @RequestParam String customerName,
                                                @RequestParam LocalDateTime startsAt,
                                                @RequestParam int durationMinutes) {
        AppointmentRequest request = new AppointmentRequest(customerName, startsAt, durationMinutes);
        String conflict = validate(request, null);
        if (conflict != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(conflict));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(appointments.insert(
                id, userId, normalizeName(customerName), startsAt, durationMinutes));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(
            @RequestAttribute(AuthenticationFilter.USER_ID_ATTRIBUTE) UUID userId,
            @PathVariable UUID id, @Valid @RequestBody AppointmentRequest request) {
        if (appointments.findById(id, userId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String conflict = validate(request, id);
        if (conflict != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(conflict));
        }
        return ResponseEntity.ok(appointments.update(
                id, userId, normalizeName(request.customerName()), request.startsAt(), request.durationMinutes()));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> updateFromDesktop(
                                                @RequestAttribute(AuthenticationFilter.USER_ID_ATTRIBUTE) UUID userId,
                                                @PathVariable UUID id,
                                                @RequestParam String customerName,
                                                @RequestParam LocalDateTime startsAt,
                                                @RequestParam int durationMinutes) {
        if (appointments.findById(id, userId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        AppointmentRequest request = new AppointmentRequest(customerName, startsAt, durationMinutes);
        String conflict = validate(request, id);
        if (conflict != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(conflict));
        }
        return ResponseEntity.ok(appointments.update(
                id, userId, normalizeName(customerName), startsAt, durationMinutes));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestAttribute(AuthenticationFilter.USER_ID_ATTRIBUTE) UUID userId,
            @PathVariable UUID id) {
        return appointments.delete(id, userId)
                ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private String validate(AppointmentRequest request, UUID excludedId) {
        if (request.customerName() == null || request.customerName().isBlank()) {
            return "Customer name is required.";
        }
        if (request.durationMinutes() != 15 && request.durationMinutes() != 30
                && request.durationMinutes() != 45 && request.durationMinutes() != 60) {
            return "Duration must be 15, 30, 45, or 60 minutes.";
        }
        if (request.startsAt().toLocalTime().isBefore(LocalTime.of(9, 0))
                || request.startsAt().plusMinutes(request.durationMinutes())
                .isAfter(request.startsAt().toLocalDate().atTime(17, 0))) {
            return "Appointments must fall between 9:00 AM and 5:00 PM.";
        }
        if (appointments.conflictsWith(request.startsAt(), request.durationMinutes(), excludedId)) {
            return "That time conflicts with an existing appointment.";
        }
        return null;
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private String toExportLine(AppointmentResponse appointment) {
        String encodedName = Base64.getUrlEncoder().withoutPadding().encodeToString(
                appointment.customerName().getBytes(StandardCharsets.UTF_8));
        return appointment.id() + "|" + encodedName + "|" + appointment.startsAt()
                + "|" + appointment.durationMinutes();
    }

    private record ApiError(String message) {
    }
}
