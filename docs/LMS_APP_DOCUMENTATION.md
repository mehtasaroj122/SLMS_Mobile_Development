# SLMS Mobile Development - Complete LMS Android App

## 📱 Project Overview

A professional, scalable **Library Management System (LMS)** mobile application built for Android using Kotlin, MVVM architecture, and REST API integration with Laravel backend.

**Current Status**: ✅ Complete architecture and core infrastructure implemented
**Target**: Production-ready mobile app for Admin, Staff, and Student users

---

## 🏗️ Architecture Overview

### MVVM + Hybrid Activity/Fragment Architecture

```
UI Layer (Activities + Fragments)
    ↓
ViewModel Layer (Business Logic)
    ↓
Repository Layer (Data Source)
    ↓
API Layer (Retrofit + Interceptors)
    ↓
DataStore (Token/Session Storage)
    ↓
REST API (Laravel Backend)
```

### Layer Breakdown

**1. UI Layer**
- `Activities`: SplashActivity, LoginActivity, Dashboard Activities
- `Fragments`: Module-specific screens (Books, Students, Issues, Fines, Profile)
- `Adapters`: RecyclerView adapters for lists
- `Layouts`: XML files for all screens

**2. ViewModel Layer**
- Business logic and state management
- LiveData for reactive UI updates
- Input validation and error handling

**3. Repository Layer**
- Single source of truth for data
- API calls encapsulation
- Error handling and response mapping

**4. API Layer**
- Retrofit service interface
- HTTP interceptors for auth tokens
- JSON serialization/deserialization

**5. Storage Layer**
- DataStore for encrypted token storage
- Session management
- User information caching

---

## 📁 Project Structure

