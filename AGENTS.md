# WiFi Hotspot Manager - Agent Instructions

## Overview
This is an Android project designed to manage a WiFi hotspot with Arabic UI support.

## Key Components
- `MainActivity.java`: Main controller.
- `HotspotManager.java`: Handles WiFi AP states.
- `DeviceAdapter.java`: RecyclerView adapter for connected devices.
- `res/values-ar/strings.xml`: Arabic translations.

## Building the project
Use Android Studio to build the project. The `app/build.gradle` is configured for SDK 33.

## Limitations
- Controlling data usage and blocking specific MAC addresses usually requires `iptables` commands which need **Root access**.
- Android 8.0+ has restrictions on programmatic hotspot enabling.
