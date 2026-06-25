# Mobile API Documentation

Generated for the Laravel mobile API routes in `routes/api.php`.

Base URL: `/api`

Authentication: Protected routes use Sanctum bearer tokens. Send `Authorization: Bearer {token}` and `Accept: application/json`.

## API Summary

| Method | Endpoint | Auth | Roles | Purpose |
| --- | --- | --- | --- | --- |
| POST | `/api/login` | No | public | Login and create a Sanctum token |
| POST | `/api/forgot-password` | No | public | Request password reset |
| POST | `/api/reset-password` | No | public | Reset password |
| GET | `/api/test-unauthorized` | No | public | Return sample 401 JSON |
| POST | `/api/logout` | Yes | student/staff/admin | Delete current token |
| GET | `/api/auth/check` | Yes | student/staff/admin | Check token and user |
| GET | `/api/profile` | Yes | student/staff/admin | Get profile |
| PUT | `/api/profile` | Yes | student/staff/admin | Update profile |
| POST | `/api/profile/password` | Yes | student/staff/admin | Change password |
| POST | `/api/profile/change-password` | Yes | student/staff/admin | Change password |
| POST | `/api/profile/photo` | Yes | student/staff/admin | Upload profile photo |
| DELETE | `/api/profile/photo` | Yes | student/staff/admin | Remove profile photo |
| GET | `/api/profile/delete-eligibility` | Yes | student/staff/admin | Check student account deletion eligibility |
| DELETE | `/api/profile/account` | Yes | student/staff | Deactivate own account |
| DELETE | `/api/profile` | Yes | student | Deactivate student account |
| GET | `/api/books` | Yes | student/staff/admin | List catalog books |
| GET | `/api/books/search` | Yes | student/staff/admin | Search catalog |
| GET | `/api/books/available` | Yes | student/staff/admin | List available books |
| GET | `/api/books/category/{category}` | Yes | student/staff/admin | List books by category |
| GET | `/api/books/{id}` | Yes | student/staff/admin | Book detail |
| GET | `/api/categories` | Yes | student/staff/admin | List categories |
| GET | `/api/dashboard` | Yes | student/staff/admin | General dashboard totals |
| GET | `/api/library/settings` | Yes | student/staff/admin | Active library/fine settings |
| GET | `/api/notifications` | Yes | student/staff/admin | List notifications |
| GET | `/api/notifications/unread` | Yes | student/staff/admin | List unread notifications |
| GET | `/api/notifications/count` | Yes | student/staff/admin | Notification counts |
| POST | `/api/notifications/read-all` | Yes | student/staff/admin | Mark all notifications read |
| POST | `/api/notifications/{id}/read` | Yes | student/staff/admin | Mark one notification read |
| DELETE | `/api/notifications/{id}` | Yes | student/staff/admin | Delete notification |
| GET | `/api/student/dashboard` | Yes | student | Student dashboard |
| GET | `/api/student/my-books` | Yes | student | Student issued book overview |
| GET | `/api/student/my-books/summary` | Yes | student | Student book summary |
| GET | `/api/student/my-books/current` | Yes | student | Student current books |
| GET | `/api/student/my-books/history` | Yes | student | Student book history |
| GET | `/api/student/my-books/due-soon` | Yes | student | Student due-soon books |
| GET | `/api/student/requests` | Yes | student | Student book requests |
| POST | `/api/student/requests` | Yes | student | Create book request |
| GET | `/api/student/requests/summary` | Yes | student | Student request summary |
| GET | `/api/student/requests/{id}` | Yes | student | Student request detail |
| POST | `/api/student/requests/{id}/cancel` | Yes | student | Cancel own request |
| GET | `/api/student/fines` | Yes | student | Student fines |
| GET | `/api/student/fines/pending` | Yes | student | Pending student fines |
| GET | `/api/student/fines/paid` | Yes | student | Paid student fines |
| GET | `/api/student/fines/summary` | Yes | student | Student fine summary |
| GET | `/api/student/fines/{id}` | Yes | student | Student fine detail |
| GET | `/api/students` | Yes | staff/admin | Shared staff/admin student list |
| GET | `/api/students/search` | Yes | staff/admin | Shared staff/admin student search |
| GET | `/api/students/{id}` | Yes | staff/admin | Shared staff/admin student detail |
| GET | `/api/issues` | Yes | staff/admin | Shared issue list |
| POST | `/api/issues` | Yes | staff/admin | Shared issue books |
| GET | `/api/issues/{id}` | Yes | staff/admin | Shared issue detail |
| POST | `/api/issues/return/{id}` | Yes | staff/admin | Shared return book |
| GET | `/api/issues/student/{studentId}` | Yes | staff/admin | Issues by student |
| GET | `/api/overdue` | Yes | staff/admin | Overdue issues |
| GET | `/api/fines` | Yes | staff/admin | Shared fine list |
| GET | `/api/fines/student/{id}` | Yes | staff/admin | Shared student fines |
| GET | `/api/book-requests` | Yes | staff/admin | Shared book request list |
| GET | `/api/book-requests/{id}` | Yes | staff/admin | Shared book request detail |
| POST | `/api/book-requests/{id}/approve` | Yes | staff/admin | Shared approve request |
| POST | `/api/book-requests/{id}/reject` | Yes | staff/admin | Shared reject request |
| GET | `/api/staff/dashboard` | Yes | staff/admin | Staff mobile dashboard |
| GET | `/api/staff/students` | Yes | staff/admin | Staff student list |
| GET | `/api/staff/students/search` | Yes | staff/admin | Staff issue-book student search |
| GET | `/api/staff/students/{student}` | Yes | staff/admin | Staff student detail |
| GET | `/api/staff/students/{student}/issue-privileges` | Yes | staff/admin | Student issue privileges |
| GET | `/api/staff/books/search` | Yes | staff/admin | Staff issue-book book search |
| POST | `/api/staff/issues/preview` | Yes | staff/admin | Preview issue operation |
| POST | `/api/staff/issues` | Yes | staff/admin | Issue one or more books |
| GET | `/api/staff/issues/search` | Yes | staff/admin | Search active issues for return |
| POST | `/api/staff/issues/{issue}/return` | Yes | staff/admin | Return issued book |
| GET | `/api/staff/fines` | Yes | staff/admin | Staff fine list |
| GET | `/api/staff/fines/summary` | Yes | staff/admin | Staff fine summary |
| GET | `/api/staff/fines/students` | Yes | staff/admin | Student-wise fine list |
| GET | `/api/staff/fines/students/{student}` | Yes | staff/admin | Student fine detail |
| GET | `/api/staff/fines/{fine}` | Yes | staff/admin | Staff fine detail |
| POST | `/api/staff/fines/{fine}/pay` | Yes | staff/admin | Mark fine paid |
| POST | `/api/staff/fines/{fine}/waive` | Yes | staff/admin | Waive fine |
| GET | `/api/staff/book-requests` | Yes | staff/admin | Staff book request list |
| GET | `/api/staff/book-requests/{bookRequest}` | Yes | staff/admin | Staff book request detail |
| POST | `/api/staff/book-requests/{bookRequest}/approve` | Yes | staff/admin | Staff approve request |
| POST | `/api/staff/book-requests/{bookRequest}/reject` | Yes | staff/admin | Staff reject request |
| GET | `/api/admin/dashboard` | Yes | admin | Admin mobile dashboard |
| GET | `/api/activity-logs` | Yes | admin | Activity logs |

