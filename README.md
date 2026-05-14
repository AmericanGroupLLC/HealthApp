# MyHealth App

A cross-platform health tracking application with Android, iOS, and Node.js server components.

## Tech Stack

| Platform | Technologies |
|----------|-------------|
| **Android** | Kotlin, Jetpack Compose, Room, Hilt, Ktor, Health Connect, SQLCipher |
| **iOS** | Swift, SwiftUI, HealthKit, CoreData, Keychain |
| **Server** | Node.js, Express, better-sqlite3, JWT, Helmet, Sentry |

## Architecture

```
┌─────────────┐  ┌─────────────┐
│   Android    │  │     iOS     │
│  (Compose)   │  │  (SwiftUI)  │
└──────┬───────┘  └──────┬──────┘
       │                 │
       │   HTTPS / JWT   │
       └────────┬────────┘
                │
       ┌────────▼────────┐
       │   Express API   │
       │   (Node.js)     │
       ├─────────────────┤
       │  better-sqlite3 │
       │    (SQLite)      │
       └─────────────────┘
```

## Setup

### Server

```bash
cd server
cp .env.example .env        # Edit with your settings
npm install
npm run dev                  # Starts with --watch
```

#### Docker

```bash
cd server
docker build -t myhealth-server .
docker run -p 4000:4000 --env-file .env myhealth-server
```

### Android

1. Open `android/` in Android Studio
2. Sync Gradle
3. Run on device/emulator (API 28+)

### iOS

1. Open `ios/MyHealth.xcodeproj` (or `.xcworkspace`) in Xcode
2. Select a simulator or device
3. Build & Run (⌘R)

## Running Tests

```bash
# Server
cd server && npm test

# Server with coverage
cd server && npm run test:coverage

# Android
cd android && ./gradlew test

# iOS
xcodebuild test -scheme MyHealth -destination 'platform=iOS Simulator,name=iPhone 16'
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `4000` | Server listen port |
| `JWT_SECRET` | — | Secret for signing JWTs (required in production) |
| `JWT_EXPIRES_IN` | `1d` | JWT expiration duration |
| `NODE_ENV` | `development` | `development` or `production` |
| `ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated CORS origins |
| `DATABASE_PATH` | `./data/myhealth.db` | SQLite database file path |
| `SENTRY_DSN` | — | Sentry DSN for error tracking (optional) |
| `POSTHOG_API_KEY` | — | PostHog key for analytics (optional) |
