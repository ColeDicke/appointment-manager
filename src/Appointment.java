
import java.time.LocalDateTime;
import java.util.UUID;

public class Appointment {
    private int apptDurationMinutes;
    private String customerName;
    private final UUID customerId;
    private LocalDateTime apptStart;
    private final UUID appointmentID;

    public Appointment(int apptD, String cusName, LocalDateTime apptS){
        apptDurationMinutes = apptD;
        customerName = cusName;
        customerId = UUID.randomUUID();
        apptStart = apptS;
        appointmentID = UUID.randomUUID();
    }

    public void AppointmentReschedule(int apptD, LocalDateTime apptS){
        setApptDuration(apptD);
        setApptStart(apptS);
    }

    public LocalDateTime getApptStart() {
        return apptStart;
    }
    public LocalDateTime getApptEnd(){
        LocalDateTime start = apptStart;
        LocalDateTime end = start.plusMinutes(apptDurationMinutes);
        return end;
    }

    public void setApptStart(LocalDateTime apptStart) {
        this.apptStart = apptStart;
    }

    public int getApptDuration() {
        return apptDurationMinutes;
    }

    public void setApptDuration(int apptDuration) {
        this.apptDurationMinutes = apptDuration;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getAppointmentID() {
        return appointmentID;
    }
    @Override
    public String toString() {
        return "Customer Name: " + customerName + "\n" +
                "Appointment Date: " + apptStart.toLocalDate() + "\n" +
                "Appointment Start Time: " + apptStart.toLocalTime() + "\n" +
                "Appointment Duration: " + apptDurationMinutes + " minutes" + "\n" +
                "Appointment ID: " + appointmentID + "\n";

    }

}