## Error Format

Unauthenticated:

```json
{"message":"Unauthenticated."}
```

Unauthorized role:

```json
{"message":"Forbidden."}
```

Validation:

```json
{"success":false,"message":"The given data was invalid.","errors":{"field":["Validation message."]}}
```

Not found:

```json
{"message":"Resource not found."}
```

Profile/staff endpoints use:

```json
{"success":false,"message":"The given data was invalid.","errors":{"field":["Validation message."]}}
```

## Authentication APIs

### Login

Method: POST

Endpoint: `/api/login`

Auth: Not required

Roles: public

Purpose: Authenticate a mobile user and return a Sanctum bearer token.

Request: `email` required, `password` required, `device_name` optional.

Success Response:

```json
{"message":"Login successful.","token_type":"Bearer","access_token":"token","user":{"id":1,"name":"Anil Kapoor","email":"staff@example.com","role":"staff"}}
```

Error Response:

```json
{"message":"Invalid email or password."}
```

Controller: `AuthController@login`

Notes: Inactive accounts return 403.

### Forgot Password

Method: POST

Endpoint: `/api/forgot-password`

Auth: Not required

Roles: public

Purpose: Send or start password reset for an email address.

Request: `email` required.

Success Response:

```json
{"message":"Password reset instructions sent successfully."}
```

Error Response:

```json
{"message":"The given data was invalid.","errors":{"email":["The selected email is invalid."]}}
```

Controller: `PasswordResetController@forgot`

Notes: Throttled by `throttle:5,1`.

### Reset Password

Method: POST

Endpoint: `/api/reset-password`

Auth: Not required

Roles: public

Purpose: Reset a user password using the reset token/OTP data accepted by the controller.

Request: reset token/OTP fields, `email`, `password`, `password_confirmation`.

Success Response:

```json
{"message":"Password reset successfully."}
```

Error Response:

```json
{"message":"The given data was invalid.","errors":{"password":["The password confirmation does not match."]}}
```

Controller: `PasswordResetController@reset`

Notes: Throttled by `throttle:5,1`.

### Test Unauthorized

Method: GET

Endpoint: `/api/test-unauthorized`

Auth: Not required

Roles: public

Purpose: Return a known 401 response for Android/Postman checks.

Request: none.

Success Response:

```json
{"message":"Unauthenticated."}
```

Error Response:

```json
{"message":"Unauthenticated."}
```

Controller: `AuthController@testUnauthorized`

Notes: Always returns HTTP 401.

### Logout

Method: POST

Endpoint: `/api/logout`

Auth: Required

Roles: student/staff/admin

Purpose: Revoke the current Sanctum token.

Request: none.

Success Response:

```json
{"success":true,"message":"Logout successful.","data":[]}
```

Error Response:

```json
{"message":"Unauthenticated."}
```

Controller: `AuthController@logout`

Notes: Deletes only the current access token.

### Auth Check

Method: GET

Endpoint: `/api/auth/check`

Auth: Required

Roles: student/staff/admin

Purpose: Validate token and return the signed-in user summary.

Request: none.

Success Response:

```json
{"authenticated":true,"user":{"id":1,"name":"Anil Kapoor","email":"staff@example.com","role":"staff","status":"active"},"student_id":null,"staff_id":1}
```

Error Response:

```json
{"message":"Unauthenticated."}
```

Controller: `AuthController@check`

Notes: Useful for Android splash/session validation.

## Common/Profile APIs

Staff Profile Android contract:

- Use these common endpoints for staff profile screens: `GET /api/profile`, `PUT /api/profile`, `POST /api/profile/photo`, `DELETE /api/profile/photo`, `POST /api/profile/password`, `DELETE /api/profile/account`, and `POST /api/logout`.
- All endpoints are protected by `auth:sanctum`; send `Authorization: Bearer {access_token}` and `Accept: application/json`.
- Staff responses use the logged-in user's linked `staff` record and department. The API does not return dummy staff data.
- Staff profile updates only write `name`, `phone`, and `address`. `email`, `role`, `username`, and staff `department` are read-only.
- Staff account deletion uses `DELETE /api/profile/account`; it deactivates the account and revokes tokens instead of hard deleting the staff/user records.

### Get Profile

Method: GET

Endpoint: `/api/profile`

Auth: Required

Roles: student/staff/admin

Purpose: Return the authenticated user's real profile. For staff users, this returns the logged-in staff user's linked staff record and department.

Request: none.

Success Response:

```json
{"success":true,"message":"Profile fetched successfully.","data":{"id":1,"name":"Anil Kapoor","email":"staff@example.com","username":null,"phone":"9800000000","address":"Kathmandu","role":"staff","status":"active","profile_photo":"profile_photos/file.jpg","profile_photo_url":"http://YOUR_SERVER/storage/profile_photos/file.jpg","staff":{"id":2,"staff_id":"STF-001","department_id":1,"department":"Library","designation":"Librarian","join_date":"2026-01-20"}}}
```

Error Response:

```json
{"message":"Unauthenticated."}
```

Controller: `AuthController@profile`

Notes: Uses Sanctum auth and `UserResource`. Email, role, username, and department are read-only.

### Update Profile

Method: PUT

Endpoint: `/api/profile`

Auth: Required

Roles: student/staff/admin

Purpose: Update editable common profile fields for the authenticated user.

Request body:

```json
{"name":"Updated Name","phone":"9812345678","address":"Kathmandu"}
```

Only `name`, `phone`, and `address` are accepted. Email, role, username, and department are read-only and are not updated by this endpoint.

Success Response:

```json
{"success":true,"message":"Profile updated successfully.","data":{"id":1,"name":"Updated Name","email":"staff@example.com","phone":"9812345678","address":"Kathmandu","role":"staff","staff":{"id":2,"staff_id":"STF-001","department":"Library"}}}
```

Error Response:

```json
{"success":false,"message":"The given data was invalid.","errors":{"phone":["The phone has already been taken."]}}
```

Controller: `ProfileController@update`

Notes: Logs profile changes through `ActivityLogger`.

### Change Profile Password

Method: POST

Endpoint: `/api/profile/password`

Auth: Required

Roles: student/staff/admin

Purpose: Change password for the authenticated user.

Request body:

```json
{"current_password":"OldPassword!123","password":"NewPassword!123","password_confirmation":"NewPassword!123"}
```

Success Response:

```json
{"success":true,"message":"Password changed successfully.","data":[]}
```

Error Response:

