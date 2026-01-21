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
    Description: The main function creates the appointment master opject that keeps track of all appointments
    created. It also runs the main loop that shows the menu that user will be prompted with until the quit the program.
    Along with this it calls to the function for the option the user selects and validates the input for any selection.
    Input: No parameters, but takes the option from user.
    Output: Outputs menu text for selection.
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
    Function: cancelAppointment
    Description: This function is where case 4 is lead from the menu. It is used for cancelling a users previously
    scheduled appointment. It does this by searching for an appointment either by a name or by and appointment ID
    number that was given to the user upon scheduling. It intakes either form of search and asks for details and then
    verifies that the details are the correct format and exist. If it makes it this far, it will ask a user to select
    which appointment they want to cancel if there are multiple, if there is a singular one then it will assume that is
    the intended appointment, if there is none then it will tell you there are no appointments scheduled for the user.
    It will the remove the selected appointment from the 
     */
    private static void cancelAppointment() {
        int choice;
        boolean cont = true;
        while(cont) {
            choice = readInt("\nSearch your appointment(s) by: \n" +
                    "1. Name\n" +
                    "2. Appointment ID\n" +
                    "Enter Number Choice: ");
            switch (choice) {
                case 1:
                    System.out.println("\nEnter your first and last name: ");
                    String name = input.nextLine();
                    List<Appointment> nameList = apptMaster.getApptsByName(name);
                    UUID IDinput1;
                    System.out.println();
                    for (Appointment e : nameList) {
                        System.out.println(e);
                    }

                    Appointment selectedApptByName = null;
                    if (nameList.isEmpty()) {
                        System.out.println("No appointments scheduled for " + name + ".\n");
                        break;
                    } else if (nameList.size() == 1) {
                        selectedApptByName = nameList.get(0);
                    } else {
                        System.out.println("Enter the appointment ID of desired appointment to cancel: ");
                        String apptIDInput = input.nextLine();
                        try {
                            IDinput1 = UUID.fromString(apptIDInput);
                        }catch (IllegalArgumentException e) {
                            System.out.println("Invalid Appointment ID. Try Again.\n");
                            cont = false;
                            break;
                        }
                        selectedApptByName = apptMaster.getApptById(IDinput1);

                        if (selectedApptByName == null) {
                            System.out.println("\nNo appointment scheduled from Appointment ID: " + IDinput1 + ". Try Again.\n");
                            cont = false;
                            break;
                        }
                    }
                    apptMaster.appointmentRemove(selectedApptByName);

                    System.out.println("\nAppointment for " + name + " cancelled on " + selectedApptByName.getApptStart().toLocalDate() +
                            " at " + selectedApptByName.getApptStart().toLocalTime() + ".\n");
                    cont = false;
                    break;
                case 2:
                    System.out.print("\nEnter your appointment ID: ");
                    String apptID = input.nextLine();
                    UUID IDinput2;
                    try {
                        IDinput2 = UUID.fromString(apptID);
                    } catch (IllegalArgumentException e) {
                        System.out.println("Invalid Appointment ID. Try Again.\n");
                        cont = false;
                        break;
                    }
                    Appointment selectedApptByID = apptMaster.getApptById(IDinput2);

                    if (selectedApptByID == null) {
                        System.out.println("\nNo appointment scheduled from Appointment ID: " + IDinput2 + ". Try Again.\n");
                        cont = false;
                        break;
                    }
                    System.out.println();
                    System.out.println(selectedApptByID);

                    apptMaster.appointmentRemove(selectedApptByID);

                    System.out.println("Appointment for " + selectedApptByID.getCustomerName() + " cancelled on " + selectedApptByID.getApptStart().toLocalDate() +
                            " at " + selectedApptByID.getApptStart().toLocalTime().format(formatter2) + ".\n");
                    cont = false;
                    break;
                default:
                    System.out.println();

                    System.out.print("Invalid Input. Try Again: ");
            }
        }
    }
    /*
    Function: rescheduleAppointment
    Description: Handles menu option 3. Allows a user to reschedule an existing appointment by
    searching either by name or appointment ID. If multiple appointments exist under a name,
    the user selects which appointment to reschedule. The original appointment is removed and
    a new appointment is created with the updated date and time.
    Input: User input for search method, appointment identification, duration, and date/time.
    Output: Displays updated appointment details and new appointment ID.
    */
    private static void rescheduleAppointment() {
        int choice;
        boolean cont = true;
        while(cont) {
            choice = readInt("\nSearch your appointment(s) by: \n" +
                    "1. Name\n" +
                    "2. Appointment ID\n" +
                    "Enter Number Choice: ");
            switch (choice) {
                case 1:
                    System.out.println("\nEnter your first and last name: ");
                    String name = input.nextLine();
                    List<Appointment> nameList = apptMaster.getApptsByName(name);
                    UUID IDinput1;
                    System.out.println();
                    for (Appointment e : nameList) {
                        System.out.println(e);
                    }

                    Appointment selectedApptByName = null;
                    if (nameList.isEmpty()) {
                        System.out.println("No appointments scheduled for " + name + ".");
                        break;
                    } else if (nameList.size() == 1) {
                        selectedApptByName = nameList.get(0);
                    } else {
                        System.out.println("Enter the appointment ID of desired appointment to reschedule: ");
                        String apptIDInput = input.nextLine();
                        try {
                            IDinput1 = UUID.fromString(apptIDInput);
                        }catch (IllegalArgumentException e) {
                            System.out.println("Invalid Appointment ID. Going back to menu.\n");
                            cont = false;
                            break;
                        }
                        selectedApptByName = apptMaster.getApptById(IDinput1);

                        if (selectedApptByName == null) {
                            System.out.println("\nNo appointment scheduled from Appointment ID: " + IDinput1 + ". Back to menu.\n");
                            cont = false;
                            break;
                        }
                    }

                    int apptDuration1 = scheduleApptDuration();
                    LocalDateTime dateTime1 = scheduleApptDate(apptDuration1);
                    if (dateTime1 == null) {
                        System.out.println("Reschedule cancelled. Keeping original appointment.\n");
                        cont = false;
                        break;
                    }
                    apptMaster.appointmentRemove(selectedApptByName);


                    Appointment newAppt1 = new Appointment(apptDuration1, selectedApptByName.getCustomerName(), dateTime1);
                    apptMaster.addAppt(newAppt1);
                    System.out.println("\nAppointment for " + name + " rescheduled from " + selectedApptByName.getApptStart().toLocalDate() +
                            " at " + selectedApptByName.getApptStart().toLocalTime().format(formatter2) + " to " + newAppt1.getApptStart().toLocalDate() + " at " +
                            newAppt1.getApptStart().toLocalTime().format(formatter2) + ".");
                    System.out.println("New Appointment ID: " + newAppt1.getAppointmentID() + "\n");
                    cont = false;
                    break;
                case 2:
                    System.out.print("\nEnter your appointment ID: ");
                    String apptID = input.nextLine();
                    UUID IDinput2;
                    try {
                        IDinput2 = UUID.fromString(apptID);
                    } catch (IllegalArgumentException e) {
                        System.out.println("Invalid Appointment ID. Going back to menu.\n");
                        cont = false;
                        break;
                    }
                    Appointment selectedApptByID = apptMaster.getApptById(IDinput2);

                    if (selectedApptByID == null) {
                        System.out.println("\nNo appointment scheduled from Appointment ID: " + IDinput2 + ". Back to menu.\n");
                        cont = false;
                        break;
                    }
                    System.out.println();
                    System.out.println(selectedApptByID);


                    int apptDuration2 = scheduleApptDuration();
                    LocalDateTime dateTime2 = scheduleApptDate(apptDuration2);
                    if (dateTime2 == null) {
                        System.out.println("Reschedule cancelled. Keeping original appointment.\n");
                        cont = false;
                        break;
                    }
                    apptMaster.appointmentRemove(selectedApptByID);

                    Appointment newAppt2 = new Appointment(apptDuration2, selectedApptByID.getCustomerName(), dateTime2);
                    apptMaster.addAppt(newAppt2);
                    System.out.println("\nAppointment for " + selectedApptByID.getCustomerName() + " rescheduled from " + selectedApptByID.getApptStart().toLocalDate() +
                            " at " + selectedApptByID.getApptStart().toLocalTime() + " to " + newAppt2.getApptStart().toLocalDate() + " at " +
                            newAppt2.getApptStart().toLocalTime() + ".");
                    System.out.println("New Appointment ID: " + newAppt2.getAppointmentID() + "\n");
                    cont = false;
                    break;
                default:
                    System.out.println();

                    System.out.print("Invalid Input. Try Again: ");

            }
        }
    }
    /*
    Function: appointmentDetails
    Description: Displays all appointments associated with a given customer name.
    If no appointments exist for the entered name, the user is notified.
    Input: Customer name entered by user.
    Output: Prints appointment details to the console.
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
    Description: Handles menu option 1. Prompts the user for their name, desired appointment
    duration, and date/time. Creates and stores a new appointment if valid input is provided.
    Input: Customer name, appointment duration, appointment date and time.
    Output: Confirms appointment creation and displays appointment ID.
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
    Description: Utility function that repeatedly prompts the user until a valid integer
    is entered. Used for menu selections and numeric input validation.
    Input: Prompt string displayed to the user.
    Output: Returns a validated integer.
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
    Description: Prompts the user for an appointment date and determines all available start
    times for the selected duration. Allows the user to choose from available time slots or
    cancel the operation.
    Input: Appointment duration in minutes.
    Output: Returns a LocalDateTime representing the selected appointment time,
    or null if cancelled.
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
    Description: Prompts the user to enter a valid appointment duration (15, 30, 45, or 60 minutes).
    Continues prompting until a valid duration is entered.
    Input: User-entered appointment duration.
    Output: Returns validated appointment duration in minutes.
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


