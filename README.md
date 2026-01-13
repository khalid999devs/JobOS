# JobOS

A job application and CV management platform built for both job seekers and recruiters. The system handles the full hiring workflow from job posting to candidate management, with real-time notifications and cross-platform support.

## What It Does

JobOS connects two types of users:

- **Job Seekers** can build CVs, search and filter jobs, apply with cover letters, and track their application status.
- **Job Posters** can create job listings, review applicants, view submitted CVs, and manage the hiring pipeline.

Both sides get real-time push notifications through Firebase for status updates, new applications, and messages.

## Tech Stack

| Layer   | Technology                                    |
| ------- | --------------------------------------------- |
| Backend | Spring Boot, PostgreSQL, Firebase Admin SDK   |
| Desktop | JavaFX with FXML/CSS, OkHttp                  |
| Android | Native Java, Firebase Cloud Messaging, OkHttp |
| Shared  | Common DTOs, Gradle multi-module              |

## Project Structure

```
JobOS/
├── backend/                    # Spring Boot REST API
│   ├── config/                 # App configuration, security, CORS, Firebase
│   ├── controller/             # REST endpoints, request/response handling
│   │   ├── AuthController
│   │   ├── JobPostController
│   │   ├── JobSearchController
│   │   ├── ApplicationController
│   │   ├── CVController
│   │   ├── ProfileController
│   │   └── NotificationController
│   ├── domain/                 # JPA entities, database models
│   │   ├── user/
│   │   ├── job/
│   │   ├── application/
│   │   ├── cv/
│   │   └── notification/
│   ├── service/                # Business logic layer
│   ├── repository/             # Database access layer (JPA repositories)
│   └── security/               # JWT authentication, authorization filters
│
├── desktop/                   # JavaFX application
│   ├── controller/             # UI controllers for FXML views
│   │   ├── auth/
│   │   ├── seeker/
│   │   ├── poster/
│   │   ├── settings/
│   │   └── shell/
│   ├── service/                # API client services (HTTP calls)
│   ├── model/                  # Desktop-specific models, state management
│   └── resources/              # UI resources
│       ├── fxml/               # Scene layouts
│       └── css/                # Styling
│
├── android/                   # Android application
│   └── app/src/main/java/com/jobos/android/
│       ├── ui/                 # Activities, fragments, view models
│       │   ├── auth/
│       │   ├── seeker/
│       │   ├── poster/
│       │   ├── cv/
│       │   ├── profile/
│       │   ├── notifications/
│       │   └── onboarding/
│       ├── data/               # Data layer
│       │   ├── model/          # Android-specific models
│       │   └── network/        # API client, Retrofit services
│       ├── adapter/            # RecyclerView adapters
│       └── service/            # Background services
│
├── shared/                    # Shared code across all platforms
│   └── dto/                    # Data transfer objects for API communication
│
└── firebase-keys/             # Firebase service account (gitignored)
```

## Features

### For Job Seekers

- Profile management with skills, desired roles, salary expectations, job type preferences
- CV builder with multiple templates and section management
- Job search with filters (location, salary, job type, work mode)
- Application tracking with status updates
- Save jobs for later
- PDF export for CVs

### For Job Posters

- Company profile with verification documents
- Job posting with requirements, salary range, application deadlines
- Applicant management with status workflow (Pending, Reviewed, Shortlisted, Hired, Rejected)
- View applicant CVs directly from application details
- Application statistics per job

### Shared Features

- JWT authentication with refresh tokens
- Multi-device session support
- Real-time push notifications via Firebase
- Role-based access control

## Getting Started

### Requirements

- JDK 21 or higher
- PostgreSQL 14+
- Android Studio (for mobile development)
- Firebase project with Cloud Messaging enabled

### Database Setup

```sql
CREATE DATABASE jobos;
CREATE USER jobos WITH PASSWORD 'jobos';
GRANT ALL PRIVILEGES ON DATABASE jobos TO jobos;
```

### Firebase Setup

1. Create a project at [Firebase Console](https://console.firebase.google.com/)
2. Download the service account key and save to `firebase-keys/jobos-firebase-key.json`
3. For Android, download `google-services.json` to `android/app/`
4. Set the environment variable:
   ```bash
   export FIREBASE_SERVICE_ACCOUNT_JSON_PATH="/path/to/firebase-keys/jobos-firebase-key.json"
   ```

### Running the Backend

```bash
./gradlew :backend:bootRun
```

Server starts at `http://localhost:8080`

### Running the Desktop App

```bash
./gradlew :desktop:run
```

### Running the Android App

Open `android/` in Android Studio and run on emulator or device.

## API Overview

The backend exposes REST endpoints under `/api`:

| Endpoint                   | Purpose                          |
| -------------------------- | -------------------------------- |
| `/auth/*`                  | Login, register, refresh, logout |
| `/users/me`                | Profile management               |
| `/users/me/preferences`    | Seeker/poster preferences        |
| `/jobs`                    | Job CRUD, search, filters        |
| `/jobs/{id}/apply`         | Submit applications              |
| `/applications`            | Application management           |
| `/applications/{id}/cv`    | View applicant CV (poster only)  |
| `/cvs`                     | CV CRUD                          |
| `/notifications`           | Notification management          |
| `/notifications/fcm-token` | Register device for push         |

## Architecture Notes

- Backend uses layered architecture: Controller -> Service -> Repository
- Desktop follows MVC with FXML for views
- Android uses package-by-feature with fragments and a single activity
- All platforms share similar data models for consistency
- Firebase handles real-time notifications across all clients

## Development

Build all modules:

```bash
./gradlew clean build
```

Run backend tests:

```bash
./gradlew :backend:test
```

Compile check:

```bash
./gradlew :backend:compileJava :desktop:compileJava
```
