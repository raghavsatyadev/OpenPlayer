# OpenPlayer

<p align="center">
  <a href="https://github.com/raghavsatyadev/OpenPlayer/releases/latest">
    <img src="https://img.shields.io/github/v/release/raghavsatyadev/OpenPlayer?include_prereleases&logo=github&style=for-the-badge&label=Latest%20Release" alt="Latest release">
  </a>
  <a href="https://github.com/raghavsatyadev/OpenPlayer/releases">
    <img src="https://img.shields.io/github/downloads/raghavsatyadev/OpenPlayer/total?logo=github&style=for-the-badge" alt="Total downloads">
  </a>
  <img src="https://img.shields.io/badge/Android-11%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 11+">
  <img src="https://img.shields.io/badge/License-GPLv3-blue?style=for-the-badge" alt="GPLv3 license">
</p>

## What It Is

OpenPlayer is an Android music player focused on local playback, self-hosted music libraries, an expressive Material 3 UI, and user-controlled online lookups.

The app works offline by default. Optional online services stay disabled until you enable them in setup or settings.

Package name: `io.github.raghavsatyadev.openplayer`

## Why This Exists

OpenPlayer stays FOSS-oriented and leaves out integrations that don't fit that direction: Telegram, NetEase, QQ Music, Google Drive, Gemini, Cast, Wear OS, Play Store billing, Firebase, Crashlytics, and Google Play Services runtime dependencies.

Cloud playback is limited to self-hosted sources: Navidrome/Subsonic and Jellyfin.

## Features

| Area | Highlights |
| --- | --- |
| Playback | Media3 playback engine, FFmpeg support, gapless playback, crossfade, custom transitions, queue controls, shuffle, repeat, sleep timer, external file playback |
| Library | Local scanning for MP3, FLAC, AAC, OGG, WAV, M4A, albums, artists, genres, folders, favorites, playlists, stats, metadata editing |
| Self-hosted | Navidrome/Subsonic and Jellyfin login, sync, streaming, artwork, and app-private offline downloads |
| Lyrics | Embedded lyrics, local `.lrc` files, lyrics import/editing, optional LRCLIB lookup |
| Artwork | Local artwork, album-art palette extraction, optional Deezer artist image lookup |
| Metadata | On-demand MusicBrainz matching for recording, release, and artist identifiers |
| UI | Jetpack Compose, Material 3, dynamic color, light/dark themes, Glance widgets, animated player surfaces |
| Backup | Preferences, playlists, favorites, lyrics, stats, and app state backup/restore |

## Online Services

OpenPlayer separates offline playback from network lookups.

| Service | Purpose | Default |
| --- | --- | --- |
| Navidrome/Subsonic | Self-hosted library sync, streaming, and offline downloads | User login required |
| Jellyfin | Self-hosted library sync, streaming, and offline downloads | User login required |
| MusicBrainz | On-demand metadata matching and identifier enrichment | Only when requested |
| LRCLIB | Search online lyrics when local or embedded lyrics are missing | Off |
| Deezer | Fetch missing artist artwork and cache it locally | Off |

LRCLIB and Deezer can be enabled during first-run setup or later from `Settings > Music Management > Optional online services`. MusicBrainz searches run only when you choose the lookup action for a track.

## Requirements

Android 11 or newer (API 30+).

## Download

OpenPlayer is available only through GitHub releases:

https://github.com/raghavsatyadev/OpenPlayer/releases

Download the APK for your device from the release assets and sideload it: `arm64-v8a` fits most modern devices, `armeabi-v7a` is for older 32-bit ones.

Stable releases are tagged like `v1.2.3`; pre-releases tagged like `v1.2.3-alpha.N` carry newer, less tested changes. Both are signed with the same key, so you can move between them without reinstalling. The app's built-in updater (`Settings > Updates`) checks either channel.

Obtainium app id: `io.github.raghavsatyadev.openplayer` (enable "Include prereleases" to get alpha builds).

## Project Structure

```text
composeApp/          Shared code (KMP library; Android code in androidMain for now)
androidCMP/          Android app: manifest entry, signing, build types, baseline profiles
desktopCMP/          Desktop app (placeholder window)

composeApp/src/androidMain/kotlin/io/github/raghavsatyadev/openplayer/
- data/             Room 3, repositories, preferences, services, workers
- di/               Koin modules and qualifiers
- presentation/     Compose screens, components, navigation, ViewModels
- ui/               Theme and Glance widgets
- utils/            Shared utilities

baselineprofile/      Macrobenchmark and baseline profile generation
```

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose |
| Design | Material 3 |
| Playback | AndroidX Media3, ExoPlayer, FFmpeg |
| Database | Room |
| Dependency Injection | Hilt |
| Preferences | DataStore |
| Background Work | WorkManager |
| Networking | Retrofit, OkHttp |
| Images | Coil |
| Metadata | TagLib |

## License

OpenPlayer is licensed under the [GNU General Public License v3.0](LICENSE) (`SPDX-License-Identifier: GPL-3.0-or-later`).

```
OpenPlayer
Copyright (C) 2026 Theo Vilardo

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
```

Distributed APKs include third-party components under their own licenses. In particular, the optional FFmpeg decoder dependency `org.jellyfin.media3:media3-ffmpeg-decoder` is GPL-3.0; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Credits

OpenPlayer is a fork of [PixelPlayerOSS](https://github.com/PixelPlayerHQ/PixelPlayerOSS) by [lostf1sh](https://github.com/lostf1sh), originally created by Theo Vilardo, with the logo designed by [Aureal](https://github.com/NPSummers).