```
app/src/main/
├── java/com/saroj/lmsmobile/
│   ├── api/
│   │   ├── ApiService.kt              (Retrofit endpoints)
│   │   ├── RetrofitClient.kt          (Retrofit configuration)
│   │   └── interceptor/
│   │       └── AuthInterceptor.kt     (Bearer token injection)
│   │
│   ├── data/
│   │   ├── models/
│   │   │   ├── auth/
│   │   │   │   └── AuthModels.kt      (LoginRequest, LoginResponse, User, Profile)
│   │   │   ├── book/
│   │   │   │   └── BookModels.kt      (Book, BookRequest)
│   │   │   ├── student/
│   │   │   │   └── StudentModels.kt   (Student, StudentResponse)
│   │   │   ├── issue/
│   │   │   │   └── IssueModels.kt     (Issue, IssueRequest)
│   │   │   ├── fine/
│   │   │   │   └── FineModels.kt      (Fine, FineResponse)
│   │   │   ├── dashboard/
│   │   │   │   └── DashboardModels.kt (Dashboard statistics)
│   │   │   └── common/
│   │   │       ├── NetworkResult.kt   (Result wrapper for API responses)
│   │   │       ├── PaginatedResponse.kt
│   │   │       └── ErrorResponse.kt
│   │   │
│   │   └── repository/
│   │       ├── AuthRepository.kt      (Login, logout, profile)
│   │       ├── BookRepository.kt      (Book CRUD operations)
│   │       ├── StudentRepository.kt   (Student CRUD operations)
│   │       ├── IssueRepository.kt     (Issue/borrowing operations)
│   │       ├── FineRepository.kt      (Fine management)
│   │       ├── DashboardRepository.kt (Dashboard data)
│   │       └── ProfileRepository.kt   (User profile)
│   │
│   ├── storage/
│   │   ├── TokenManager.kt            (DataStore for token/session)
│   │   ├── SessionManager.kt          (User session state)
│   │   └── PreferencesHelper.kt       (General preferences)
│   │
│   ├── ui/
│   │   ├── auth/
│   │   │   ├── SplashActivity.kt      (Entry point with role-based routing)
│   │   │   ├── LoginActivity.kt       (Login form)
│   │   │   └── viewmodel/
│   │   │       └── LoginViewModel.kt  (Login logic)
│   │   │
│   │   ├── admin/
│   │   │   ├── AdminDashboardActivity.kt
│   │   │   └── fragments/
│   │   │       ├── AdminDashboardFragment.kt
│   │   │       ├── AdminBooksFragment.kt
│   │   │       ├── AdminStudentsFragment.kt
│   │   │       ├── AdminIssuesFragment.kt
│   │   │       └── AdminFinesFragment.kt
│   │   │
│   │   ├── staff/
│   │   │   ├── StaffDashboardActivity.kt
│   │   │   └── fragments/
│   │   │       ├── StaffDashboardFragment.kt
│   │   │       ├── StaffBooksFragment.kt
│   │   │       ├── StaffStudentsFragment.kt
│   │   │       ├── StaffIssuesFragment.kt
│   │   │       └── StaffFinesFragment.kt
│   │   │
│   │   ├── student/
│   │   │   ├── StudentDashboardActivity.kt
│   │   │   └── fragments/
│   │   │       ├── StudentDashboardFragment.kt
│   │   │       ├── StudentSearchBooksFragment.kt
│   │   │       ├── StudentMyBooksFragment.kt
│   │   │       ├── StudentMyFinesFragment.kt
│   │   │       └── StudentProfileFragment.kt
│   │   │
│   │   ├── common/
│   │   │   ├── BookDetailActivity.kt
│   │   │   ├── StudentDetailActivity.kt
│   │   │   ├── ProfileActivity.kt
│   │   │   └── UnauthorizedActivity.kt
│   │   │
│   │   ├── adapter/
│   │   │   ├── BookAdapter.kt
│   │   │   ├── StudentAdapter.kt
│   │   │   ├── IssueAdapter.kt
│   │   │   ├── FineAdapter.kt
│   │   │   ├── BookRequestAdapter.kt
│   │   │   └── DashboardCardAdapter.kt
│   │   │
│   │   └── components/
│   │       ├── BaseActivity.kt        (Base class for all activities)
│   │       ├── LoadingDialog.kt
│   │       ├── ErrorDialog.kt
│   │       └── EmptyStateView.kt
│   │
│   ├── utils/
│   │   ├── Constants.kt               (API URLs, app constants)
│   │   ├── DateUtils.kt               (Date formatting)
│   │   ├── ValidationUtils.kt         (Input validation)
│   │   ├── ToastHelper.kt             (Toast messages)
│   │   ├── LogUtils.kt                (Logging)
│   │   └── Extensions.kt              (Kotlin extensions)
│   │
│   └── MainApplication.kt             (Application class)
│
└── res/
    ├── layout/
    │   ├── activity_splash.xml
    │   ├── activity_login.xml
    │   ├── activity_admin_dashboard.xml
    │   ├── activity_staff_dashboard.xml
    │   ├── activity_student_dashboard.xml
    │   ├── activity_unauthorized.xml
    │   ├── fragment_admin_*.xml (5 fragments)
    │   ├── fragment_staff_*.xml (5 fragments)
    │   └── fragment_student_*.xml (5 fragments)
    │
    ├── drawable/
    │   ├── bg_button_primary.xml
    │   ├── bg_input_field.xml
    │   ├── bg_card.xml
    │   ├── ic_dashboard.xml
    │   ├── ic_books.xml
    │   ├── ic_student.xml
    │   ├── ic_issue.xml
    │   ├── ic_fine.xml
    │   ├── ic_search.xml
    │   ├── ic_profile.xml
    │   ├── ic_logout.xml
    │   ├── ic_error.xml
    │   └── ic_visibility.xml
    │
    ├── values/
    │   ├── colors.xml        (Light mode colors)
    │   ├── strings.xml       (String resources)
    │   ├── themes.xml        (Light theme)
    │   └── dimens.xml        (Dimensions)
    │
    ├── values-night/
    │   ├── colors.xml        (Dark mode colors)
    │   └── themes.xml        (Dark theme)
    │
    ├── menu/
    │   ├── menu_main.xml                   (Toolbar menu)
    │   ├── menu_admin_bottom_navigation.xml
    │   ├── menu_staff_bottom_navigation.xml
    │   └── menu_student_bottom_navigation.xml
    │
    └── xml/
        ├── network_security_config.xml  (Cleartext HTTP config)
        ├── data_extraction_rules.xml
        └── backup_rules.xml
```

