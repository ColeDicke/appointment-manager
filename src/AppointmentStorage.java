import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/** Communicates with the Appointment API. Database credentials never leave the backend. */
public final class AppointmentStorage {
    private static final String DEFAULT_API_URL = "http://localhost:8080";
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private AppointmentStorage() {
    }

    public static List<Appointment> load() throws IOException {
        HttpResponse<String> response = send(HttpRequest.newBuilder(apiUri("/api/appointments/export"))
                .GET().build());
        List<Appointment> appointments = new ArrayList<>();
        if (response.body().isBlank()) {
            return appointments;
        }

        for (String line : response.body().split("\\R")) {
            String[] values = line.split("\\|", -1);
            if (values.length != 4) {
                throw new IOException("The appointment API returned invalid data.");
            }
            try {
                UUID id = UUID.fromString(values[0]);
                String name = new String(Base64.getUrlDecoder().decode(values[1]), StandardCharsets.UTF_8);
                LocalDateTime start = LocalDateTime.parse(values[2]);
                int duration = Integer.parseInt(values[3]);
                appointments.add(Appointment.fromSavedData(duration, name, start, id));
            } catch (IllegalArgumentException exception) {
                throw new IOException("The appointment API returned invalid data.", exception);
            }
        }
        return appointments;
    }

    public static void create(Appointment appointment) throws IOException {
        sendForm("POST", "/api/appointments", appointment);
    }

    public static void update(Appointment appointment) throws IOException {
        sendForm("PUT", "/api/appointments/" + appointment.getAppointmentID(), appointment);
    }

    public static void delete(Appointment appointment) throws IOException {
        send(HttpRequest.newBuilder(apiUri("/api/appointments/" + appointment.getAppointmentID()))
                .DELETE().build());
    }

    private static void sendForm(String method, String path, Appointment appointment) throws IOException {
        String form = "id=" + encode(appointment.getAppointmentID().toString())
                + "&customerName=" + encode(appointment.getCustomerName())
                + "&startsAt=" + encode(appointment.getApptStart().toString())
                + "&durationMinutes=" + appointment.getApptDuration();
        HttpRequest request = HttpRequest.newBuilder(apiUri(path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .method(method, HttpRequest.BodyPublishers.ofString(form))
                .build();
        send(request);
    }

    private static HttpResponse<String> send(HttpRequest request) throws IOException {
        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Appointment API error (" + response.statusCode() + "): " + response.body());
            }
            return response;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Request to the appointment API was interrupted.", exception);
        }
    }

    private static URI apiUri(String path) {
        String baseUrl = System.getenv().getOrDefault("APPOINTMENT_API_URL", DEFAULT_API_URL);
        return URI.create(baseUrl.replaceAll("/+$", "") + path);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