```json
{"success":false,"message":"The given data was invalid.","errors":{"current_password":["The current password is incorrect."]}}
```

Controller: `ProfileController@changePassword`

Notes: Verifies the current password, validates the new password, clears the forced password change flag, and keeps legacy alias `/api/profile/change-password`.

### Upload Profile Photo

Method: POST

Endpoint: `/api/profile/photo`

Auth: Required

Roles: student/staff/admin

Purpose: Upload profile photo.

Request body: `multipart/form-data` with `photo` image file. Allowed types: jpg, jpeg, png, webp. Max size: 2048 KB.

Success Response:

```json
{"success":true,"message":"Profile photo uploaded successfully.","data":{"profile_photo":"profile_photos/file.jpg","profile_photo_url":"http://YOUR_SERVER/storage/profile_photos/file.jpg"},"profile_photo":"profile_photos/file.jpg","profile_photo_url":"http://YOUR_SERVER/storage/profile_photos/file.jpg"}
```

Error Response:

```json
{"success":false,"message":"The given data was invalid.","errors":{"photo":["The photo field is required."]}}
```

Controller: `ProfileController@uploadPhoto`

Notes: Stores the file on the public disk, deletes any previous stored profile photo, saves `users.profile_photo`, and returns a public photo URL.

### Remove Profile Photo

Method: DELETE

Endpoint: `/api/profile/photo`

Auth: Required

Roles: student/staff/admin

Purpose: Remove authenticated user's profile photo.

Request: none.

Success Response:

```json
{"success":true,"message":"Profile photo removed successfully.","data":{"profile_photo":null,"profile_photo_url":null},"profile_photo":null,"profile_photo_url":null}
```

Error Response:

```json
{"message":"Unauthenticated."}
```

Controller: `ProfileController@removePhoto`

Notes: Deletes the stored photo file when present and sets `users.profile_photo` to null.

### Delete Account

Method: DELETE

Endpoint: `/api/profile/account`

Auth: Required

Roles: student/staff

Purpose: Deactivate the authenticated mobile account after password and DELETE text confirmation. Staff accounts are not hard deleted, so issued books, fines, book requests, and activity history remain intact.

Request body:

```json
{"current_password":"Password!123","confirmation_text":"DELETE"}
```

Success Response:

```json
{"success":true,"message":"Your account has been deactivated successfully.","data":{"status":"inactive"}}
```

Error Response:

```json
{"success":false,"message":"The given data was invalid.","errors":{"current_password":["The current password is incorrect."]}}
```

Controller: `ProfileController@destroyAccount`

Notes: Student accounts must pass active-issued-book, pending-fine, and active-request checks. Staff accounts are deactivated by setting `users.status` to `inactive`; all tokens are revoked.

### Delete Eligibility

Method: GET

Endpoint: `/api/profile/delete-eligibility`

Auth: Required

Roles: student/staff/admin

Purpose: Check whether a student account can be deactivated from mobile.

Request: none.

Success Response:

```json
{"can_delete":true,"message":"Your account is eligible for deletion.","issued_books":0,"pending_fines":0,"active_requests":0,"reasons":[]}
```

Error Response:

```json
{"message":"Student profile not found."}
```

Controller: `ProfileController@deleteEligibility`

Notes: Legacy helper for student deletion checks. Staff users should call `DELETE /api/profile/account` when deactivating their own account.

### Delete Profile Legacy

Method: DELETE

Endpoint: `/api/profile`

Auth: Required

Roles: student

Purpose: Deactivate authenticated student account.

Request: `confirmation` must equal `DELETE`.

Success Response:

```json
{"message":"Your account has been deactivated successfully."}
```

Error Response:

```json
{"message":"Account deletion is currently unavailable.","errors":{"issued_books":1,"pending_fines":0,"active_requests":0}}
```

Controller: `ProfileController@destroy`

Notes: Legacy student-only endpoint. Android staff profile should use `DELETE /api/profile/account`.

### General Dashboard

Method: GET

Endpoint: `/api/dashboard`

Auth: Required

Roles: student/staff/admin

Purpose: Return role-aware/general dashboard totals.

Request: none.

Success Response:

```json
{"message":"Dashboard loaded successfully.","data":{"total_books":100,"issued_books":20}}
```

Error Response:

```json
{"message":"Unauthenticated."}
```

Controller: `DashboardController@index`

Notes: Existing general Android home endpoint.

### Library Settings

Method: GET

Endpoint: `/api/library/settings`

Auth: Required

Roles: student/staff/admin

Purpose: Return active library/fine settings.

Request: none.

Success Response:

```json
{"data":{"per_day_fine":5,"issue_duration_days":14,"max_books_per_student":5}}
```

Error Response:

```json
{"message":"Unauthenticated."}
```

Controller: `LibrarySettingController@__invoke`

Notes: Uses `FineSetting::resolveActive()`.

## Catalog APIs

### List Books

Method: GET

Endpoint: `/api/books`

Auth: Required

Roles: student/staff/admin

Purpose: Paginated catalog book list.

Request: query `category`, `availability`, `condition`, `sort`, `per_page`.

Success Response:

```json
{"data":[{"id":1,"accession_no":"0001000000","title":"Introduction to Algorithms","available_quantity":2}],"links":{},"meta":{}}
```

Error Response:

```json
{"message":"Unauthenticated."}
```

Controller: `BookController@index`

Notes: Uses `BookResource`.

### Search Books

Method: GET

Endpoint: `/api/books/search`

Auth: Required

Roles: student/staff/admin

Purpose: Search catalog books.

Request: `q` required; optional catalog filters and `per_page`.

Success Response:

```json
{"data":[{"id":3,"title":"Clean Code","author":"Robert C. Martin","status":"available"}]}
```

Error Response:

```json
{"message":"The given data was invalid.","errors":{"q":["The q field is required."]}}
```

Controller: `BookController@search`

Notes: Searches title, author, publisher, ISBN, and category.

### Available Books

Method: GET

Endpoint: `/api/books/available`

Auth: Required

Roles: student/staff/admin

Purpose: List books with available copies.

Request: `per_page` optional.

Success Response:

```json
{"data":[{"id":5,"title":"Effective Java","available_quantity":10}]}
```

Error Response:

```json
{"message":"Unauthenticated."}
```

Controller: `BookController@available`

Notes: Filters `available_copies > 0`.

### Books By Category

Method: GET

Endpoint: `/api/books/category/{category}`

Auth: Required

Roles: student/staff/admin

Purpose: List books by category id or name.

Request: path `category`; optional `per_page`.

Success Response:

```json
{"data":[{"id":1,"title":"Introduction to Algorithms","category":"Computer Science"}]}
```

Error Response:

```json
{"message":"Resource not found."}
```

Controller: `BookController@category`

Notes: Category can be numeric id or exact name.

### Book Detail

Method: GET

Endpoint: `/api/books/{id}`

Auth: Required

Roles: student/staff/admin

Purpose: Return one book.

