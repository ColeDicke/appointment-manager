import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/** Communicates with the Appointment API. Database credentials never leave the backend. */
public final class AppointmentStorage {
    /** Public shared backend; APPOINTMENT_API_URL can override it for local development. */
    private static final String DEFAULT_API_URL = "https://appointment-manager-wxnw.onrender.com";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private static String accessToken;
    private static String refreshToken;
    private static String currentEmail;

    private AppointmentStorage() {
    }

    public static void login(String email, String password) throws IOException {
        authenticate("/api/auth/login", email, password, false);
    }

    /** Returns true when signup also signed the user in, or false when email confirmation is required. */
    public static boolean signUp(String email, String password) throws IOException {
        return authenticate("/api/auth/signup", email, password, true);
    }

    public static void logout() {
        accessToken = null;
        refreshToken = null;
        currentEmail = null;
    }

    public static String getCurrentEmail() {
        return currentEmail;
    }

    public static List<Appointment> load() throws IOException {
        HttpResponse<String> response = send(authenticatedRequest("/api/appointments/export")
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
        send(authenticatedRequest("/api/appointments/" + appointment.getAppointmentID())
                .DELETE().build());
    }

    public static List<java.time.LocalTime> getAvailableStarts(
            java.time.LocalDate date, int durationMinutes, Appointment ignoredAppointment) throws IOException {
        String path = "/api/appointments/availability?date=" + encode(date.toString())
                + "&durationMinutes=" + durationMinutes;
        if (ignoredAppointment != null) {
            path += "&excludedId=" + encode(ignoredAppointment.getAppointmentID().toString());
        }
        HttpResponse<String> response = send(authenticatedRequest(path).GET().build());
        List<java.time.LocalTime> times = new ArrayList<>();
        if (!response.body().isBlank()) {
            for (String line : response.body().split("\\R")) {
                times.add(java.time.LocalTime.parse(line));
            }
        }
        return times;
    }

    private static void sendForm(String method, String path, Appointment appointment) throws IOException {
        String form = "id=" + encode(appointment.getAppointmentID().toString())
                + "&customerName=" + encode(appointment.getCustomerName())
                + "&startsAt=" + encode(appointment.getApptStart().toString())
                + "&durationMinutes=" + appointment.getApptDuration();
        HttpRequest request = authenticatedRequest(path)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .method(method, HttpRequest.BodyPublishers.ofString(form))
                .build();
        send(request);
    }

    private static HttpResponse<String> send(HttpRequest request) throws IOException {
        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                if (refreshToken != null) {
                    refreshSession();
                    response = HTTP.send(withCurrentToken(request), HttpResponse.BodyHandlers.ofString());
                }
                if (response.statusCode() == 401) {
                    logout();
                    throw new IOException("Your session expired. Sign in again.");
                }
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Appointment API error (" + response.statusCode() + "): " + response.body());
            }
            return response;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Request to the appointment API was interrupted.", exception);
        }
    }

    private static boolean authenticate(String path, String email, String password,
                                        boolean allowConfirmation) throws IOException {
        String normalizedEmail = email.trim().toLowerCase();
        String form = "email=" + encode(normalizedEmail) + "&password=" + encode(password);
        HttpRequest request = HttpRequest.newBuilder(apiUri(path))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = sendAnonymous(request);

        if (allowConfirmation && response.statusCode() == 202
                && response.body().trim().equals("CONFIRM_EMAIL")) {
            return false;
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(response.body().isBlank() ? "Authentication failed." : response.body());
        }
        applySession(response.body());
        currentEmail = normalizedEmail;
        return true;
    }

    private static void refreshSession() throws IOException {
        String form = "refreshToken=" + encode(refreshToken);
        HttpRequest request = HttpRequest.newBuilder(apiUri("/api/auth/refresh"))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = sendAnonymous(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            logout();
            throw new IOException("Your session expired. Sign in again.");
        }
        applySession(response.body());
    }

    private static void applySession(String responseBody) throws IOException {
        String[] session = responseBody.split("\\R", -1);
        if (session.length != 2 || session[0].isBlank() || session[1].isBlank()) {
            throw new IOException("Authentication succeeded without a complete session.");
        }
        accessToken = session[0].trim();
        refreshToken = session[1].trim();
    }

    private static HttpResponse<String> sendAnonymous(HttpRequest request) throws IOException {
        try {
            return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Request to the authentication service was interrupted.", exception);
        }
    }

    private static HttpRequest.Builder authenticatedRequest(String path) throws IOException {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IOException("Sign in is required.");
        }
        return HttpRequest.newBuilder(apiUri(path))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + accessToken);
    }

    private static HttpRequest withCurrentToken(HttpRequest original) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(original.uri())
                .timeout(Duration.ofSeconds(45));
        original.headers().map().forEach((name, values) -> {
            if (!name.equalsIgnoreCase("Authorization")) {
                values.forEach(value -> builder.header(name, value));
            }
        });
        builder.header("Authorization", "Bearer " + accessToken);
        return builder.method(original.method(),
                original.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody())).build();
    }

    private static URI apiUri(String path) {
        String baseUrl = System.getenv().getOrDefault("APPOINTMENT_API_URL", DEFAULT_API_URL);
        return URI.create(baseUrl.replaceAll("/+$", "") + path);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
