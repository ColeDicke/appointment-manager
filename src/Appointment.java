
import java.time.LocalDateTime;
import java.util.UUID;

/*
Class: Appointment
Description: Represents a single appointment within the appointment management system.
Stores customer information, appointment start time, duration, and unique identifiers.
Provides methods for rescheduling, accessing, and displaying appointment details.
*/
public class Appointment {
    private int apptDurationMinutes;
    private String customerName;
    private final UUID customerId;
    private LocalDateTime apptStart;
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
        apptDurationMinutes = apptD;
        customerName = cusName;
        customerId = UUID.randomUUID();
        apptStart = apptS;
        appointmentID = UUID.randomUUID();
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
                "Appointment Date: " + apptStart.toLocalDate() + "\n" +
                "Appointment Start Time: " + apptStart.toLocalTime() + "\n" +
                "Appointment Duration: " + apptDurationMinutes + " minutes" + "\n" +
                "Appointment ID: " + appointmentID + "\n";

    }

}

