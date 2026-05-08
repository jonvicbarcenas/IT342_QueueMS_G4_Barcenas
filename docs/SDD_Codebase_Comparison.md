# QueueMS SDD vs Current Codebase Comparison

Date prepared: May 8, 2026  
Source design document: `docs/SDD_QueueMS_Barcenas.docx`  
SDD version referenced: 2.1, dated March 31, 2026  
Codebase scope reviewed: `backend/src/main`, `web/src`, `mobile/app/src/main`

## Purpose

This document compares the current QueueMS codebase against the requirements in `SDD_QueueMS_Barcenas.docx`. It also records the latest implementation changes made to bring the project closer to the SDD.

The SDD describes QueueMS as a smart campus queue management system with:

- Web app for Superadmins, Tellers, and Users.
- Android mobile app for Users.
- Role-based access control.
- Service request CRUD.
- Google OAuth.
- SMTP email notifications.
- Firebase Cloud Messaging push notifications.
- Public holiday blocking.
- Real-time WebSocket updates.
- Supporting document upload.

## Status Legend

| Status | Meaning |
| --- | --- |
| Implemented | The current codebase has a working implementation for the requirement. |
| Partial | Some support exists, but the implementation differs from the SDD or is incomplete. |
| Gap | The feature is described in the SDD but is not currently implemented. |
| Different Design | The codebase implements the intent through a different technical approach. |

## Executive Summary

The backend and web app now cover most of the SDD's core queue-management workflow:

- User registration/login/profile retrieval.
- Backend JWT authentication and BCrypt password hashing.
- Role-based endpoint protection.
- User queue request creation, viewing, cancellation, and attachment upload.
- Teller queue serving and counter status management.
- Superadmin user and counter management.
- Public holiday blocking and holiday banner support.
- SMTP notification hooks.
- FCM notification hooks with Android channel metadata.
- WebSocket queue broadcasts.
- Cloudinary-backed supporting document storage.
- Google OAuth via Spring Security OAuth2.
- Shared web account menu with profile name editing and sign out.

The largest remaining mismatch is authentication architecture. The SDD specifies Firebase Authentication on the clients, Firebase ID token verification on the backend, and backend JWT issuance after Firebase verification. The current codebase uses custom email/password authentication stored in Firestore with BCrypt, backend JWTs, and Spring OAuth2 for Google login.

The mobile app has been brought forward from an auth-only shell into the SDD's main user workflow. It now supports the user dashboard, create/list/detail/cancel service requests, public holiday blocking, supporting document upload to the backend Cloudinary flow, profile editing, Firebase Cloud Messaging token registration/receiver hooks, native phone push notification display, an in-app notification feed, backend-calculated queue position/estimated wait, and queue refresh through foreground polling plus a native STOMP WebSocket subscription to the backend user topic.

## Latest Implementation Changes

### Authentication and Account UX

Implemented:

- Added backend profile update endpoint:
  - `PUT /api/auth/me`
- Added `UpdateProfileDTO`.
- Added `AuthService.updateProfile(...)`.
- Web auth context now exposes `updateProfile(...)`.
- Added shared `AccountMenu` component:
  - Displays initials, name, email, and role.
  - Supports editing first and last name.
  - Contains sign-out action.
- Replaced direct dashboard logout buttons with the shared account menu in:
  - `web/src/pages/admin/AdminDashboardPage.tsx`
  - `web/src/pages/teller/TellerDashboardPage.tsx`
  - `web/src/pages/users/UserDashboardPage.tsx`
- Teller name changes are synced to assigned counter display names.

Relevant files:

- `backend/src/main/java/edu/cit/barcenas/queuems/controller/AuthController.java`
- `backend/src/main/java/edu/cit/barcenas/queuems/dto/UpdateProfileDTO.java`
- `backend/src/main/java/edu/cit/barcenas/queuems/service/AuthService.java`
- `web/src/components/common/AccountMenu.tsx`
- `web/src/context/AuthContext.tsx`
- `web/src/services/authService.ts`

### Google OAuth

Implemented:

