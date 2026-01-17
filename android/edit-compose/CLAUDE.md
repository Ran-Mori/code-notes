# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android application project named "edit-compose" - an image editing app with 5-tab navigation built using Jetpack Compose and MVI architecture.

**Key Details:**
- **Application ID**: com.edit.compose
- **Package Structure**: com.baj (MainActivity location)
- **Language**: Kotlin
- **Build System**: Gradle with Android Gradle Plugin 7.2.1
- **UI Framework**: Jetpack Compose (Material 2)
- **Architecture**: MVI (Model-View-Intent) pattern
- **Current State**: Fully functional with 5 tabs and navigation

## Build Commands

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug build on connected device
./gradlew installDebug

# Run all tests
./gradlew test

# Run Android instrumented tests
./gradlew connectedAndroidTest

# Build and install in one command
./gradlew installDebug
```

## Architecture

### Project Structure
- **MainActivity**: Compose Activity at `app/src/main/java/com/baj/MainActivity.kt`
- **MVI Architecture**: Base classes in `presentation/mvi/`
- **5 Tabs**: Adjust, Filter, Crop, AI Erase, Create
- **Navigation**: Jetpack Navigation Compose with bottom bar

### Key Components
- **Adjust Tab**: Image adjustments (exposure, highlight, shadow, brightness)
- **Crop Tab**: Image cropping with rotation controls (hides bottom bar)
- **Filter Tab**: Placeholder for image filters
- **AI Erase Tab**: Placeholder for AI-powered object removal
- **Create Tab**: Placeholder for collage creation

### Key Dependencies
- **Jetpack Compose**: UI framework (1.2.0)
- **Compose Navigation**: Navigation between screens
- **Coil**: Image loading and display
- **ViewModel**: State management with MVI pattern
- **CodeLocator**: ByteDance debugging tool
- **Retrofit**: HTTP networking (for future use)
- **RxJava3**: Reactive programming (for future use)

### SDK Configuration
- **Compile SDK**: 34
- **Min SDK**: 33 (Android 13+ only)
- **Target SDK**: 33
- **Kotlin**: 1.7.0
- **Compose Compiler**: 1.2.0
- **JVM Target**: 1.8

## Development Notes

### MVI Architecture Pattern
Each tab follows the MVI pattern with:
- **State**: Current UI state (data classes)
- **Intent**: User actions (sealed interfaces)
- **Effect**: Side effects like toasts/errors (sealed interfaces)
- **ViewModel**: Processes intents and manages state

### Navigation Behavior
- Bottom navigation with 5 tabs
- Bottom bar hidden in Crop tab (as per requirements)
- Back navigation returns to previous tab
- Each tab maintains its own state

### Current Implementation Status
✅ **Completed**:
- 5-tab bottom navigation
- MVI architecture foundation
- Adjust tab with working sliders
- Crop tab with rotation controls
- Navigation state management
- Compose UI with Material 2

🔄 **UI Structure Only** (no actual image processing):
- Image display using Coil with mock data
- Adjustment controls (sliders work but don't process images)
- Crop overlay UI (visual only)

### Technical Considerations
1. **High Min SDK**: Only Android 13+ devices supported
2. **Material 2**: Using Material 2 due to compatibility with Kotlin 1.7.0
3. **Mock Data**: Using placeholder images from picsum.photos
4. **No Image Processing**: UI controls are functional but don't actually process images

### Future Development
To add actual image processing:
- Implement image manipulation algorithms
- Add image file picker/gallery access
- Connect UI controls to real image processing
- Add save/export functionality