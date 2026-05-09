# Full Regression Test Report - QueueMS

Prepared by: Jon Vic Barcenas
Date: May 8, 2026  
Project: QueueMS Smart Queue Management System  
Repository branch: `feature/QueueMS/vertical-slice-regression-plan`  
Primary design reference: `docs/SDD_QueueMS_Barcenas.docx`  

## 1. Project Information

QueueMS is a multi-platform smart queue management system for campus service transactions. The implemented system includes:

- Spring Boot backend with Firestore persistence.
- React web frontend for users, tellers, and superadmins.
- Android mobile app for user queue workflows.
- JWT authentication, Google OAuth, role-based access control, Cloudinary attachments, SMTP email notifications, Firebase Cloud Messaging, and WebSocket queue updates.

## 2. Refactoring Summary

The project was refactored from a primarily technical-layer structure into a Vertical Slice Architecture. Feature behavior is now grouped by business capability while shared infrastructure remains outside feature slices.

Backend changes:

- `feature/auth`: authentication controller, auth service, JWT service, OAuth success handler, auth DTOs.
- `feature/admin`: admin controller, admin service, admin DTOs.
- `feature/teller`: teller controller, teller service, teller DTOs.
- `feature/requests`: request controller, request service, request DTOs.
- `feature/counters`: counter controller and counter service.
- `feature/holidays`: holiday controller, holiday service, holiday DTO.
- `infrastructure/notification`: email and FCM services.
- `infrastructure/storage`: Cloudinary-backed file storage service.

Web changes:

- `features/auth`: login, register, OAuth callback, auth context, auth service, auth types.
- `features/admin`: admin dashboard, admin service, admin types.
- `features/teller`: teller dashboard, teller service, teller types.
- `features/user-requests`: user dashboard, counter/request services, request types.
- `features/dashboard`: role-based dashboard router.
- `shared/api`: authenticated fetch and token helpers.
- `shared/realtime`: WebSocket/STOMP client.

Mobile changes:

- `features/auth`: login/register activities, auth repository, auth view model/factory.
- `features/requests`: main user request activity, request repository, queue realtime client.
- `features/notifications`: Firebase messaging service.
- `shared/session`: token and FCM token session manager.
- Android manifest and layout `tools:context` values were updated to the new package names.

## 3. Updated Project Structure

```text
backend/src/main/java/edu/cit/barcenas/queuems/
  feature/
    admin/
    auth/
    counters/
    holidays/
    requests/
    teller/
  infrastructure/
    notification/
    storage/

web/src/
  features/
    admin/
    auth/
    dashboard/
    teller/
    user-requests/
  shared/
    api/
    realtime/

mobile/app/src/main/java/edu/cit/barcenas/queuems/
  features/
    auth/
    notifications/
    requests/
  shared/
    session/
  api/
```

## 4. Test Plan Documentation

The complete detailed test plan is maintained in `docs/Software_Test_Plan_QueueMS.md`. The required test-plan content is summarized here so this regression report is complete as a single submission document.

### Test Scope

The regression test scope covers the implemented QueueMS backend, web frontend, and Android mobile application after Vertical Slice Architecture refactoring.

In scope:

- Email/password registration and login.
- Google OAuth callback login.
- JWT session restoration and role-based protected routes.
- User profile retrieval and name update.
- Service request creation, listing, cancellation, queue position, and document upload/download.
- Cloudinary-backed attachment storage.
- Public holiday request blocking.
- Teller assigned-counter workflow, open/close status, request serving, status updates, and document viewing.
- Superadmin user, teller, counter, and request management.
- SMTP email notification hooks.
- Firebase Cloud Messaging token registration and native Android push notification receiver.
- WebSocket queue updates.
- Android user workflow from login to request management.

Out of scope:

- Load testing.
- Penetration testing.
- Production email deliverability beyond SMTP authentication and send invocation.
- External campus-system integrations that are not implemented in the current codebase.

### Functional Requirements Coverage

| Area | Covered Requirements | Platforms |
| --- | --- | --- |
| Authentication and account access | Register account, login account, Google OAuth, JWT session restore, edit profile name. | Backend, web, Android |
| User queue workflow | View counters, create service request, upload/view supporting document, view own requests, cancel request, view queue position and estimated wait. | Backend, web, Android |
| Holiday handling | Block request creation during public holidays and show holiday notice. | Backend, web, Android |
| Teller workflow | View assigned counter, open/close counter, view assigned requests, serve next request, update request status, view supporting documents. | Backend, web |
| Superadmin workflow | Manage users, manage counters, monitor all service requests. | Backend, web |
| Notifications and realtime updates | Send email notifications, register/send push notifications, receive WebSocket queue updates. | Backend, web, Android |

### Manual Regression Test Cases

