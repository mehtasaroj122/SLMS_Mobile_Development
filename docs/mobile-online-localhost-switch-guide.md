# Mobile API Environment Switch Guide

## 1. Current Active Mode

The app is currently configured to use the hosted online Laravel API:

```text
https://lms.saroj00.com.np/api/
```

## 2. Online API Mode

Use online API mode when:

- Laravel project is hosted on cPanel/server
- Domain and SSL are working
- App is being tested on real mobile device
- App is being prepared for final/demo use

Active code example:

```kotlin
const val BASE_URL = "https://lms.saroj00.com.np/api/"
```

## 3. Localhost API Mode

Use localhost API mode when:

- Laravel project is running locally
- Testing with Android Emulator
- Using `php artisan serve`

Local code example:

```kotlin
const val BASE_URL = "http://10.0.2.2:8000/api/"
```

`10.0.2.2` is used because Android Emulator cannot access computer localhost directly using `localhost`.

## 4. How to Switch Back to Localhost

1. Open the file where `BASE_URL` is defined.
2. Comment the online URL.
3. Uncomment the localhost URL.
4. Make sure Laravel is running locally:

```bash
php artisan serve
```

5. Test login again from emulator.

Example:

```kotlin
// Online hosted API
// const val BASE_URL = "https://lms.saroj00.com.np/api/"

// Local emulator API
const val BASE_URL = "http://10.0.2.2:8000/api/"
```

## 5. How to Switch Again to Online Hosting

1. Open the file where `BASE_URL` is defined.
2. Comment the localhost URL.
3. Uncomment the online URL.
4. Make sure hosted Laravel API is working.
5. Test login in Postman first.
6. Then test from Android app.

Example:

```kotlin
// Local emulator API
// const val BASE_URL = "http://10.0.2.2:8000/api/"

// Online hosted API
const val BASE_URL = "https://lms.saroj00.com.np/api/"
```

## 6. Important Notes

- Retrofit base URL must always end with `/`.
- Do not keep two active `BASE_URL` values at once.
- If login works but dashboard/profile fails, check Bearer token header.
- If images do not load, check whether backend returns full image URL.
- If online API does not work, test the same endpoint in Postman first.
- If localhost API does not work, confirm Laravel server is running.

## 7. Testing Checklist

- [ ] App builds successfully
- [ ] Login works using online API
- [ ] Token saves successfully
- [ ] Dashboard loads from online API
- [ ] Books load from online API
- [ ] Profile loads from online API
- [ ] Images/files load correctly
- [ ] Logout works
- [ ] No hardcoded localhost URL remains active
- [ ] Localhost URL is preserved as commented option
