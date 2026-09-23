# Appointment Manager

Appointment Manager is a JavaFX desktop application for scheduling shared
appointments without double-booking. Each person creates their own account and
can view and manage only their own appointments, while the shared schedule
still prevents anyone from booking an occupied time.

The desktop client communicates with a Spring Boot API hosted on Render. The
API manages authentication with Supabase Auth and stores appointments in a
Supabase PostgreSQL database.

## What it does

- Creates email/password accounts with optional email confirmation.
- Schedules appointments in 15-minute increments from 9:00 AM to 5:00 PM.
- Supports 15, 30, 45, and 60 minute appointments.
- Prevents overlapping bookings across every account.
- Shows only times that are still in the future when scheduling today.
- Lets users reschedule appointments, including correcting the customer name.
- Confirms before an appointment is cancelled.
- Separates active appointments from completed appointments in **Appointment
  History**.
- Filters active appointments by **Today**, **This Week**, or **Upcoming**.
- Keeps the interface responsive while data is loading or Render is waking up.
- Keeps each account's appointment details private.

## How it is structured

```text
JavaFX desktop app
        |
        v
Spring Boot API on Render
        |
        v
Supabase Auth + PostgreSQL
```

The JavaFX app never contains the database password. The API verifies each
Supabase access token and scopes appointment reads, updates, and deletes to the
signed-in user. The database also has Row Level Security enabled.

## Run the shared app after cloning

The project is configured to use the shared deployed API by default. You do
**not** need Supabase credentials, Render credentials, or a local database just
to run the desktop client and create your own account.

### Prerequisites

- Windows, macOS, or Linux
- JDK 21
- JavaFX SDK 21 (the project was developed with JavaFX 21.0.11)
- IntelliJ IDEA, or another IDE configured to run JavaFX

### IntelliJ setup

1. Clone or download this repository and open it in IntelliJ IDEA.
2. Install a JDK 21 and select it under **File → Project Structure → Project**.
3. Download the [JavaFX SDK](https://openjfx.io/) matching JDK 21 and unzip it
   somewhere on your computer.
4. In **File → Project Structure → Libraries**, add the SDK's `lib` folder. For
   example:

   ```text
   C:\path\to\javafx-sdk-21.0.11\lib
   ```

5. Create or edit a run configuration whose main class is
   `AppointmentManagerLauncher`.
6. Add these VM options to that run configuration, updating the path to match
   where you installed JavaFX:

   ```text
   --module-path "C:\path\to\javafx-sdk-21.0.11\lib" --add-modules javafx.controls
   ```

7. Run `AppointmentManagerLauncher`.

If IntelliJ reports that `javafx` packages cannot be found, JavaFX has not been
added as a library or the VM options do not point to the correct `lib` folder.

### First use

1. Choose **Create Account**.
2. Enter an email address and a password with at least 8 characters.
3. Confirm the email if prompted, then return to the desktop app and sign in.
4. Use **Show Available Times** before scheduling.

Accounts are private: you cannot see another person's appointment details. A
time booked by another person will correctly be unavailable because everyone
uses the same schedule.

## Hosted-service availability

This project uses free Render and Supabase plans for the shared deployment.

- Render spins the API down after 15 minutes without traffic. The first request
  afterward can take around a minute while the service wakes. The app remains
  responsive; if the first request times out, wait a moment and try again.
- A Supabase Free Plan project can pause after a period of low activity. The
  project owner receives a warning email and can keep it active by visiting the
  dashboard or using the app. If it pauses, the owner must select **Resume
  project** in the Supabase dashboard before the shared app can work again.
- Supabase's built-in development email provider has a small confirmation-email
  limit. For frequent testing or production use, configure a custom SMTP
  provider.

## Develop with your own backend

To run a separate local or deployed backend, configure these environment
variables for the **Spring Boot API**. Do not commit their values.

```text
DATABASE_URL=jdbc:postgresql://aws-REGION.pooler.supabase.com:5432/postgres?sslmode=require
DATABASE_USERNAME=postgres.PROJECT_REF
DATABASE_PASSWORD=your-database-password
SUPABASE_URL=https://PROJECT_REF.supabase.co
SUPABASE_PUBLISHABLE_KEY=your-publishable-key
```

Use the **Session pooler** connection details from Supabase's **Connect** panel.
From the `server` folder, start the API with:

```powershell
mvn spring-boot:run
```

The first successful connection runs `server/src/main/resources/schema.sql` to
create the appointment table and indexes.

To point the desktop client at a local API or a different deployment, set this
environment variable in the `AppointmentManagerLauncher` run configuration:

```text
APPOINTMENT_API_URL=http://localhost:8080
```

The included `server/Dockerfile` packages the API for Docker-compatible hosts
such as Render. The public health check is available at `/health`, and the
email-confirmation landing page is `/confirmed`.

## Technology

- Java 21 and JavaFX
- Java time API, collections, and HTTP client
- Spring Boot and JDBC
- Supabase Auth and PostgreSQL
- Render
