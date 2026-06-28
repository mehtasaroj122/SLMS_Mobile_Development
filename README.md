# LMS Mobile App

## Overview

LMS Mobile App is an Android application for a Library Management System. It provides mobile portals for students and library staff to access library services such as searching books, requesting books, viewing issued books, managing fines, handling book issue/return workflows, viewing notifications, and updating profiles.

The app is built for the existing Laravel LMS backend API. The current active API base URL points to the hosted Laravel API, and the project also keeps a local Android Emulator API URL as a commented development option.

## Key Features

### Student Portal Features

- Splash screen with session check
- Student login with role-based navigation
- Remember Me for saved login email
- Complete registration for invited student accounts
- Forgot password flow
- Student dashboard with backend data
- Search/browse books
- View book details
- Request available books
- View current books, history books, and due-soon books
- View fines, pending fines, paid fines, and fine detail
- View, filter, and cancel own book requests
- Notifications list, unread count, mark as read, mark all read, and delete notification
- Profile view/edit
- Profile photo upload/remove
- Password change
- Account delete eligibility/deactivation flow
- More section with Profile, Notifications, My Requests, Dark Mode, and Logout
- Session persistence through DataStore
- Room-backed cache for previously loaded read-only data
- Network state handling with offline messages and online refresh
- Dark mode toggle with persisted preference

### Staff Portal Features

- Staff login with role-based navigation
- Complete registration for invited staff accounts
- Staff dashboard with backend data and quick actions
- Issue book workflow with student search, student issue context, book search, and multi-book issue
- Return book workflow with return settings, student/active issue search, fine preview, and return processing
- Student management/list screen
- Student detail view with issued books, fines, and book requests
- Staff fines overview, student-wise fine list, fine detail, mark paid, and waive fine
- Staff book requests list, summary, approve, and reject
- Notifications list, unread count, mark as read, mark all read, and delete notification
- Staff profile view/edit
- Profile photo upload/remove
- Password change
- Staff account deactivation flow
- More section with Book Requests, Students, Profile, Notifications, Dark Mode, and Logout
- Room-backed cache for many previously loaded read-only staff screens
- Network state handling with offline messages and online refresh
- Dark mode toggle with persisted preference

### Admin Portal Status

Admin routing and bottom navigation are present. The admin portal currently has Dashboard, Books, Students, Issues, and Fines tabs. The Books tab uses the shared book list implementation; the other admin fragments are currently lightweight/placeholder screens.

## Tech Stack

- Kotlin and Java
- Android SDK with XML layouts
- MVVM-style UI structure using Activities, Fragments, ViewModels, Repositories, and model classes
- Retrofit 2.9.0
- OkHttp logging interceptor 4.11.0
- Gson 2.10.1
- Kotlin Coroutines 1.7.3
- LiveData and ViewModel from AndroidX Lifecycle 2.6.2
- DataStore Preferences 1.0.0
- Room 2.6.1 for local API response cache
- Material Components 1.14.0
- AppCompat, ConstraintLayout, RecyclerView, CardView, Fragment KTX, Activity KTX
- SwipeRefreshLayout
- Laravel backend API using bearer token authentication; backend docs describe Sanctum tokens

No Jetpack Compose, Navigation Component, Hilt, Coil, Glide, or Picasso dependency is currently declared.

## Architecture

```text
UI Layer
Activities / Fragments / XML layouts
        |
        v
ViewModel Layer
LiveData state + user actions
        |
        v
Repository Layer
API calls, parsing, cache reads/writes, offline guards
        |
        v
Remote + Local Data
Retrofit API / Room api_cache / DataStore preferences
        |
        v
Laravel LMS Backend
```

- UI classes display data, handle clicks, and route between screens.
- ViewModels hold screen state and call repositories.
- Repositories call Retrofit services, map API responses into UI models, and use Room cache where implemented.
- `AuthInterceptor` adds the saved bearer token to protected requests.
- `NetworkMonitor` tracks connectivity and `BaseActivity` shows offline/online messages.
- DataStore stores auth/session data, remembered email, and dark mode preference.

## Folder Structure

