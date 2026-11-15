# Proguard rules for ShortBan (ScrollBlock)

# Accessibility Service & Tile Service
-keep class com.example.scrollblock.service.** { *; }

# Short-form video and comment detectors
-keep class com.example.scrollblock.detector.** { *; }

# Overlay and UI managers
-keep class com.example.scrollblock.overlay.** { *; }

# Preferences and policies
-keep class com.example.scrollblock.preferences.** { *; }

# Security and hashing
-keep class com.example.scrollblock.security.** { *; }

# Updater and package managers
-keep class com.example.scrollblock.updater.** { *; }

# Android 12 isolation modules
-keep class com.example.scrollblock.android12.** { *; }
