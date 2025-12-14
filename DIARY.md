# JobOS Development Diary

## December 14, 2025

### Initial Setup

- Created Gradle multi-module monorepo (shared, backend, desktop, android)
- Configured Java, Gradle, and Spring Boot
- Set up PostgreSQL database

### Backend

- Implemented REST API with Spring Boot
- Added Firebase Admin SDK for real-time notifications
- Created endpoints: /api/health, /api/ping, /api/notifications/send

### Desktop

- Built JavaFX app with FXML/CSS
- Organized MVC structure with controllers, services, utilities

### Android

- Restructured to package-by-feature architecture
- Integrated Firebase Realtime Database for notifications
- Fixed persistence initialization order issue
- Added connection state monitoring

### Infrastructure

- Configured gitignore for build artifacts and secrets
- Created README.md and android/STRUCTURE.md documentation