```text
SLMS_Mobile_Development/
+-- app/
|   +-- build.gradle.kts
|   +-- proguard-rules.pro
|   +-- src/
|       +-- main/
|       |   +-- AndroidManifest.xml
|       |   +-- java/com/saroj/lmsmobile/
|       |   |   +-- MainApplication.kt
|       |   |   +-- MainActivity.kt
|       |   |   +-- api/
|       |   |   |   +-- ApiService.kt
|       |   |   |   +-- RetrofitClient.kt
|       |   |   |   +-- interceptor/AuthInterceptor.kt
|       |   |   +-- data/
|       |   |   |   +-- local/
|       |   |   |   |   +-- LmsDatabase.java
|       |   |   |   |   +-- cache/
|       |   |   |   +-- models/
|       |   |   |   +-- repository/
|       |   |   +-- network/
|       |   |   +-- storage/
|       |   |   +-- ui/
|       |   |   |   +-- admin/
|       |   |   |   +-- auth/
|       |   |   |   +-- books/
|       |   |   |   +-- common/
|       |   |   |   +-- components/
|       |   |   |   +-- staff/
|       |   |   |   +-- student/
|       |   |   |   +-- theme/
|       |   |   +-- utils/
|       |   +-- res/
|       |       +-- color/
|       |       +-- drawable/
|       |       +-- drawable-v24/
|       |       +-- layout/
|       |       +-- menu/
|       |       +-- mipmap-*/
|       |       +-- values/
|       |       +-- values-night/
|       |       +-- xml/
|       +-- androidTest/
|       +-- test/
+-- docs/
+-- gradle/
|   +-- libs.versions.toml
|   +-- wrapper/
+-- build.gradle.kts
+-- settings.gradle.kts
+-- gradle.properties
+-- gradlew
+-- gradlew.bat
+-- README.md
```

Package name/application ID: `com.saroj.lmsmobile`

App label: `SLMS Mobile`

## API Configuration

The API base URL is configured in:

```text
app/src/main/java/com/saroj/lmsmobile/utils/Constants.kt
```

Current active URL:

```kotlin
const val BASE_URL = "https://lms.saroj00.com.np/api/"
```

Commented local Android Emulator option:

```kotlin
// const val BASE_URL = "http://10.0.2.2:8000/api/"
```

`RetrofitClient.kt` uses `Constants.BASE_URL` when creating Retrofit:

```kotlin
Retrofit.Builder()
    .baseUrl(Constants.BASE_URL)
```

`network_security_config.xml` and the manifest allow cleartext HTTP so local Laravel testing over `http://10.0.2.2:8000/api/` can work.

## Authentication

- `SplashActivity` is the launcher activity.
- On launch, it waits for the splash duration, reads the token and role from DataStore, then routes to Admin, Staff, Student, or Login.
- `LoginActivity` validates email/password and calls `LoginViewModel`.
- `LoginViewModel` calls `AuthRepository.login()`.
- `AuthRepository` sends `POST /api/login`, normalizes the user role, stores the access token and user info through `TokenManager`, and returns success/error state.
- `AuthInterceptor` attaches `Authorization: Bearer <token>` plus JSON headers to protected requests.
- Roles are normalized in `Constants.normalizeRole()`.
- Student users open `StudentDashboardActivity`.
- Staff users open `StaffDashboardActivity`.
- Admin users open `AdminDashboardActivity`.
- Logout clears token/session data and Room cache through `TokenManager.clearAllData()`, while preserving remembered email when Remember Me was enabled.
- Remember Me stores only the remembered email and enabled flag, not the password.
- `ForgotPasswordActivity` calls `POST /api/forgot-password`.
- `CompleteRegistrationActivity` calls `POST /api/auth/complete-registration` for invited student/staff accounts.

## Offline / Local Database Support

Offline support is partially implemented through Room cache and network monitoring.

Implemented:

- Room database: `lms_mobile_cache.db`
- Cache table: `api_cache`
- Cache classes:
  - `LmsDatabase.java`
  - `ApiCacheEntity.java`
  - `ApiCacheDao.java`
  - `LocalCacheRepository.kt`
  - `LocalCacheProvider.kt`
- Network monitor:
  - `NetworkMonitor.kt`
  - `NetworkStatusInterceptor.kt`
  - `NoConnectivityException.kt`
