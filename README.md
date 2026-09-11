# Appointment Manager

A JavaFX desktop application for scheduling, cancelling, and rescheduling
appointments without double-booking.

## Features
- 15-minute time slot scheduling
- Variable appointment durations (15–60 minutes)
- Conflict prevention using overlap detection
- Search appointments by name or ID
- Case-insensitive customer-name searches
- Flexible date entry (`M/D/YYYY` or `MM/DD/YYYY`)
- Rescheduling and cancellation
- A shared PostgreSQL database through a backend API (Supabase)
- Email/password accounts with per-user appointment privacy

## Tech Stack
- Java
- java.time API
- HashMap / ArrayList
- Spring Boot
- Supabase Auth and PostgreSQL

## Shared PostgreSQL database (Supabase)

The `server` directory contains the Spring Boot API. It is the only part of the
system that connects to Supabase; the JavaFX client must never contain the
database password.

1. Copy `.env.example` values into environment variables on the computer that
   will run the API. Replace the placeholders with the **Session pooler**
   values from Supabase's **Connect** panel.
2. In PowerShell, set values for the current terminal session (do not add these
   to Git):

   ```powershell
   $env:DATABASE_URL = 'jdbc:postgresql://aws-REGION.pooler.supabase.com:5432/postgres?sslmode=require'
   $env:DATABASE_USERNAME = 'postgres.PROJECT_REF'
   $env:DATABASE_PASSWORD = 'your-database-password'
   $env:SUPABASE_URL = 'https://PROJECT_REF.supabase.co'
   $env:SUPABASE_PUBLISHABLE_KEY = 'your-publishable-key'
   ```

3. Run the API from `server` with `mvn spring-boot:run`. Its first successful
   connection creates the `appointments` table and indexes automatically.

The API is available locally at `http://localhost:8080`. Appointment endpoints
require a valid Supabase Auth access token and only return rows owned by that
user. Configure all five environment variables above in the deployed backend.
Use `/health` as the hosting service's public health-check path.

The database table has Row Level Security enabled and direct access is revoked
from Supabase's `anon` and `authenticated` database roles. The desktop client
does not query the table directly; the backend verifies each user with Supabase
Auth and applies the ownership filter to every query.

The API includes a `server/Dockerfile` for deployment services such as Render.

## Run the desktop app

Start the API first, then run `AppointmentManagerLauncher` from IntelliJ. Sign
up with an email and password, confirm the email if prompted, and sign in. The
desktop app calls the API at `http://localhost:8080` by default. To point it at
a deployed API, set `APPOINTMENT_API_URL` to that API's HTTPS base URL in the
desktop run configuration. Login tokens remain in memory and are cleared when
the user logs out or closes the app.