- Google OAuth now auto-enables when `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` exist.
- Frontend asks the backend for the OAuth URL through:
  - `GET /api/auth/google/login`
- OAuth redirects through Spring Security:
  - `/oauth2/authorization/google`
  - `/login/oauth2/code/google`
- Backend success handler creates or finds the user and issues a backend JWT.

Difference from SDD:

- SDD specifies Google OAuth through Firebase Authentication.
- Current code uses Spring Security OAuth2 directly.

Required environment variables:

```env
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
GOOGLE_OAUTH_ENABLED=auto
```

### Supporting Document Upload

Implemented:

- Service requests now support attachment metadata:
  - `attachmentOriginalName`
  - `attachmentStoredName`
  - `attachmentContentType`
  - `attachmentUrl`
- Added upload endpoint:
  - `POST /api/requests/{id}/attachment`
- Added attachment redirect endpoint:
  - `GET /api/requests/{id}/attachment`
- User dashboard supports selecting a PDF/JPG/PNG/WEBP file when creating a request.
- User dashboard opens the Cloudinary attachment URL directly.
- Attachments are stored in Cloudinary, not local disk.
- PDFs upload as Cloudinary `raw` resources.
- Images upload as Cloudinary `image` resources.

Relevant files:

- `backend/src/main/java/edu/cit/barcenas/queuems/model/ServiceRequest.java`
- `backend/src/main/java/edu/cit/barcenas/queuems/service/FileStorageService.java`
- `backend/src/main/java/edu/cit/barcenas/queuems/service/ServiceRequestService.java`
- `backend/src/main/java/edu/cit/barcenas/queuems/controller/users/UserServiceRequestController.java`
- `web/src/pages/users/UserDashboardPage.tsx`
- `web/src/services/users/userServiceRequestService.ts`

Required environment variables:

```env
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
CLOUDINARY_FOLDER=queuems/service-request-attachments
```

Notes:

- Existing attachments uploaded before the Cloudinary resource-type fix may have invalid `/image/upload/...pdf.pdf` URLs. Re-upload those attachments so PDFs are stored under `/raw/upload/...`.

### SMTP Email Notifications

Implemented:

- `EmailService` sends simple SMTP messages.
- Registration sends a welcome email.
- Service request creation sends a queue confirmation email.
- Status changes send update emails through `NotificationObserver`.
- SMTP sender is configurable through environment variables.
- Email sending does not crash the main request flow if SMTP fails.

Relevant files:

- `backend/src/main/java/edu/cit/barcenas/queuems/service/EmailService.java`
- `backend/src/main/java/edu/cit/barcenas/queuems/service/AuthService.java`
- `backend/src/main/java/edu/cit/barcenas/queuems/service/ServiceRequestService.java`
- `backend/src/main/java/edu/cit/barcenas/queuems/pattern/observer/NotificationObserver.java`

Required Gmail-style environment variables:

```env
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=yourgmail@gmail.com
SMTP_PASSWORD=your_16_character_google_app_password
SMTP_FROM=yourgmail@gmail.com
```

Notes:

- `SMTP_PASSWORD` must be a Google App Password, not the normal Gmail password.
- Gmail requires 2-Step Verification before App Passwords can be created.

### FCM Push Notifications

Implemented:

- Backend has `FcmService`.
- Backend can store a user's FCM token through:
  - `PUT /api/auth/fcm-token`
- Queue creation and status updates attempt FCM notification delivery when a user has `fcmToken`.

Implemented:

- Mobile app requests notification permission on Android 13+.
- Mobile app retrieves the Firebase Messaging token and sends it to `PUT /api/auth/fcm-token`.
- `QueueMessagingService` stores refreshed FCM tokens, syncs refreshed tokens to the backend when an auth token is available, and displays received queue notifications through the Android notification tray.
- Backend FCM messages include high-priority Android notification metadata and the `queue_updates` notification channel.
- Mobile dashboard has an in-app notification button/feed for request status changes and received queue updates.

### Public Holiday Blocking

Implemented:

- Backend integrates Nager public holiday API:
  - `https://date.nager.at/api/v3/PublicHolidays/{year}/PH`
