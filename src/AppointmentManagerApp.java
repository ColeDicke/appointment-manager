import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** JavaFX user interface for the appointment manager. */
public class AppointmentManagerApp extends Application {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    private AppointmentMaster appointmentMaster = new AppointmentMaster();
    private final TextField nameField = new TextField();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final ComboBox<Integer> durationBox = new ComboBox<>();
    private final ListView<LocalTime> availableTimes = new ListView<>();
    private final ListView<Appointment> appointmentList = new ListView<>();
    private final ListView<Appointment> appointmentHistoryList = new ListView<>();
    private final Label statusLabel = new Label();
    private final Button saveButton = new Button("Schedule Appointment");

    private Appointment appointmentBeingRescheduled;
    private TabPane tabPane;
    private Tab scheduleTab;
    private Scene mainScene;
    private final Label accountLabel = new Label();
    private Timeline historyRefresh;
    private AppointmentFilter appointmentFilter = AppointmentFilter.UPCOMING;

    private enum AppointmentFilter {
        TODAY, THIS_WEEK, UPCOMING
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Appointment Manager");
        showLoginScene(stage);
        stage.show();
        maximizeAfterSceneChange(stage);
        historyRefresh = new Timeline(new KeyFrame(javafx.util.Duration.minutes(1), event -> refreshAppointments()));
        historyRefresh.setCycleCount(Timeline.INDEFINITE);
        historyRefresh.play();
    }

    private void showLoginScene(Stage stage) {
        TextField emailField = new TextField();
        emailField.setPromptText("you@example.com");
        emailField.setPrefWidth(300);
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("At least 8 characters");
        passwordField.setPrefWidth(300);
        Label loginStatus = new Label();

        GridPane fields = new GridPane();
        fields.getStyleClass().add("login-fields");
        fields.setHgap(10);
        fields.setVgap(12);
        fields.addRow(0, new Label("Email:"), emailField);
        fields.addRow(1, new Label("Password:"), passwordField);

        Button signInButton = new Button("Sign In");
        signInButton.getStyleClass().add("primary-button");
        signInButton.setDefaultButton(true);
        Button createAccountButton = new Button("Create Account");
        createAccountButton.getStyleClass().add("secondary-button");
        signInButton.setOnAction(event -> {
            String email = emailField.getText();
            String password = passwordField.getText();
            signInButton.setDisable(true);
            createAccountButton.setDisable(true);
            loginStatus.setText("Signing in...");
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws IOException {
                    AppointmentStorage.login(email, password);
                    return null;
                }
            };
            task.setOnSucceeded(ignored -> {
                passwordField.clear();
                showMainScene(stage);
                signInButton.setDisable(false);
                createAccountButton.setDisable(false);
            });
            task.setOnFailed(ignored -> {
                loginStatus.setText(taskMessage(task));
                signInButton.setDisable(false);
                createAccountButton.setDisable(false);
            });
            startTask(task);
        });

