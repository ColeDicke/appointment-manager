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


