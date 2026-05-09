# QueueMS Regression Test Evidence

Prepared: May 8, 2026  
Branch: `feature/QueueMS/vertical-slice-regression-plan`

## Evidence Summary

| Evidence ID | Type | Command / Source | Result |
| --- | --- | --- | --- |
| EV-01 | Branch evidence | `git branch --show-current` | `feature/QueueMS/vertical-slice-regression-plan` |
| EV-02 | Source layout scan | `rg --files backend/src/main/java/.../feature web/src/features mobile/.../features` | Feature-oriented backend, web, and Android folders are present. |
| EV-03 | Backend stale import scan | `rg "edu\.cit\.barcenas\.queuems\.(controller\|service\|dto)" backend/src/main/java backend/src/test/java` | No matches after refactor. |
| EV-04 | Web stale import scan | `rg "@/features/dashboard/pages\|from '../api'\|from './api'\|from './admin\|from './teller\|from './users" web/src` | No matches after import repair. |
| EV-05 | Android stale package scan | `rg "\.ui\.\|\.repository\.\|\.viewmodel\.\|\.service\.\|\.utils\." mobile/app/src/main` | No stale pre-refactor app package references expected. |
| EV-06 | Whitespace check | `git diff --check` | Passed after normalizing moved Kotlin files. |
| EV-07 | Backend automated test | `backend\mvnw.cmd test` | Passed. Spring context test executed: 1 test, 0 failures, 0 errors. Log: `docs/testing/evidence/logs/backend-test.log`. |
| EV-08 | Web lint | `web\npm run lint` | Passed after replacing the moved WebSocket client's `any` callback type with a generic. Log: `docs/testing/evidence/logs/web-lint.log`. |
| EV-09 | Web production build | `web\npm run build` | Passed. Vite emitted the normal `stompjs` browser-compatibility warning for Node `net`, but build completed successfully. Log: `docs/testing/evidence/logs/web-build.log`. |
| EV-10 | Android unit tests | `mobile\gradlew.bat test` | Passed after adding the missing `R` import in moved `features.requests.MainActivity`. Log: `docs/testing/evidence/logs/mobile-test.log`. |
| EV-11 | Android instrumented tests | `mobile\gradlew.bat connectedAndroidTest` | Environment-blocked. Test APK compiled, but execution failed because no emulator/device was connected. Log: `docs/testing/evidence/logs/mobile-connected-android-test.log`. |
| EV-12 | Browser role verification | Browser plugin DOM inspection plus Playwright screenshots | Passed for user, teller, and admin dashboards. Screenshots and text snapshots are in `docs/testing/evidence/screenshots`. |

## Executed Automated Commands

The following commands were executed during regression verification:

```powershell
cd backend
.\mvnw test

cd ..\web
npm run lint
npm run build

cd ..\mobile
.\gradlew test
.\gradlew connectedAndroidTest

cd ..
powershell -ExecutionPolicy Bypass -File docs\testing\run-regression-smoke.ps1
```

## Screenshot Evidence

| Screenshot | Evidence |
| --- | --- |
| User dashboard | `docs/testing/evidence/screenshots/user-dashboard.png` |
| Teller dashboard | `docs/testing/evidence/screenshots/teller-dashboard.png` |
| Admin dashboard | `docs/testing/evidence/screenshots/admin-dashboard.png` |
| Browser DOM evidence | `docs/testing/evidence/screenshots/user-dashboard.txt`, `teller-dashboard.txt`, `admin-dashboard.txt` |
| Playwright text evidence | `docs/testing/evidence/screenshots/user-playwright-dashboard.txt`, `teller-playwright-dashboard.txt`, `admin-playwright-dashboard.txt` |

## Test Logs

| Log | Description |
| --- | --- |
| `docs/testing/evidence/logs/regression-smoke.log` | Source layout, stale import, endpoint marker, and whitespace checks. |
| `docs/testing/evidence/logs/backend-test.log` | Maven backend test output. |
| `docs/testing/evidence/logs/web-lint.log` | ESLint output. |
| `docs/testing/evidence/logs/web-build.log` | TypeScript and Vite production build output. |
| `docs/testing/evidence/logs/mobile-test.log` | Android JVM unit test output. |
| `docs/testing/evidence/logs/mobile-connected-android-test.log` | Android instrumented test attempt; blocked by no connected device. |
| `docs/testing/evidence/logs/playwright-role-screenshots.log` | Role screenshot capture output. |
| `docs/testing/evidence/logs/playwright-role-screenshots.json` | Structured role screenshot results. |

