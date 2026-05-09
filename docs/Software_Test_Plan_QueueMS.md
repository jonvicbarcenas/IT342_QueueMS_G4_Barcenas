# QueueMS Software Test Plan

Prepared: May 8, 2026  
Project: QueueMS Smart Queue Management System  
Scope: Backend, web frontend, Android mobile application  
Branch: `feature/QueueMS/vertical-slice-regression-plan`

## 1. Purpose

This test plan defines the regression coverage required after restructuring QueueMS with Vertical Slice Architecture. It verifies that all implemented functional requirements from `docs/SDD_QueueMS_Barcenas.docx` and the latest codebase comparison remain functional after moving backend, web, and Android code into feature-oriented modules.

## 2. Test Scope

In scope:

- Email/password authentication.
- Google OAuth login redirect flow.
- JWT-protected route access.
- User profile retrieval and name update.
- User service request creation, listing, cancellation, queue position, and supporting document upload/download.
- Public holiday blocking.
- Teller counter visibility, open/close status, request serving, request status updates, and document viewing.
- Superadmin user management, teller management, counter management, and request monitoring.
- SMTP notification hooks.
- Firebase Cloud Messaging token registration and native Android push notification receiver.
- WebSocket queue update subscriptions.
- Android user workflow from login to request management.
- Cloudinary attachment storage behavior.

Out of scope:

- Load testing.
- Penetration testing.
- Real payment or external campus system integrations.
- Production email deliverability beyond SMTP authentication and send invocation.

## 3. Test Environment

| Component | Target |
| --- | --- |
| Backend | Spring Boot, Firestore, Cloudinary, SMTP, Firebase Admin |
| Web | Vite React app |
| Mobile | Android app package `edu.cit.barcenas.queuems` |
| Backend base URL | `http://localhost:8080` for web, `http://10.0.2.2:8080` or LAN IP for emulator/device |
| Web URL | `http://localhost:5173` |
| Test date | May 8, 2026 |

Required backend environment variables:

```env
FIREBASE_API_KEY=...
JWT_SECRET=...
JWT_EXPIRATION_MS=86400000
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
CLOUDINARY_FOLDER=queuems/service-request-attachments
SMTP_HOST=...
SMTP_PORT=...
SMTP_USERNAME=...
SMTP_PASSWORD=...
```

## 4. Functional Requirements Coverage

| ID | Requirement | Backend | Web | Mobile | Priority |
| --- | --- | --- | --- | --- | --- |
| FR-01 | Register account | `/api/auth/register` | Register page | Register activity | High |
| FR-02 | Login account | `/api/auth/login` | Login page | Login activity | High |
| FR-03 | Google OAuth | `/api/auth/google/login`, OAuth callback | Google login button and callback page | Deep link callback | High |
| FR-04 | JWT session restore | `/api/auth/me` | Auth context/protected routes | SessionManager | High |
| FR-05 | Edit profile name | `PUT /api/auth/me` | Account menu | Main screen profile dialog | Medium |
| FR-06 | View counters | `/api/counters` | User request form | Android request form | High |
| FR-07 | Create service request | `POST /api/requests` | User dashboard | Android main screen | High |
| FR-08 | Upload supporting document | `POST /api/requests/{id}/attachment` | User upload control | Android document picker | High |
| FR-09 | Download/view supporting document | user/teller attachment endpoints | User/teller/admin links | Android request details | High |
| FR-10 | View own requests | `/api/requests/me` | User dashboard | Android list | High |
| FR-11 | Cancel own request | `DELETE /api/requests/{id}` | User dashboard | Android cancel action | High |
| FR-12 | Queue position and estimated wait | `/api/requests/{id}/position` | Queue status display | Android status display | Medium |
| FR-13 | Public holiday blocking | `/api/holidays/today` | Holiday banner and form disable | Holiday notice and create disable | Medium |
| FR-14 | Teller assigned counter | `/api/teller/counter` | Teller dashboard | Not applicable | High |
| FR-15 | Teller open/close counter | `/api/teller/counter/status` | Teller dashboard | Not applicable | High |
| FR-16 | Teller view assigned requests | `/api/teller/requests` | Teller dashboard | Not applicable | High |
| FR-17 | Teller serve next/request | teller serve endpoints | Teller dashboard | Not applicable | High |
| FR-18 | Teller update request status | `/api/teller/requests/{id}/status` | Teller dashboard | Not applicable | High |
| FR-19 | Superadmin manage users | `/api/admin/users` | Admin dashboard | Not applicable | High |
| FR-20 | Superadmin manage counters | `/api/admin/counters` | Admin dashboard | Not applicable | High |
| FR-21 | Superadmin view all requests | `/api/admin/requests` | Admin dashboard | Not applicable | High |
| FR-22 | Email notifications | EmailService/observer hooks | Triggered by workflows | Triggered by workflows | Medium |
| FR-23 | Push notifications | FcmService, token endpoint | Not applicable | Firebase messaging service | Medium |
| FR-24 | Real-time queue updates | WebSocket topics | Web STOMP client | Android STOMP client | Medium |

## 5. Manual Regression Test Cases

