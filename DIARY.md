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

## December 22, 2025

### Profile & Preferences System

- Extended User entity with profile fields (name, phone, avatar, bio, location, timezone)
- Created role-specific preferences (SeekerPreferences and PosterProfile entities)
- Built JSON storage for flexible arrays (skills, roles, job types, documents)
- Added profileCompleted flag for frontend onboarding detection
- Implemented AuthenticatedUser principal (userId + email accessible in controllers)
- Role-aware responses automatically populate seeker or poster data based on user type

## January 10, 2026

### Desktop Application Complete

- Finished full implementation of all desktop features
- Built seeker flows: dashboard, job search with filters, applications list, CV builder
- Built poster flows: dashboard with stats, job management, applicant review
- Added CV preview with PDF export
- Implemented settings and profile editing
- Made sure all API calls are synced with backend

## January 12, 2026

### Android Application Complete

- Finished full Android implementation matching desktop features
- Built all seeker screens: home with recommendations, job search, saved jobs, applications, CV management
- Built all poster screens: dashboard, job listings, applicant details with CV viewing
- Added PDF generation for CVs directly on device
- Integrated Firebase Cloud Messaging for push notifications
- FCM token registration on login and app startup

### Android Fixes and Enhancements

- Fixed dashboard to show user's first name instead of email
- Fixed CV preview to show all sections (skills, experience, education) when data exists
- Fixed PDF generation to use actual CV data instead of placeholders
- Added seeker job preferences to edit profile (job types, work modes, experience levels, salary range)
- Added timezone selector to profile
- Fixed applicant CV viewing for posters using the correct endpoint
- Added navigation actions for CV preview from applicant details