Request: path `id`.

Success Response:

```json
{"data":{"id":1,"title":"Introduction to Algorithms","isbn":"0001000000","available_quantity":2}}
```

Error Response:

```json
{"message":"Resource not found."}
```

Controller: `BookController@show`

Notes: Numeric id only.

### Categories

Method: GET

Endpoint: `/api/categories`

Auth: Required

Roles: student/staff/admin

Purpose: List book categories.

Request: none.

Success Response:

```json
{"data":[{"id":1,"name":"Computer Science"}]}
```

Error Response:

```json
{"message":"Unauthenticated."}
```

Controller: `CategoryController@index`

Notes: Uses `CategoryResource`.

## Notification APIs

### List Notifications

Method: GET

Endpoint: `/api/notifications`

Auth: Required

Roles: student/staff/admin

Purpose: Paginated notifications for current user.

Request: `per_page` optional.

Success Response:

```json
{"data":[{"id":1,"title":"Request Approved","read_at":null}]}
```

Error Response:

```json
{"message":"Unauthenticated."}
```

Controller: `NotificationController@index`

Notes: User can only see own notifications.

### Unread Notifications

Method: GET

Endpoint: `/api/notifications/unread`

Auth: Required

Roles: student/staff/admin

Purpose: Paginated unread notifications.

Request: `per_page` optional.

Success Response:

```json
{"data":[{"id":1,"title":"Fine Reminder","read_at":null}]}
```

Error Response:

```json
{"message":"Unauthenticated."}
```

Controller: `NotificationController@unread`

Notes: Filters `read_at IS NULL`.

### Notification Count

Method: GET

Endpoint: `/api/notifications/count`

Auth: Required

Roles: student/staff/admin

Purpose: Get unread and total notification counts.

Request: none.

Success Response:

```json
{"unread_count":3,"total_count":12}
```

Error Response:

```json
{"message":"Unauthenticated."}
```

Controller: `NotificationController@count`

Notes: Current user only.

### Read All Notifications

Method: POST

Endpoint: `/api/notifications/read-all`

Auth: Required

Roles: student/staff/admin

Purpose: Mark all current user's unread notifications as read.

Request: none.

Success Response:

```json
{"message":"All notifications marked as read.","data":{"updated_count":3}}
```

Error Response:

```json
{"message":"Unauthenticated."}
```

Controller: `NotificationController@readAll`

Notes: Bulk update on current user's notifications.

### Read Notification

Method: POST

Endpoint: `/api/notifications/{id}/read`

Auth: Required

Roles: student/staff/admin

Purpose: Mark one notification as read.

Request: path `id`.

Success Response:

```json
{"message":"Notification marked as read.","data":{"id":1,"read_at":"2026-06-21 12:00:00"}}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `NotificationController@read`

Notes: Returns 403 if notification belongs to another user.

### Delete Notification

Method: DELETE

Endpoint: `/api/notifications/{id}`

Auth: Required

Roles: student/staff/admin

Purpose: Delete one notification.

Request: path `id`.

Success Response:

```json
{"message":"Notification deleted successfully."}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `NotificationController@destroy`

Notes: Current user's notifications only.

## Student Portal APIs

### Student Dashboard

Method: GET

Endpoint: `/api/student/dashboard`

Auth: Required

Roles: student

Purpose: Student home/dashboard data.

Request: none.

Success Response:

```json
{"message":"Student dashboard loaded successfully.","data":{"summary":{"issued_books":2,"pending_fines":0}}}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StudentDashboardController@__invoke`

Notes: Requires authenticated student role.

### Student My Books

Method: GET

Endpoint: `/api/student/my-books`

Auth: Required

Roles: student

Purpose: Student issued/current/history book overview.

Request: optional `per_page`, filters supported by controller.

Success Response:

```json
{"data":[{"id":1,"book":{"title":"Clean Code"},"status":"issued"}]}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StudentBookController@index`

Notes: Uses authenticated student's profile.

### Student My Books Summary

Method: GET

Endpoint: `/api/student/my-books/summary`

Auth: Required

Roles: student

Purpose: Counts for current, returned, overdue, and due-soon books.

Request: none.

Success Response:

```json
{"message":"Book summary fetched successfully.","data":{"current":2,"history":5,"due_soon":1}}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StudentBookController@summary`

Notes: Current student only.

### Student Current Books

Method: GET

Endpoint: `/api/student/my-books/current`

Auth: Required

Roles: student

Purpose: Current active issued books.

Request: `per_page` optional.

Success Response:

```json
{"data":[{"id":10,"book":{"title":"Clean Code"},"return_date":null}]}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StudentBookController@current`

Notes: Filters active issues.

### Student Book History

Method: GET

Endpoint: `/api/student/my-books/history`

Auth: Required

Roles: student

Purpose: Returned/history books.

Request: `per_page` optional.

Success Response:

```json
{"data":[{"id":9,"book":{"title":"Design Patterns"},"status":"returned"}]}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StudentBookController@history`

Notes: Current student only.

### Student Due Soon Books

Method: GET

Endpoint: `/api/student/my-books/due-soon`

Auth: Required

Roles: student

Purpose: Books approaching due date.

Request: `per_page` optional.

Success Response:

```json
{"data":[{"id":10,"book":{"title":"Clean Code"},"due_date":"2026-06-23"}]}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StudentBookController@dueSoon`

Notes: Current student only.

### Student Requests

Method: GET

Endpoint: `/api/student/requests`

Auth: Required

Roles: student

Purpose: List authenticated student's book requests.

Request: optional `status`, `per_page`.

Success Response:

```json
{"data":[{"id":1,"book":{"title":"Clean Code"},"status":"pending"}]}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StudentBookRequestController@index`

Notes: Student sees only own requests.

### Create Student Request

Method: POST

Endpoint: `/api/student/requests`

Auth: Required

Roles: student

Purpose: Create a book request.

Request: `book_id` required.

Success Response:

```json
{"message":"Book request submitted successfully.","data":{"id":1,"status":"pending"}}
```

Error Response:

```json
{"message":"The given data was invalid.","errors":{"book_id":["This student already has an active request for this book."]}}
```

Controller: `StudentBookRequestController@store`

Notes: Uses `BookRequestManagementActionService`.

### Student Request Summary

Method: GET

Endpoint: `/api/student/requests/summary`

Auth: Required

Roles: student

Purpose: Count requests by status.

Request: none.

Success Response:

```json
{"message":"Request summary fetched successfully.","data":{"pending":1,"approved":0,"rejected":0}}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StudentBookRequestController@summary`

Notes: Current student only.

### Student Request Detail

Method: GET

Endpoint: `/api/student/requests/{id}`

Auth: Required

Roles: student

Purpose: Show one of the student's book requests.

Request: path `id`.

Success Response:

```json
{"data":{"id":1,"book":{"title":"Clean Code"},"status":"pending"}}
```

Error Response:

```json
{"message":"Resource not found."}
```