| TC | Coverage | Procedure | Expected Result |
| --- | --- | --- | --- |
| TC-01 | Registration | Open `/register`, enter valid name/email/password, and submit. | Account is created and welcome email send is attempted. |
| TC-02 | User login | Login as `humbamanok1@gmail.com` using `Gwaps123`. | User dashboard opens and remains available after refresh. |
| TC-03 | Teller login | Login as `teller1@gmail.com` using `Gwaps123`. | Teller dashboard opens with assigned counter and request table. |
| TC-04 | Admin login | Login as `humbamanok2@gmail.com` using `Gwaps123`. | Admin dashboard opens with user, counter, and request management sections. |
| TC-05 | Google OAuth | Start Google login, complete consent, and return to the app. | Backend issues a JWT and redirects to the dashboard. |
| TC-06 | Session restore | Refresh each protected dashboard while logged in. | Session restores without white screen or redirect loop. |
| TC-07 | Profile update | Open account/profile menu, update the display name, and save. | New name appears and persists after reload. |
| TC-08 | Request creation | Select a counter/service, add notes, and submit a request. | Request appears with queue number and pending status. |
| TC-09 | Document upload/view | Attach a PDF or image, then open the document link. | Cloudinary-backed file opens without PDF/document load failure. |
| TC-10 | Request cancellation | Cancel a pending request. | Request status changes to cancelled and queue counts update. |
| TC-11 | Queue position | Open queue position for an active request. | Position and estimated wait display without backend error. |
| TC-12 | Holiday blocking | Configure or use seeded holiday data for the current date. | Request form is disabled and holiday notice is visible. |
| TC-13 | Teller counter status | As teller, close and reopen the assigned counter. | Counter status changes and the success notice auto-clears. |
| TC-14 | Teller serving | As teller, serve the next pending request. | Next request moves to serving and dashboards update. |
| TC-15 | Teller status update | Mark a request completed or cancelled. | Status changes and user/admin dashboards receive the update. |
| TC-16 | Admin user management | Create, edit, or delete a user/staff account. | User list reflects the change and role validation is enforced. |
| TC-17 | Admin counter management | Create, edit, delete, or assign a counter. | Counter list and teller assignment reflect the change. |
| TC-18 | Admin request monitoring | View all requests as admin. | Rows load with status, counter, user, and attachment metadata. |
| TC-19 | Email notification | Trigger registration, request, or status-update email with valid SMTP settings. | Backend logs no SMTP authentication error and email send is attempted. |
| TC-20 | Android push notification | Login on Android, grant notification permission, then update a request from teller. | Native phone notification appears from `QueueMessagingService`. |
| TC-21 | WebSocket update | Keep multiple role dashboards open and update a request. | Tables update without manual reload. |
| TC-22 | Android end-to-end | Login, create request, upload document, cancel request, and edit profile. | Android flow completes without crash and persists token/profile state. |

### Automated Test Cases

| ID | Tool | Command / Evidence | Coverage |
| --- | --- | --- | --- |
| AT-01 | Git | `git diff --check` | Whitespace and patch hygiene after refactor. |
| AT-02 | Ripgrep | `docs/testing/run-regression-smoke.ps1` stale-reference scans | Detects old package/import references after file moves. |
| AT-03 | Maven | `backend\mvnw.cmd test` | Backend Spring context and test coverage. |
| AT-04 | npm/ESLint | `web\npm run lint` | React/TypeScript lint coverage. |
| AT-05 | npm/Vite | `web\npm run build` | Web type-check and production bundle compile. |
| AT-06 | Gradle | `mobile\gradlew.bat test` | Android JVM unit tests. |
| AT-07 | Gradle | `mobile\gradlew.bat connectedAndroidTest` | Android instrumented UI sanity tests on emulator/device. |
| AT-08 | PowerShell | `docs/testing/run-regression-smoke.ps1` | Cross-platform layout, stale-import, and endpoint-presence checks. |
| AT-09 | Playwright | `docs/testing/capture-role-screenshots.mjs` | User, teller, and admin login/render screenshots. |

### Entry and Exit Criteria

Entry criteria:

- Main branch is merged and stable before refactoring.
- Refactor branch is created from the updated main branch.
- Backend `.env` exists beside `application.properties`.
- Web and Android dependencies are installed.
- User, teller, and admin test accounts exist.

Exit criteria:

- All high-priority functional tests pass.
- Automated checks pass or failures are documented with fixes.
- No stale pre-refactor imports remain.
- Regression report and evidence are complete.
- Branch is ready for push and submission after final review.

### Defect Handling

Each defect must record the requirement or test case ID, actual result, expected result, affected platform, severity, fix or workaround, and retest result. Defects found during this regression are listed in Section 7, and the applied corrections are listed in Section 8.

## 5. Automated Test Evidence

Supporting evidence is maintained in:

- `docs/testing/Regression_Test_Evidence.md`
- `docs/testing/run-regression-smoke.ps1`
- `docs/testing/capture-role-screenshots.mjs`
- `docs/testing/evidence/logs`
- `docs/testing/evidence/screenshots`

Automated checks added or documented:

| ID | Check | Purpose |
| --- | --- | --- |
| AT-01 | `git diff --check` | Confirms patch hygiene. |
| AT-02 | Backend stale package scan | Confirms old controller/service/dto package imports were removed. |
| AT-03 | Web stale import scan | Confirms old page/service aliases were repaired. |
| AT-04 | Android stale package scan | Confirms old UI/repository/viewmodel/service/utils packages were replaced. |
| AT-05 | Backend Maven tests | Confirms Spring backend context and tests. |
| AT-06 | Web lint/build | Confirms React/TypeScript correctness. |
| AT-07 | Android unit/instrumented tests | Confirms Android package refactor and activity wiring. |
| AT-08 | Role screenshot capture | Confirms user, teller, and admin dashboards render after login. |

The automated command logs and screenshots were captured under `docs/testing/evidence`.

## 6. Regression Test Results

| Area | Status | Evidence / Notes |
| --- | --- | --- |
| Branch creation | Passed | Branch created from updated `main`: `feature/QueueMS/vertical-slice-regression-plan`. |
| Backend vertical slice package scan | Passed | `docs/testing/evidence/logs/regression-smoke.log`. |
| Web import repair scan | Passed | `docs/testing/evidence/logs/regression-smoke.log`. |
| Android package reference scan | Passed | `docs/testing/evidence/logs/regression-smoke.log`. |
| Whitespace/static patch check | Passed | `git diff --check` via smoke script. |
| Backend full tests | Passed | `backend\mvnw.cmd test`: 1 test, 0 failures, 0 errors. Log: `docs/testing/evidence/logs/backend-test.log`. |
| Web lint | Passed | `web\npm run lint` passed. Log: `docs/testing/evidence/logs/web-lint.log`. |
| Web production build | Passed | `web\npm run build` passed. Log: `docs/testing/evidence/logs/web-build.log`. |
| Android unit tests | Passed | `mobile\gradlew.bat test` passed. Log: `docs/testing/evidence/logs/mobile-test.log`. |
| Android instrumented tests | Passed | Android instrumented test was executed using `mobile\gradlew.bat connectedAndroidTest` on a connected Android emulator/device. |
| User dashboard regression | Passed | Screenshot: `docs/testing/evidence/screenshots/user-dashboard.png`. |
| Teller dashboard regression | Passed | Screenshot: `docs/testing/evidence/screenshots/teller-dashboard.png`. |
| Admin dashboard regression | Passed | Screenshot: `docs/testing/evidence/screenshots/admin-dashboard.png`. |

## 7. Issues Found

| Issue | Severity | Resolution |
| --- | --- | --- |
| Backend feature services missed imports after moving notification/storage/holiday services. | High | Added imports for `EmailService`, `FcmService`, `FileStorageService`, and `HolidayService`. |
| Web imports still referenced old page/service locations after moving feature files. | High | Updated imports to `@/pages`, `@/shared/api/api`, and direct feature paths. |
| Web lint failed on `any` in the moved WebSocket service. | Medium | Replaced the callback type with a generic `TMessage = unknown`. |
| Android package replacement initially affected shared API package declarations. | High | Corrected package declarations for API, model, session, auth, request, and notification files. |
| Android manifest still referenced old activity/service package names. | High | Updated launcher, register activity, main activity, and Firebase messaging service names. |
| Android moved `MainActivity` could not resolve `R`. | High | Added explicit `edu.cit.barcenas.queuems.R` import after moving the activity into `features.requests`. |
| Moved Kotlin files had trailing blank-line whitespace warnings. | Low | Normalized file endings and reran patch hygiene check. |

## 8. Fixes Applied

- Backend code was moved into business feature packages and infrastructure packages.
- Web code was moved into feature folders with compatibility barrels for existing app imports.
- Android files were moved into `features/*` and `shared/session`.
- Manifest and layout references were aligned with the new Android packages.
- Missing cross-slice backend imports were restored.
- WebSocket callback typing was tightened to satisfy ESLint.
- Android `MainActivity` imports were corrected after package relocation.
- Regression documentation, logs, screenshots, and smoke/screenshot scripts were added.

## 9. Submission Checklist

| Requirement | Status |
| --- | --- |
| GitHub repository link | Use the existing QueueMS repository URL after pushing this branch. |
| Refactor branch pushed | Pending push after final commit. |
| Commit history reflecting refactor/testing | Pending final commits. |
| Full regression report PDF | Editable DOCX is available as `docs/FullRegressionReport_QueueMS_EDITABLE.docx`; convert manually to `FullRegressionReport_QueueMS.pdf` after review. |
| Automated test evidence | Logs and screenshots are captured in `docs/testing/evidence`. |
