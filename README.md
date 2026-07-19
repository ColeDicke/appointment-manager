# Appointment Manager

A Java console application that allows users to schedule, cancel, and reschedule appointments without double-booking.

## Features
- 15-minute time slot scheduling
- Variable appointment durations (15–60 minutes)
- Conflict prevention using overlap detection
- Search appointments by name or ID
- Case-insensitive customer-name searches
- Flexible date entry (`M/D/YYYY` or `MM/DD/YYYY`)
- Rescheduling and cancellation
- Automatic local saving to `appointments.txt`

## Tech Stack
- Java
- java.time API
- HashMap / ArrayList

## GUI version

The console app remains available through `Main`. To run the JavaFX GUI, run
`AppointmentManagerApp` from IntelliJ. The GUI uses the same `appointments.txt`
file as the console version.
