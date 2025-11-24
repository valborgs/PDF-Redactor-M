# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PDF Redactor M is an Android app for detecting and masking personally identifiable information (PII) in PDF files. The app allows users to load PDFs, create redaction masks (both automatic PII detection and manual drawing), and export redacted PDF files. The app features bilingual support (Korean/English) and includes an interactive user manual.

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
- **Models**:
  - `PdfDocument` - Core PDF metadata with optional thumbnail support
  - `RedactionMask` - Redaction coordinates and type
  - `DetectedPii` - PII detection results with position data
  - `RedactionType` enum - MANUAL, PHONE_NUMBER, EMAIL, RRN, BIRTH_DATE, ADDRESS
- **Repository Interfaces**: `PdfRepository` - Contracts for PDF operations and PII detection
- **Use Cases** (5 total):
  - `LoadPdfUseCase` - Load PDF files with auto-save to history
  - `SaveRedactedPdfUseCase` - Export redacted PDFs
  - `GetRedactionsUseCase`, `SaveRedactionsUseCase` - Manage redaction data persistence
  - `DetectPiiUseCase` - PII detection and mask conversion

### Data Layer (`data/`)
- **Repository Implementation**: `PdfRepositoryImpl` (295 lines) - PDF operations and PII detection
  - PDF rendering via Android PdfRenderer
  - Redaction application via PDFBox
  - PII detection with character-level position tracking
  - Coordinate conversion between PDF (bottom-left origin) and UI (top-left origin) systems
- **Local Database**: Room database (`pdf_redactor_db`, version 1)
  - `ProjectDao` - Project CRUD operations with Flow
  - `RedactionDao` - Redaction persistence with cascade delete on project removal
- **Entities**:
  - `ProjectEntity` (table: "projects") - PDF metadata with thumbnail URI support
  - `RedactionEntity` (table: "redactions") - Mask coordinates with RedactionType enum storage
- **PII Detection Components**:
  - `PiiPatterns` - 5 regex patterns for Korean PII types
  - `PiiTextStripper` - Custom PDFBox TextStripper for text position extraction

### Presentation Layer (`presentation/`)
- **MVVM Pattern**: ViewModels manage UI state and business logic
- **Screens**:
  - `HomeScreen` (152 lines) - Recent projects list, file selection, help dialog integration
  - `EditorScreen` (421 lines) - Canvas-based PDF rendering with touch gestures, redaction UI
  - `HelpDialog` (215 lines) - 6-page interactive user manual with ViewPager
- **ViewModels**:
  - `HomeViewModel` - Project management with Flow-based reactive data
  - `EditorViewModel` (298 lines) - Comprehensive state management
    - PdfRenderer lifecycle management (proper cleanup in onCleared)
    - Manual redaction CRUD operations
    - PII detection orchestration (single page and full document)
    - Temporary file and database cleanup on save
- **Navigation**: Jetpack Navigation Compose with type-safe routing via sealed classes

## Technology Stack

- **Language**: Kotlin 2.2.21 (JVM target 1.8)
- **UI Framework**: Jetpack Compose
  - Compose BOM: 2025.11.01
  - Material3 with extended icon set
  - Activity Compose: 1.12.0
- **Architecture**: Clean Architecture with MVVM
- **Dependency Injection**: Hilt 2.57.2 with KSP 2.2.21-2.0.4
- **Database**: Room 2.8.4 for local persistence
- **Async & Reactive**: Coroutines + Flow, Lifecycle 2.10.0
- **PDF Processing**:
  - PdfBox-Android 2.0.27.0 - PDF manipulation, text extraction, and redaction
  - Android PdfRenderer - PDF viewing and bitmap rendering (2048px width)
- **Image Loading**: Coil 2.7.0 for Compose
- **Navigation**: Navigation Compose 2.9.6
- **Testing**: JUnit 4.13.2, AndroidX Test, Espresso 3.7.0, Compose UI Test
- **Build Tools**: AGP 8.12.3, Gradle 8.12
- **Future Libraries**: Retrofit 3.0.0, OkHttp 5.3.2 (configured but not yet used)
- **Internationalization**: Korean (default), English via resource qualifiers

