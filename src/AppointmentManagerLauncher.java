import javafx.application.Application;

/** Starts the Appointment Manager desktop application. */
public final class AppointmentManagerLauncher {
    private AppointmentManagerLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(AppointmentManagerApp.class, args);
    }
}
