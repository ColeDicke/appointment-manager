package com.appointmentmanager.api;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AppointmentRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public AppointmentRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AppointmentResponse> findAll(UUID ownerId, String customerName) {
        String sql = """
                SELECT id, customer_name, starts_at, duration_minutes
                FROM appointments
                WHERE owner_id = :ownerId
                  AND (CAST(:customerName AS TEXT) IS NULL
                    OR LOWER(customer_name) = LOWER(CAST(:customerName AS TEXT)))
                ORDER BY starts_at
                """;
        MapSqlParameterSource values = new MapSqlParameterSource()
                .addValue("ownerId", ownerId)
                .addValue("customerName", customerName);
        return jdbc.query(sql, values,
                (rs, rowNum) -> map(rs.getObject("id", UUID.class), rs.getString("customer_name"),
                        rs.getTimestamp("starts_at").toLocalDateTime(), rs.getInt("duration_minutes")));
    }

    public Optional<AppointmentResponse> findById(UUID id, UUID ownerId) {
        String sql = """
                SELECT id, customer_name, starts_at, duration_minutes
                FROM appointments
                WHERE id = :id AND owner_id = :ownerId
                """;
        MapSqlParameterSource values = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("ownerId", ownerId);
        List<AppointmentResponse> results = jdbc.query(sql, values,
                (rs, rowNum) -> map(rs.getObject("id", UUID.class), rs.getString("customer_name"),
                        rs.getTimestamp("starts_at").toLocalDateTime(), rs.getInt("duration_minutes")));
        return results.stream().findFirst();
    }

    public boolean conflictsWith(LocalDateTime startsAt, int durationMinutes, UUID excludedId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM appointments
                    WHERE starts_at < :endsAt
                      AND starts_at + duration_minutes * INTERVAL '1 minute' > :startsAt
                      AND (CAST(:excludedId AS UUID) IS NULL OR id <> CAST(:excludedId AS UUID))
                )
                """;
        MapSqlParameterSource values = new MapSqlParameterSource()
                .addValue("startsAt", startsAt)
                .addValue("endsAt", startsAt.plusMinutes(durationMinutes))
                .addValue("excludedId", excludedId);
        return Boolean.TRUE.equals(jdbc.queryForObject(sql, values, Boolean.class));
    }

    public List<LocalTime> findAvailableStarts(LocalDate date, int durationMinutes, UUID excludedId) {
        String sql = """
                SELECT id, starts_at, duration_minutes
                FROM appointments
                WHERE starts_at >= :dayStart AND starts_at < :nextDayStart
                ORDER BY starts_at
                """;
        MapSqlParameterSource values = new MapSqlParameterSource()
                .addValue("dayStart", date.atStartOfDay())
                .addValue("nextDayStart", date.plusDays(1).atStartOfDay());
        List<OccupiedAppointment> occupied = jdbc.query(sql, values,
                (rs, rowNum) -> new OccupiedAppointment(
                        rs.getObject("id", UUID.class),
                        rs.getTimestamp("starts_at").toLocalDateTime(),
                        rs.getInt("duration_minutes")));

        List<LocalTime> available = new ArrayList<>();
        for (LocalTime time = LocalTime.of(9, 0);
             !time.plusMinutes(durationMinutes).isAfter(LocalTime.of(17, 0));
             time = time.plusMinutes(15)) {
            LocalDateTime candidateStart = date.atTime(time);
            LocalDateTime candidateEnd = candidateStart.plusMinutes(durationMinutes);
            boolean conflict = occupied.stream().anyMatch(existing ->
                    !existing.id().equals(excludedId)
                            && candidateStart.isBefore(existing.startsAt().plusMinutes(existing.durationMinutes()))
                            && existing.startsAt().isBefore(candidateEnd));
            if (!conflict) {
                available.add(time);
            }
        }
        return available;
    }

    public AppointmentResponse insert(UUID ownerId, String customerName,
                                      LocalDateTime startsAt, int durationMinutes) {
        return insert(UUID.randomUUID(), ownerId, customerName, startsAt, durationMinutes);
    }

    public AppointmentResponse insert(UUID id, UUID ownerId, String customerName,
                                      LocalDateTime startsAt, int durationMinutes) {
        String sql = """
                INSERT INTO appointments (id, owner_id, customer_name, starts_at, duration_minutes)
                VALUES (:id, :ownerId, :customerName, :startsAt, :durationMinutes)
                """;
        jdbc.update(sql, values(id, ownerId, customerName, startsAt, durationMinutes));
        return new AppointmentResponse(id, customerName, startsAt, durationMinutes);
    }

    public AppointmentResponse update(UUID id, UUID ownerId, String customerName,
                                      LocalDateTime startsAt, int durationMinutes) {
        String sql = """
                UPDATE appointments
                SET customer_name = :customerName, starts_at = :startsAt,
                    duration_minutes = :durationMinutes, updated_at = CURRENT_TIMESTAMP
                WHERE id = :id AND owner_id = :ownerId
                """;
        jdbc.update(sql, values(id, ownerId, customerName, startsAt, durationMinutes));
        return new AppointmentResponse(id, customerName, startsAt, durationMinutes);
    }

    public boolean delete(UUID id, UUID ownerId) {
        String sql = "DELETE FROM appointments WHERE id = :id AND owner_id = :ownerId";
        MapSqlParameterSource values = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("ownerId", ownerId);
        return jdbc.update(sql, values) > 0;
    }

    private MapSqlParameterSource values(UUID id, UUID ownerId, String customerName,
                                         LocalDateTime startsAt, int durationMinutes) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("ownerId", ownerId)
                .addValue("customerName", customerName)
                .addValue("startsAt", startsAt)
                .addValue("durationMinutes", durationMinutes);
    }

    private AppointmentResponse map(UUID id, String customerName, LocalDateTime startsAt, int durationMinutes) {
        return new AppointmentResponse(id, customerName, startsAt, durationMinutes);
    }

    private record OccupiedAppointment(UUID id, LocalDateTime startsAt, int durationMinutes) {
    }
}
