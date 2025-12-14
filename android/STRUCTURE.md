# Android App Structure

## Package Organization (Production-Ready)

```
com.jobos.android/
├── JobOSApplication.java          # Application class (Firebase init)
├── config/                         # Configuration classes
│   └── ApiConfig.java             # API endpoints configuration
├── data/                          # Data layer
│   ├── model/                     # Data models (DTOs/Entities)
│   │   ├── Notification.java
│   │   └── PingResponse.java
│   └── network/                   # Network/API clients
│       └── ApiClient.java
├── ui/                            # UI layer (Activities/Fragments)
│   ├── main/                      # Main screen
│   │   └── MainActivity.java
│   └── notifications/             # Notifications feature
│       └── NotificationAdapter.java
└── utils/                         # Utility classes (future)
```

## Resources Organization

```
res/
├── drawable/                      # Vector drawables, shapes
├── layout/                        # XML layouts
│   ├── activity_main.xml
│   └── item_notification.xml
├── mipmap-*/                      # App icons (all densities)
├── values/                        # Strings, colors, themes
│   ├── colors.xml
│   ├── strings.xml
│   └── themes.xml
├── values-night/                  # Dark theme
│   └── themes.xml
└── xml/                          # XML configs
    ├── backup_rules.xml
    ├── data_extraction_rules.xml
    └── network_security_config.xml
```

## Architecture Pattern

Currently: **Basic MVC** (suitable for simple apps)

For production scaling, consider:

- **MVVM** (Model-View-ViewModel) with LiveData
- **Repository Pattern** for data abstraction
- **Dependency Injection** (Hilt/Dagger)
- **Navigation Component** for multi-screen apps

## Build Configuration

- **Version Catalog**: `gradle/libs.versions.toml` (centralized dependencies)
- **Gradle KTS**: Type-safe build configuration
- **Min SDK**: 24 (Android 7.0+)
- **Target SDK**: 36 (Latest)

## Firebase Setup

- **Realtime Database**: For live notification sync
- **google-services.json**: gitignored (download from Firebase Console)
- **Rules**: Set in Firebase Console for read/write permissions

## Mipmap Folders

The `mipmap-*` folders contain app icons for different screen densities:

- `mipmap-mdpi`: ~160dpi (low density)
- `mipmap-hdpi`: ~240dpi (medium density)
- `mipmap-xhdpi`: ~320dpi (high density)
- `mipmap-xxhdpi`: ~480dpi (extra high)
- `mipmap-xxxhdpi`: ~640dpi (extra extra high)
- `mipmap-anydpi-v26`: Adaptive icon (Android 8.0+)

**Standard practice**: Keep all mipmap folders - Android automatically picks the right icon based on device density.

## Future Scalability

### Recommended additions for production:

1. **Repository layer** (`data/repository/`)
2. **ViewModels** (`ui/*/viewmodel/`)
3. **Use cases** (`domain/usecase/`)
4. **Dependency Injection** (Hilt)
5. **Room Database** for offline support
6. **WorkManager** for background tasks
7. **Retrofit** instead of raw OkHttp
8. **Unit tests** (`test/`)
9. **UI tests** (`androidTest/`)

### Code quality tools:

- **ktlint** or **Detekt** for linting
- **LeakCanary** for memory leak detection
- **Timber** for logging
- **Crashlytics** for crash reporting

## Notes

- Structure follows **Clean Architecture principles**
- Easy to migrate to MVVM when needed
- Package-by-feature organization
- Separation of concerns (UI, Data, Config)