---

## 🔐 Authentication Flow

### Login Process

1. **SplashActivity**: 
   - Checks if token exists in DataStore
   - If yes → Navigate to appropriate dashboard
   - If no → Navigate to LoginActivity

2. **LoginActivity**:
   - User enters email and password
   - ViewModel validates inputs
   - Repository calls API with credentials

3. **AuthRepository.login()**:
   - Makes POST request to `/api/login`
   - Receives token and user info
   - Saves token to DataStore via TokenManager
   - Returns LoginResponse

4. **Role-Based Navigation**:
   - Reads user role from DataStore
   - Navigates to:
     - `AdminDashboardActivity` if role = admin
     - `StaffDashboardActivity` if role = staff
     - `StudentDashboardActivity` if role = student

### Token Management

- **AuthInterceptor** automatically adds `Authorization: Bearer <token>` to all requests
- If API returns 401 Unauthorized:
  - Token is cleared
  - User is redirected to UnauthorizedActivity
  - User must login again

---

## 🔌 API Integration

### Base Configuration

```kotlin
// Constants.kt
BASE_URL = "http://10.0.2.2:8000/api/"  // Emulator
// Or: "http://YOUR_IP:8000/api/"       // Physical device
```

### API Endpoints Implemented

**Authentication**
- `POST /api/login` - Login with email/password
- `POST /api/logout` - Logout user
- `GET /api/profile` - Get user profile

**Books**
- `GET /api/books` - Get all books (paginated)
- `GET /api/books/{id}` - Get book details
- `GET /api/books/search?q=value` - Search books
- `GET /api/books/available` - Get available books
- `GET /api/books/category/{category}` - Get books by category

**Students**
- `GET /api/students` - Get all students
- `GET /api/students/{id}` - Get student details
- `GET /api/students/search?q=value` - Search students

**Issues/Borrowing**
- `POST /api/issues` - Create new issue (borrow book)
- `GET /api/issues` - Get all issues
- `GET /api/issues/{id}` - Get issue details
- `POST /api/issues/return/{id}` - Return book
- `GET /api/issues/student/{studentId}` - Get student's issues

**Fines**
- `GET /api/fines` - Get all fines
- `GET /api/fines/student/{id}` - Get student's fines

**Dashboard**
- `GET /api/dashboard` - Get dashboard statistics

---

## 🎯 Data Flow Example: Login

```
User enters email & password
         ↓
LoginActivity.performLogin()
         ↓
viewModel.login(email, password)
         ↓
authRepository.login(email, password)
         ↓
apiService.login(LoginRequest)
         ↓
HTTP POST /api/login
         ↓
Laravel API processes
         ↓
Returns 200 + LoginResponse
         ↓
tokenManager.saveToken()
tokenManager.saveUserInfo()
         ↓
Emit NetworkResult.Success(LoginResponse)
         ↓
ViewModel updates LoginActivity LiveData
         ↓
LoginActivity observes Success state
         ↓
Navigate to role-specific Dashboard
```

---

## 🛠️ Technologies & Dependencies

### Core Android
- `androidx.appcompat` - Backward compatibility
- `androidx.activity-ktx` - Activity extensions
- `androidx.fragment-ktx` - Fragment extensions
- `androidx.lifecycle` - MVVM components
- `com.google.android.material` - Material Design

