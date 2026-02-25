# Ava Userscript Development Guide

Lightweight userscript manager, based on the Android-WebMonkey project.

## Supported Greasemonkey APIs

### Data Storage
```javascript
// Store data
GM_setValue('key', 'value');
GM_setValue('count', 123);
GM_setValue('config', {a: 1, b: 2});

// Read data
var value = GM_getValue('key', 'default');

// Delete data
GM_deleteValue('key');

// List all keys
var keys = GM_listValues();
```

### Page Styling
```javascript
// Add CSS styles
GM_addStyle('body { background: #000 !important; }');
GM_addStyle('.ad { display: none !important; }');

// Add DOM elements
GM_addElement('script', { src: 'https://example.com/lib.js' });
GM_addElement('link', { rel: 'stylesheet', href: 'https://example.com/style.css' });
```

### Network Requests
```javascript
// Send HTTP request
GM_xmlhttpRequest({
    method: 'GET',
    url: 'https://api.example.com/data',
    headers: { 'Content-Type': 'application/json' },
    onload: function(response) {
        console.log(response.responseText);
        var data = JSON.parse(response.responseText);
    },
    onerror: function(error) {
        console.error('Request failed', error);
    }
});

// POST request
GM_xmlhttpRequest({
    method: 'POST',
    url: 'https://api.example.com/submit',
    data: JSON.stringify({ name: 'test' }),
    headers: { 'Content-Type': 'application/json' },
    onload: function(response) {
        console.log('Submit successful');
    }
});

// Using fetch-style API
GM_fetch('https://api.example.com/data')
    .then(response => response.json())
    .then(data => console.log(data));
```

### Cookie Management
```javascript
// Get cookie list
GM_cookie.list({}).then(cookies => {
    console.log(cookies);
});

// Set cookie
GM_cookie.set({
    name: 'token',
    value: 'abc123',
    domain: 'example.com',
    path: '/'
});

// Delete cookie
GM_cookie.delete({ name: 'token' });

// Delete all cookies
GM_removeAllCookies();
```

### Notifications and Logging
```javascript
// Show Toast notification
GM_notification('Operation successful');
GM_toastShort('Short message');
GM_toastLong('Long message');

// Output log
GM_log('Debug info');
```

### Page Control
```javascript
// Get current URL
var url = GM_getUrl();

// Load new page
GM_loadUrl('https://example.com');
GM_loadUrl('https://example.com', { 'Referer': 'https://google.com' });

// Load iframe
GM_loadFrame('https://example.com/frame.html', 'https://example.com');

// Resolve relative URL
var fullUrl = GM_resolveUrl('/path/to/page', 'https://example.com');

// Exit/close
GM_exit();
```

### User-Agent
```javascript
// Get current UA
var ua = GM_getUserAgent();

// Set custom UA
GM_setUserAgent('Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/100.0.0.0');
```

### Other Features
```javascript
// Open in new tab
GM_openInTab('https://example.com');

// Copy to clipboard
GM_setClipboard('Text to copy');

// Launch Android Intent
GM_startIntent('android.intent.action.VIEW', 'https://example.com', null, null);

// Register menu command (logging only)
GM_registerMenuCommand('Settings', function() {
    console.log('Settings clicked');
});
```

### GM 4 Promise-style API
All APIs support Promise-style calls:
```javascript
await GM.getValue('key');
await GM.setValue('key', 'value');
await GM.addStyle('body { color: red; }');
await GM.cookie.list({});
```

## Script Template

```javascript
// ==UserScript==
// @name         My Script
// @namespace    https://example.com
// @version      1.0
// @description  Script description
// @match        https://example.com/*
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_addStyle
// @grant        GM_xmlhttpRequest
// ==/UserScript==

(function() {
    'use strict';
    
    // Your code here
    console.log('Script loaded');
    
    // Example: Hide ads
    GM_addStyle('.ad, .advertisement { display: none !important; }');
    
    // Example: Modify page content
    document.querySelectorAll('h1').forEach(el => {
        el.style.color = 'red';
    });
})();
```

## Unsupported Features

- `GM_download` - File download
- `GM_webRequest` - Request interception
- `unsafeWindow` - Direct access to page window object
- `GM_getTab` / `GM_saveTab` - Tab data
- `@require` - External script dependencies
- `@resource` - External resources (partial support)

## Notes

1. Scripts execute after page load completes
2. Cross-origin requests are subject to browser security policies
3. Some websites may detect and block script execution
4. Recommend using try-catch to wrap code to prevent errors

## Project Repository

https://github.com/warren-bank/Android-WebMonkey