| TC | Requirement | Steps | Expected Result |
| --- | --- | --- | --- |
| TC-01 | FR-01 | Open `/register`, enter valid name/email/password, submit. | Account is created, welcome email send is attempted, user is logged in or redirected according to app flow. |
| TC-02 | FR-02 | Open `/login`, login as `humbamanok1@gmail.com` with `Gwaps123`. | User dashboard opens and protected route remains visible after reload. |
| TC-03 | FR-02 | Login as `teller1@gmail.com` with `Gwaps123`. | Teller dashboard opens with assigned counter and request table. |
| TC-04 | FR-02 | Login as `humbamanok2@gmail.com` with `Gwaps123`. | Admin dashboard opens with user, counter, and request management sections. |
| TC-05 | FR-03 | Click Google login, complete Google consent, return to app. | Backend issues JWT and callback redirects to dashboard. |
| TC-06 | FR-04 | Refresh each dashboard while logged in. | Session restores without white screen or redirect loop. |
| TC-07 | FR-05 | Open account menu, update first/last name, save. | New name appears in the menu and remains after reload. |
| TC-08 | FR-06, FR-07 | As user, select a counter/service, add notes, submit request. | New request appears with queue number and pending status. |
| TC-09 | FR-08, FR-09 | Attach a PDF or image to a request, then click view/download. | Attachment opens from a Cloudinary-backed URL without "Failed to load PDF document". |
| TC-10 | FR-10, FR-11 | Cancel a pending request. | Request status changes to cancelled and queue counts update. |
| TC-11 | FR-12 | Open queue position for an active request. | Position and estimated wait display without backend error. |
| TC-12 | FR-13 | Configure backend date as a public holiday or use seeded holiday data. | Request form is disabled and a holiday notice is visible. |
| TC-13 | FR-14, FR-15 | Login as teller and close/open assigned counter. | Counter status changes and success notice auto-clears. |
| TC-14 | FR-16, FR-17 | As teller, serve next pending request. | Old serving request is resolved according to flow and next request moves to serving. |
| TC-15 | FR-18 | As teller, mark request completed/cancelled. | Status changes and user/admin dashboards receive updated request state. |
| TC-16 | FR-19 | As admin, create/edit/delete a staff or user account. | User list reflects the change and role validation is enforced. |
| TC-17 | FR-20 | As admin, create/edit/delete a counter and assign teller. | Counter list and teller assigned counter reflect the change. |
| TC-18 | FR-21 | As admin, view all requests. | All request rows load with status, counter, user, and attachment metadata. |
| TC-19 | FR-22 | Trigger registration/request/status update with valid SMTP credentials. | Backend logs no SMTP authentication error and recipient receives email. |
| TC-20 | FR-23 | Login on Android 13+, grant notification permission, update a request from teller. | Native phone notification appears from `QueueMessagingService`. |
| TC-21 | FR-24 | Keep user/teller/admin dashboards open, update a request in another role. | Tables update through WebSocket without manual reload. |
| TC-22 | Mobile end-to-end | On emulator/device, login, create request, upload document, cancel request, edit profile. | Android flow completes without crash and persists token/profile state. |

## 6. Automated Test Cases

| Automated ID | Tool | Command | Coverage |
| --- | --- | --- | --- |
| AT-01 | Git | `git diff --check` | Whitespace and patch hygiene after refactor. |
| AT-02 | Ripgrep | route/import scan in `docs/testing/run-regression-smoke.ps1` | Detects stale pre-refactor package/import references. |
| AT-03 | Maven | `cd backend; ./mvnw test` or `mvn test` | Spring context and backend unit/integration tests. |
| AT-04 | npm/ESLint | `cd web; npm run lint` | React/TypeScript lint coverage. |
| AT-05 | npm/TypeScript/Vite | `cd web; npm run build` | Web type-check and production bundle compile. |
| AT-06 | Gradle | `cd mobile; ./gradlew test` | Android JVM unit tests. |
| AT-07 | Gradle | `cd mobile; ./gradlew connectedAndroidTest` | Android instrumented UI sanity tests on emulator/device. |
| AT-08 | PowerShell smoke script | `powershell -ExecutionPolicy Bypass -File docs/testing/run-regression-smoke.ps1` | Cross-platform source layout, stale imports, and endpoint presence checks. |
| AT-09 | Playwright | `node docs/testing/capture-role-screenshots.mjs` | User, teller, and admin dashboard login/render verification with screenshots. |

## 7. Executed Regression Results

| ID | Result | Evidence |
| --- | --- | --- |
| AT-01/AT-02/AT-08 | Passed | `docs/testing/evidence/logs/regression-smoke.log` |
| AT-03 | Passed | `docs/testing/evidence/logs/backend-test.log` |
| AT-04 | Passed | `docs/testing/evidence/logs/web-lint.log` |
| AT-05 | Passed | `docs/testing/evidence/logs/web-build.log` |
| AT-06 | Passed | `docs/testing/evidence/logs/mobile-test.log` |
| AT-07 | Passed | Android instrumented test executed using `mobile\gradlew.bat connectedAndroidTest` on a connected Android emulator/device. Log: `docs/testing/evidence/logs/mobile-connected-android-test.log`. |
| AT-09 | Passed | `docs/testing/evidence/screenshots/user-dashboard.png`, `teller-dashboard.png`, `admin-dashboard.png` |

## 8. Regression Entry and Exit Criteria

Entry criteria:

- Main branch is merged and clean.
- Refactor branch is created from updated main.
- Backend `.env` exists beside `application.properties`.
- Web and Android dependencies are installed.
- Test accounts exist for user, teller, and admin roles.

Exit criteria:

- All high-priority functional tests pass.
- Automated checks pass or failures are documented with fixes.
- No stale pre-refactor imports remain.
- Regression report and evidence are committed.
- Branch is pushed for submission.

## 9. Defect Handling

Each defect must include:

- Requirement/test case ID.
- Actual result.
- Expected result.
- Affected platform.
- Severity.
- Fix commit or workaround.
- Retest result.
