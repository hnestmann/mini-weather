# Mini Weather

A minimal weather Android app optimized for the **Minimal Phone** (600×800 e-ink display) and other e-paper devices.

Mostly created by cursor composer model. Highly Higly inspired by


## Features

- **Current weather** with temperature and condition
- **Hourly forecast** – 5 hours at a time with right arrow to jump to next set
- **5-day forecast** with high/low temps
- **Bottom tabs**: Home, Description, Details, Air Quality
- **Multiple cities** with location management
- **Monochrome UI** – high contrast black & white, no animations
- **Keyboard shortcuts** (Minimal Phone physical keyboard):
  - **Space**: Next tab (Home → Description → Details → Pollen)
  - **Enter**: Next city
  - **R**: Refresh from remote
  - **A**: Add city
  - **L**: Menu

## Data Source

Uses the [Open-Meteo API](https://open-meteo.com/) – free, no API key required.

## Build

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

For release:

```bash
./gradlew assembleRelease
```

## Tech Stack

- Kotlin, Jetpack Compose
- Retrofit for API calls
- DataStore for preferences
- Open-Meteo API (weather, air quality, geocoding)
