# GB4Aura v2.0

A powerful, modular Android application designed for Nothing Phone users to compose, synchronize, and visualize custom Glyph lighting patterns.

## 🚀 Key Features

- **Composer (V1 & V2)**: Modular step-by-step lighting sequence creator with real-time hardware sync.
- **Music Studio**: Advanced DSP-based synchronization. Pick any audio file and automatically generate patterns using multiple algorithms (FFT, Peak Detection, Spectral Flux, BPM Grid).
- **Pattern Lab**: Merge, invert, mirror, and randomize existing sequences to create complex new lighting effects.
- **Library**: Centralized management for saved sequences and music studio projects with export/import capabilities.
- **Live Preview**: A persistent top-bar visualization that mirrors the physical Glyph hardware state.
- **Battery Sync**: Integrated background service to visualize charging progress via the Glyph interface.

## 🏗 Project Structure

The project follows a strict **MVVM (Model-View-ViewModel)** architecture and is modularized by feature.

```text
app/src/main/java/com/smaarig/glyphbarcomposer/
├── controller/          # Hardware Interface (Glyph SDK wrappers)
├── data/                # Data Layer (Room Database, DAOs, Entities)
├── model/               # Domain Models (GlyphSequence logic)
├── repository/          # Data Repository (Single source of truth)
├── service/             # Android Services (Battery Monitor, Background Tasks)
├── ui/                  # UI Layer (Jetpack Compose)
│   ├── composer/        # Composer Feature (V1 & V2 components)
│   ├── library/         # Library & Project Management
│   ├── patternlab/      # Pattern Lab & Sequence Merging
│   ├── studio/          # Music Studio & DSP Components
│   ├── theme/           # Design System (Typography, Colors)
│   └── viewmodel/       # Feature ViewModels & State Management
└── utils/               # Utilities (Audio Processing, Zip, Preferences)
```

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM + Repository Pattern
- **Database**: Room Persistence Library
- **Hardware**: Nothing Glyph SDK
- **Testing**: JUnit 4, MockK, Robolectric, JaCoCo (90%+ Business Logic Coverage)
- **Navigation**: Compose Navigation with Directional Animations

## 📈 Testing & Coverage

The project includes a comprehensive test suite.
- **Unit Tests**: Full branch coverage for all ViewModels and business logic.
- **Instrumented Tests**: Verified UI interactions and navigation.
- **Coverage**: Run `./gradlew jacocoTestReport` to generate the latest report.

## 📦 Versioning

Current Version: **2.0**
- Major refactor to modular architecture.
- Smooth directional navigation and UX polished.
- Unified hardware synchronization logic.