- Offline helpers:
  - `OfflineRepositorySupport.kt`
  - `OfflineUi.kt`
  - `OnlineRefreshable.kt`

Cached/read-only data includes many book, dashboard, notification, profile, student, student-my-books, student-fines, student-requests, staff dashboard, staff students, staff fines, and staff book request responses.

Limitations:

- This is not a full offline sync system.
- Write actions such as login, registration, book requests, issue/return, fine payment/waive, profile updates, and deletes require a network connection.
- Offline screens can only show data that was previously fetched and cached.
- When the app comes back online, `BaseActivity` attempts to refresh the current `OnlineRefreshable` screen.

## Dark Mode

Dark mode is implemented.

- Theme resources exist in `values/` and `values-night/`.
- `ThemePreferenceManager` stores the selected mode in DataStore using `app_theme_mode`.
- `MainApplication` applies the saved theme at startup.
- `DarkModeToggleBinder` binds the dark mode row/switch in the More UI.
- Student More and Staff More include the dark mode toggle.

## Screens / Modules

Authentication:

- Splash
- Login
- Forgot Password
- Complete Registration
- Unauthorized/session expired

Student Portal:

- Dashboard
- Search Books
- Book Details
- My Books
- My Fines
- My Requests
- Notifications
- Profile
- More

Staff Portal:

- Dashboard
- Issue Book
- Return Book
- Fines
- Fine Detail
- Book Requests
- Students
- Student Detail
- Notifications
- Profile
- More

Admin Portal:

- Dashboard
- Books
- Students
- Issues
- Fines

Admin portal note: Admin navigation is present, but most admin fragments are still placeholder-level except the shared books list.

## Main API Areas Used

The Retrofit interface is defined in `ApiService.kt`. Important API groups include:

- Auth: `login`, `logout`, `profile`, `forgot-password`, `auth/complete-registration`
- Books: `books`, `books/search`, `books/{id}`, `books/available`, `books/category/{category}`
- Student: `student/dashboard`, `student/my-books/*`, `student/fines/*`, `student/requests/*`
- Staff: `staff/dashboard`, `staff/students/*`, `staff/books/search`, `staff/issues`, `staff/returns/*`, `staff/book-requests/*`, `staff/fines/*`
- Notifications: `notifications`, `notifications/unread`, `notifications/count`, read/delete endpoints
- Shared staff/admin: `students`, `issues`, `fines`, `overdue`, `book-requests`

The detailed backend API reference is available in `docs/mobile-api-documentation.md`.

## Installation and Setup

1. Clone the repository:

```bash
git clone <repository-url>
cd SLMS_Mobile_Development
```

2. Open the project in Android Studio.

3. Wait for Gradle sync to complete.

4. Check the API base URL in:

```text
app/src/main/java/com/saroj/lmsmobile/utils/Constants.kt
```

5. If using the hosted API, keep:

```kotlin
const val BASE_URL = "https://lms.saroj00.com.np/api/"
```

6. If using a local Laravel backend with Android Emulator, switch to:

```kotlin
const val BASE_URL = "http://10.0.2.2:8000/api/"
```

7. For local Laravel development, run the backend separately:

```bash
php artisan serve
```

8. Build or run the app:

```bash
./gradlew assembleDebug
```

On Windows:

```bash
.\gradlew.bat assembleDebug
```

## Localhost API Notes

For Android Emulator:

```text
http://10.0.2.2:8000/api/
```

For a physical Android device:

```text
http://YOUR_PC_IP:8000/api/
```

For hosted API mode:

```text
https://lms.saroj00.com.np/api/
```

Notes:

- Retrofit base URLs must end with `/`.
- Keep only one `BASE_URL` active at a time.
- The Laravel backend must be running for API features.
- `localhost` from an Android Emulator points to the emulator itself, not your computer.
- Use `10.0.2.2` for emulator-to-computer localhost access.
- Physical devices need your computer's LAN IP address.

## Required Permissions

Declared in `AndroidManifest.xml`:

- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`

## Dependencies

Main dependencies from `gradle/libs.versions.toml` and `app/build.gradle.kts`:

- Android Gradle Plugin 9.2.1
- AndroidX Core KTX 1.10.1
- AppCompat 1.7.1
- Material Components 1.14.0
- Activity KTX 1.13.0
- Fragment KTX 1.6.1
- ConstraintLayout 2.2.1
- Retrofit 2.9.0
- Gson converter for Retrofit 2.9.0
- Gson 2.10.1
- OkHttp logging interceptor 4.11.0
- Lifecycle ViewModel/LiveData/Runtime 2.6.2
- Kotlin Coroutines 1.7.3
- DataStore Preferences 1.0.0
- Room Runtime/KTX/Compiler 2.6.1
- RecyclerView 1.3.2
- CardView 1.0.0
- SwipeRefreshLayout 1.1.0
- JUnit, AndroidX JUnit, Espresso for tests

## Build Requirements

- Android Studio with support for Gradle 9.x / AGP 9.x
- Gradle wrapper: 9.4.1
- Android Gradle Plugin: 9.2.1
- Compile SDK: 36.1
- Minimum SDK: 23
- Target SDK: 36
- Java source/target compatibility: Java 11
- Language: Kotlin and Java

## Environment / Configuration Notes

- API URL and global constants: `Constants.kt`
- Retrofit/OkHttp setup: `RetrofitClient.kt`
- Bearer token injection: `AuthInterceptor.kt`
- Session and Remember Me storage: `TokenManager.kt`
- Shared DataStore name: `lms_mobile_app_prefs`
- Dark mode preference: `ThemePreferenceManager.kt`
- Room cache database: `lms_mobile_cache.db`
- Network status tracking: `NetworkMonitor.kt`
- Cleartext HTTP policy: `network_security_config.xml`

Do not commit private API tokens, passwords, or local machine-specific settings. `local.properties` should remain local to each developer machine.

## Testing Checklist

- Build debug APK
- Launch app and verify Splash routing
- Login as student
- Login as staff
- Verify Remember Me stores the email only
- Test forgot password email flow
- Test complete registration for student/staff invitation data
- Verify student dashboard data
- Search books and open book details
- Request a book as student
- View My Books
- View My Fines and fine detail
- View/cancel My Requests
- Open notifications, mark read, mark all read, and delete
- Edit student profile and change password
- Test student account delete eligibility/deactivation flow
- Verify staff dashboard data
- Search student and issue books
- Return issued books and preview fines
- View staff fines, mark paid, and waive
- Approve/reject staff book requests
- Open staff student detail
- Edit staff profile and change password
- Test staff account deactivation flow
- Toggle dark mode from Student More and Staff More
- Turn off network after loading screens and verify cached data/offline messages
- Turn network back on and verify refresh behavior
- Logout and confirm session data clears

## Known Issues / Limitations

- Admin portal is only partially implemented; most admin tabs are placeholder-level.
- Offline support is cache-based only and depends on previously loaded data.
- Offline write/action flows are blocked; there is no queued sync.
- Push notifications are not implemented as Android push/Firebase notifications; notifications are fetched from backend API endpoints.
- Image loading is implemented manually in several adapters/screens with `URL.openStream()` and `BitmapFactory`; no dedicated image loading library is configured.
- API features require a compatible running Laravel backend.

## Future Improvements

- Complete the Admin mobile portal
- Add a dedicated image loading library such as Coil or Glide
- Add full offline sync with queued write operations
- Add Firebase/cloud push notifications
- Add biometric login
- Add stronger automated unit/UI test coverage
- Add advanced book filters and sorting across student/staff screens
- Add downloadable reports or exports for staff/admin workflows

## Documentation

Additional project documentation:

- `docs/mobile-api-documentation.md`
- `docs/mobile-online-localhost-switch-guide.md`
- `docs/AUTH_TESTING_GUIDE.md`
- `docs/BUILD_VERIFICATION_SUMMARY.md`
- `docs/LMS_APP_DOCUMENTATION.md`
- `docs/WHAT_WAS_CREATED.md`

Some older docs describe earlier implementation stages, so the root README should be treated as the current high-level project guide.

## Build Verification

Verified on this checkout:

```bash
.\gradlew.bat assembleDebug
```

Result:

```text
BUILD SUCCESSFUL
```

## Author / Developer

Developed by: Saroj Kumar Mehta

Project: Library Management System Mobile App
