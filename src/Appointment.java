
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/*
Class: Appointment
Description: Represents a single appointment within the appointment management system.
Stores customer information, appointment start time, duration, and unique identifiers.
Provides methods for rescheduling, accessing, and displaying appointment details.
*/
public class Appointment {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/uuuu");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
    private final int apptDurationMinutes;
    private final String customerName;
    private final LocalDateTime apptStart;
    private final UUID appointmentID;

        /*
    Constructor: Appointment
    Description: Creates a new Appointment object with a specified duration, customer name,
    and start date/time. Generates unique IDs for both the customer and the appointment.
    Input:
        apptD - appointment duration in minutes
        cusName - customer's full name
        apptS - appointment start date and time
    Output: Initializes a new Appointment object.
    */
    public Appointment(int apptD, String cusName, LocalDateTime apptS){
        this(apptD, cusName, apptS, UUID.randomUUID());
    }

    private Appointment(int apptD, String cusName, LocalDateTime apptS, UUID appointmentID){
        apptDurationMinutes = apptD;
        customerName = cusName;
        apptStart = apptS;
        this.appointmentID = appointmentID;
    }

    /** Recreates an appointment that was previously saved to local storage. */
    public static Appointment fromSavedData(int duration, String customerName,
                                            LocalDateTime start, UUID appointmentID) {
        return new Appointment(duration, customerName, start, appointmentID);
    }
    /*
    Function: getApptStart
    Description: Returns the start date and time of the appointment.
    Input: None
    Output: LocalDateTime representing the appointment start.
    */
    public LocalDateTime getApptStart() {
        return apptStart;
    }
    /*
    Function: getApptDuration
    Description: Returns the duration of the appointment in minutes.
    Input: None
    Output: Appointment duration in minutes.
    */
    public int getApptDuration() {
        return apptDurationMinutes;
    }
    /*
    Function: getCustomerName
    Description: Returns the customer's name associated with the appointment.
    Input: None
    Output: Customer name as a String.
    */
    public String getCustomerName() {
        return customerName;
    }
    /*
    Function: getAppointmentID
    Description: Returns the unique appointment ID.
    Input: None
    Output: UUID representing the appointment ID.
    */
    public UUID getAppointmentID() {
        return appointmentID;
    }

    /** Returns a replacement appointment while retaining its reference ID. */
    public Appointment rescheduledTo(String updatedCustomerName, int duration, LocalDateTime start) {
        return new Appointment(duration, updatedCustomerName, start, appointmentID);
    }
    /*
    Function: toString
    Description: Overrides the default toString method to return a formatted string
    containing all relevant appointment details.
    Input: None
    Output: Formatted appointment information as a String.
    */
    @Override
    public String toString() {
        return "Customer Name: " + customerName + "\n" +
                "Appointment Date: " + apptStart.toLocalDate().format(DATE_FORMATTER) + "\n" +
                "Appointment Start Time: " + apptStart.toLocalTime().format(TIME_FORMATTER) + "\n" +
                "Appointment Duration: " + apptDurationMinutes + " minutes" + "\n" +
                "Appointment ID: " + appointmentID + "\n";

    }

}

