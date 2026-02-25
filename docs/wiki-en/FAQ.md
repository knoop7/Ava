# FAQ

This page collects common questions and solutions for using Ava.

---

## Connection Issues

### Device not discovered by Home Assistant

**Possible causes:**
1. Device and HA not on same network
2. Firewall blocking connection
3. Ava service not started

**Solutions:**
1. Ensure device and HA connected to same WiFi
2. Check router firewall settings, open port 6053
3. Start voice satellite service in Ava
4. Try manually adding device (enter IP address)

### Connection frequently disconnects

**Possible causes:**
1. Unstable network
2. Ava killed by system
3. Battery optimization restrictions

**Solutions:**
1. Check WiFi signal strength
2. Disable battery optimization for Ava
3. Add Ava to system whitelist
4. Keep device charging

### How to disable battery optimization

1. Open phone **Settings**
2. Go to **Battery** or **App Management**
3. Find **Ava**
4. Select **Unrestricted** or **Allow background activity**

---

## Voice Issues

### Ava cannot hear me

**Checklist:**
- [ ] Microphone permission granted
- [ ] Mute mode disabled
- [ ] Device volume not muted
- [ ] Wake word configured correctly

**Solutions:**
1. Check microphone permission in phone settings
2. Disable mute in Ava settings
3. Press volume keys to increase volume
4. Try different wake word

### Wake word recognition inaccurate

**Possible causes:**
1. Environment too noisy
2. Pronunciation unclear
3. Too far from device

**Solutions:**
1. Use in quiet environment
2. Speak wake word clearly and slowly
3. Move closer to device
4. Try other wake words

### Speech recognition results incorrect

This is usually a Home Assistant issue:
1. Check HA voice assistant configuration
2. Ensure Whisper and other components working
3. Check network latency
4. Try speaking more clearly

### No voice response

**Checklist:**
- [ ] HA TTS configured correctly
- [ ] Ava volume not muted
- [ ] Network connection normal

---

## Display Issues

### Screensaver not showing

**Checklist:**
- [ ] Screensaver enabled
- [ ] Overlay permission granted
- [ ] Timeout configured
- [ ] Screensaver URL correct (if using web screensaver)

**Solutions:**
1. Enable screensaver in settings
2. Grant overlay permission
3. Set appropriate timeout
4. Try built-in Xiaomi Wallpaper

### Weather not showing

**Checklist:**
- [ ] Weather enabled
- [ ] City selected
- [ ] Chinese city selected
- [ ] Network connection normal

**Note:** Weather feature only supports Chinese cities.

### Notification scenes not showing

**Checklist:**
- [ ] Overlay permission granted
- [ ] Scene ID correct
- [ ] HA service call successful

**Solutions:**
1. Check overlay permission
2. Confirm scene ID spelling
3. Test service call in HA Developer Tools

### Browser showing blank

**Possible causes:**
1. URL incorrect
2. Network issue
3. Rendering issue

**Solutions:**
1. Check if URL is accessible
2. Check network connection
3. Try switching render mode (hardware/software)

---

## Permission Issues

### What permissions are required

| Permission | Purpose | Required |
|------------|---------|----------|
| Microphone | Voice input | Yes |
| Overlay | Display screensaver, notifications | Yes |
| Foreground Service | Keep service running | Yes |
| Camera | Photos and video | No |
| Bluetooth | Presence detection | No |
| Location | Bluetooth scanning | No |

### How to grant overlay permission

1. Open phone **Settings**
2. Go to **Apps** -> **Ava**
3. Find **Permissions** or **Special permissions**
4. Enable **Display over other apps** or **Overlay**

### How to re-grant denied permissions

1. Open phone **Settings**
2. Go to **Apps** -> **Ava** -> **Permissions**
3. Manually enable required permissions

---

## Performance Issues

### Device overheating

**Possible causes:**
1. Screen on for extended periods
2. Video stream frame rate too high
3. Too many background tasks

**Solutions:**
1. Lower screen brightness
2. Use screensaver (moves content)
3. Lower video frame rate and resolution
4. Disable unused features

### Battery draining fast

**Solutions:**
1. Keep device charging
2. Enable light sensor screen off
3. Lower screen brightness
4. Reduce background refresh frequency

### App lagging

**Solutions:**
1. Restart Ava service
2. Clear app cache
3. Reduce simultaneous features
4. Check device storage space

---

## Update Issues

### How to update Ava

**Auto update:**
Ava checks for updates hourly and auto-downloads new versions.

**Manual update:**
1. Visit GitHub Releases
2. Download latest APK
3. Install over old version