Controller: `StudentBookRequestController@show`

Notes: Fails if request does not belong to current student.

### Cancel Student Request

Method: POST

Endpoint: `/api/student/requests/{id}/cancel`

Auth: Required

Roles: student

Purpose: Cancel own active request.

Request: path `id`.

Success Response:

```json
{"message":"Book request cancelled successfully.","data":{"id":1,"status":"cancelled"}}
```

Error Response:

```json
{"message":"Only pending or approved requests can be cancelled."}
```

Controller: `StudentBookRequestController@cancel`

Notes: Current student only.

### Student Fines

Method: GET

Endpoint: `/api/student/fines`

Auth: Required

Roles: student

Purpose: List authenticated student's fines.

Request: optional `status`, `per_page`.

Success Response:

```json
{"data":[{"id":1,"amount":25,"status":"pending"}]}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StudentFineController@index`

Notes: Uses `FineResource`.

### Student Pending Fines

Method: GET

Endpoint: `/api/student/fines/pending`

Auth: Required

Roles: student

Purpose: List pending fines.

Request: `per_page` optional.

Success Response:

```json
{"data":[{"id":1,"amount":25,"status":"pending"}]}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StudentFineController@pending`

Notes: Current student only.

### Student Paid Fines

Method: GET

Endpoint: `/api/student/fines/paid`

Auth: Required

Roles: student

Purpose: List paid fines.

Request: `per_page` optional.

Success Response:

```json
{"data":[{"id":2,"amount":10,"status":"paid"}]}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StudentFineController@paid`

Notes: Current student only.

### Student Fine Summary

Method: GET

Endpoint: `/api/student/fines/summary`

Auth: Required

Roles: student

Purpose: Fine totals for current student.

Request: none.

Success Response:

```json
{"message":"Fine summary fetched successfully.","data":{"pending_amount":25,"paid_amount":10}}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StudentFineController@summary`

Notes: Current student only.

### Student Fine Detail

Method: GET

Endpoint: `/api/student/fines/{id}`

Auth: Required

Roles: student

Purpose: Show one fine belonging to current student.

Request: path `id`.

Success Response:

```json
{"data":{"id":1,"amount":25,"status":"pending","book":{"title":"Clean Code"}}}
```

Error Response:

```json
{"message":"Resource not found."}
```

Controller: `StudentFineController@show`

Notes: Current student only.

## Shared Staff/Admin APIs

### Shared Students

Method: GET

Endpoint: `/api/students`

Auth: Required

Roles: staff/admin

Purpose: Existing paginated staff/admin student list.

Request: `per_page` optional.

Success Response:

```json
{"data":[{"id":1,"name":"Saroj Mehta","roll_no":"CS-2023-001","status":"active"}]}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StudentController@index`

Notes: Kept for backward compatibility.

### Shared Student Search

Method: GET

Endpoint: `/api/students/search`

Auth: Required

Roles: staff/admin

Purpose: Existing student search.

Request: `q` required, `per_page` optional.

Success Response:

```json
{"data":[{"id":1,"name":"Saroj Mehta","roll_no":"CS-2023-001"}]}
```

Error Response:

```json
{"message":"The given data was invalid.","errors":{"q":["The q field is required."]}}
```

Controller: `StudentController@search`

Notes: New staff issue flow should prefer `/api/staff/students/search`.

### Shared Student Detail

Method: GET

Endpoint: `/api/students/{id}`

Auth: Required

Roles: staff/admin

Purpose: Existing student detail.

Request: path `id`.

Success Response:

```json
{"data":{"id":1,"name":"Saroj Mehta","email":"student@example.com"}}
```

Error Response:

```json
{"message":"Resource not found."}
```

Controller: `StudentController@show`

Notes: Uses `StudentResource`.

### Shared Issues

Method: GET

Endpoint: `/api/issues`

Auth: Required

Roles: staff/admin

Purpose: Existing issue list.

Request: optional `status`, `per_page`.

Success Response:

```json
{"data":[{"id":10,"issue_id":10,"status":"issued","book":{"title":"Clean Code"}}]}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `IssueController@index`

Notes: Kept for existing Android clients.

### Shared Issue Books

Method: POST

Endpoint: `/api/issues`

Auth: Required

Roles: staff/admin

Purpose: Existing issue creation endpoint.

Request: `student_id`, `book_id` or `book_ids`, optional `issue_date`, `due_date`, `remarks`.

Success Response:

```json
{"message":"Book issue completed successfully.","issued_count":1,"data":[{"id":10,"status":"issued"}]}
```

Error Response:

```json
{"message":"This issue would exceed the student borrowing limit."}
```

Controller: `IssueController@store`

Notes: New staff issue flow should prefer `/api/staff/issues`.

### Shared Issue Detail

Method: GET

Endpoint: `/api/issues/{id}`

Auth: Required

Roles: staff/admin

Purpose: Existing issue detail.

Request: path `id`.

Success Response:

```json
{"data":{"id":10,"status":"issued","due_date":"2026-07-09"}}
```

Error Response:

```json
{"message":"Resource not found."}
```

Controller: `IssueController@show`

Notes: Uses `IssueResource`.

### Shared Return Book

Method: POST

Endpoint: `/api/issues/return/{id}`

Auth: Required

Roles: staff/admin

Purpose: Existing return endpoint.

Request: optional `condition`, `return_date`, `remarks`.

Success Response:

```json
{"message":"Book returned successfully.","data":{"id":10,"status":"returned"}}
```

Error Response:

```json
{"message":"This book has already been returned."}
```

Controller: `IssueController@returnBook`

Notes: New staff return flow should prefer `/api/staff/issues/{issue}/return`.

### Shared Student Issues

Method: GET

Endpoint: `/api/issues/student/{studentId}`

Auth: Required

Roles: staff/admin

Purpose: Existing issues by student.

Request: path `studentId`, optional `per_page`.

Success Response:

```json
{"data":[{"id":10,"status":"issued","book":{"title":"Clean Code"}}]}
```

Error Response:

```json
{"message":"Resource not found."}
```

Controller: `IssueController@studentIssues`

Notes: Student must exist.

### Overdue Issues

Method: GET

Endpoint: `/api/overdue`

Auth: Required

Roles: staff/admin

Purpose: Existing overdue issue list.

Request: `per_page` optional.

Success Response:

```json
{"data":[{"id":10,"status":"issued","due_date":"2026-06-01"}]}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `IssueController@overdue`

Notes: Filters active issues where due date is before today.

### Shared Fines

Method: GET

Endpoint: `/api/fines`

Auth: Required

Roles: staff/admin

Purpose: Existing fine list.

Request: `per_page` optional.

Success Response:

