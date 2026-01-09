# Music Playback

Ava can serve as a Home Assistant media player, playing music and voice announcements.

---

## Overview

Ava supports the following audio features:
- Media playback (music, podcasts, etc.)
- TTS voice announcements
- Wake prompt sounds
- Timer sounds

---

## Media Player

### Entity Type

In Home Assistant, Ava registers as a media player entity:
- `media_player.your_device_name`

### Supported Operations

| Operation | Description |
|-----------|-------------|
| Play | Play media |
| Pause | Pause playback |
| Stop | Stop playback |
| Volume | 0-100% |
| Mute | Enable/disable mute |

---

## Playing Music

### Via Home Assistant Service

```yaml
service: media_player.play_media
target:
  entity_id: media_player.your_device_name
data:
  media_content_id: "http://example.com/music.mp3"
  media_content_type: "music"
```

### Supported Formats

| Format | Description |
|--------|-------------|
| MP3 | Most common |
| AAC | Apple format |
| OGG | Open source format |
| WAV | Lossless format |
| FLAC | Lossless compressed |

### Supported Sources

- HTTP/HTTPS URLs
- Local file paths
- Streaming URLs

---

## Vinyl Record Cover

When playing music, Ava can display beautiful vinyl record style album covers.

### How to Enable

1. Go to **Settings** → **Interaction**
2. Turn on **Vinyl Record Cover** switch

### Cover Sources

Ava automatically tries to get album covers:

1. **Media Metadata**: Extract from playing media
2. **NetEase Music**: Auto-match NetEase covers
3. **Custom Cover**: Set via service

### Set Cover

```yaml
service: esphome.your_device_name_media_cover
data:
  url: "http://example.com/cover.jpg"
```

---

## TTS Voice Announcements

### Via Home Assistant

```yaml
service: tts.speak
target:
  entity_id: media_player.your_device_name
data:
  message: "Hello, welcome home"
```

### TTS Engines

Ava uses the TTS engine configured in Home Assistant. Recommended:
- Piper (local, fast)
- Google TTS (online, good quality)
- Azure TTS (online, good quality)

---

## Volume Control

### Set Volume

```yaml
service: media_player.volume_set
target:
  entity_id: media_player.your_device_name
data:
  volume_level: 0.8  # 0.0 - 1.0
```

### Mute

```yaml
service: media_player.volume_mute
target:
  entity_id: media_player.your_device_name
data:
  is_volume_muted: true
```

### In Ava Settings

1. Go to **Settings** → **Interaction**
2. Adjust **Volume** slider

---

## Conversation Subtitles

Display text content of voice conversations.

### Display Content

- **Your speech**: Displayed at top of screen
- **Ava's response**: Displayed at bottom of screen

### How to Enable

1. Go to **Settings** → **Interaction**
2. Turn on **Conversation Subtitles** switch

---

## Settings Summary

| Setting | Location | Description | Default |
|---------|----------|-------------|---------|
| Volume | Interaction | Media playback volume | 80% |
| Mute | Interaction | Mute switch | Off |
| Vinyl Record Cover | Interaction | Show album cover | On |
| Conversation Subtitles | Interaction | Show conversation text | Off |

---

## Home Assistant Services

### Play Media

```yaml
service: media_player.play_media
target:
  entity_id: media_player.your_device_name
data:
  media_content_id: "http://example.com/music.mp3"
  media_content_type: "music"
```

### Pause

```yaml
service: media_player.media_pause
target:
  entity_id: media_player.your_device_name
```

### Resume

```yaml
service: media_player.media_play
target:
  entity_id: media_player.your_device_name
```

### Stop

```yaml
service: media_player.media_stop
target:
  entity_id: media_player.your_device_name
```

### Set Volume

```yaml
service: media_player.volume_set
target:
  entity_id: media_player.your_device_name
data:
  volume_level: 0.8
```

---

## FAQ

### Music not playing?

1. Check if URL is accessible
2. Check if audio format is supported
3. Check if volume is muted
4. Check Home Assistant connection

### Cover not showing?

1. Check if Vinyl Record Cover is enabled
2. Check network connection
3. Try manually setting cover URL

### Volume too low?

1. Increase volume in Ava settings
2. Increase device system volume
3. Check if mute is enabled

---

*Back to [Home](Home.md)*
