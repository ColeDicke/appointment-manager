import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Maintains the signed-in user's appointments currently loaded in the desktop UI. */
public final class AppointmentMaster {
    private final Map<UUID, Appointment> appointmentsById = new HashMap<>();

    public void addAppt(Appointment appointment) {
        appointmentsById.put(appointment.getAppointmentID(), appointment);
    }

    public List<Appointment> getAllAppointments() {
        return Collections.unmodifiableList(new ArrayList<>(appointmentsById.values()));
    }

    public void appointmentRemove(Appointment appointment) {
        appointmentsById.remove(appointment.getAppointmentID());
    }
}