### Networking
- `com.squareup.retrofit2` - REST client
- `com.squareup.retrofit2:converter-gson` - JSON serialization
- `com.squareup.okhttp3:logging-interceptor` - HTTP logging

### Async & Coroutines
- `org.jetbrains.kotlinx:kotlinx-coroutines-core` - Coroutines
- `org.jetbrains.kotlinx:kotlinx-coroutines-android` - Android support

### Storage
- `androidx.datastore:datastore-preferences` - Secure preferences

### UI Components
- `androidx.recyclerview` - RecyclerView
- `androidx.cardview` - CardView

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest version)
- Android SDK 23+ (Minimum)
- Gradle 8.0+
- Laravel backend running

### Setup Steps

1. **Clone Repository**
   ```bash
   git clone https://github.com/mehtasaroj122/SLMS_Mobile_Development.git
   cd SLMS_Mobile_Development
   ```

2. **Configure API Base URL**
   - Edit `app/src/main/java/com/saroj/lmsmobile/utils/Constants.kt`
   - Update `BASE_URL` to your Laravel server

3. **Build & Run**
   ```bash
   gradlew assembleDebug
   ```

4. **In Android Studio**
   - Open project
   - Run on emulator or device
   - Emulator will use `http://10.0.2.2:8000/api/`
   - Physical device uses your computer's IP

---

## 📋 Development Roadmap

### ✅ Phase 1: Foundation (COMPLETED)
- [x] Project architecture setup
- [x] Gradle dependencies
- [x] Retrofit + API configuration
- [x] DataStore token management
- [x] Authentication models
- [x] Login flow
- [x] Role-based navigation

### 🔄 Phase 2: Admin Module (IN PROGRESS)
- [ ] Dashboard with statistics
- [ ] Book management (CRUD)
- [ ] Student management (CRUD)
- [ ] Issue/borrowing management
- [ ] Fine management
- [ ] Reports generation

### 🔄 Phase 3: Staff Module
- [ ] Dashboard with quick stats
- [ ] Book listing and search
- [ ] Issue/return books
- [ ] Fine tracking
- [ ] Student view

### 🔄 Phase 4: Student Module
- [ ] Dashboard with personal stats
- [ ] Search and browse books
- [ ] View issued books
- [ ] Track fines and dues
- [ ] Profile management

### 🔄 Phase 5: Polish & Testing
- [ ] UI/UX improvements
- [ ] Animations and transitions
- [ ] Unit tests
- [ ] Integration tests
- [ ] Performance optimization

---

## 💡 Key Features

### Security
✅ Bearer token authentication (Laravel Sanctum)
✅ Encrypted DataStore for token storage
✅ Automatic token injection via interceptor
✅ Session expiration handling (401 redirects)

### User Experience
✅ Splash screen with logo
✅ Smooth login form with validation
✅ Real-time error messages
✅ Loading indicators
✅ Role-specific dashboards
✅ Bottom navigation for modules
✅ Dark mode support

### Architecture
✅ Clean MVVM separation
✅ Repository pattern for data
✅ LiveData for reactive updates
✅ Coroutines for async operations
✅ Modular fragment-based UI
✅ Centralized error handling

---

## 🎨 UI/UX Design

### Color Scheme
- **Primary**: #2196F3 (Blue)
- **Primary Dark**: #1976D2 (Dark Blue)
- **Accent**: #FF5722 (Orange)
- **Success**: #4CAF50 (Green)
- **Error**: #F44336 (Red)
- **Warning**: #FFC107 (Amber)

### Animations
- Splash screen: 2-second display
- Login: Fade-in effects
- Navigation: Fragment transitions
- RecyclerView: Item animations
- Button: Loading animation

### Dark Mode
- Automatic detection
- Full color scheme inversion
- Separate `values-night` resources

---

## 🧪 Testing Strategy

