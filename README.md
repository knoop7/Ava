# Android Voice Assistant (Ava)

Experimental Android voice assistant for [Home Assistant][homeassistant] that uses the [ESPHome][esphome] protocol.

Intended for turning your existing Android wall panel or similar into a local voice assistant for controlling your smart home using Home Assistant.

⚠️**Warning: This app is intended for 'static' local devices like a wall panel on a trusted network, it is not suitable for your mobile device (though it probably will work) for the following reasons**⚠️
- It exposes a port to allow incoming connections from Home Assistant with no authentication whatsoever (the device is the server in the ESPHome API) so shouldn't be used on any untrusted networks. Additionally the device needs to be reachable from Home Assistant for the app to function, which probably means the device will need to remain on the local network.
- It is constantly using the microphone to listen for the wake word, which is not only a privacy issue but also a battery drain. Android does not allow non-system apps to listen 'passively' like the built in assistants.

Requires Android 8 or above.

[![paypal](https://www.paypalobjects.com/en_GB/i/btn/btn_donate_LG.gif)](https://www.paypal.com/donate/?business=EU5MC92FG3JF6&no_recurring=0&currency_code=GBP)

# Implemented
- Local wake word detection using microWakeWord models
- Stop word detection
- Voice commands
- Announcements and conversations
- Media player
- Microphone mute switch
- Wake/preannounce sounds
- Timers

# Todo
- Multiple wake word support

# Setup
- Install and run the app
- Optionally change the satellite name and port settings
- Start the satellite service, it will continue to run in the background until manually stopped.
- In Home Assistant:
  - Add or open the ESPHome integration
  - The satellite should be auto-detected in most cases, if not, manually add the satellite using the device's IP address and configured port (Default: 6053)
  - Complete the voice assistant setup wizard
 
# App Settings

Once connected, the satellite is fully configurable from within Home Assistant as a ESPHome voice satellite and media player, however some settings can also be changed from within the app.

- Name: The display name of the satellite. Requires service restart
- Port: The port to listen on. Requires service restart
- Wake word: The wake word to use. Note that changing this in the app does immediately change the wake word, however currently the change is not reflected in Home Assistant. It is recommended to change the wake word from Home Assistant to ensure the configuration remains in sync.
- Enable wake sound: Whether to play a sound when the satellite is woken by a wake word

# Development
1. Clone the repo
2. Open in Android Studio

# Debugging on an emulated device
1. Telnet into the device, from a terminal run `telnet localhost 5554`
2. Enter your auth token if required
3. Add a redirect for the server port (default 6053) to allow incoming connections to be routed to the device, run `redir add tcp:6053:6053`

<!-- Links -->
[homeassistant]: https://www.home-assistant.io/
[esphome]: https://esphome.io/
