# Bunk & Chutti (srmpro)

An Android attendance calculator for students: it shows how many classes you can still skip, or must attend, to stay above a target percentage.

## Features

- **Sync Portal:** log in to the SRM student portal inside the app (WebView) and fetch attendance; the page is parsed with jsoup
- **Manual Entry** and **Load Saved** (offline data) as alternatives to syncing
- **Smart status per subject:** how many classes you can leave, or need to attend
- **Time Machine:** simulate future attendance ("what if I miss next week?")
- **Semester Forecast:** projection of attendance until the end of the semester
- **Encrypted local storage:** saved data uses AndroidX `EncryptedSharedPreferences` (AES-256-GCM, Android Keystore)

## Download

Prebuilt APKs: [Releases](https://github.com/JayD108/srmpro/releases)

## Screenshots

<!-- TODO: add screenshots, e.g. docs/screenshot.png -->

## Tech stack

Java, Android (minSdk 24, compileSdk 34), jsoup, MPAndroidChart, AndroidX Security Crypto, Gson, Material Components.

## Getting started

Prerequisites: Android Studio and an Android SDK.

1. Clone the repository and open it in Android Studio.
2. Let Gradle sync, then run the `app` configuration on a device or emulator.

The app only requests the `INTERNET` permission. (The build was not run as part of this documentation update.)

## Project structure

```
app/src/main/java/com/example/srmistpro/
  MainActivity, InputActivity     dashboard and data entry
  PortalLoginActivity, HtmlParser portal login and attendance parsing
  MathEngine                      attendance maths and forecast
  TimeMachineActivity, SemForecastActivity
  SecureStore                     encrypted storage
  AppData                         in-memory data and default timetable
```

## Roadmap / known limitations

- The default timetable (`AppData.java`) and the semester end date / holidays (`MathEngine.java`) are hardcoded for one semester and must be edited in code.
- An earlier version of this README mentioned timetable OCR scanning with ML Kit; the code contains no OCR feature, so it is not listed above.
- Targets a specific university portal; other portals are not supported.

## Author

Mrittunjay Dubey, GitHub [@JayD108](https://github.com/JayD108)

## License

No license file is included yet, so all rights are reserved by default.