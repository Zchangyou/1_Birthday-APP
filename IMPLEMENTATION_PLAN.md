# Implementation Plan

## Stage 1: Fix Android 15/16 Theme Crash
**Goal**: Resolve the issue where clicking the app icon doesn't open the app on Honor Magic 6.
**Root Cause**: The app crashes immediately upon launch because `themes.xml` tries to use `?attr/colorPrimaryVariant` for `android:statusBarColor`. The app's theme inherits from `Theme.Material3.DayNight.NoActionBar`, but Material 3 removed the `colorPrimaryVariant` attribute. This causes the system to throw a fatal exception when inflating the activity window.
**Success Criteria**: The `themes.xml` file is updated to use `@android:color/transparent`.
**Tests**: N/A - Manual verification on an Android 15 device (Honor Magic 6).
**Status**: Complete

## Stage 2: Application Build & Verification
**Goal**: Ensure the app continues to compile successfully.
**Success Criteria**: `./gradlew build` runs successfully.
**Tests**: `assembleDebug`
**Status**: Complete