```json
{"data":[{"id":1,"amount":25,"status":"pending"}]}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `IssueController@fines`

Notes: New staff fine management should prefer `/api/staff/fines`.

### Shared Student Fines

Method: GET

Endpoint: `/api/fines/student/{id}`

Auth: Required

Roles: staff/admin

Purpose: Existing fines for a student.

Request: path student `id`, optional `per_page`.

Success Response:

```json
{"data":[{"id":1,"student":{"id":1},"amount":25,"status":"pending"}]}
```

Error Response:

```json
{"message":"Resource not found."}
```

Controller: `IssueController@studentFines`

Notes: Student must exist.

### Shared Book Requests

Method: GET

Endpoint: `/api/book-requests`

Auth: Required

Roles: staff/admin

Purpose: Existing staff/admin request list.

Request: optional `status`, `per_page`.

Success Response:

```json
{"data":[{"id":1,"status":"pending","book":{"title":"Clean Code"}}]}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `BookRequestController@index`

Notes: New staff portal may prefer `/api/staff/book-requests`.

### Shared Book Request Detail

Method: GET

Endpoint: `/api/book-requests/{id}`

Auth: Required

Roles: staff/admin

Purpose: Existing request detail.

Request: path `id`.

Success Response:

```json
{"data":{"id":1,"status":"pending","book":{"title":"Clean Code"}}}
```

Error Response:

```json
{"message":"Resource not found."}
```

Controller: `BookRequestController@show`

Notes: Uses `BookRequestResource`.

### Shared Approve Book Request

Method: POST

Endpoint: `/api/book-requests/{id}/approve`

Auth: Required

Roles: staff/admin

Purpose: Existing approve request endpoint.

Request: path `id`.

Success Response:

```json
{"message":"Book request approved successfully.","data":{"id":1,"status":"approved"}}
```

Error Response:

```json
{"message":"The given data was invalid.","errors":{"request":["Only pending book requests can be updated from this page."]}}
```

Controller: `BookRequestController@approve`

Notes: Reuses `BookRequestManagementActionService`.

### Shared Reject Book Request

Method: POST

Endpoint: `/api/book-requests/{id}/reject`

Auth: Required

Roles: staff/admin

Purpose: Existing reject request endpoint.

Request: optional `remarks`.

Success Response:

```json
{"message":"Book request rejected successfully.","data":{"id":1,"status":"rejected"}}
```

Error Response:

```json
{"message":"The given data was invalid.","errors":{"request":["Only pending book requests can be updated from this page."]}}
```

Controller: `BookRequestController@reject`

Notes: Rejection remarks are attached to latest rejection notification.

## Staff Portal APIs

### Staff Dashboard

Method: GET

Endpoint: `/api/staff/dashboard`

Auth: Required

Roles: staff/admin

Purpose: Mobile dashboard data for staff.

Request: none.

Success Response:

```json
{"success":true,"message":"Staff dashboard loaded successfully.","data":{"staff":{"id":2,"name":"Anil Kapoor","role":"staff"},"summary":{"issued_books":74,"pending_requests":48,"pending_fines_amount":2386,"overdue_books":69},"due_today":[],"latest_pending_requests":[],"top_overdue_books":[],"recent_issues":[],"weekly_activity":{"issued_this_week":7,"returned_this_week":3,"average_per_day":1.4}}}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StaffDashboardController@__invoke`

Notes: Also keeps legacy keys such as `pending_requests_list` and `overdue_books_list`.

### Staff Students

Method: GET

Endpoint: `/api/staff/students`

Auth: Required

Roles: staff/admin

Purpose: Paginated mobile staff student list.

Request: optional `query`, `per_page`.

Success Response:

```json
{"success":true,"message":"Students fetched successfully.","data":[{"id":1,"user_id":37,"name":"Saroj Mehta","student_id":"CS-2023-001","current_issued":2,"max_books":5,"can_issue":true,"profile_photo":"profile_photos/student.jpg","profile_photo_url":"http://YOUR_SERVER/storage/profile_photos/student.jpg"}],"meta":{"total":1}}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StaffStudentController@index`

Notes: Includes issue capacity summary and student photo fields from `users.profile_photo` when available. Run `php artisan storage:link` and set `APP_URL` to a phone-accessible server URL, not `localhost`, for Android devices.

### Search Students For Issue Book

Method: GET

Endpoint: `/api/staff/students/search`

Auth: Required

Roles: staff/admin

Purpose: Staff searches students by name, student id, roll no, email, phone, or department.

Request: query `query` or `q`.

Success Response:

```json
{"success":true,"message":"Students fetched successfully.","data":[{"id":1,"user_id":37,"name":"Saroj Mehta","email":"student@example.com","student_id":"CS-2023-001","roll_no":"CS-2023-001","department":"Computer Science","status":"active","current_issued":2,"max_books":5,"can_issue":true,"profile_photo":"profile_photos/student.jpg","profile_photo_url":"http://YOUR_SERVER/storage/profile_photos/student.jpg"}]}
```

Error Response:

```json
{"message":"Unauthenticated."}
```

Controller: `StaffStudentController@search`

Notes: Returns at most 20 students. `profile_photo_url` is generated with `url(Storage::url($path))`; configure `APP_URL` for the Android device network.

### Staff Student Detail

Method: GET

Endpoint: `/api/staff/students/{student}`

Auth: Required

Roles: staff/admin

Purpose: Staff mobile student detail.

Request: path `student`.

Success Response:

```json
{"success":true,"message":"Student fetched successfully.","data":{"id":1,"name":"Saroj Mehta","profile_photo":"profile_photos/student.jpg","profile_photo_url":"http://YOUR_SERVER/storage/profile_photos/student.jpg","active_issued_books":[],"returned_books_count":3,"pending_fines_amount":0,"pending_requests_count":1}}
```

Error Response:

```json
{"message":"Resource not found."}
```

Controller: `StaffStudentController@show`

Notes: Includes active issues, pending requests, pending fines, and student photo fields.

### Student Issue Privileges

Method: GET

Endpoint: `/api/staff/students/{student}/issue-privileges`

Auth: Required

Roles: staff/admin

Purpose: Android Issue Book page checks whether selected student may borrow.

Request: path `student`.

Success Response:

```json
{"success":true,"message":"Issue privileges fetched successfully.","data":{"student":{"id":1,"name":"Saroj Mehta","email":"student@example.com","student_id":"CS-2023-001","department":"Computer Science","status":"active","profile_photo":"profile_photos/student.jpg","profile_photo_url":"http://YOUR_SERVER/storage/profile_photos/student.jpg"},"privileges":{"allowed":true,"reason":null,"max_books":5,"already_issued":2,"can_issue":3,"duration_days":14,"fine_rate":5,"issue_date":"2026-06-21","due_date":"2026-07-05","has_pending_fines":false,"pending_fine_amount":0,"account_status":"active","policy_source":"fine_settings","policy_name":"Default library policy"}}}
```

Error Response:

```json
{"message":"Resource not found."}
```

Controller: `StaffIssueController@privileges`

Notes: Uses `StudentIssuePrivilegeService`; inactive account, suspended borrowing, or issue limit reached set `allowed=false`. The nested `student` object includes `profile_photo` and `profile_photo_url`.

