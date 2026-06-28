# Authentication Flow Testing Guide

**Date**: June 18, 2026  
**Status**: Ready for testing  
**Build Status**: ✅ Successful

---

## 🔐 Authentication Implementation Summary

### What's Been Implemented

1. **TokenManager.kt**: Secure token and session storage using DataStore
2. **AuthInterceptor.kt**: Automatic Bearer token injection for all API requests
3. **RetrofitClient.kt**: Retrofit configuration with interceptors
4. **ApiService.kt**: Login endpoint defined
5. **AuthModels.kt**: LoginRequest, LoginResponse, User models
6. **AuthRepository.kt**: Login/logout/profile business logic
7. **LoginViewModel.kt**: Input validation and UI state management
8. **SplashActivity.kt**: Token check and role-based navigation
9. **LoginActivity.kt**: Professional login UI with error handling
10. **Role-based Dashboards**: Admin, Staff, Student activities

---

## 📋 Testing Checklist

### Step 1: Prepare Test Environment

**Setup Your Laravel API**:
```bash
# In your Laravel project
cd your-laravel-project

# Make sure these endpoints work:
curl -X POST http://YOUR_IP:8000/api/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"password"}'
```

**Expected Response**:
```json
{
  "message": "Login successful",
  "access_token": "your_token_here",
  "token_type": "Bearer",
  "user": {
    "id": 1,
    "name": "Admin User",
    "email": "admin@example.com",
    "role": "admin"
  }
}
```

### Step 2: Configure Emulator/Device

**For Emulator**:
- Base URL is already set to `http://10.0.2.2:8000/api/`
- No changes needed

**For Physical Device**:
- Edit `app/src/main/java/com/saroj/lmsmobile/utils/Constants.kt`
- Change BASE_URL to `http://YOUR_COMPUTER_IP:8000/api/`
- Example: `http://192.168.1.100:8000/api/`

### Step 3: Build and Deploy

```bash
# Build debug APK
./gradlew assembleDebug

# Install on emulator/device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or run directly from Android Studio
# - Click "Run" or press Shift+F10
```

### Step 4: Test Authentication Flow

#### Test 4.1: No Token → Show Login Screen

**Steps**:
1. First app launch
2. SplashActivity appears (2 seconds)
3. SplashActivity checks for token (no token exists)
4. LoginActivity displays

**Expected Result**: ✅ LoginActivity shows email/password form

---

#### Test 4.2: Invalid Credentials → Show Error

**Steps**:
1. Enter email: `invalid@example.com`
2. Enter password: `wrongpassword`
3. Click "Login"

**Expected Behavior**:
- Loading indicator shows briefly
- Error dialog appears: "Invalid credentials" or "User not found"
- User returns to login form
- Can retry login

**Expected Result**: ✅ Error handled gracefully

---

#### Test 4.3: Valid Admin Credentials → Admin Dashboard

**Steps**:
1. Enter email: `admin@example.com`
2. Enter password: `password`
3. Click "Login"

**Expected Behavior**:
- Loading indicator shows
- API call POST /api/login sent
- Token, user ID, name, email, role saved to DataStore
- Redirect to AdminDashboardActivity
- Shows "Admin Dashboard" in toolbar
- Bottom navigation with 5 items visible (Dashboard, Books, Students, Issues, Fines)

**Expected Result**: ✅ Admin dashboard loads with correct role-based UI

---

#### Test 4.4: Valid Staff Credentials → Staff Dashboard

**Steps**:
1. Logout (tap menu icon, click logout) if logged in as admin
2. Enter email: `staff@example.com`
3. Enter password: `password`
4. Click "Login"

**Expected Behavior**:
- Redirect to StaffDashboardActivity
- Shows "Staff Dashboard" in toolbar
- Bottom navigation with 5 items visible (Dashboard, Books, Students, Issues, Fines)

**Expected Result**: ✅ Staff dashboard loads with correct role-based UI

---

#### Test 4.5: Valid Student Credentials → Student Dashboard

**Steps**:
1. Logout if logged in
2. Enter email: `student@example.com`
3. Enter password: `password`
4. Click "Login"

**Expected Behavior**:
- Redirect to StudentDashboardActivity
- Shows "Student Dashboard" in toolbar
- Bottom navigation with 5 items (Dashboard, Search Books, My Books, My Fines, Profile)

**Expected Result**: ✅ Student dashboard loads with correct role-based UI

---

#### Test 4.6: Token Persistence → App Restart

**Steps**:
1. Login successfully as any role
2. Note the dashboard displayed
3. Close app (remove from memory)
4. Relaunch app

**Expected Behavior**:
- SplashActivity appears (2 seconds)
- Token is read from DataStore
- User role is read from DataStore
- User is redirected directly to same dashboard (no login needed)
- Same user info displayed

**Expected Result**: ✅ Token persists and user remains logged in

---

#### Test 4.7: Token Expiration → Logout & Login Again

**Steps**:
1. Login successfully
2. Click menu icon (top-right)
3. Click "Logout"

**Expected Behavior**:
- Token is cleared from DataStore
- LoginActivity is shown
- Can login again

**Expected Result**: ✅ Logout works and user can login again

---

#### Test 4.8: 401 Unauthorized Response → Redirect to Login

**Steps**:
1. Login successfully
2. (Optional: Manually test by calling API with expired token)
3. API returns 401 Unauthorized

