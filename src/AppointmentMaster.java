import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

    /*
    Class: AppointmentMaster
    Description: Manages and stores all appointments in the system. Appointments are indexed
    by date, appointment ID, and customer name to allow efficient searching, scheduling,
    rescheduling, and cancellation.
    */
public class AppointmentMaster {
    private final Map<LocalDate, List<Appointment>> byDate;
    private final Map<UUID, Appointment> byID;
    private final Map<String, List<Appointment>> byName;

        /*
        Constructor: AppointmentMaster
        Description: Initializes the data structures used to store and organize appointments
        by date, appointment ID, and customer name.
        Input: None
        Output: Creates an empty AppointmentMaster object.
        */
    public AppointmentMaster(){
        byDate = new HashMap<>();
        byID = new HashMap<>();
        byName = new HashMap<>();
    }

        /*
    Function: addAppt
    Description: Adds a new appointment to all tracking maps (by date, by ID, and by name).
    Ensures the appointment can be efficiently retrieved using multiple search methods.
    Input:
        appt - Appointment object to be added
    Output: Stores the appointment in the system.
    */
    public void addAppt(Appointment appt){
        LocalDate date = appt.getApptStart().toLocalDate();
        List<Appointment> list = byDate.get(date);
        if(list == null){
            list = new ArrayList<>();
            byDate.put(date, list);
        }
        list.add(appt);

        byID.put(appt.getAppointmentID(), appt);

        List<Appointment> listName = byName.get(appt.getCustomerName());
        if(listName == null){
            listName = new ArrayList<>();
            byName.put(appt.getCustomerName(), listName);
        }
        listName.add(appt);

    }

        /*
    Function: getApptsByName
    Description: Retrieves all appointments associated with a given customer name.
    Input:
        cusName - customer's full name
    Output: List of appointments for the specified customer, or an empty list if none exist.
    */
    public List<Appointment> getApptsByName(String cusName){
        return byName.getOrDefault(cusName, Collections.emptyList());
    }

        /*
    Function: getApptById
    Description: Retrieves a single appointment using its unique appointment ID.
    Input:
        apptID - appointment UUID
    Output: Appointment object if found, or null if no match exists.
    */
    public Appointment getApptById(UUID apptID){
        return byID.getOrDefault(apptID, null);
    }

        /*
     Function: getApptsByDate (LocalDate)
     Description: Retrieves all appointments scheduled on a specific date.
     Input:
         date - appointment date
     Output: List of appointments scheduled on the specified date.
     */
    public List<Appointment> getApptsByDate(LocalDate date){
        return byDate.getOrDefault(date, Collections.emptyList());
    }

        /*
    Function: appointmentRemove
    Description: Removes an appointment from all tracking maps (by date, by name, and by ID).
    Input:
        appt - Appointment object to be removed
    Output: Deletes the appointment from the system.
    */
    public void appointmentRemove(Appointment appt){
        LocalDate date = appt.getApptStart().toLocalDate();
        List<Appointment> listDate = byDate.get(date);
        listDate.remove(appt);

        List<Appointment> listName = byName.get(appt.getCustomerName());
        listName.remove(appt);

        byID.remove(appt.getAppointmentID());
    }

        /*
    Function: getAvaliableStarts
    Description: Determines all available appointment start times for a given date and
    appointment duration. Checks for conflicts with existing appointments and only
    returns valid start times within business hours.
    Input:
        date - desired appointment date
        duration - desired appointment duration in minutes
    Output: List of available LocalTime start times.
    */
    public List<LocalTime> getAvaliableStarts(LocalDate date, int duration) {
        List<LocalTime> timeList = new ArrayList<>();
        List<Appointment> apptList = getApptsByDate(date);

        LocalTime open = LocalTime.of(9, 0);
        LocalTime close = LocalTime.of(17, 0);

        LocalTime current = open;

        while (!current.plusMinutes(duration).isAfter(close)) {
            LocalDateTime candidateStart = LocalDateTime.of(date, current);
            LocalDateTime candidateEnd = candidateStart.plusMinutes(duration);

            boolean conflict = false;

            for (Appointment appt : apptList) {
                LocalDateTime existingStart = appt.getApptStart();
                LocalDateTime existingEnd =
                        existingStart.plusMinutes(appt.getApptDuration());

                if (candidateStart.isBefore(existingEnd)
                        && existingStart.isBefore(candidateEnd)) {
                    conflict = true;
                    break;
                }
            }
            if (!conflict){
                timeList.add(current);
            }
            current = current.plusMinutes(15);
        }
        return timeList;
    }
}