### Search Books For Issue Book

Method: GET

Endpoint: `/api/staff/books/search`

Auth: Required

Roles: staff/admin

Purpose: Staff searches books to issue.

Request: query `query` or `q`; optional `student_id` or `studentId`.

Success Response:

```json
{"success":true,"message":"Books fetched successfully.","data":[{"id":3,"book_id":3,"copy_id":null,"title":"Clean Code","author":"Robert C. Martin","isbn":"0001000002","accession_no":"0001000002","category":"Software Engineering","total_copies":5,"available_copies":6,"is_available":true,"status":"available","reason":null}]}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StaffIssueController@searchBooks`

Notes: This project issues by `book_id`; no separate copy table exists.

### Staff Issue Preview

Method: POST

Endpoint: `/api/staff/issues/preview`

Auth: Required

Roles: staff/admin

Purpose: Preview selected books before issuing.

Request:

```json
{"student_id":1,"book_ids":[3,5]}
```

Success Response:

```json
{"success":true,"message":"Issue preview fetched successfully.","data":{"student":{"id":1,"name":"Saroj Mehta"},"selected_books":[],"privileges":{"can_issue":3},"issue_date":"2026-06-21","due_date":"2026-07-05","warnings":[],"errors":[]}}
```

Error Response:

```json
{"success":false,"message":"Issue preview contains validation errors.","data":{"errors":["The student already has 'Clean Code' issued."]}}
```

Controller: `StaffIssueController@preview`

Notes: Does not mutate database.

### Issue Multiple Books

Method: POST

Endpoint: `/api/staff/issues`

Auth: Required

Roles: staff/admin

Purpose: Issue one or more books to a student.

Request:

```json
{"student_id":1,"book_ids":[3,5],"remarks":"Optional"}
```

Success Response:

```json
{"success":true,"message":"Books issued successfully.","data":{"student":{"id":1,"name":"Saroj Mehta","student_id":"CS-2023-001"},"issued_count":2,"issue_date":"2026-06-21","due_date":"2026-07-05","issued_books":[{"issue_id":10,"book_id":3,"title":"Clean Code","author":"Robert C. Martin","due_date":"2026-07-05","status":"issued"}]}}
```

Error Response:

```json
{"success":false,"message":"Student can only issue 1 more book.","errors":{"book_ids":["Selected books exceed allowed issue limit."]}}
```

Controller: `StaffIssueController@store`

Notes: Atomic DB transaction; validates active student, borrowing privilege, issue limit, availability, duplicate active issue, and decrements `available_copies`.

### Search Active Issues

Method: GET

Endpoint: `/api/staff/issues/search`

Auth: Required

Roles: staff/admin

Purpose: Staff searches active issues before returning books.

Request: query `query` or `q`.

Success Response:

```json
{"success":true,"message":"Active issues fetched successfully.","data":[{"issue_id":10,"book_title":"Clean Code","student_name":"Saroj Mehta","issue_date":"2026-06-21","due_date":"2026-07-05","overdue_days":0,"estimated_fine":0,"status":"issued"}]}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StaffReturnController@search`

Notes: Searches issue id, student name/id/email, book title/author/ISBN.

### Return Book

Method: POST

Endpoint: `/api/staff/issues/{issue}/return`

Auth: Required

Roles: staff/admin

Purpose: Return an issued book and calculate pending fine if needed.

Request:

```json
{"condition":"good","notes":"Optional"}
```

Success Response:

```json
{"success":true,"message":"Book returned successfully.","data":{"issue":{"issue_id":10,"status":"returned","fine_amount":0},"fine":null}}
```

Error Response:

```json
{"success":false,"message":"This book has already been returned."}
```

Controller: `StaffReturnController@returnBook`

Notes: Creates/updates pending fine for overdue/fair/damaged/lost returns; increments availability for good/fair returns.

### Staff Fines

Method: GET

Endpoint: `/api/staff/fines`

Auth: Required

Roles: staff/admin

Purpose: Mobile staff fine list.

Request: optional `status`, `student`, `student_id`, `date`, `query`, `search`, `per_page`.

Success Response:

```json
{"success":true,"message":"Fines fetched successfully.","data":[{"fine_id":1,"student_name":"Saroj Mehta","book_title":"Clean Code","amount":25,"status":"pending","issue_id":10,"due_date":"2026-06-01","paid_date":null}],"meta":{"total":1}}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StaffFineController@index`

Notes: Searches fine id, remarks, student, and book fields.

### Staff Fine Summary

Method: GET

Endpoint: `/api/staff/fines/summary`

Auth: Required

Roles: staff/admin

Purpose: Return total, collected, pending, waived, record counts, and overdue record count for the staff mobile fines page.

Request: none.

Success Response:

```json
{"success":true,"message":"Fine summary fetched successfully.","data":{"total_fines":3790,"collected":0,"pending":3790,"waived":0,"total_records":83,"paid_records":0,"pending_records":83,"waived_records":0,"overdue_records":83}}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StaffFineController@summary`

Notes: Uses real `fines` records. Amounts are grouped by `pending`, `paid`, and `waived` status.

### Student-wise Staff Fine List

Method: GET

Endpoint: `/api/staff/fines/students`

Auth: Required

Roles: staff/admin

Purpose: Return one fine summary row per student for the staff mobile fines list.

Request: optional `search`. Search checks student name, email, student id, roll/symbol number, department, book title/author/isbn, fine amount, fine id, fine reason, and waiver reason.

Success Response:

```json
{"success":true,"message":"Student fine records fetched successfully.","data":[{"student_id":1,"user_id":5,"name":"Saroj Mehta","email":"saroj@example.com","roll_no":"CS-2023-001","symbol_no":"CS-2023-001","department":"Computer Science","photo":null,"total_fine":1300,"pending_amount":1300,"paid_amount":0,"waived_amount":0,"fine_records_count":3,"pending_records_count":3,"paid_records_count":0,"waived_records_count":0,"overdue_records_count":3,"status":"pending"}]}
```

No Records Response:

```json
{"success":true,"message":"No fine records found.","data":[]}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StaffFineController@students`

Notes: This endpoint is grouped by student and does not return every fine record.

### Staff Student Fine Detail

Method: GET

Endpoint: `/api/staff/fines/students/{student}`

Auth: Required

Roles: staff/admin

Purpose: Return student information, that student's fine summary, and all fine records for the selected student.

Request: path `student` is the students table id.

Success Response:

```json
{"success":true,"message":"Student fine detail fetched successfully.","data":{"student":{"id":1,"student_id":1,"user_id":5,"name":"Saroj Mehta","email":"saroj@example.com","roll_no":"CS-2023-001","symbol_no":"CS-2023-001","department":"Computer Science","photo":null},"summary":{"total_fine":1300,"pending_amount":1300,"paid_amount":0,"waived_amount":0,"records_count":3,"pending_records":3,"paid_records":0,"waived_records":0,"overdue_records":3},"fines":[{"id":10,"issue_id":25,"book_id":4,"book_title":"Clean Code","author":"Robert C. Martin","due_date":"2026-07-11","return_date":"2026-07-12","days_overdue":0,"amount":250,"status":"pending","reason":"Damaged book","fine_type":"Damaged book","waive_reason":null,"paid_at":null,"waived_at":null,"created_at":"2026-06-21 17:00:00","updated_at":"2026-06-21 17:00:00"}]}}
```