**Expected Behavior**:
- AuthInterceptor detects 401
- Token is cleared
- UnauthorizedActivity is shown: "Session Expired"
- User clicks "Login Again"
- Redirected to LoginActivity
- Can login again

**Expected Result**: ✅ 401 errors handled properly

---

### Step 5: Verify DataStore Storage

**Check Saved Data**:
```bash
# Via ADB, view DataStore contents
adb shell run-as com.saroj.lmsmobile cat /data/data/com.saroj.lmsmobile/files/datastore/lms_mobile_app_prefs.pb

# Or via Android Studio debugger:
# - Run app in debug mode
# - Set breakpoint in TokenManager.saveUserInfo()
# - View saved values
```

**Expected Data Saved**:
- `access_token`: Token from API response
- `user_id`: User ID from API response
- `user_name`: User name from API response
- `user_email`: User email from API response
- `user_role`: User role (admin, staff, student)
- `is_logged_in`: true

---

### Step 6: Verify API Integration

**Check Network Requests**:
```bash
# Via Android Studio Network Profiler:
# 1. Go to Profiler tab
# 2. Select Network tab
# 3. Run login
# 4. View POST /api/login request
# 5. Verify request includes:
#    - Content-Type: application/json
#    - Accept: application/json
#    - Authorization: Bearer <token> (on subsequent requests)
```

**Expected Headers**:
```
POST /api/login HTTP/1.1
Content-Type: application/json
Accept: application/json

{
  "email": "admin@example.com",
  "password": "password"
}
```

**Expected Response** (200 OK):
```json
{
  "access_token": "1|...",
  "token_type": "Bearer",
  "user": {
    "id": 1,
    "name": "Admin User",
    "email": "admin@example.com",
    "role": "admin"
  }
}
```

---

## 🐛 Troubleshooting

### Issue: "Connection refused" or "Unable to connect"

**Solution**:
- Verify Laravel API is running: `php artisan serve`
- If using emulator, BASE_URL must be `http://10.0.2.2:8000/api/`
- If using physical device, change BASE_URL to your computer IP

### Issue: "Unresolved host"

**Solution**:
- Check network connectivity on device/emulator
- Verify firewall allows port 8000
- Check Base URL in Constants.kt

### Issue: "Login button doesn't respond"

**Solution**:
- Check email validation (must be valid email format)
- Check password length (minimum 6 characters)
- View error messages below input fields
- Check Logcat for errors

### Issue: "Token not saved"

**Solution**:
- Check DataStore implementation is correct
- Verify app has permission to write to DataStore
- Clear app data and retry: `adb shell pm clear com.saroj.lmsmobile`

### Issue: "Always shows LoginActivity after restart"

**Solution**:
- Token not being saved properly to DataStore
- Check if SplashActivity is finding token
- Add logging to `checkAuthenticationAndNavigate()` method

---

## 📊 Test Results Template

Use this to document your testing:

```
Auth Testing Results - [Date]

✅ Test 4.1: No Token → Login Screen: [PASS/FAIL]
✅ Test 4.2: Invalid Credentials: [PASS/FAIL]
✅ Test 4.3: Admin Login: [PASS/FAIL]
✅ Test 4.4: Staff Login: [PASS/FAIL]
✅ Test 4.5: Student Login: [PASS/FAIL]
✅ Test 4.6: Token Persistence: [PASS/FAIL]
✅ Test 4.7: Logout & Login Again: [PASS/FAIL]
✅ Test 4.8: 401 Unauthorized: [PASS/FAIL]

Notes:
- API Response Times: [record average]
- Device/Emulator: [specify]
- Network: WiFi/USB/4G
- Issues Found: [list any]
```

---

## 📱 Debug Logging

To see detailed logs of authentication flow:

**Add to LoginActivity.kt**:
```kotlin
// In observeViewModel()
viewModel.loginResult.observe(this) { result ->
    when (result) {
        is NetworkResult.Loading -> {
            android.util.Log.d("LoginActivity", "Loading...")
        }
        is NetworkResult.Success -> {
            android.util.Log.d("LoginActivity", "Success: ${result.data.user.role}")
        }
        is NetworkResult.Error -> {
            android.util.Log.e("LoginActivity", "Error: ${result.message}")
        }
        is NetworkResult.Unauthorized -> {
            android.util.Log.e("LoginActivity", "Unauthorized - Session expired")
        }
    }
}
```

**View logs in Android Studio**:
```bash
adb logcat | grep LoginActivity
```

---

## ✨ Next Steps After Testing

Once authentication is fully tested and working:

1. **Implement Books Module**
   - BookRepository for API calls
   - BookViewModel for list logic
   - Fragment UI for book listing
   - RecyclerView adapter

2. **Implement Students Module**
   - StudentRepository
   - StudentViewModel
   - Student listing UI

3. **Implement Issues Module**
   - IssueRepository
   - IssueViewModel
   - Issue management UI

4. **Implement Fines Module**
   - FineRepository
   - FineViewModel
   - Fine listing UI

---

## 📚 Reference

- **Auth Flow Diagram**: See LMS_APP_DOCUMENTATION.md
- **Constants**: app/src/main/java/com/saroj/lmsmobile/utils/Constants.kt
- **Build Status**: ✅ Successful with `./gradlew assembleDebug`
- **Git Status**: 4 commits on saroj branch

---

**Ready to test!** Follow the checklist above and document your results. 🚀

