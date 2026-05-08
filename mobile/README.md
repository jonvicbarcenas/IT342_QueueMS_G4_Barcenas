# QueueMS Android Configuration

The Android app reads its backend URL from the Gradle property `QUEUEMS_API_BASE_URL`.
It also retries comma-separated `QUEUEMS_API_FALLBACK_URLS` when the configured backend address cannot be reached.

Default:

```properties
QUEUEMS_API_BASE_URL=http://10.0.2.2:8080/
QUEUEMS_API_FALLBACK_URLS=http://10.0.2.2:8080/,http://127.0.0.1:8080/
```

Use the default for the Android emulator when the Spring Boot backend is running on the same computer at port `8080`.

For a physical phone on the same Wi-Fi network, use the computer's LAN IP address instead:

```properties
QUEUEMS_API_BASE_URL=http://192.168.1.23:8080/
```

If the phone cannot reach the PC LAN address because of firewall or router client isolation, use USB debugging with ADB reverse and point the app at device localhost:

```powershell
adb reverse tcp:8080 tcp:8080
```

```properties
QUEUEMS_API_BASE_URL=http://127.0.0.1:8080/
```

You can put this in `mobile/gradle.properties` or pass it at build time:

```powershell
.\gradlew assembleDebug -PQUEUEMS_API_BASE_URL=http://192.168.1.23:8080/
```

If login shows `Cannot reach QueueMS backend`, confirm the backend is running, the port is correct, and the phone/emulator can reach that address.

## Push Notifications

Actual phone push notifications require Firebase Android configuration.

1. In Firebase Console, add an Android app with package name:

```text
edu.cit.barcenas.queuems
```

2. Download `google-services.json`.
3. Place it at:

```text
mobile/app/google-services.json
```

4. Rebuild and reinstall the app, then log in again so the app can send the device FCM token to the backend.

Without `mobile/app/google-services.json`, the in-app notification feed can still work from polling/WebSocket updates, but Firebase Cloud Messaging cannot deliver real phone push notifications.
