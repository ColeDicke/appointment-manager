import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Stores appointments in a small local text file beside the application. */
public final class AppointmentStorage {
    private static final Path FILE_PATH = Paths.get("appointments.txt");

    private AppointmentStorage() {
    }

    public static List<Appointment> load() throws IOException {
        List<Appointment> appointments = new ArrayList<>();
        if (!Files.exists(FILE_PATH)) {
            return appointments;
        }

        for (String line : Files.readAllLines(FILE_PATH, StandardCharsets.UTF_8)) {
            if (line.trim().isEmpty()) {
                continue;
            }

            String[] values = line.split("\\|", -1);
            if (values.length != 4) {
                System.out.println("Skipping an invalid saved appointment.");
                continue;
            }

            try {
                String name = new String(Base64.getUrlDecoder().decode(values[0]), StandardCharsets.UTF_8);
                LocalDateTime start = LocalDateTime.parse(values[1]);
                int duration = Integer.parseInt(values[2]);
                UUID id = UUID.fromString(values[3]);
                appointments.add(Appointment.fromSavedData(duration, name, start, id));
            } catch (IllegalArgumentException | DateTimeException exception) {
                System.out.println("Skipping an invalid saved appointment.");
            }
        }
        return appointments;
    }

    public static void save(List<Appointment> appointments) throws IOException {
        List<String> lines = new ArrayList<>();
        List<Appointment> sortedAppointments = new ArrayList<>(appointments);
        sortedAppointments.sort(Comparator.comparing(Appointment::getApptStart));

        for (Appointment appointment : sortedAppointments) {
            String encodedName = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    appointment.getCustomerName().getBytes(StandardCharsets.UTF_8));
            lines.add(encodedName + "|" + appointment.getApptStart() + "|"
                    + appointment.getApptDuration() + "|" + appointment.getAppointmentID());
        }

        Files.write(FILE_PATH, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }
}
