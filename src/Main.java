import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class Main {
    static AppointmentMaster apptMaster;
    static Scanner input = new Scanner(System.in);
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    static DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("h:mm a");


    /*
    Function: main
    Description: Initializes the appointment management system and displays the main menu.
    Continuously prompts the user for a menu option and calls the corresponding function
    until the user chooses to exit the program.
    Input: User menu selection.
    Output: Displays menu options, appointment information, and program messages.
    */
    public static void main(String[] args) {
        apptMaster = new AppointmentMaster();
        System.out.println("\nWelcome to the Appointment Manager\n");
        boolean cont = true;
        while (cont) {
            int option = readInt("1. Schedule Appointment\n" +
                    "2. Appointment Details\n" +
                    "3. Reschedule Appointment\n" +
                    "4. Cancel Appointment\n" +
                    "5. Quit\n" +
                    "Enter an Option Number: ");
            switch (option) {
                case 1:
                    scheduleAppointment();
                    break;
                case 2:
                    appointmentDetails();
                    break;
                case 3:
                    rescheduleAppointment();
                    break;
                case 4:
                    cancelAppointment();
                    break;
                case 5:
                    System.out.println("\nThank you! See you soon!");
                    cont = false;
                    break;
                default:
                    System.out.println("\nInvalid input. Try again.\n");
            }
        }
    }

    /*
Function: selectAppointment
Description: Searches for and returns an appointment by customer name or appointment ID.
If multiple appointments exist for a customer, the user selects the desired appointment.
Returns null if no valid appointment is found.
Input: Search method, customer name, and/or appointment ID.
Output: Selected Appointment object or null.
*/
    private static Appointment selectAppointment() {
        int choice = readInt("\nSearch your appointment(s) by: \n" +
                "1. Name\n" +
                "2. Appointment ID\n" +
                "Enter Number Choice: ");

        switch (choice) {
            case 1:
                System.out.print("\nEnter your first and last name: ");
                String name = input.nextLine();

                List<Appointment> nameList = apptMaster.getApptsByName(name);

                if (nameList.isEmpty()) {
                    System.out.println("No appointments scheduled for " + name + ".\n");
                    return null;
                }

                System.out.println();
                for (Appointment appt : nameList) {
                    System.out.println(appt);
                }

                if (nameList.size() == 1) {
                    return nameList.get(0);
                }

                System.out.print("\nEnter the appointment ID: ");
                String apptIDInput = input.nextLine();

                try {
                    UUID id = UUID.fromString(apptIDInput);
                    Appointment appt = apptMaster.getApptById(id);

                    if (appt == null) {
                        System.out.println("No appointment found with ID: " + id);
                    }

                    return appt;

                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid Appointment ID.\n");
                    return null;
                }

            case 2:
                System.out.print("\nEnter your appointment ID: ");
                String apptID = input.nextLine();

                try {
                    UUID id = UUID.fromString(apptID);
                    Appointment appt = apptMaster.getApptById(id);

                    if (appt == null) {
                        System.out.println("No appointment found with ID: " + id);
                    }

                    return appt;

                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid Appointment ID.\n");
                    return null;
                }

            default:
                System.out.println("Invalid Input.\n");
                return null;
        }
    }

    /*
Function: cancelAppointment
Description: Allows a user to cancel an existing appointment. After an
appointment is selected, it is removed from the appointment system and
a confirmation message is displayed.
Input: Appointment selected by the user.
Output: Displays appointment cancellation details.
*/
    private static void cancelAppointment() {

        Appointment appt = selectAppointment();

        if (appt == null) {
            return;
        }

        apptMaster.appointmentRemove(appt);

        System.out.println(
                "\nAppointment for " +
                        appt.getCustomerName() +
                        " cancelled on " +
                        appt.getApptStart().toLocalDate() +
                        " at " +
                        appt.getApptStart().toLocalTime().format(formatter2) +
                        ".\n");
    }
    /*
    Function: rescheduleAppointment
    Description: Allows a user to reschedule an existing appointment by selecting
    a new duration and appointment time. The original appointment is replaced
    with an updated appointment.
    Input: Appointment selected by the user, appointment duration, and new date/time.
    Output: Displays updated appointment details and the new appointment ID.
    */
    private static void rescheduleAppointment() {

        Appointment oldAppt = selectAppointment();

        if (oldAppt == null) {
            return;
        }

        int apptDuration = scheduleApptDuration();

        LocalDateTime newDateTime = scheduleApptDate(apptDuration);

        if (newDateTime == null) {
            System.out.println("Reschedule cancelled. Keeping original appointment.\n");
            return;
        }

        apptMaster.appointmentRemove(oldAppt);

        Appointment newAppt = new Appointment(
                apptDuration,
                oldAppt.getCustomerName(),
                newDateTime
        );

        apptMaster.addAppt(newAppt);

        System.out.println(
                "\nAppointment for " +
                        oldAppt.getCustomerName() +
                        " rescheduled from " +
                        oldAppt.getApptStart().toLocalDate() +
                        " at " +
                        oldAppt.getApptStart().toLocalTime().format(formatter2) +
                        " to " +
                        newAppt.getApptStart().toLocalDate() +
                        " at " +
                        newAppt.getApptStart().toLocalTime().format(formatter2) +
                        "."
        );

        System.out.println(
                "New Appointment ID: " +
                        newAppt.getAppointmentID() +
                        "\n"
        );
    }
    /*
Function: appointmentDetails
Description: Retrieves and displays all appointments associated with a
specified customer name.
Input: Customer name entered by the user.
Output: Displays appointment details or a notification if no appointments exist.
*/
    private static void appointmentDetails() {
        System.out.println("Enter your name (John Doe): ");
        String name = input.nextLine().trim();
        System.out.println();
        List<Appointment> nameList = apptMaster.getApptsByName(name);
        if(nameList.isEmpty()){
            System.out.println("No appointments scheduled for " + name + ".\n");

        }
        for(Appointment e : nameList){
            System.out.println(e);
        }
    }
    /*
 Function: scheduleAppointment
 Description: Allows a user to schedule a new appointment by selecting a
 duration and available appointment time.
 Input: Customer name, appointment duration, and appointment date/time.
 Output: Displays appointment confirmation details and the appointment ID.
 */
    private static void scheduleAppointment() {
        System.out.print("Enter your first and last name: ");
        String name = input.nextLine();

        int apptDuration = scheduleApptDuration();
        LocalDateTime dateTime = scheduleApptDate(apptDuration);
        if(dateTime == null){
            System.out.println("Option cancelled. Returning to menu.\n");
            return;
        }

        Appointment newAppt = new Appointment(apptDuration, name, dateTime);
        apptMaster.addAppt(newAppt);
        System.out.println("\nAppointment Created!\n" +
                            "Appointment ID for future reference: " + newAppt.getAppointmentID() + "\n");
        }
    /*
  Function: readInt
  Description: Reads and validates integer input from the user.
  Input: Prompt message displayed to the user.
  Output: Returns a validated integer value.
  */
    public static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String in = input.nextLine();
            try {
                return Integer.parseInt(in.trim());
            } catch (NumberFormatException e) {
                System.out.println("\nInvalid Input. Try Again: \n");
            }
        }
    }
    /*
Function: scheduleApptDate
Description: Retrieves available appointment times for a selected date and
allows the user to choose an appointment time.
Input: Appointment duration in minutes.
Output: Returns the selected LocalDateTime or null if cancelled.
*/
    private static LocalDateTime scheduleApptDate(int apptDuration){
        List<LocalTime> availableTimes = new ArrayList<>();
        LocalDate date = null;

        while (availableTimes.isEmpty()) {
            while (date == null) {
                System.out.print("Enter date (MM/DD/YYYY) or 0 to cancel: ");
                String dateInput = input.nextLine();
                if (dateInput.equals("0")) {
                    return null; // go back to menu
                }
                try {
                    date = LocalDate.parse(dateInput, formatter);

                    if (date.isBefore(LocalDate.now())) {
                        System.out.println("Date cannot be in the past.\n");
                        date = null;
                    }
                } catch (DateTimeParseException e) {
                    System.out.println("Invalid date format. Use MM/DD/YYYY (example: 03/15/2026).\n");
                }
            }

            availableTimes = apptMaster.getAvaliableStarts(date, apptDuration);
            if (availableTimes.isEmpty()) {
                System.out.println("No available times on selected date. Try another date or appointment duration.\n");
                date = null;
            }

        }

        for (int i = 0; i < availableTimes.size(); i++) {
            System.out.println((i + 1) + ") " +
                    availableTimes.get(i).format(formatter2));
        }
        int choice = readInt("Enter the option number of desired time (ex. 12): ");
        while (choice < 1 || choice > availableTimes.size()) {
            choice = readInt("Choice not in available range. Try Again: ");
            System.out.println();
        }
        LocalTime selectedTime = availableTimes.get(choice - 1);
        LocalDateTime dateTime = LocalDateTime.of(date, selectedTime);
        return dateTime;
    }
    /*
 Function: scheduleApptDuration
 Description: Reads and validates the user's appointment duration selection.
 Input: User-entered appointment duration.
 Output: Returns a validated appointment duration in minutes.
 */
    private static int scheduleApptDuration(){
        int apptDuration = 60;

        apptDuration = readInt("Enter your desired appointment duration (15,30,45,60) minutes: ");
        while (apptDuration != 15 && apptDuration != 30 && apptDuration != 45 && apptDuration != 60) {
            apptDuration = readInt("Invalid Input. Try Again: \n\n" +

                                    "Enter your desired appointment duration (15,30,45,60) minutes: ");
        }
        return apptDuration;
    }
}