- Service request creation is blocked on public holidays.
- Added holiday status endpoint:
  - `GET /api/holidays/today`
- User dashboard displays a holiday warning and disables request creation on holidays.

Relevant files:

- `backend/src/main/java/edu/cit/barcenas/queuems/service/HolidayService.java`
- `backend/src/main/java/edu/cit/barcenas/queuems/controller/users/HolidayController.java`
- `backend/src/main/java/edu/cit/barcenas/queuems/dto/HolidayStatusDTO.java`
- `web/src/pages/users/UserDashboardPage.tsx`

### Real-Time Queue Updates

Implemented:

- Spring WebSocket/STOMP configuration exists.
- Queue status changes broadcast through observers.
- Backend publishes updates to:
  - `/topic/queue`
  - `/topic/user/{userId}`
  - `/topic/counter/{counterId}`
- Web dashboards subscribe through `webSocketService`.

Relevant files:

- `backend/src/main/java/edu/cit/barcenas/queuems/config/WebSocketConfig.java`
- `backend/src/main/java/edu/cit/barcenas/queuems/pattern/observer/WebSocketQueueObserver.java`
- `web/src/services/websocketService.ts`

Implemented:

- Mobile dashboard refreshes requests while the activity is foregrounded.
- `QueueRealtimeClient` connects to the backend native STOMP WebSocket endpoint and subscribes to `/topic/user/{uid}` so user-specific queue broadcasts trigger a dashboard refresh.

### Admin User Management

Implemented:

- Superadmin can list users.
- Superadmin can create staff users.
- Superadmin can update users.
- Superadmin can delete users.
- Admin dashboard supports edit/delete user actions.

Endpoints:

- `GET /api/admin/users`
- `GET /api/admin/tellers`
- `POST /api/admin/users`
- `PUT /api/admin/users/{id}`
- `DELETE /api/admin/users/{id}`

Relevant files:

- `backend/src/main/java/edu/cit/barcenas/queuems/controller/admin/AdminController.java`
- `backend/src/main/java/edu/cit/barcenas/queuems/service/AdminService.java`
- `web/src/pages/admin/AdminDashboardPage.tsx`
- `web/src/services/admin/adminService.ts`

### Counter Management

Implemented:

- Superadmin can list counters.
- Superadmin can create counters.
- Superadmin can update counters.
- Superadmin can delete counters.
- Teller assignment syncs between user and counter records.
- Users can list open counters.

Endpoints:

- `GET /api/admin/counters`
- `POST /api/admin/counters`
- `PUT /api/admin/counters/{id}`
- `DELETE /api/admin/counters/{id}`
- `GET /api/counters`

### Teller Queue Management

Implemented:

- Teller can view assigned counter.
- Teller can open/close counter.
- Teller can view assigned requests.
- Teller can serve next request.
- Teller can serve a specific request.
- Teller can update request status.

Endpoints:

- `GET /api/teller/counter`
- `PUT /api/teller/counter/status`
- `GET /api/teller/requests`
- `PUT /api/teller/requests/next/serve`
- `PUT /api/teller/requests/{id}/serve`
- `PUT /api/teller/requests/{id}/status`

Difference from SDD:

- SDD lists `PUT /requests/{id}/serve` and `PUT /requests/{id}` for teller actions.
- Current implementation scopes these under `/api/teller/requests/...`.

### User Service Request Workflow

Implemented:

- User can create a request.
- User can view own requests.
- User can view one owned request.
- User can cancel own pending request.
- User can attach supporting document to pending request.
- Request creation validates counter existence and open status.
- Request creation blocks on public holidays.

Endpoints:

- `POST /api/requests`
- `GET /api/requests`
- `GET /api/requests/me`
- `GET /api/requests/{id}`
- `DELETE /api/requests/{id}`
- `PUT /api/requests/{id}/cancel`
- `POST /api/requests/{id}/attachment`
- `GET /api/requests/{id}/attachment`

## Requirement-by-Requirement Comparison

