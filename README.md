# JobOS

**Job search, hiring, and CV management for desktop and Android.**

JobOS brings job seekers and recruiters into one workflow: build a CV, discover opportunities, submit applications, and review candidates. It started as a lab project and grew into a Java application with two native clients and a shared backend. The project is open source and welcomes anyone who wants to learn, contribute, improve it, or build a fork.

![JobOS desktop and Android applications](screenshots/thumbnail.png)

[Demo](#demo) · [Screen gallery](#screen-gallery) · [Features](#features) · [Architecture](#architecture) · [Getting started](#getting-started) · [Contributing](#contributing) · [MIT license](LICENSE)

## Demo

A 20-second screenshot tour of job discovery, applications, CV editing, recruiter review, and notifications.

![Animated tour of JobOS desktop and Android workflows](screenshots/jobos-demo.gif)

For a closer look, expand a gallery below and click any image to open the original screenshot.

## Screen gallery

The screenshots show desktop and Android views side by side where available. Each gallery follows a related part of the user journey.

<details>
<summary><strong>1. Getting into JobOS — welcome, registration, and onboarding</strong></summary>

Choose a role, create an account, and complete a seeker or company profile.

| Welcome | Desktop registration and seeker profile |
| --- | --- |
| [![Welcome screen](screenshots/1.png)](screenshots/1.png) | [![Desktop registration and seeker onboarding](screenshots/2.png)](screenshots/2.png) |
| **Company profile, sign-in, and password recovery** | **Android registration and role setup** |
| [![Desktop company onboarding, sign-in, and password recovery](screenshots/3.png)](screenshots/3.png) | [![Android registration, role selection, and profile setup](screenshots/4.png)](screenshots/4.png) |

</details>

<details open>
<summary><strong>2. Finding a job — discover, apply, and track progress</strong></summary>

Start at the seeker dashboard, browse opportunities, inspect a listing, and apply with a CV and cover letter. Return to application history to follow the outcome.

| Seeker dashboard | Browse and filter jobs |
| --- | --- |
| [![Seeker dashboard on desktop and Android](screenshots/9.png)](screenshots/9.png) | [![Job search on desktop and Android](screenshots/10.png)](screenshots/10.png) |
| **Job details** | **Submit an application** |
| [![Job requirements and details](screenshots/11.png)](screenshots/11.png) | [![CV selection and cover letter submission](screenshots/12.png)](screenshots/12.png) |
| **Application history** | **Application details** |
| [![Application list and statuses](screenshots/13.png)](screenshots/13.png) | [![Submitted application and attached CV details](screenshots/14.png)](screenshots/14.png) |

</details>

<details>
<summary><strong>3. Building a CV — templates, editing, and export</strong></summary>

Manage CVs, select a template, edit sections, and preview the exported document.

| CV manager and templates | CV editor |
| --- | --- |
| [![CV manager and template selection](screenshots/15.png)](screenshots/15.png) | [![Desktop CV editor with section controls and preview](screenshots/16.png)](screenshots/16.png) |
| **Exported document preview** | |
| [![Exported CV opened in a PDF viewer](screenshots/17.png)](screenshots/17.png) | |

</details>

<details>
<summary><strong>4. Hiring candidates — manage jobs and review applicants</strong></summary>

Use the recruiter dashboard to manage listings, inspect applications, review candidate CVs, and maintain a company profile.

| Recruiter dashboard | Job management |
| --- | --- |
| [![Recruiter dashboard on desktop and Android](screenshots/5.png)](screenshots/5.png) | [![Recruiter job listings and editing screens](screenshots/6.png)](screenshots/6.png) |
| **Applicant review** | **Recruiter and company profiles** |
| [![Applicant list, application details, and status updates](screenshots/7.png)](screenshots/7.png) | [![Recruiter profile and company information screens](screenshots/8.png)](screenshots/8.png) |

</details>

<details>
<summary><strong>5. Managing an account — plans, notifications, and settings</strong></summary>

View credits and plans, follow application updates, and adjust account preferences. The plans screen demonstrates the project's credit and subscription model; payment processing is simulated.

| Credits and plans | Notifications |
| --- | --- |
| [![Desktop credit balance and subscription plans](screenshots/18.png)](screenshots/18.png) | [![Notification history on desktop and Android](screenshots/19.png)](screenshots/19.png) |
| **Settings** | |
| [![Account and notification settings](screenshots/20.png)](screenshots/20.png) | |

</details>

## Features

| For job seekers | For recruiters |
| --- | --- |
| Search by location, salary, job type, work mode, and experience level | Create, edit, close, and reopen job posts |
| Save opportunities for later | Review applicants for each listing |
| Build CVs from templates and manage their sections | View candidate CVs from application details |
| Preview CVs and export them as PDFs | Update application status and follow hiring activity |
| Apply with a CV and cover letter; track application history | Maintain recruiter and company profiles |

Shared capabilities include JWT authentication with refresh tokens, device-specific sessions, role-based access checks, profile preferences, password recovery by email, and notification history. Android supports Firebase Cloud Messaging push notifications; the desktop client polls the API for updates.

Application statuses are `PENDING`, `REVIEWED`, `SHORTLISTED`, `ACCEPTED`, and `REJECTED`.

**Project scope:** JobOS is a lab-origin project available for continued development. Credit balances, template unlocking, and subscription records are implemented, while purchases use simulated flows without a payment gateway. Screenshots contain demonstration data and UI copy, including sample usage counts and plan benefits.

## Architecture

Both clients communicate with the same REST API. The backend owns authentication, authorization, business rules, and persistence; each client handles its native interface and local PDF generation.

```mermaid
flowchart LR
    Desktop[JavaFX desktop] -->|HTTP / JSON| API[Spring Boot REST API]
    Android[Native Android] -->|HTTP / JSON| API
    API --> DB[(PostgreSQL)]
    API --> FCM[Firebase Cloud Messaging]
    FCM -->|Push notifications| Android
    API --> SMTP[SMTP email]
```

| Component | Implementation |
| --- | --- |
| Backend | Java 25, Spring Boot 4.0.0, Spring Security, Spring Data JPA, PostgreSQL |
| Desktop | Java 25, JavaFX 25.0.1, FXML/CSS, OkHttp, Jackson, Apache PDFBox |
| Android | Native Java, AndroidX, Material Components, View Binding, OkHttp, Firebase Messaging; Android 7.0 / API 24 or newer |
| Shared contracts | Java DTOs and validation annotations used by the backend and desktop |
| Build | Gradle wrapper; root multi-project build plus a separate Android build |

The backend follows a controller → service → repository structure. Desktop views use FXML and controllers; Android organizes activities, fragments, and view models by feature. Android maintains its own API models rather than depending on the root `shared` module.

```text
JobOS/
├── backend/src/main/
│   ├── java/com/jobos/backend/   # Controllers, services, entities, repositories, security
│   └── resources/               # Application configuration and email templates
├── desktop/src/main/
│   ├── java/com/jobos/desktop/   # JavaFX controllers, API services, and application state
│   └── resources/               # FXML views, CSS, and desktop configuration
├── android/                     # Independent Gradle project; open in Android Studio
│   └── app/src/main/            # Native UI, network layer, models, and resources
├── shared/src/main/java/        # Backend and desktop DTOs
├── screenshots/                 # Product screenshots, thumbnail, and demo GIF
├── DIARY.md                     # Development history
└── LICENSE                      # MIT license
```

## Getting started

### Prerequisites

- **JDK 25** for the backend, desktop, and shared modules.
- **PostgreSQL** running locally, with permission to create a database and role.
- **Android Studio and Android SDK 36** for the Android app. Use [JDK 17 for its Gradle build](https://developer.android.com/build/releases/agp-8-13-0-release-notes); Android source compatibility is Java 11.
- A **Firebase project** for Android configuration and push notifications. Firebase is optional when running only the backend and desktop.
- An **SMTP account** if you want to exercise email password recovery.

The repository includes Gradle wrappers; no global Gradle installation is required.

```bash
git clone https://github.com/khalid999devs/JobOS.git
cd JobOS
```

### 1. Create the database

Connect to PostgreSQL as an administrator and run:

```sql
CREATE USER jobos WITH PASSWORD 'replace-with-your-local-password';
CREATE DATABASE jobos OWNER jobos;
```

The backend currently uses Hibernate's `ddl-auto: update` to create and update tables. Startup seeders populate CV templates and subscription plans when those tables are empty.

### 2. Configure and start the backend

Set these variables in the terminal where you will run the backend. They override the local defaults in [application.yml](backend/src/main/resources/application.yml).

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/jobos'
export SPRING_DATASOURCE_USERNAME='jobos'
export SPRING_DATASOURCE_PASSWORD='replace-with-your-local-password'
export JWT_SECRET="$(openssl rand -hex 32)"

./gradlew :backend:bootRun
```

Keep the same `JWT_SECRET` between runs if you want previously issued tokens to remain valid. These instructions use shell environment variables; the application does not automatically load a root `.env` file.

The API listens at `http://localhost:8080`. In another terminal, check that it responds:

```bash
curl http://localhost:8080/api/health
```

Optional integrations, configured before starting the backend:

| Variable | Purpose |
| --- | --- |
| `FIREBASE_SERVICE_ACCOUNT_JSON_PATH` | Absolute path to your Firebase service-account JSON file. If unset, backend Firebase features are disabled. |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP credentials for password-recovery emails. The default transport is Gmail SMTP on port 587. |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` | Override the SMTP host and port for another provider. |

Keep service-account files under the ignored `firebase-keys/` directory and keep credentials out of commits. [FirebaseConfig.java](backend/src/main/java/com/jobos/backend/config/FirebaseConfig.java) also contains a project-specific Realtime Database URL; update it if you use your own Firebase database.

### 3. Run the desktop app

From a new terminal at the repository root:

```bash
./gradlew :desktop:run
```

The desktop client defaults to `http://localhost:8080`. Set `JOBOS_API_BASE_URL` to use another backend, and `JOBOS_POLL_NOTIF_SECONDS` to change the default 30-second notification polling interval.

### 4. Run the Android app

1. Open the **`android/` directory** in Android Studio.
2. Register an Android app with package name `com.jobos.android` in your Firebase project and place its `google-services.json` in `android/app/`.
3. Install Android SDK 36 and let Gradle sync finish.
4. Run on an emulator or a device with Android 7.0 or newer.

[ApiConfig.java](android/app/src/main/java/com/jobos/android/config/ApiConfig.java) defaults to `http://10.0.2.2:8080`, which reaches the host machine from the Android emulator. For a physical device, change `BASE_URL` to your development machine's reachable LAN address and keep both devices on the same network.

### 5. Try the full workflow

Create separate recruiter and seeker accounts through the app. As the recruiter, complete a company profile and publish a job. As the seeker, create a CV, find the job, and submit an application. Return to the recruiter account to review the CV and update the application status, then check the seeker's application history and notifications.

## API overview

Routes use the `/api` prefix. Most require an access token in the `Authorization: Bearer <token>` header; the backend also checks user roles and resource ownership.

| Route | Purpose |
| --- | --- |
| `GET /api/health` | Public health response |
| `/api/auth/*` | Registration, login, token refresh, logout, and password recovery |
| `/api/users/me`, `/api/users/me/preferences` | Profile and role-specific preferences |
| `/api/job-posts` | Recruiter job creation and management |
| `GET /api/job-posts/{id}/applicants` | Applicants for a recruiter's job |
| `POST /api/jobs/search`, `GET /api/jobs/{id}` | Job discovery and details |
| `/api/jobs/saved`, `/api/jobs/{id}/save` | Saved jobs |
| `POST /api/applications`, `GET /api/applications` | Submit and list applications |
| `PATCH /api/applications/{id}/status` | Update an application's status |
| `GET /api/applications/{applicationId}/cv` | Recruiter access to an applicant's CV |
| `/api/cvs`, `/api/cvs/{cvId}/sections` | CVs and their sections |
| `/api/cv-templates` | Template catalog and unlocking |
| `/api/notifications` | Notification history, read state, preferences, and FCM token registration |
| `/api/credits`, `/api/plans` | Credit balances, transactions, and demonstration subscriptions |

See the [controllers](backend/src/main/java/com/jobos/backend/controller) for supported methods and the [shared DTOs](shared/src/main/java/com/jobos/shared/dto) for request and response contracts.

## Development

Build the backend, desktop, and shared modules from the repository root:

```bash
./gradlew clean build
```

For a focused compilation check:

```bash
./gradlew :backend:compileJava :desktop:compileJava
```

Android has its own wrapper and build lifecycle. After configuring its SDK and Firebase file:

```bash
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Automated test coverage is currently limited: the root modules have no committed test suites, and Android contains starter tests. A successful build is a compilation and packaging check; verify changes through the affected seeker and recruiter flows as well. The [development diary](DIARY.md) records the project's implementation history.

## Contributing

JobOS is open to everyone. Bug reports, documentation fixes, accessibility improvements, tests, new features, and independent forks are welcome.

1. [Open an issue](https://github.com/khalid999devs/JobOS/issues) to report a bug or discuss a substantial change. Include reproduction steps and relevant platform details.
2. Fork the repository and create a branch for your change.
3. Keep changes focused, follow the surrounding code conventions, and update documentation when behavior or setup changes.
4. Build the affected modules, verify the relevant workflows, and add meaningful tests where applicable.
5. [Open a pull request](https://github.com/khalid999devs/JobOS/pulls) describing the problem, the change, and how you validated it. Include screenshots for UI changes.

Useful starting points include automated integration tests, reproducible development setup, accessibility, and improvements to CV editing and cross-platform behavior.

## License

JobOS is licensed under the [MIT License](LICENSE). You may use, study, modify, fork, distribute, and build on it, including for commercial purposes, provided you retain the copyright and permission notices. See the [standard MIT terms](https://opensource.org/license/mit) or the repository's license file for the full text.