### Settings lost after update

Settings stored in app data, normal updates won't lose them. If lost:
1. Check if uninstalled then reinstalled (clears data)
2. Reconfigure settings

---

## Android System Compatibility

### MIUI/Xiaomi

**Common issues:**
- Background service killed
- Overlay permission restricted
- Auto-start disabled

**Solutions:**
1. Settings -> Apps -> Ava -> Battery saver -> No restrictions
2. Settings -> Apps -> Ava -> Permissions -> Background pop-up -> Allow
3. Settings -> Apps -> Auto-start management -> Allow Ava
4. Add Ava to battery optimization whitelist

### Huawei/Honor EMUI/HarmonyOS

**Common issues:**
- App killed by battery management
- Overlay permission needs extra setup

**Solutions:**
1. Settings -> Apps -> App launch management -> Ava -> Allow manual management
2. Settings -> Battery -> App launch management -> Ava -> Allow all
3. Settings -> Apps -> Ava -> Permissions -> Overlay -> Allow

### OPPO/Realme ColorOS

**Common issues:**
- Background running restricted
- Notification permission needs separate enabling

**Solutions:**
1. Settings -> Battery -> More battery settings -> Optimization strategy -> Ava -> Don't optimize
2. Settings -> Apps -> Ava -> Permissions -> Allow background running
3. Settings -> Notifications -> Ava -> Allow notifications

### vivo/iQOO OriginOS

**Common issues:**
- Background service restricted
- Auto-start needs manual enabling

**Solutions:**
1. Settings -> Battery -> Background power consumption -> Ava -> Allow high power consumption
2. Settings -> Apps & permissions -> Auto-start management -> Ava -> Allow

### Samsung One UI

**Common issues:**
- Battery optimization kills background service

**Solutions:**
1. Settings -> Apps -> Ava -> Battery -> Unrestricted
2. Settings -> Battery and device care -> Battery -> Background usage limits -> Add Ava to Never auto disable

### Stock Android/Pixel

Usually fewer issues, but still need:
1. Settings -> Apps -> Ava -> Battery -> Unrestricted

### Other Brands/Generic Systems

Most issues are permission-related, general solutions:

**Background running permission:**
1. Settings -> App management -> Ava -> Battery/Power saving -> Unrestricted/Don't optimize
2. Settings -> Battery -> Background management -> Allow Ava background running

**Overlay permission:**
1. Settings -> App management -> Ava -> Permissions -> Overlay/Display over other apps
2. Some systems may have it under "Special permissions"

**Auto-start permission:**
1. Settings -> App management -> Auto-start management -> Allow Ava
2. Some systems may have it in "Security Center" or "Phone Manager"

**General tips:**
- Add Ava to battery optimization whitelist
- Lock Ava in recent tasks (tap lock icon on card)
- Keep device charging

---

## Technical Issues

### WebView crash

**Possible causes:**
1. WebView version outdated
2. Insufficient memory
3. Web content too complex

**Solutions:**
1. Update system WebView (via Play Store)
2. Switch to software rendering mode
3. Ava has built-in crash recovery, will auto-rebuild WebView

### Scenes not loading

**Possible causes:**
1. Network cannot access GitHub
2. Cache corrupted

**Solutions:**
1. Check network connection
2. Restart Ava app
3. Scenes cache locally, next startup loads from cache first

### Bluetooth proxy not working

**Checklist:**
- [ ] Bluetooth permission granted
- [ ] Location permission granted (Android requirement)
- [ ] Bluetooth enabled
- [ ] Device slots configured

---

## About Open Source

### Why is some code not open source

Ava is a personal project, originally just for my own use. Code for features like Bluetooth proxy is not open source to prevent copying.

All other feature code is available on GitHub.

### Can I contribute code

Pull Requests welcome. But honestly, few people contribute. This project isn't meant to please everyone - it was originally just for myself.

### How to report issues

1. Visit GitHub Issues
2. Describe the problem
3. Provide device info and logs
4. Attach screenshots (if any)

**GitHub Issues**: https://github.com/knoop7/Ava/issues

---

## Other Issues

### How to restart service

1. Go to **Settings** -> **Voice Satellite**
2. Tap **Stop Service**
3. Wait a few seconds
4. Tap **Start Service**

### How to view logs

Use ADB to view logs:
```bash
adb logcat | grep -i ava
```

### How to report issues

1. Visit GitHub Issues
2. Describe the problem
3. Provide device info and logs
4. Attach screenshots (if any)

**GitHub Issues**: https://github.com/knoop7/Ava/issues

---

*Back to [Home](Home.md)*
