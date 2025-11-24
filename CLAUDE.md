# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PDF Redactor M is an Android app for detecting and masking personally identifiable information (PII) in PDF files. The app allows users to load PDFs, create redaction masks (both automatic and manual), and export redacted PDF files.

## Development Commands

### Building
```bash
./gradlew build                  # Build the entire project
./gradlew assembleDebug         # Build debug APK
./gradlew assembleRelease       # Build release APK
```

### Testing
```bash
./gradlew test                      # Run all unit tests
./gradlew testDebugUnitTest        # Run debug unit tests
./gradlew connectedAndroidTest     # Run instrumented tests on connected devices
./gradlew connectedDebugAndroidTest # Run debug instrumented tests
```

### Code Quality
```bash
./gradlew lint                  # Run lint analysis
./gradlew lintDebug            # Run lint on debug variant
./gradlew check                # Run all checks (lint + tests)
```

### Installation
```bash
./gradlew installDebug         # Install debug build on connected device
./gradlew uninstallDebug      # Uninstall debug build
```

## Architecture

The project follows **Clean Architecture** principles with three main layers:

### Domain Layer (`domain/`)
- **Models**: `PdfDocument`, `RedactionMask` - Core business entities
- **Repository Interfaces**: `PdfRepository` - Contracts for data access
- **Use Cases**: 
  - `LoadPdfUseCase` - Load PDF files
  - `SaveRedactedPdfUseCase` - Export redacted PDFs
  - `GetRedactionsUseCase`, `SaveRedactionsUseCase` - Manage redaction data

### Data Layer (`data/`)
- **Repository Implementation**: `PdfRepositoryImpl` - Implements PDF operations using PdfBox-Android
- **Local Database**: Room database with `ProjectDao` and `RedactionDao`
- **Entities**: `ProjectEntity`, `RedactionEntity` - Database models

### Presentation Layer (`presentation/`)
- **MVVM Pattern**: ViewModels manage UI state and business logic
- **Screens**: 
  - `HomeScreen` - Recent projects, file selection
  - `EditorScreen` - PDF viewer with redaction capabilities
- **Navigation**: Jetpack Navigation Compose with centralized `NavGraph`

## Technology Stack

- **Language**: Kotlin 2.2.21
- **UI**: Jetpack Compose with Material3
- **DI**: Hilt for dependency injection
- **Database**: Room for local persistence
- **PDF Processing**: 
  - PdfBox-Android (Primary) - PDF manipulation and redaction
  - Android PdfRenderer - PDF viewing and rendering
- **Async**: Coroutines + Flow for reactive programming

## Key Implementation Details

### PDF Processing
- **PDFBox Initialization**: Done in `AppModule.kt:49` with `PDFBoxResourceLoader.init(context)`
- **Coordinate System**: PDF coordinates are bottom-left origin; UI coordinates are top-left. Conversion handled in `PdfRepositoryImpl.kt:89-90`
- **Redaction Implementation**: Black rectangles drawn over content using PDFBox `PDPageContentStream`

### Database Schema
- **Projects Table**: Stores PDF file metadata and session information
- **Redactions Table**: Stores redaction mask coordinates and types per project/page
- **Auto-save**: Work is automatically persisted to prevent data loss

### Navigation
- Home route: `"home"`
- Editor route: `"editor/{pdfId}"` - PDF ID passed as navigation parameter

## Project Structure

```
app/src/main/java/org/comon/pdfredactorm/
├── MainActivity.kt                    # Entry point with Hilt setup
├── PdfRedactorApplication.kt         # Application class
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt           # Room database configuration
│   │   ├── dao/                     # Data Access Objects
│   │   └── entity/                  # Database entities
│   └── repository/
│       └── PdfRepositoryImpl.kt     # Core PDF operations
├── di/
│   └── AppModule.kt                 # Hilt dependency injection setup
├── domain/
│   ├── model/                       # Business models
│   ├── repository/                  # Repository interfaces
│   └── usecase/                     # Business logic use cases
├── presentation/
│   ├── editor/                      # PDF editor screen
│   ├── home/                        # Home screen
│   └── navigation/                  # App navigation setup
└── ui/theme/                        # Material3 theme configuration
```

## Development Notes

### Version Catalog
Dependencies are managed through `gradle/libs.versions.toml` using Gradle's version catalog feature.

### Build Configuration
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36
- **Compile SDK**: 36
- **Kotlin**: 2.2.21 with Compose Compiler

### Testing Structure
- Unit tests: `app/src/test/java/`
- Instrumented tests: `app/src/androidTest/java/`

When working with this codebase, always initialize PDFBox in dependency injection, handle coordinate system differences between PDF and Android UI, and ensure proper database transaction management for auto-save functionality.