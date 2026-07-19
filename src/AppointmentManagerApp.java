import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** JavaFX user interface for the appointment manager. */
public class AppointmentManagerApp extends Application {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    private final AppointmentMaster appointmentMaster = new AppointmentMaster();
    private final TextField nameField = new TextField();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final ComboBox<Integer> durationBox = new ComboBox<>();
    private final ListView<LocalTime> availableTimes = new ListView<>();
    private final ListView<Appointment> appointmentList = new ListView<>();
    private final TextField searchField = new TextField();
    private final Label statusLabel = new Label();
    private final Button saveButton = new Button("Schedule Appointment");

    private Appointment appointmentBeingRescheduled;
    private TabPane tabPane;
    private Tab scheduleTab;

    @Override
    public void start(Stage stage) {
        loadAppointments();

        tabPane = new TabPane();
        scheduleTab = new Tab("Schedule", createSchedulePane());
        scheduleTab.setClosable(false);
        Tab appointmentsTab = new Tab("Appointments", createAppointmentsPane());
        appointmentsTab.setClosable(false);
        tabPane.getTabs().addAll(scheduleTab, appointmentsTab);

        refreshAppointments();

        Scene scene = new Scene(tabPane, 760, 540);
        stage.setTitle("Appointment Manager");
        stage.setScene(scene);
        stage.show();
    }

    private BorderPane createSchedulePane() {
        durationBox.getItems().addAll(15, 30, 45, 60);
        durationBox.setValue(30);

        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(10);
        fields.setPadding(new Insets(18));
        fields.addRow(0, new Label("Customer name:"), nameField);
        fields.addRow(1, new Label("Date:"), datePicker);
        fields.addRow(2, new Label("Duration (minutes):"), durationBox);

        Button findTimesButton = new Button("Show Available Times");
        findTimesButton.setOnAction(event -> showAvailableTimes());
        fields.add(findTimesButton, 1, 3);

        availableTimes.setPrefHeight(230);
        availableTimes.setCellFactory(list -> new javafx.scene.control.ListCell<LocalTime>() {
            @Override
            protected void updateItem(LocalTime time, boolean empty) {
                super.updateItem(time, empty);
                setText(empty || time == null ? null : time.format(TIME_FORMATTER));
            }
        });

        saveButton.setOnAction(event -> saveAppointment());
        Button cancelRescheduleButton = new Button("Clear");
        cancelRescheduleButton.setOnAction(event -> clearScheduleForm());
        HBox actions = new HBox(10, saveButton, cancelRescheduleButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(10, fields, new Label("Available times:"), availableTimes,
                actions, statusLabel);
        content.setPadding(new Insets(0, 18, 18, 18));

        BorderPane pane = new BorderPane(content);
        return pane;
    }

    private BorderPane createAppointmentsPane() {
        searchField.setPromptText("Search by customer name (leave blank to show all)");
        Button searchButton = new Button("Search");
        searchButton.setOnAction(event -> refreshAppointments());
        HBox searchBar = new HBox(10, searchField, searchButton);
        searchBar.setPadding(new Insets(18));

        Button rescheduleButton = new Button("Reschedule Selected");
        rescheduleButton.setOnAction(event -> beginReschedule());
        Button cancelButton = new Button("Cancel Selected");
        cancelButton.setOnAction(event -> cancelSelectedAppointment());
        HBox actions = new HBox(10, rescheduleButton, cancelButton);
        actions.setPadding(new Insets(12, 18, 18, 18));

        BorderPane pane = new BorderPane();
        pane.setTop(searchBar);
        pane.setCenter(appointmentList);
        pane.setBottom(actions);
        BorderPane.setMargin(appointmentList, new Insets(0, 18, 0, 18));
        return pane;
    }

    private void showAvailableTimes() {
        LocalDate date = datePicker.getValue();
        if (date == null || date.isBefore(LocalDate.now())) {
            setStatus("Choose today or a future date.");
            return;
        }

        List<LocalTime> times = appointmentMaster.getAvailableStarts(
                date, durationBox.getValue(), appointmentBeingRescheduled);
        availableTimes.setItems(FXCollections.observableArrayList(times));
        setStatus(times.isEmpty() ? "No times are available for that date." : "Select an available time.");
    }

    private void saveAppointment() {
        String name = nameField.getText().trim().replaceAll("\\s+", " ");
        LocalTime selectedTime = availableTimes.getSelectionModel().getSelectedItem();

        if (name.isEmpty()) {
            setStatus("Customer name cannot be blank.");
            return;
        }
        if (selectedTime == null) {
            setStatus("Choose an available appointment time first.");
            return;
        }

        if (appointmentBeingRescheduled == null) {
            appointmentMaster.addAppt(new Appointment(durationBox.getValue(), name,
                    datePicker.getValue().atTime(selectedTime)));
            setStatus("Appointment scheduled.");
        } else {
            Appointment updated = appointmentBeingRescheduled.rescheduledTo(
                    durationBox.getValue(), datePicker.getValue().atTime(selectedTime));
            appointmentMaster.appointmentRemove(appointmentBeingRescheduled);
            appointmentMaster.addAppt(updated);
            appointmentBeingRescheduled = null;
            nameField.setEditable(true);
            saveButton.setText("Schedule Appointment");
            setStatus("Appointment rescheduled.");
        }

        saveAppointments();
        refreshAppointments();
        availableTimes.getItems().clear();
    }

    private void beginReschedule() {
        Appointment selected = appointmentList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select an appointment to reschedule.");
            return;
        }

        appointmentBeingRescheduled = selected;
        nameField.setText(selected.getCustomerName());
        nameField.setEditable(false);
        datePicker.setValue(selected.getApptStart().toLocalDate());
        durationBox.setValue(selected.getApptDuration());
        saveButton.setText("Save Rescheduled Appointment");
        tabPane.getSelectionModel().select(scheduleTab);
        showAvailableTimes();
        setStatus("Select a new date, duration, and time for the appointment.");
    }

    private void cancelSelectedAppointment() {
        Appointment selected = appointmentList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select an appointment to cancel.");
            return;
        }

        appointmentMaster.appointmentRemove(selected);
        saveAppointments();
        refreshAppointments();
        setStatus("Appointment cancelled.");
    }

    private void clearScheduleForm() {
        appointmentBeingRescheduled = null;
        nameField.clear();
        nameField.setEditable(true);
        datePicker.setValue(LocalDate.now());
        durationBox.setValue(30);
        availableTimes.getItems().clear();
        saveButton.setText("Schedule Appointment");
        setStatus("");
    }

    private void refreshAppointments() {
        String search = searchField.getText().trim();
        List<Appointment> appointments = search.isEmpty()
                ? appointmentMaster.getAllAppointments()
                : appointmentMaster.getApptsByName(search);
        List<Appointment> sortedAppointments = new ArrayList<>(appointments);
        sortedAppointments.sort(Comparator.comparing(Appointment::getApptStart));
        appointmentList.setItems(FXCollections.observableArrayList(sortedAppointments));
    }

    private void loadAppointments() {
        try {
            for (Appointment appointment : AppointmentStorage.load()) {
                appointmentMaster.addAppt(appointment);
            }
        } catch (IOException exception) {
            showMessage("Could not load saved appointments: " + exception.getMessage());
        }
    }

    private void saveAppointments() {
        try {
            AppointmentStorage.save(appointmentMaster.getAllAppointments());
        } catch (IOException exception) {
            showMessage("Could not save appointments: " + exception.getMessage());
        }
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
