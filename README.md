# Ava Pro · Home Assistant's Android Companion

[![DeepWiki](https://img.shields.io/badge/DeepWiki-AI_Docs-0D1117?style=for-the-badge&labelColor=21262d&logoColor=white)](https://deepwiki.com/knoop7/Ava)
[![Discord](https://img.shields.io/badge/Discord-Join-0D1117?style=for-the-badge&labelColor=21262d&logo=discord&logoColor=white)](https://discord.gg/Znd68HA8)

For more practical guides, visit the [Wiki](https://deepwiki.com/knoop7/Ava).

Share a project I've been working on: **Ava**, an Android voice assistant app that turns old tablets, phones, **car head units, smart mirrors, and Android-powered appliances** into powerful smart home control panels. **Compatible with the full range of Android ecosystems.**

## Background

This project is based on [brownard/Ava](https://github.com/brownard/Ava) with extensive modifications and extensions. The original was a great ESPHome voice satellite implementation, but with basic features. I researched existing solutions: [Fully Kiosk](https://www.fully-kiosk.com/) is powerful but paid. [WallPanel](https://wallpanel.xyz/) is no longer maintained. The Android smart home panel space has been stagnant for years. So I decided to combine the best of these projects, using Ava as the foundation to build something truly useful.

## Core Philosophy

`No Extra Integrations`
No MQTT, no HACS. Devices are discovered natively by Home Assistant, just like ESPHome nodes.

`Built for Low-End Hardware`
Supports Android 7–16, including 32-bit devices. Cheap tablets and old phones work reliably.

`Expanding to Android 4.4–6`
Earlier Android versions are currently under active development. The goal is full BLE proxy compatibility without compromises.

---

## Bluetooth Proxy - Exclusive Feature

**This is a feature no other Android panel app has.**

Your Android device becomes a complete Bluetooth gateway, extending Home Assistant's Bluetooth coverage. No ESP32 needed - just use your spare Android device!

**Features:**
`BLE Proxy` Forwards all Bluetooth Low Energy data to Home Assistant.
`Whole-Home Coverage` Deploy multiple Ava devices, each extends Bluetooth range.
`Presence Detection` Auto-detect phones, smartwatches, bands; trigger home/away automations.
`All BLE Devices Compatible` Temperature sensors, plant monitors, smart locks, all supported.

**Why This Matters:**
ESP32 Bluetooth proxies need firmware flashing and configuration. Ava works out of the box - your Android device is already a powerful Bluetooth transceiver. Perfect for apartments or multi-story homes where one Bluetooth source isn't enough. Turn that dusty old tablet in the bedroom into a Bluetooth relay point.

**Presence Detection Use Cases:**
Phone enters Bluetooth range → Auto lights on, AC on, play welcome message. Phone leaves for a while → Auto lights off, security mode on. Detect specific family member devices → Personalized scene triggers. Adjustable RSSI threshold and away delay for precise sensitivity control.

This feature alone is worth trying if you've struggled with Bluetooth coverage in Home Assistant.

> **Note:** Bluetooth proxy source code is not open source, only available in release builds. All other feature code is available on GitHub.

---

## Floating Windows - Visual Differentiation

This is what makes Ava visually unique. **Floating windows overlay on top of any app** - display a full-screen HA dashboard while still seeing clock, weather, and notifications overlaid on top.

Other apps force you to choose: dashboard or clock. With Ava, you get both.

*   `Always Visible` Overlays on any app, including full-screen browsers.
*   `HA Control` Remotely toggle each window from Home Assistant.
*   `Non-Intrusive` Designed to complement, not block the main interface.

Available floating windows:
`Dream Clock` Elegant always-on clock.
`Vinyl Cover` Rotating record cover when playing music.
`Conversation Subtitles` Shows what you said and AI responses.
`Notification Scenes` Full-screen alerts for doorbell, alarms, etc.

---

## What is Ava?

Ava is a voice assistant app based on the ESPHome protocol. It turns your Android device into a voice satellite for Home Assistant.

**What can you do with it?**
Control smart home devices with voice (lights, AC, music, etc.). Display beautiful screensavers and clocks. Show real-time weather information. Display full-screen notifications (doorbell, alerts, etc.). Play music with album cover display. Take photos and stream video.

Put your old phone or tablet at home, and it becomes a smart control panel!

**System Requirements:**
Android 7.0 or higher. Home Assistant connection required.

> This makes Ava suitable for turning *any* phone or tablet into a native-looking Home Assistant panel, consistent with the rest of your HA ecosystem.

> Ava does not impose a custom UI identity.
> Instead, it deliberately follows Home Assistant’s visual language and iconography.
> This ensures that any device running Ava feels like an extension of Home Assistant itself, not a third-party layer on top.

---

## Quick Start

### Step 1: Install

1. Download the Ava APK file from [GitHub Releases](https://github.com/knoop7/Ava/releases) Built-in auto-update feature.
2. Tap to install.
3. Allow all permission requests (microphone, overlay, etc.).

### Step 2: Connect to Home Assistant

**In Home Assistant:**
1. Go to **Settings** → **Devices & Services** → **Integrations**.
2. Search and add **ESPHome** integration.
3. Ava will be discovered automatically, tap **Configure**.

**In Ava app:**
1. Open Ava app.
2. Tap the **Settings** icon in the top right.
3. Select **Voice Satellite**.
4. Tap **Start Service**.

**After successful connection:**
Status bar will show "Connected". You can see this device in Home Assistant.

### Step 3: Talk!

Say the wake word (default is "Hey Jarvis"), then say your command: "Turn on the living room light". "What time is it". "Play music". "What's the weather tomorrow".

---

## Permissions

**Required:**
`Microphone` To hear you speak.
`Overlay` To show screensaver, notifications, weather, etc.
`Foreground Service` To keep service running.

**Optional:**
`Camera` For photos and video.
`Bluetooth` To detect if you're home.
`Location` Required for Bluetooth scanning.
`System Settings` Screen brightness control.

**Root (Recommended):**
Better background protection, boot scripts, screen control, reboot. Works without Root, but more stable with it.

---

## Stability

Optimized for 24/7 operation: `battery optimization exemption` `WiFi wake lock` `auto-reconnect` `boot auto-start` `auto-recovery when killed`.

---

## Credits

*   Original Project: [brownard/Ava](https://github.com/brownard/Ava)
*   ESPHome: https://esphome.io/
*   Home Assistant: https://www.home-assistant.io/

---

*Last Updated: 2026-03-12*