## Key Implementation Details

### PDF Processing
- **PDFBox Initialization**: Done in `AppModule.kt:49` with `PDFBoxResourceLoader.init(context)`
- **Coordinate System**: PDF coordinates are bottom-left origin; UI coordinates are top-left
  - Conversion in `PdfRepositoryImpl.kt:92-93` (saveRedactedPdf)
  - Conversion in `PdfRepositoryImpl.kt:226-227` (detectPii - exact matching)
  - Conversion in `PdfRepositoryImpl.kt:249-250` (detectPii - estimation fallback)
- **Redaction Implementation**: Black rectangles drawn over content using PDFBox `PDPageContentStream`
- **Rendering**: PdfRenderer generates bitmaps at 2048px width for display in EditorScreen

### PII Detection System
The app includes automatic PII (Personally Identifiable Information) detection using regex-based pattern matching optimized for Korean documents.

#### Supported PII Types
1. **RRN** (Resident Registration Number) - Korean SSN format (YYMMDD-#######)
2. **PHONE_NUMBER** - Korean mobile numbers (010-####-####)
3. **EMAIL** - Standard email addresses
4. **BIRTH_DATE** - Multiple date formats (YYYY-MM-DD, YYYY.MM.DD, YYYY/MM/DD)
5. **ADDRESS** - Korean addresses with administrative divisions (시/도/구/동)

#### Implementation Architecture
- **PiiPatterns** (`data/pii/PiiPatterns.kt:12-47`) - Regex pattern definitions for all 5 PII types
- **PiiTextStripper** (`data/pii/PiiTextStripper.kt`) - Custom PDFBox TextStripper that extracts text with precise character position data
- **DetectPiiUseCase** (`domain/usecase/DetectPiiUseCase.kt`) - Business logic for pattern matching and result aggregation
- **Repository Methods** (`domain/repository/PdfRepository.kt:17-18`):
  - `detectPii(file: File, pageIndex: Int): List<DetectedPii>` - Single page detection
  - `detectPiiInAllPages(file: File): List<DetectedPii>` - Full document scan

#### Coordinate Calculation Algorithm
PII detection uses a sophisticated two-tier approach for accurate bounding box calculation:

**Tier 1 - Exact Matching** (`PdfRepositoryImpl.kt:216-239`):
- Used when `textPositions.size == text.length` (one-to-one character mapping)
- Character-level position tracking for precise bounds
- Baseline-to-top coordinate conversion
- Most accurate method for standard text

**Tier 2 - Estimation Fallback** (`PdfRepositoryImpl.kt:240-263`):
- Used for ligatures, combined characters, or complex text rendering
- Ratio-based position calculation using text length proportions
- Width estimation based on substring position
- Ensures all PII detections receive valid coordinates

#### EditorViewModel Integration
- `detectPiiInCurrentPage()` (line 213) - Detects PII in visible page
- `detectPiiInAllPages()` (line 236) - Scans entire document
- `convertDetectedPiiToMask()` (line 257) - Converts detection to redaction mask
- `removeDetectedPii()` (line 278) - Removes PII from pending list

See `docs/pii_detection.md` for comprehensive documentation (Korean).

### State Management
- **EditorUiState** - Comprehensive state object with 11 properties
  - Document and page info (`document`, `currentPage`, `pageCount`)
  - Rendered content (`currentPageBitmap`, `pdfPageWidth`, `pdfPageHeight`)
  - Redaction data (`redactions`, `detectedPii`)
  - UI modes (`isMaskingMode`, `isDetecting`)
  - Feedback states (`isLoading`, `error`, `saveSuccess`)
- **Flow-based Reactivity**: Recent projects exposed as StateFlow in HomeViewModel
- **Auto-save**: Redactions persisted to Room database on every add/remove operation
- **Lifecycle Management**: PdfRenderer properly closed in `EditorViewModel.onCleared()` (line 296)
- **File Cleanup**: Temporary PDF files deleted after save, projects removed from database (`EditorViewModel.savePdf:189-197`)

### Database Schema
- **Database Name**: "pdf_redactor_db" (configured in `AppModule.kt:27`)
- **Database Version**: 1
- **Projects Table** (`ProjectEntity`):
  - Stores PDF file path, display name, creation timestamp
  - Includes optional thumbnailUri field for preview images
  - Primary key: Auto-generated ID
- **Redactions Table** (`RedactionEntity`):
  - Stores x, y, width, height coordinates (PDF coordinate system)
  - pageNumber (0-indexed)
  - redactionType (enum: MANUAL or PII types)
  - Foreign key: projectId with CASCADE delete
- **Auto-save**: Triggered on every redaction add/update/delete operation
- **Type Conversion**: Room automatically handles RedactionType enum serialization

### Navigation
- **Routes**:
  - Home: `"home"` (defined in `Screen.kt:4`)
  - Editor: `"editor/{pdfId}"` (defined in `Screen.kt:5`)
- **Type Safety**: Sealed class `Screen` with `createRoute()` factory methods
- **Parameter Passing**: Path parameters (not argument bundles) for clean URL-style navigation
- **Navigation Graph**: Centralized in `NavGraph.kt` with Hilt ViewModel integration

### Internationalization
- **Supported Languages**: Korean (default), English
- **Implementation**: Android resource qualifiers (`values/` and `values-en/`)
- **String Resources**: 58+ localized strings including:
  - UI labels and buttons
  - PII type names
  - Error messages
  - 6-page help dialog content
- **Coverage**: Complete UI localization across all screens
- **Language Switching**: Automatic based on system locale

## Project Structure

```
app/src/main/java/org/comon/pdfredactorm/
├── MainActivity.kt                    # Entry point with Hilt and edge-to-edge
├── PdfRedactorApplication.kt         # Application class with @HiltAndroidApp
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt           # Room database (version 1, "pdf_redactor_db")
│   │   ├── dao/
│   │   │   ├── ProjectDao.kt        # Project CRUD with Flow queries
│   │   │   └── RedactionDao.kt      # Redaction CRUD with cascade delete
│   │   └── entity/
│   │       ├── ProjectEntity.kt     # Projects table with thumbnailUri
│   │       └── RedactionEntity.kt   # Redactions table with RedactionType
│   ├── pii/                         # PII detection components
│   │   ├── PiiPatterns.kt          # Regex patterns for 5 PII types
│   │   └── PiiTextStripper.kt      # Custom PDFBox TextStripper
│   └── repository/
│       └── PdfRepositoryImpl.kt     # Core PDF operations (295 lines)
├── di/
│   └── AppModule.kt                 # Hilt DI with PDFBox initialization
├── domain/
│   ├── model/
│   │   ├── PdfDocument.kt          # With thumbnailUri support
│   │   ├── RedactionMask.kt        # With RedactionType enum
│   │   └── DetectedPii.kt          # PII detection results
│   ├── repository/
│   │   └── PdfRepository.kt        # Including PII detection methods
│   └── usecase/
│       ├── LoadPdfUseCase.kt       # Load with auto-save
│       ├── SaveRedactedPdfUseCase.kt # Export redacted PDF
│       ├── GetRedactionsUseCase.kt # Retrieve saved redactions
│       ├── SaveRedactionsUseCase.kt # Persist redactions
│       └── DetectPiiUseCase.kt     # PII detection logic
├── presentation/
│   ├── editor/
│   │   ├── EditorScreen.kt         # 421 lines, Canvas-based rendering
│   │   └── EditorViewModel.kt      # 298 lines, comprehensive state
│   ├── home/
│   │   ├── HomeScreen.kt           # Recent projects list
│   │   ├── HomeViewModel.kt        # Project management
│   │   └── HelpDialog.kt           # 215 lines, 6-page manual
│   └── navigation/
│       ├── NavGraph.kt             # Compose navigation routes
│       └── Screen.kt               # Sealed class navigation
└── ui/theme/                        # Material3 theme configuration

app/src/main/res/
├── drawable/
│   ├── ic_highlighter.xml          # Masking mode icon
│   └── ic_pdf_background.xml       # Home screen background icon
├── values/
│   └── strings.xml                 # Korean strings (default)
└── values-en/
    └── strings.xml                 # English strings

docs/
├── pii_detection.md                # Comprehensive PII detection docs (Korean)
└── development_plan.md             # Project roadmap
```

## Development Notes

### Version Catalog
Dependencies are managed through `gradle/libs.versions.toml` using Gradle's version catalog feature. All library versions are centralized and referenced using type-safe accessors in `build.gradle.kts`.

### Build Configuration
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36
- **Compile SDK**: 36
- **Kotlin**: 2.2.21 with Compose Compiler plugin
- **Namespace**: org.comon.pdfredactorm
- **ProGuard**: Configured but not enabled for debug builds (`isMinifyEnabled = false`)
- **Packaging**: META-INF license files excluded to prevent conflicts

### Testing Structure
- **Unit tests**: `app/src/test/java/`
- **Instrumented tests**: `app/src/androidTest/java/`
- **Current Status**: Testing infrastructure configured with JUnit, Espresso, and Compose UI Test. Only example tests are present; domain and business logic tests need implementation.
- **Test Dependencies**: JUnit 4.13.2, AndroidX Test JUnit 1.3.0, Espresso 3.7.0, Compose UI Test JUnit4
- **IMPORTANT**: DO NOT run tests automatically unless explicitly requested by the user. Tests should only be executed when the user specifically asks to run them.

### UI/UX Features
- **Edge-to-edge**: Implemented with proper status bar handling
- **Canvas Rendering**: Custom PDF page rendering with touch gesture support
- **Masking Modes**: Toggle between view-only and manual drawing modes
- **Visual Feedback**: Loading states, error messages, success confirmations
- **Help System**: Interactive 6-page manual with swipe navigation and page indicators
- **Recent Projects**: Lazy-loaded list with delete functionality
- **File Picker**: System integration for PDF selection and save location

### Code Quality
- **Lint**: Available via `./gradlew lint` (default Android rules)
- **Architecture**: Clean separation of concerns across layers
- **Error Handling**: Result wrapper pattern in use cases
- **Resource Management**: Proper cleanup of PdfRenderer and temporary files
- **Type Safety**: Sealed classes for navigation, enums for PII types

## Additional Documentation

- **PII Detection**: See `docs/pii_detection.md` for comprehensive Korean documentation covering pattern details, architecture decisions, performance considerations, and future enhancement plans.
- **Development Plan**: See `docs/development_plan.md` for project roadmap and implementation phases.
- **Progress Tracking**: See `README.md` for current implementation status and completed features checklist.

## Working with This Codebase

When making changes to this project:

1. **PDFBox Initialization**: Always ensure PDFBox is initialized in `AppModule` before use
2. **Coordinate Systems**: Handle conversions between PDF (bottom-left) and UI (top-left) coordinate origins
3. **Database Transactions**: Use proper transaction management for auto-save functionality
4. **PII Detection**: Test with Korean documents as patterns are optimized for Korean PII types
5. **Internationalization**: Add strings to both `values/strings.xml` and `values-en/strings.xml`
6. **State Management**: Update EditorUiState immutably using copy() in ViewModel
7. **Lifecycle**: Properly clean up resources (PdfRenderer, temp files) in ViewModel.onCleared()
8. **Navigation**: Use sealed class factory methods for type-safe route construction
9. **Testing**: DO NOT run tests automatically. Only execute tests when explicitly requested by the user. Write unit tests for new domain logic and use cases before implementation.
10. **Build**: Use version catalog accessors when adding new dependencies
