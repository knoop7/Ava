**StemDock Quick Entity Dashboard**

Full-screen floating panel that displays up to 6 smart home devices - see status at a glance, control with a tap.

**Where to Find**
Screen Display -> Quick Entity Panel -> Enable main switch -> Add device IDs at the bottom.
You can also enable "HA Slot Configuration" to configure the panel directly from Home Assistant by entering device IDs.

**Controls**
**Single tap** = Toggle device
**Long press and drag** = Rearrange card position
**Long press empty area** = Hide panel

**Auto Layout**
1-6 devices automatically arrange into corresponding grid, adapts to landscape and portrait orientation.

**Colors and Icons**
Each device gets auto-assigned soft colors (22 color tones), lights use fixed warm yellow. Icons auto-match based on device name, or manually select color using color wheel. Device names are automatically fetched from HA.

**Sensors**
Temperature, humidity and other values update in real-time, up to 2 decimal places, units auto-fetched.

**HA Configuration**
After enabling "HA Slot Configuration", 6 text input fields appear in HA (ava_quick_entity_slot_1 to 6), enter device IDs. Clear all slots to auto-close panel, fill any slot to auto-open.

**Supported Devices**
Lights, switches, fans, covers, buttons, scripts, scenes, sensors.
