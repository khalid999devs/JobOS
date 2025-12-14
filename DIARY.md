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

### Authentication & Security

- Implemented JWT-based auth with access tokens (15 min) and refresh tokens (30 days)
- Session-specific logout with unique sessionId per device (multi-device support)
- BCrypt password hashing, SHA-256 token hashing, HMAC-SHA512 JWT signing
- Complete error handling with ApiResponse wrapper and custom exceptions