| SDD Requirement | Current Status | Notes |
| --- | --- | --- |
| User registration, login, logout, `/me` | Implemented | Backend custom auth and JWT. Web supports login/register/logout/profile. Mobile supports auth basics. |
| Firebase Authentication plus backend JWT | Partial / Different Design | Backend uses Firestore-stored users, BCrypt, and backend JWT. Firebase Admin SDK is used for Firestore/FCM, not Firebase Auth ID-token verification. |
| BCrypt password hashing | Implemented | Passwords are encoded through Spring `PasswordEncoder`. |
| Role-based access control | Implemented | Security config protects `/api/admin/**`, `/api/teller/**`, and authenticated user endpoints. |
| ServiceRequest CRUD | Partial | Create/read/cancel implemented for users. Teller/admin update flows exist. Full generic CRUD is not exposed exactly as SDD paths. |
| SMTP email notifications | Implemented | Welcome, request confirmation, and status update hooks exist. Depends on valid SMTP configuration. |
| FCM push notifications | Implemented | Backend sends Android high-priority FCM messages; mobile registers/syncs FCM tokens and displays tray notifications through the `queue_updates` channel. Requires `google-services.json`. |
| External public holiday API | Implemented | Nager API integration and request blocking are implemented. |
| File upload support | Implemented | Cloudinary-backed attachment upload is implemented for service requests. |
| Real-time queue updates | Implemented | Backend broadcasts queue updates over STOMP; mobile subscribes through the native `/ws-native` endpoint and refreshes active queue data. |
| Estimated waiting time | Implemented | Backend exposes queue position with a simple 5-minute-per-person estimate; Android displays it on active requests. |
| Queue history tracking | Partial | Requests persist with statuses and dates, but no dedicated history/audit log exists. |
| Basic admin statistics | Implemented | Web admin dashboard computes request/counter summary counts. |
| In-app notification bell/feed | Implemented | Mobile has an in-app notification feed for request creation, cancellation, status changes, and real-time queue update events. |
| Rate limiting | Gap | SDD requires 100 requests/min per IP. No rate limiter is present. |
| Firestore security rules | Gap in repo | Firestore rules are not present in this codebase. |
| API response envelope | Gap / Different Design | SDD specifies `{ success, data, error, timestamp }`. Current endpoints mostly return raw DTO/model bodies or plain error strings. |
| Base URL `/api/v1` | Different Design | Current app uses `/api/...` routes. Changing this now would require coordinated backend, web, and mobile updates. |

## API Contract Differences

The SDD defines:

```text
https://[server_hostname]:[port]/api/v1
```

The current codebase uses:

```text
http://localhost:8080/api
```

Examples:

| SDD Path | Current Path |
| --- | --- |
| `/auth/me` | `/api/auth/me` |
| `/requests` | `/api/requests` |
| `/teller/requests` | `/api/teller/requests` |
| `/admin/users` | `/api/admin/users` |
| `/admin/counters` | `/api/admin/counters` |

The current clients are already wired to `/api/...`, so a move to `/api/v1/...` should be handled as a deliberate migration.

## Authentication Architecture Difference

The SDD expects this flow:

1. Client authenticates with Firebase Authentication.
2. Client sends Firebase ID token to backend.
3. Backend verifies Firebase ID token.
4. Backend loads role/profile from Firestore.
5. Backend issues its own JWT.

The current codebase uses this flow:

1. User submits email/password to backend.
2. Backend verifies Firestore user and BCrypt password.
3. Backend issues JWT.
4. JWT authorizes protected endpoints.

For Google login:

1. Web redirects to Spring OAuth2 Google login.
2. Backend receives OAuth2 success callback.
3. Backend creates/finds Firestore user.
4. Backend issues JWT and redirects to web callback route.

This is a valid implementation, but it is not the Firebase Authentication architecture described in the SDD.

## Web App Status

Implemented web views:

- Login
- Register
- OAuth callback
- Role-based dashboard router
- User dashboard
- Teller dashboard
- Superadmin dashboard
- Account/profile menu

Implemented user web features:

- Create request.
- View own requests.
- Cancel pending request.
- Upload supporting document.
- Open Cloudinary attachment.
- View holiday warning.
- Receive real-time updates through WebSocket.

Implemented teller web features:

- View assigned counter.
- Open/close counter.
- View requests assigned to counter.
- Serve next request.
- Serve specific request.
- Complete/cancel serving requests.

Implemented admin web features:

- View all counters.
- Create/edit/delete counters.
- View all users.
- Create/edit/delete users.
- View all requests.
- Dashboard summary counts.

## Mobile App Status

Current mobile implementation:

- Login screen.
- Register screen.
- Main dashboard with profile loading and editable profile dialog.
- Retrofit client.
- Auth repository/view model.
- Request repository.
- Session token storage.
- Open-counter loading through `GET /api/counters`.
- Service request creation through `POST /api/requests`.
- Supporting document selection and upload through `POST /api/requests/{id}/attachment`.
- My Requests and Active Queue sections through `GET /api/requests/me`.
- Request detail dialog.
- Pending request cancellation through `DELETE /api/requests/{id}`.
- Queue position and estimated wait loading through `GET /api/requests/{id}/position`.
- Direct opening of Cloudinary attachment URLs.
- Public holiday banner and create-request blocking through `GET /api/holidays/today`.
- FCM token registration through `PUT /api/auth/fcm-token`.
- FCM receiver service and Android notification-channel display for queue notifications.
- In-app notification feed.
- Foreground queue refresh and native STOMP user-topic subscription.

## Environment Configuration Summary

Backend `.env` should include:

```env
JWT_SECRET=...
JWT_EXPIRATION_MS=86400000

GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
GOOGLE_OAUTH_ENABLED=auto

CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
CLOUDINARY_FOLDER=queuems/service-request-attachments

SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=yourgmail@gmail.com
SMTP_PASSWORD=your_google_app_password_without_spaces
SMTP_FROM=yourgmail@gmail.com
```

Notes:

- Gmail SMTP requires a Google App Password, not the normal account password.
- Cloudinary PDFs are uploaded as raw resources. Images are uploaded as image resources.
- Bad PDF URLs under `/image/upload/...pdf.pdf` came from older uploads and should be re-uploaded.

## Known Risks and Technical Debt

### Raw model exposure

Many endpoints return model objects directly. The SDD recommends DTO usage to prevent sensitive fields from leaking. `User` includes `password`; admin/user endpoints should eventually return safe response DTOs.

### Error response format

The SDD defines a structured error envelope. Current controllers return plain strings or raw models. This is simple but inconsistent with the SDD.

### Authentication mismatch

The codebase does not verify Firebase Auth ID tokens. If strict SDD compliance is required, authentication should be migrated to Firebase client auth plus backend verification.

### Mobile runtime dependencies

The Android app now covers the SDD's mobile user journey, but real phone push delivery still depends on a valid Firebase Android configuration file at `mobile/app/google-services.json`. Without that configuration, the app still works for login and queue management, while FCM token retrieval may fail and foreground polling/WebSocket refresh remains the fallback.

### No rate limiting

The SDD requires 100 requests per minute per IP. No rate-limiting filter or gateway configuration is currently implemented.

### Cloudinary public URLs

Attachments currently use Cloudinary `secure_url` and are opened directly. This is practical, but if documents are sensitive, the system should use authenticated or signed delivery URLs.

## Recommended Next Work

1. Add safe response DTOs for users and auth profile responses.
2. Add a global API response envelope if strict SDD contract compliance is required.
3. Add Firebase Android configuration for real-device FCM delivery if it is not already supplied outside the repository.
4. Replace raw model responses with safe DTOs before production use.
5. Add automated mobile UI tests around login, create request, upload attachment, cancel request, and profile edit flows.
7. Add rate limiting.
8. Decide whether to keep current backend JWT auth or migrate to Firebase Auth ID-token verification.
9. Add tests around role restrictions, request ownership, Cloudinary upload behavior, and holiday blocking.

## Verification Performed For This Comparison

Static inspection was performed on the latest workspace files. No Maven test, Gradle build, or web production build was run as part of creating this document.