        createAccountButton.setOnAction(event -> {
            String email = emailField.getText();
            String password = passwordField.getText();
            signInButton.setDisable(true);
            createAccountButton.setDisable(true);
            loginStatus.setText("Creating account...");
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() throws IOException {
                    return AppointmentStorage.signUp(email, password);
                }
            };
            task.setOnSucceeded(ignored -> {
                boolean signedIn = task.getValue();
                passwordField.clear();
                if (signedIn) {
                    showMainScene(stage);
                } else {
                    loginStatus.setText("Account created. Check your email, then return here to sign in.");
                }
                signInButton.setDisable(false);
                createAccountButton.setDisable(false);
            });
            task.setOnFailed(ignored -> {
                loginStatus.setText(taskMessage(task));
                signInButton.setDisable(false);
                createAccountButton.setDisable(false);
            });
            startTask(task);
        });

        HBox actions = new HBox(10, signInButton, createAccountButton);
        actions.getStyleClass().add("login-actions");
        actions.setAlignment(Pos.CENTER);
        Label brandMark = new Label("AM");
        brandMark.getStyleClass().add("brand-mark");
        Label title = new Label("Appointment Manager");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Sign in to manage your appointments");
        subtitle.getStyleClass().add("app-subtitle");
        loginStatus.getStyleClass().add("status-label");
        loginStatus.setAlignment(Pos.CENTER);
        loginStatus.setMaxWidth(Double.MAX_VALUE);
        VBox loginCard = new VBox(16, brandMark, title, subtitle, fields, actions, loginStatus);
        loginCard.getStyleClass().add("login-card");
        loginCard.setAlignment(Pos.CENTER);
        loginCard.setMaxWidth(520);
        StackPane loginPane = new StackPane(loginCard);
        loginPane.getStyleClass().add("login-pane");
        stage.setScene(createScene(loginPane, 560, 360));
    }

    private void showMainScene(Stage stage) {
        appointmentMaster = new AppointmentMaster();
        appointmentBeingRescheduled = null;

        if (mainScene == null) {
            buildMainScene(stage);
        }
        accountLabel.setText("Signed in as " + AppointmentStorage.getCurrentEmail());
        clearScheduleForm();
        refreshAppointments();
        stage.setScene(mainScene);
        maximizeAfterSceneChange(stage);
        loadAppointments();
    }

    private void buildMainScene(Stage stage) {

        tabPane = new TabPane();
        scheduleTab = new Tab("Schedule", createSchedulePane());
        scheduleTab.setClosable(false);
        Tab appointmentsTab = new Tab("Appointments", createAppointmentsPane());
        appointmentsTab.setClosable(false);
        Tab historyTab = new Tab("Appointment History", createHistoryPane());
        historyTab.setClosable(false);
        tabPane.getTabs().addAll(scheduleTab, appointmentsTab, historyTab);

        Button logoutButton = new Button("Log Out");
        logoutButton.getStyleClass().add("secondary-button");
        logoutButton.setOnAction(event -> {
            AppointmentStorage.logout();
            appointmentMaster = new AppointmentMaster();
            appointmentList.getItems().clear();
            appointmentHistoryList.getItems().clear();
            showLoginScene(stage);
            maximizeAfterSceneChange(stage);
        });
        HBox accountBar = new HBox(12, accountLabel, logoutButton);
        accountBar.getStyleClass().add("account-bar");
        accountLabel.getStyleClass().add("account-label");
        accountBar.setAlignment(Pos.CENTER_RIGHT);
        accountBar.setPadding(new Insets(10, 18, 10, 18));

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-shell");
        root.setTop(accountBar);
        root.setCenter(tabPane);
        mainScene = createScene(root, 760, 580);
    }

    private BorderPane createSchedulePane() {
        durationBox.getItems().addAll(15, 30, 45, 60);
        durationBox.setValue(30);

        GridPane fields = new GridPane();
        fields.getStyleClass().add("form-card");
        fields.setHgap(10);
        fields.setVgap(10);
        fields.setPadding(new Insets(18));
        fields.addRow(0, new Label("Customer name:"), nameField);
        fields.addRow(1, new Label("Date:"), datePicker);
        fields.addRow(2, new Label("Duration (minutes):"), durationBox);

        Button findTimesButton = new Button("Show Available Times");
        findTimesButton.getStyleClass().add("secondary-button");
        findTimesButton.setOnAction(event -> showAvailableTimes());
        fields.add(findTimesButton, 1, 3);

        availableTimes.setPrefHeight(230);
        availableTimes.getStyleClass().add("appointment-list");
        availableTimes.setCellFactory(list -> new javafx.scene.control.ListCell<LocalTime>() {
            @Override
            protected void updateItem(LocalTime time, boolean empty) {
                super.updateItem(time, empty);
                setText(empty || time == null ? null : time.format(TIME_FORMATTER));
            }
        });

        saveButton.setOnAction(event -> saveAppointment());
        saveButton.getStyleClass().add("primary-button");
        Button cancelRescheduleButton = new Button("Clear");
        cancelRescheduleButton.getStyleClass().add("secondary-button");
        cancelRescheduleButton.setOnAction(event -> clearScheduleForm());
        HBox actions = new HBox(10, saveButton, cancelRescheduleButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        Label availableTimesLabel = new Label("Available times");
        availableTimesLabel.getStyleClass().add("section-label");
        statusLabel.getStyleClass().add("status-label");
        VBox content = new VBox(14, fields, availableTimesLabel, availableTimes,
                actions, statusLabel);
        content.getStyleClass().add("schedule-content");
        content.setMaxWidth(640);
        content.setPadding(new Insets(24, 18, 24, 18));

        StackPane centeredContent = new StackPane(content);
        StackPane.setAlignment(content, Pos.TOP_CENTER);
        BorderPane pane = new BorderPane(centeredContent);
        return pane;
    }

    private BorderPane createAppointmentsPane() {
        Label heading = new Label("Upcoming appointments");
        heading.getStyleClass().add("section-label");
        Label description = new Label("Your scheduled appointments appear here.");
        description.getStyleClass().add("history-description");
        ToggleGroup filters = new ToggleGroup();
        ToggleButton todayButton = createFilterButton("Today", AppointmentFilter.TODAY, filters);
        ToggleButton weekButton = createFilterButton("This Week", AppointmentFilter.THIS_WEEK, filters);
        ToggleButton upcomingButton = createFilterButton("Upcoming", AppointmentFilter.UPCOMING, filters);
        upcomingButton.setSelected(true);
        HBox filterBar = new HBox(8, todayButton, weekButton, upcomingButton);
        filterBar.getStyleClass().add("filter-bar");
        VBox header = new VBox(10, heading, description, filterBar);
        header.setPadding(new Insets(22, 18, 14, 18));

        Button rescheduleButton = new Button("Reschedule Selected");
        rescheduleButton.getStyleClass().add("secondary-button");
        rescheduleButton.setOnAction(event -> beginReschedule());
        Button cancelButton = new Button("Cancel Selected");
        cancelButton.getStyleClass().add("danger-button");
        cancelButton.setOnAction(event -> cancelSelectedAppointment());
        HBox actions = new HBox(10, rescheduleButton, cancelButton);
        actions.setPadding(new Insets(12, 18, 18, 18));

        BorderPane pane = new BorderPane();
        pane.getStyleClass().add("appointments-pane");
        pane.setTop(header);
        pane.setCenter(appointmentList);
        pane.setBottom(actions);
        BorderPane.setMargin(appointmentList, new Insets(0, 18, 0, 18));
        return pane;
    }

    private ToggleButton createFilterButton(String label, AppointmentFilter filter, ToggleGroup group) {
        ToggleButton button = new ToggleButton(label);
        button.setToggleGroup(group);
        button.getStyleClass().add("filter-button");
        button.setOnAction(event -> {
            appointmentFilter = filter;
            refreshAppointments();
        });
        return button;
    }

    private BorderPane createHistoryPane() {
        Label heading = new Label("Appointment history");
        heading.getStyleClass().add("section-label");
        Label description = new Label("Completed appointments are kept here for reference.");
        description.getStyleClass().add("history-description");
        VBox header = new VBox(4, heading, description);
        header.setPadding(new Insets(22, 18, 14, 18));

        BorderPane pane = new BorderPane();
        pane.getStyleClass().add("appointments-pane");
        pane.setTop(header);
        pane.setCenter(appointmentHistoryList);
        BorderPane.setMargin(appointmentHistoryList, new Insets(0, 18, 18, 18));
        return pane;
    }

    private void showAvailableTimes() {
        LocalDate date = datePicker.getValue();
        if (date == null || date.isBefore(LocalDate.now())) {
            setStatus("Choose today or a future date.");
            return;
        }

        int duration = durationBox.getValue();
        Appointment ignoredAppointment = appointmentBeingRescheduled;
        availableTimes.setDisable(true);
        setStatus("Loading available times...");
        Task<List<LocalTime>> task = new Task<>() {
            @Override
            protected List<LocalTime> call() throws IOException {
                return AppointmentStorage.getAvailableStarts(date, duration, ignoredAppointment);
            }
        };
        task.setOnSucceeded(ignored -> {
            List<LocalTime> times = task.getValue();
            if (date.equals(LocalDate.now())) {
                LocalTime currentTime = LocalTime.now();
                times = times.stream()
                        .filter(time -> time.isAfter(currentTime))
                        .toList();
            }
            availableTimes.setItems(FXCollections.observableArrayList(times));
            availableTimes.setDisable(false);
            setStatus(times.isEmpty() ? "No times are available for that date." : "Select an available time.");
        });
        task.setOnFailed(ignored -> {
            availableTimes.setDisable(false);
            setStatus("Could not load available times: " + taskMessage(task));
        });
        startTask(task);
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

        Appointment originalAppointment = appointmentBeingRescheduled;
        Appointment submittedAppointment = originalAppointment == null
                ? new Appointment(durationBox.getValue(), name, datePicker.getValue().atTime(selectedTime))
                : originalAppointment.rescheduledTo(name, durationBox.getValue(), datePicker.getValue().atTime(selectedTime));
        saveButton.setDisable(true);
        setStatus(originalAppointment == null ? "Scheduling appointment..." : "Rescheduling appointment...");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws IOException {
                if (originalAppointment == null) {
                    AppointmentStorage.create(submittedAppointment);
                } else {
                    AppointmentStorage.update(submittedAppointment);
                }
                return null;
            }
        };
        task.setOnSucceeded(ignored -> {
            if (originalAppointment == null) {
                appointmentMaster.addAppt(submittedAppointment);
                setStatus("Appointment scheduled.");
            } else {
                appointmentMaster.appointmentRemove(originalAppointment);
                appointmentMaster.addAppt(submittedAppointment);
                appointmentBeingRescheduled = null;
                nameField.setEditable(true);
                saveButton.setText("Schedule Appointment");
                setStatus("Appointment rescheduled.");
            }
            saveButton.setDisable(false);
            refreshAppointments();
            availableTimes.getItems().clear();
        });
        task.setOnFailed(ignored -> {
            saveButton.setDisable(false);
            showMessage("Could not save appointment: " + taskMessage(task));
        });
        startTask(task);
    }

    private void beginReschedule() {
        Appointment selected = appointmentList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select an appointment to reschedule.");
            return;
        }

        appointmentBeingRescheduled = selected;
        nameField.setText(selected.getCustomerName());
        nameField.setEditable(true);
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

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Cancel appointment");
        confirmation.setHeaderText("Cancel this appointment?");
        confirmation.setContentText(selected.toString());
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        appointmentList.setDisable(true);
        setStatus("Cancelling appointment...");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws IOException {
                AppointmentStorage.delete(selected);
                return null;
            }
        };
        task.setOnSucceeded(ignored -> {
            appointmentMaster.appointmentRemove(selected);
            appointmentList.setDisable(false);
            refreshAppointments();
            setStatus("Appointment cancelled.");
        });
        task.setOnFailed(ignored -> {
            appointmentList.setDisable(false);
            showMessage("Could not cancel appointment: " + taskMessage(task));
        });
        startTask(task);
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
        List<Appointment> appointments = appointmentMaster.getAllAppointments();
        List<Appointment> sortedAppointments = new ArrayList<>(appointments);
        sortedAppointments.sort(Comparator.comparing(Appointment::getApptStart));
        appointmentList.setItems(FXCollections.observableArrayList(sortedAppointments.stream()
                .filter(appointment -> !isComplete(appointment))
                .filter(this::matchesAppointmentFilter)
                .toList()));
        appointmentHistoryList.setItems(FXCollections.observableArrayList(sortedAppointments.stream()
                .filter(this::isComplete)
                .toList()));
    }

    private boolean isComplete(Appointment appointment) {
        LocalDateTime end = appointment.getApptStart().plusMinutes(appointment.getApptDuration());
        return !end.isAfter(LocalDateTime.now());
    }

    private boolean matchesAppointmentFilter(Appointment appointment) {
        LocalDate appointmentDate = appointment.getApptStart().toLocalDate();
        LocalDate today = LocalDate.now();
        return switch (appointmentFilter) {
            case TODAY -> appointmentDate.equals(today);
            case THIS_WEEK -> !appointmentDate.isBefore(today)
                    && !appointmentDate.isAfter(today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
            case UPCOMING -> true;
        };
    }

    private void loadAppointments() {
        setStatus("Loading appointments...");
        Task<List<Appointment>> task = new Task<>() {
            @Override
            protected List<Appointment> call() throws IOException {
                return AppointmentStorage.load();
            }
        };
        task.setOnSucceeded(ignored -> {
            for (Appointment appointment : task.getValue()) {
                appointmentMaster.addAppt(appointment);
            }
            refreshAppointments();
            setStatus("");
        });
        task.setOnFailed(ignored -> showMessage("Could not load saved appointments: " + taskMessage(task)));
        startTask(task);
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void startTask(Task<?> task) {
        Thread thread = new Thread(task, "appointment-api-request");
        thread.setDaemon(true);
        thread.start();
    }

    private String taskMessage(Task<?> task) {
        Throwable exception = task.getException();
        return exception == null || exception.getMessage() == null
                ? "An unexpected error occurred." : exception.getMessage();
    }

    private Scene createScene(Parent root, int width, int height) {
        Scene scene = new Scene(root, width, height);
        URL stylesheet = getClass().getResource("/appointment-manager.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        return scene;
    }

    /** Reassert maximization after JavaFX has applied a replacement scene's preferred size. */
    private void maximizeAfterSceneChange(Stage stage) {
        stage.setMaximized(false);
        Platform.runLater(() -> stage.setMaximized(true));
    }
}
