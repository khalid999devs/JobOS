# JobOS

A modern job application flow and CV management system designed to streamline the hiring process for both job seekers and recruiters. Built with a focus on interactive user experience and powered by a freemium, credit-based pricing model.

## Overview

JobOS bridges the gap between talented job seekers and companies looking to hire. The platform supports two primary user roles:

- **Job Seeker**: Create professional profiles, upload CVs, browse job listings, and track application status in real-time.
- **Job Poster**: Post job openings, review candidate profiles, manage applications, and communicate with potential hires.

The system features real-time notifications powered by Firebase, ensuring users stay updated on application status, new job postings, and important messages.

## Tech Stack

### Backend

- **Spring Boot 4.0.0** - REST API server
- **PostgreSQL 42.7.8** - Primary database
- **Firebase Admin SDK 9.7.0** - Real-time notifications
- **Java 25 LTS** - Language runtime

### Desktop Application

- **JavaFX 25.0.1** - Rich desktop UI with FXML/CSS
- **OkHttp 5.3.2** - HTTP client
- **MVC Architecture** - Clean separation of concerns

### Android Application

- **Android SDK (minSdk 24, targetSdk 36)** - Java-only
- **Firebase BOM 33.7.0** - Real-time database client
- **OkHttp 5.3.2** - Network layer
- **Package-by-feature** - Production-ready structure

### Shared

- **Common DTOs** - Reusable data models across platforms
- **Gradle 9.2.1** - Multi-module build system

## Project Structure

```
JobOS/
├── backend/              # Spring Boot REST API
│   ├── config/          # Firebase, Security, CORS
│   ├── controller/      # REST endpoints
│   ├── service/         # Business logic
│   └── resources/       # application.yml
│
├── desktop/             # JavaFX desktop app
│   ├── controller/      # FXML controllers
│   ├── service/         # API communication
│   ├── util/            # Helper classes
│   └── resources/
│       ├── fxml/        # UI layouts
│       └── css/         # Stylesheets
│
├── android/             # Android mobile app
│   └── app/src/main/java/com/jobos/android/
│       ├── JobOSApplication.java
│       ├── config/      # API configuration
│       ├── data/
│       │   ├── model/   # Data models
│       │   └── network/ # API clients
│       └── ui/
│           ├── main/    # Main activity
│           └── notifications/
│
└── shared/              # Shared DTOs
    └── dto/common/      # Common data models
```

## Getting Started

### Prerequisites

- **JDK 25** or higher
- **Gradle 9.2.1** (included via wrapper)
- **PostgreSQL** (running on localhost:5432)
- **Android Studio** (for Android development)
- **Firebase Project** (for real-time features)

### Database Setup

1. Install PostgreSQL and create a database:

   ```sql
   CREATE DATABASE jobos;
   CREATE USER jobos WITH PASSWORD 'jobos';
   GRANT ALL PRIVILEGES ON DATABASE jobos TO jobos;
   ```

2. Update `backend/src/main/resources/application.yml` with your credentials if needed.

### Firebase Setup

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Download service account key:

   - Go to Project Settings → Service Accounts
   - Click "Generate New Private Key"
   - Save as `firebase-keys/jobos-firebase-key.json`

3. Enable Firebase Realtime Database:

   - Set database URL in `backend/config/FirebaseConfig.java`
   - Configure rules to allow read/write on `/users/{userId}/notifications`

4. For Android:

   - Download `google-services.json` from Firebase Console
   - Place in `android/app/` directory

5. Set environment variable:
   ```bash
   echo 'export FIREBASE_SERVICE_ACCOUNT_JSON_PATH="/path/to/firebase-keys/jobos-firebase-key.json"' >> ~/.zshrc
   source ~/.zshrc
   ```

### Running the Backend

```bash
./gradlew :backend:bootRun
```

The server will start on `http://localhost:8080`

**Available endpoints:**

- `GET /api/health` - Health check
- `GET /api/ping` - Ping endpoint
- `POST /api/notifications/send` - Send notification

### Running the Desktop App

```bash
./gradlew :desktop:run
```

The JavaFX application will launch with a clean, professional interface.

### Running the Android App

1. Open the `android/` directory in Android Studio
2. Sync Gradle files
3. Run on emulator or physical device

**Note:** Ensure `google-services.json` is present in `android/app/` before building.

### Building All Modules

```bash
./gradlew clean build
```

This compiles all modules (shared, backend, desktop) and runs tests.

## Development

### Testing Backend API

Send a test notification:

```bash
curl -X POST http://localhost:8080/api/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user-123",
    "title": "Welcome to JobOS",
    "body": "Your profile is now active!"
  }'
```

### Architecture Patterns

- **Backend**: Layered architecture (Controller → Service → Repository)
- **Desktop**: MVC with FXML
- **Android**: Package-by-feature, preparing for MVVM

See `android/STRUCTURE.md` for detailed Android architecture documentation.
