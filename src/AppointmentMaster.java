import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class AppointmentMaster {
    private final Map<LocalDate, List<Appointment>> byDate;
    private final Map<UUID, Appointment> byID;
    private final Map<String, List<Appointment>> byName;

    public AppointmentMaster(){
        byDate = new HashMap<>();
        byID = new HashMap<>();
        byName = new HashMap<>();
    }
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
    public List<Appointment> getApptsByName(String cusName){
        return byName.getOrDefault(cusName, Collections.emptyList());
    }
    public Appointment getApptById(UUID apptID){
        return byID.getOrDefault(apptID, null);
    }
    public List<Appointment> getApptsByDate(LocalDateTime dateTime){
        LocalDate date = dateTime.toLocalDate();
        return byDate.getOrDefault(date, Collections.emptyList());
    }
    public List<Appointment> getApptsByDate(LocalDate date){
        return byDate.getOrDefault(date, Collections.emptyList());
    }
    public void appointmentRemove(Appointment appt){
        LocalDate date = appt.getApptStart().toLocalDate();
        List<Appointment> listDate = byDate.get(date);
        listDate.remove(appt);

        List<Appointment> listName = byName.get(appt.getCustomerName());
        listName.remove(appt);

        byID.remove(appt.getAppointmentID());
    }
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
