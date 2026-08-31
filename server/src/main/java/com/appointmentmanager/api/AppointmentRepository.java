package com.appointmentmanager.api;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AppointmentRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public AppointmentRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AppointmentResponse> findAll(String customerName) {
        String sql = """
                SELECT id, customer_name, starts_at, duration_minutes
                FROM appointments
                WHERE (CAST(:customerName AS TEXT) IS NULL
                    OR LOWER(customer_name) = LOWER(CAST(:customerName AS TEXT)))
                ORDER BY starts_at
                """;
        return jdbc.query(sql, new MapSqlParameterSource("customerName", customerName),
                (rs, rowNum) -> map(rs.getObject("id", UUID.class), rs.getString("customer_name"),
                        rs.getTimestamp("starts_at").toLocalDateTime(), rs.getInt("duration_minutes")));
    }

    public Optional<AppointmentResponse> findById(UUID id) {
        String sql = "SELECT id, customer_name, starts_at, duration_minutes FROM appointments WHERE id = :id";
        List<AppointmentResponse> results = jdbc.query(sql, new MapSqlParameterSource("id", id),
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

    public AppointmentResponse insert(String customerName, LocalDateTime startsAt, int durationMinutes) {
        return insert(UUID.randomUUID(), customerName, startsAt, durationMinutes);
    }

    public AppointmentResponse insert(UUID id, String customerName, LocalDateTime startsAt, int durationMinutes) {
        String sql = """
                INSERT INTO appointments (id, customer_name, starts_at, duration_minutes)
                VALUES (:id, :customerName, :startsAt, :durationMinutes)
                """;
        jdbc.update(sql, values(id, customerName, startsAt, durationMinutes));
        return new AppointmentResponse(id, customerName, startsAt, durationMinutes);
    }

    public AppointmentResponse update(UUID id, String customerName, LocalDateTime startsAt, int durationMinutes) {
        String sql = """
                UPDATE appointments
                SET customer_name = :customerName, starts_at = :startsAt,
                    duration_minutes = :durationMinutes, updated_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """;
        jdbc.update(sql, values(id, customerName, startsAt, durationMinutes));
        return new AppointmentResponse(id, customerName, startsAt, durationMinutes);
    }

    public boolean delete(UUID id) {
        return jdbc.update("DELETE FROM appointments WHERE id = :id", new MapSqlParameterSource("id", id)) > 0;
    }

    private MapSqlParameterSource values(UUID id, String customerName, LocalDateTime startsAt, int durationMinutes) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("customerName", customerName)
                .addValue("startsAt", startsAt)
                .addValue("durationMinutes", durationMinutes);
    }

    private AppointmentResponse map(UUID id, String customerName, LocalDateTime startsAt, int durationMinutes) {
        return new AppointmentResponse(id, customerName, startsAt, durationMinutes);
    }
}
