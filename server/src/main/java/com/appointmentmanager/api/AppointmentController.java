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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.time.LocalDateTime;
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
    public List<AppointmentResponse> list(@RequestParam(required = false) String name) {
        String normalizedName = name == null || name.isBlank() ? null : normalizeName(name);
        return appointments.findAll(normalizedName);
    }

    /** A compact, JDK-friendly format used by the existing JavaFX desktop client. */
    @GetMapping(value = "/export", produces = MediaType.TEXT_PLAIN_VALUE)
    public String export() {
        return appointments.findAll(null).stream()
                .map(this::toExportLine)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@Valid @RequestBody AppointmentRequest request) {
        String conflict = validate(request, null);
        if (conflict != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(conflict));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(appointments.insert(
                normalizeName(request.customerName()), request.startsAt(), request.durationMinutes()));
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> createFromDesktop(@RequestParam UUID id,
                                                @RequestParam String customerName,
                                                @RequestParam LocalDateTime startsAt,
                                                @RequestParam int durationMinutes) {
        AppointmentRequest request = new AppointmentRequest(customerName, startsAt, durationMinutes);
        String conflict = validate(request, null);
        if (conflict != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(conflict));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(appointments.insert(
                id, normalizeName(customerName), startsAt, durationMinutes));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody AppointmentRequest request) {
        if (appointments.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String conflict = validate(request, id);
        if (conflict != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(conflict));
        }
        return ResponseEntity.ok(appointments.update(
                id, normalizeName(request.customerName()), request.startsAt(), request.durationMinutes()));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> updateFromDesktop(@PathVariable UUID id,
                                                @RequestParam String customerName,
                                                @RequestParam LocalDateTime startsAt,
                                                @RequestParam int durationMinutes) {
        if (appointments.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        AppointmentRequest request = new AppointmentRequest(customerName, startsAt, durationMinutes);
        String conflict = validate(request, id);
        if (conflict != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(conflict));
        }
        return ResponseEntity.ok(appointments.update(id, normalizeName(customerName), startsAt, durationMinutes));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return appointments.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private String validate(AppointmentRequest request, UUID excludedId) {
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