### Unit Tests (To be implemented)
- ViewModel logic validation
- Input validation functions
- Repository error handling
- Date utility functions

### Integration Tests (To be implemented)
- API response mapping
- Token storage/retrieval
- Authentication flow
- Navigation logic

### UI Tests (To be implemented)
- Activity/Fragment rendering
- User interactions
- Navigation transitions
- Error dialog display

---

## 📚 Code Examples

### Example 1: Making an API Call

```kotlin
// In Repository
fun getBooks(): Flow<NetworkResult<PaginatedResponse<Book>>> = flow {
    emit(NetworkResult.Loading())
    try {
        val response = apiService.getBooks()
        if (response.isSuccessful) {
            emit(NetworkResult.Success(response.body()!!))
        } else {
            emit(NetworkResult.Error(response.message(), response.code()))
        }
    } catch (e: Exception) {
        emit(NetworkResult.Error(e.message ?: "Unknown error"))
    }
}.catch { e ->
    emit(NetworkResult.Error(e.message ?: "Unknown error"))
}

// In ViewModel
fun loadBooks() {
    viewModelScope.launch {
        repository.getBooks().collect { result ->
            _books.value = result
        }
    }
}

// In Activity/Fragment
viewModel.books.observe(this) { result ->
    when (result) {
        is NetworkResult.Success -> showBooks(result.data.data)
        is NetworkResult.Error -> showError(result.message)
        is NetworkResult.Loading -> showLoading()
        is NetworkResult.Unauthorized -> navigateToLogin()
    }
}
```

### Example 2: Implementing a New Feature (Fragment)

```kotlin
// 1. Create Fragment
class MyFeatureFragment : Fragment() {
    private lateinit var viewModel: MyFeatureViewModel
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_my_feature, container, false)
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(MyFeatureViewModel::class.java)
        observeData()
    }
    
    private fun observeData() {
        viewModel.items.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Success -> renderItems(result.data)
                is NetworkResult.Error -> showError(result.message)
                is NetworkResult.Loading -> showLoading()
                else -> {}
            }
        }
    }
}

// 2. Create ViewModel
class MyFeatureViewModel(
    private val repository: MyFeatureRepository
) : ViewModel() {
    private val _items = MutableLiveData<NetworkResult<List<Item>>>()
    val items: LiveData<NetworkResult<List<Item>>> = _items
    
    fun loadItems() {
        viewModelScope.launch {
            repository.getItems().collect { result ->
                _items.value = result
            }
        }
    }
}

// 3. Create Repository
class MyFeatureRepository(private val apiService: ApiService) {
    fun getItems(): Flow<NetworkResult<List<Item>>> = flow {
        // Implementation using apiService
    }
}
```

---

## 🤝 Contributing

1. Create a new branch: `git checkout -b feature/your-feature`
2. Make changes and commit: `git commit -m "Add feature"`
3. Push to branch: `git push origin feature/your-feature`
4. Submit pull request to `main` branch

---

## 📝 Commit Message Convention

```
<type>: <subject>

<body>

<footer>

Types: feat, fix, docs, style, refactor, test, chore, perf
```

Example:
```
feat: implement book search functionality

- Added search endpoint integration
- Implemented live search with debouncing
- Added error handling for search failures

Closes #123
```

---

## 📞 Support & Contact

**Developer**: Saroj Kumar  
**Email**: saroj@example.com  
**GitHub**: [mehtasaroj122](https://github.com/mehtasaroj122)

---

## 📄 License

This project is part of a diploma project and follows academic guidelines.

---

## 🎓 Educational Notes

This project demonstrates:
- Professional Android development practices
- MVVM architecture pattern
- REST API integration
- Secure authentication
- Modern Android development with Kotlin
- Reactive programming with LiveData & Coroutines

Perfect for diploma/degree projects and production-ready development!

---

**Last Updated**: June 18, 2026
**Version**: 1.0.0 (Architecture Complete)