No Fine Records Response:

```json
{"success":true,"message":"Student fine detail fetched successfully.","data":{"student":{"id":1,"student_id":1,"name":"Saroj Mehta"},"summary":{"total_fine":0,"pending_amount":0,"paid_amount":0,"waived_amount":0,"records_count":0,"pending_records":0,"paid_records":0,"waived_records":0,"overdue_records":0},"fines":[]}}
```

Error Response:

```json
{"message":"Resource not found."}
```

Controller: `StaffFineController@studentDetails`

### Staff Fine Detail

Method: GET

Endpoint: `/api/staff/fines/{fine}`

Auth: Required

Roles: staff/admin

Purpose: Mobile staff fine detail.

Request: path `fine`.

Success Response:

```json
{"success":true,"message":"Fine fetched successfully.","data":{"fine_id":1,"amount":25,"status":"pending","book_title":"Clean Code","waive_reason":null,"paid_at":null,"waived_at":null}}
```

Error Response:

```json
{"message":"Resource not found."}
```

Controller: `StaffFineController@show`

Notes: Includes student, issue, and book summary fields.

### Pay Fine

Method: POST

Endpoint: `/api/staff/fines/{fine}/pay`

Auth: Required

Roles: staff/admin

Purpose: Mark a pending fine as paid.

Request: optional `payment_method` in `cash`, `card`, `online`.

Success Response:

```json
{"success":true,"message":"Fine marked as paid successfully.","data":{"fine_id":1,"status":"paid","amount":25,"paid_at":"2026-06-21 17:30:00","paid_by":2}}
```

Error Response:

```json
{"success":false,"message":"Only pending fines can be updated from this page.","errors":{"fine":["Only pending fines can be updated from this page."]}}
```

Controller: `StaffFineController@pay`

Notes: Reuses `FineManagementActionService`, sends notification/email, and logs activity.

### Waive Fine

Method: POST

Endpoint: `/api/staff/fines/{fine}/waive`

Auth: Required

Roles: staff/admin

Purpose: Waive a pending fine.

Request:

```json
{"reason":"Approved by librarian"}
```

Success Response:

```json
{"success":true,"message":"Fine waived successfully.","data":{"fine_id":1,"status":"waived","amount":25,"waive_reason":"Approved by librarian","waived_at":"2026-06-21 17:32:00","waived_by":2}}
```

Error Response:

```json
{"success":false,"message":"The reason field is required.","errors":{"reason":["The reason field is required."]}}
```

Controller: `StaffFineController@waive`

Notes: Requires `reason` between 3 and 500 characters, persists it in `waive_reason`, and only acts on pending fines.

### Staff Book Requests

Method: GET

Endpoint: `/api/staff/book-requests`

Auth: Required

Roles: staff/admin

Purpose: Mobile staff book request list.

Request: optional `status`, `student_id`, `query`, `search`, `per_page`.

Success Response:

```json
{"success":true,"message":"Book requests fetched successfully.","data":[{"request_id":1,"student_name":"Saroj Mehta","book_title":"Clean Code","available_copies":2,"status":"pending"}],"meta":{"total":1}}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `StaffBookRequestController@index`

Notes: Searches request id, student, and book fields.

### Staff Book Request Detail

Method: GET

Endpoint: `/api/staff/book-requests/{bookRequest}`

Auth: Required

Roles: staff/admin

Purpose: Show one mobile staff book request.

Request: path `bookRequest`.

Success Response:

```json
{"success":true,"message":"Book request fetched successfully.","data":{"request_id":1,"status":"pending","book_title":"Clean Code"}}
```

Error Response:

```json
{"message":"Resource not found."}
```

Controller: `StaffBookRequestController@show`

Notes: Includes availability for approve screen.

### Staff Approve Book Request

Method: POST

Endpoint: `/api/staff/book-requests/{bookRequest}/approve`

Auth: Required

Roles: staff/admin

Purpose: Approve a pending book request.

Request: path `bookRequest`.

Success Response:

```json
{"success":true,"message":"Book request approved successfully.","data":{"request_id":1,"status":"approved"}}
```

Error Response:

```json
{"success":false,"message":"Requested book is not available."}
```

Controller: `StaffBookRequestController@approve`

Notes: Validates availability and pending status. It approves only; issuing remains a separate staff issue operation.

### Staff Reject Book Request

Method: POST

Endpoint: `/api/staff/book-requests/{bookRequest}/reject`

Auth: Required

Roles: staff/admin

Purpose: Reject a pending book request.

Request:

```json
{"reason":"Duplicate request"}
```

Success Response:

```json
{"success":true,"message":"Book request rejected successfully.","data":{"request_id":1,"status":"rejected"}}
```

Error Response:

```json
{"success":false,"message":"Only pending book requests can be updated from this page.","errors":{"request":["Only pending book requests can be updated from this page."]}}
```

Controller: `StaffBookRequestController@reject`

Notes: Optional reason is added to the rejection notification data.

## Admin APIs

### Admin Dashboard

Method: GET

Endpoint: `/api/admin/dashboard`

Auth: Required

Roles: admin

Purpose: Admin mobile dashboard.

Request: none.

Success Response:

```json
{"message":"Admin dashboard loaded successfully.","data":{"total_books":100,"total_students":50}}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `AdminDashboardController@__invoke`

Notes: Admin role only.

### Activity Logs

Method: GET

Endpoint: `/api/activity-logs`

Auth: Required

Roles: admin

Purpose: List activity logs.

Request: optional filters supported by `ActivityLogController`.

Success Response:

```json
{"data":[{"id":1,"action":"book_issued","description":"Book issued"}]}
```

Error Response:

```json
{"message":"Forbidden."}
```

Controller: `ActivityLogController@__invoke`

Notes: Admin role only.

## Authentication Rules

- Public routes: `/api/login`, `/api/forgot-password`, `/api/reset-password`, `/api/test-unauthorized`.
- All other routes require `auth:sanctum`.
- Send bearer token as `Authorization: Bearer {access_token}`.
- Missing or invalid token returns HTTP 401 JSON.

## Role Access Rules

- Student-only routes are under `/api/student/...` and require role `student`.
- Staff portal routes are under `/api/staff/...` and require role `staff` or `admin`.
- Shared staff/admin routes include `/api/students`, `/api/issues`, `/api/fines`, `/api/book-requests`, `/api/overdue`.
- Admin-only routes are `/api/admin/dashboard` and `/api/activity-logs`.
- A student token calling any staff route returns HTTP 403 JSON.
