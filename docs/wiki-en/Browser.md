# Built-in Browser

The built-in browser lets you display web pages inside Ava, such as Home Assistant dashboards.

---

## Overview

The built-in browser is a full-screen WebView that can display any web content.

**Main Uses:**
- Display Home Assistant dashboards
- Display custom control panels
- Display information websites

---

## Features

### Basic Features

| Feature | Description |
|---------|-------------|
| Full Screen | Web page fills entire screen |
| Pull to Refresh | Pull down to refresh page |
| JavaScript | Full JS support |
| Local Storage | LocalStorage supported |

### Advanced Features

| Feature | Description |
|---------|-------------|
| Custom CSS | Inject custom styles |
| Custom JS | Inject custom scripts |
| Render Mode | Hardware/software rendering switch |

---

## Settings

### How to Open

Open browser via Home Assistant service:

```yaml
service: esphome.your_device_name_ha_remote_url
data:
  url: "http://your-ha-address:8123/lovelace/0"
```

### Settings Options

Go to **Settings** → **Browser**

| Setting | Description | Default |
|---------|-------------|---------|
| Advanced Control | Enable advanced browser control | Off |
| Pull to Refresh | Allow pull-down to refresh page | On |
| Initial Scale | Page initial zoom (0-500, 0=auto) | 0 |
| Font Size | Page font size percentage (50-300) | 100 |
| Touch Enabled | Allow touch interaction | On |
| Drag Enabled | Allow drag operations | On |
| Render Mode | Hardware/software rendering | Hardware |
| Settings Button | Show settings button | Off |
| Back Key Hide | Press back key to hide browser | On |

### Render Mode

| Mode | Description | Use Case |
|------|-------------|----------|
| Hardware | Uses GPU, better performance | Most devices |
| Software | Uses CPU, better compatibility | Old devices or rendering issues |

---

## Home Assistant Dashboard

### Recommended Setup

Create a dedicated dashboard view for Ava:

```yaml
# configuration.yaml
lovelace:
  mode: yaml
  dashboards:
    ava-dashboard:
      mode: yaml
      filename: ava_dashboard.yaml
      title: Ava
      icon: mdi:tablet
      show_in_sidebar: false
```

### Hide Sidebar

Use custom CSS to hide HA sidebar and header:

```css
/* Hide sidebar */
ha-sidebar { display: none !important; }

/* Hide header */
.header { display: none !important; }

/* Full screen content */
home-assistant { --app-drawer-width: 0px; }
```

---

## Advanced Control

After enabling Advanced Control, you can remotely control the browser via Home Assistant services.

### Supported Operations

- Open specified URL
- Close browser
- Refresh page
- Forward/back navigation

### Note

Enabling Advanced Control will restart the Voice Satellite service.

---

## Home Assistant Control

Browser is controlled via `ha_remote_url` text entity, commands executed via `browser_command` text entity.

### Open Web Page

```yaml
service: text.set_value
target:
  entity_id: text.your_device_name_ha_remote_url
data:
  value: "http://your-ha-address:8123/lovelace/0"
```

### Close Browser

```yaml
service: text.set_value
target:
  entity_id: text.your_device_name_ha_remote_url
data:
  value: ""
```

### Execute Command

Commands use JSON format:

```yaml
service: text.set_value
target:
  entity_id: text.your_device_name_browser_command
data:
  value: '{"reload": true}'
```

### Supported Commands

| Command | Format | Description |
|---------|--------|-------------|
| Reload page | `{"reload": true}` | Reload current page |
| Clear cache | `{"clearCache": true}` | Clear browser cache |
| Open settings | `{"settings": true}` | Open Ava settings page |
| Set brightness | `{"brightness": 128}` | Set screen brightness (0-255) |
| Execute JS | `{"eval": "alert('hello')"}` | Execute JavaScript code |
| Clear injected | `{"eval": ""}` | Clear injected effects |

---

## Using AI to Generate Animation Effects

You can use the Gemini AI tool to generate custom animation effects:

https://gemini.google.com/gem/ee3cb858f9d0

Tell the AI what effect you want (like "cherry blossoms falling", "snow", "stars", etc.), and it will generate a usable JSON command.

### Example: Cherry Blossom Animation

```yaml
service: text.set_value
target:
  entity_id: text.your_device_name_browser_command
data:
  value: '{"eval": "!function(d,b,c,s){d=document,b=d.body,c=d.createElement(\\\"div\\\"),s=d.createElement(\\\"style\\\"),c.style.cssText=\\\"position:fixed;top:0;left:0;width:100%;height:100%;z-index:9999;overflow:hidden;pointer-events:none\\\",s.textContent=\\\".s{position:absolute;top:-10%;background:linear-gradient(135deg,#fff 20%,#ffb7c5 100%);border-radius:100% 0 120% 0;box-shadow:1px 1px 2px rgba(0,0,0,.1);animation:f linear infinite,w ease-in-out infinite alternate}@keyframes f{0%{opacity:0;transform:translateY(0) rotateX(0) rotateZ(0)}20%{opacity:1}100%{opacity:0;transform:translateY(110vh) rotateX(360deg) rotateZ(720deg)}}@keyframes w{0%{margin-left:-50px}100%{margin-left:50px}}\\\",d.head.appendChild(s),b.appendChild(c),setInterval(function(p,w,t){p=d.createElement(\\\"div\\\"),w=Math.random()*20+8,t=Math.random()*10+6,p.className=\\\"s\\\",p.style.cssText=\\\"left:\\\"+Math.random()*100+\\\"%%;width:\\\"+w+\\\"px;height:\\\"+w*1.4+\\\"px;animation-duration:\\\"+t+\\\"s,\\\"+(Math.random()*4+3)+\\\"s;animation-delay:-\\\"+Math.random()*8+\\\"s\\\",c.appendChild(p),setTimeout(function(){c.removeChild(p)},t*1e3)},100)}()"}'
```

### Clear Animation Effects

```yaml
service: text.set_value
target:
  entity_id: text.your_device_name_browser_command
data:
  value: '{"eval": ""}'
```

---

## WebView Stability

### Renderer Crash Handling

Ava implements automatic WebView renderer crash recovery:

1. Detect renderer crash
2. Destroy old WebView
3. Wait 500ms then rebuild
4. Reload previous URL

### Correct Destruction Order

To avoid crashes, WebView destruction follows this order:

1. `stopLoading()` - Stop loading
2. `onPause()` - Pause
3. `pauseTimers()` - Pause timers
4. `loadUrl("about:blank")` - Load blank page
5. `removeView()` - Remove from parent
6. `destroy()` - Destroy

---

## FAQ

### Web page shows blank?

1. Check if URL is correct
2. Check network connection
3. Try switching render mode
4. Check if HA is accessible

### Web page loads slowly?

1. Check network speed
2. Simplify dashboard content
3. Reduce animation effects

### Touch not responding?

1. Check overlay permission
2. Try restarting service
3. Check if other overlays are blocking

### How to return to main interface?

1. Use Home Assistant service to close browser
2. Or add a return button in HA dashboard

---

*Back to [Home](Home.md)*
