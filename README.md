# BeatFlow

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Material Design 3](https://img.shields.io/badge/Design-Material_3-4285F4?style=flat-square&logo=materialdesign&logoColor=white)](https://m3.material.io)
[![AndroidX Media3](https://img.shields.io/badge/Media-AndroidX_Media3-FF6F00?style=flat-square&logo=google&logoColor=white)](https://developer.android.com/media/media3)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

Premium Offline Local Music Player for Android.

## Overview

BeatFlow is a modern offline music player designed with a privacy-first approach. It focuses on delivering high-performance audio playback, absolute user privacy, and a fluid, modern user interface. Crafted using Jetpack Compose, the application adopts Material Design 3 guidelines integrated with sophisticated glassmorphism aesthetics, ensuring a polished and modern visual experience.

Key focus areas include:

- Performance: Built for speed, instant indexation, and a highly efficient memory footprint.
- Privacy: All operations run locally on the device with zero network calls for playback.
- Modern UI: Fully customized interfaces adhering to modern Material Design 3 standards.
- Smooth Playback: Driven by AndroidX Media3 for robust background audio and uninterrupted sessions.
- Offline Experience: Designed entirely around playing files stored directly on internal or external storage.
- Material Design 3: Dynamic colors, clean layout systems, and modern components.
- Glassmorphism: Smooth blur effects, translucent surfaces, and depth-based UI hierarchy.

## Official Website

For more information, downloads, and documentation, visit the official website:

[BeatFlow Official Website](https://beatflowmusic.vercel.app/)

Through the official website, users can:

- Explore BeatFlow and its unique user interface.
- Learn about core system capabilities and audio engine specifications.
- Read the comprehensive Privacy Policy.
- Review the official Terms of Use.
- Download the latest pre-compiled application packages (APK).
- Visit the public GitHub repository.

## Features

BeatFlow is equipped with a rich set of features tailored for offline local music listening:

- Offline First: Operates fully offline without requiring an active internet connection.
- Local Music Playback: Scans and organizes local audio files.
- AndroidX Media3: Utilizes the latest Media3 ExoPlayer engine for reliable audio decoding.
- Material Design 3: Full compliance with M3 guidelines, including dynamic accent color synchronization.
- Glassmorphism UI: Clean blur states and polished container styles.
- AMOLED Dark Mode: Deep-black themes optimized to save battery on OLED/AMOLED screens.
- Dynamic Theme: Leverages system-wide dynamic colors for personalized styling.
- Smart Music Scan: Intelligent scanning of file structures to identify valid music files.
- Optional Ignore Files Under 1 Minute: Excludes short notifications and voice memos.
- Optional Ignore Files Under 100 KB: Filters out low-quality sound effects and visual assets.
- Album Art Support: Extracts and displays high-quality embedded metadata.
- Default Artwork: Stylish fallback artwork for songs missing embedded assets.
- Background Playback: Seamless execution while the screen is off or in the background.
- Media Notification Controls: Interactive playback system with customized control layouts.
- Lock Screen Controls: Seamless integration with system-wide lock screen media widgets.
- Bluetooth Controls: Supports standard media key listeners for remote play, pause, next, and previous triggers.
- Sleep Timer: Configurable countdown timers to gradually fade out volume and pause audio.
- Favorites: Fast-access marking for highly-rated tracks.
- Playlist Management: Easily create, update, and manage customized music playlists.
- Queue Management: Add to queue, reorder active playlists, and clear sessions instantly.
- Search: Responsive keyword scanning across title, artist, and album fields.
- Folder Browsing: Direct filesystem navigation to locate music directories.
- Albums: Automatic cataloging by album meta tags.
- Artists: Structured indexing based on contributing artists.
- Recently Played: Automatic tracking of historical playback sessions.
- Music Statistics: Informative visual insights showcasing total listening time and play frequencies.
- User Profile: Customizable user profiles including name entries and avatars.
- Profile Picture: Local profile photo support for UI personalization.
- Privacy First: No user registration, no telemetry, and absolute local storage boundaries.
- No Ads: Clean interface with absolutely zero advertisements.
- No Tracking: No monitoring of user interactions, listening habits, or device details.
- No Analytics: Completely free of third-party analytic SDKs or monitoring tools.

## Supported Audio Formats

BeatFlow leverages standard platform-level decoders to support a wide array of formats:

| Format | File Extension | Description |
| :--- | :--- | :--- |
| MP3 | `.mp3` | MPEG Layer-3 Audio |
| AAC | `.aac` | Advanced Audio Coding |
| M4A | `.m4a` | MPEG-4 Audio Container |
| FLAC | `.flac` | Free Lossless Audio Codec |
| WAV | `.wav` | Waveform Audio File Format |
| OGG | `.ogg` | Ogg Vorbis Audio Container |
| OPUS | `.opus` | Opus Interactive Audio Codec |
| AMR | `.amr` | Adaptive Multi-Rate Audio |
| 3GP | `.3gp` | 3GPP Multimedia Container |

*Note: Availability of certain formats may depend on individual device codec configurations and hardware capabilities.*

## Tech Stack

The application is written in accordance with official Android development guidelines:

| Component | Technology | Description |
| :--- | :--- | :--- |
| Programming Language | Kotlin | Modern, expressive, and type-safe language. |
| Media Engine | AndroidX Media3 (ExoPlayer) | High-performance playback engine and media session binder. |
| UI Framework | Jetpack Compose | Modern declarative UI toolkit for native design structures. |
| Architecture Pattern | MVVM | Clean Model-View-ViewModel separating presentation from logic. |
| Local Database | Room Database | SQLite wrapper for robust structural local data caching. |
| Preferences Storage | Jetpack DataStore | Modern, asynchronous key-value data storage framework. |
| Image Loading | Coil | Kotlin-first, lightweight, and asynchronous image loader. |
| Concurrency | Kotlin Coroutines & Flow | Asynchronous programming pipelines and state flows. |
| Background Operations | MediaSessionService | Standardized system service for reliable background media sessions. |

## Project Structure

```
BeatFlow/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── data/             # Database, Entities, Repositories, Preferences
│   │   │   │   ├── playback/         # Media3 MusicService, PlaybackManager
│   │   │   │   ├── ui/               # ViewModels, Screens, Theme, Components
│   │   │   │   └── MainActivity.kt   # Entry Point Activity
│   │   │   └── AndroidManifest.xml   # Manifest definitions & permissions
│   │   └── build.gradle.kts          # App-level dependencies and SDK settings
│   └── BeatFlow.apk                  # Pre-compiled application package
├── gradle/                           # Gradle configuration & version catalog
├── build.gradle.kts                  # Project-level build configuration
└── settings.gradle.kts               # Multi-module settings
```

## Getting Started

To compile or run the project locally, follow these steps:

1. Clone the repository to your local system:
   ```bash
   git clone https://github.com/monxcode/BeatFlow.git
   ```
2. Open Android Studio and select **File > Open**, then choose the cloned root directory.
3. Allow Android Studio to sync the Gradle configuration files.
4. Ensure you have an Android device connected via USB debugging or a configured virtual device.
5. Click **Run** in Android Studio to build and install the application on your device.

## APK

For quick installations, a pre-compiled, production-ready APK package is available within the repository:

`app/BeatFlow.apk`

## Source Code

You can access and explore the full project repository here:

[BeatFlow Source Code](https://github.com/monxcode/BeatFlow)

## Privacy

BeatFlow is built with a strict privacy-first philosophy:

- Music never leaves the device: Playback and audio file scanning are handled 100% locally.
- No login required: Access all features immediately without setting up accounts or providing emails.
- No account required: Your personal data, play histories, and favorites remain local.
- No advertisements: Enjoy a clean visual interface completely free from disruptive network ads.
- No analytics: We do not integrate tracking toolsets, keeping your app interactions confidential.
- No tracking: No telemetry collection, background pings, or device identity fingerprinting.
- Local playback only: Built exclusively to play files hosted on your physical storage.

## Roadmap

Future development milestones and enhancements planned for BeatFlow:

- Android Auto: Complete optimization for standard vehicular entertainment screens.
- Wear OS Support: Dedicated companion application for modern Android smartwatches.
- Equalizer: Integrated sound frequency adjustments and custom audio presets.
- Lyrics Support: Support for embedded and synchronized lyric file viewing.
- Backup & Restore: Offline local backup structures for custom playlists and statistics.
- Cloud Playlist Sync (Optional): Secure, end-to-end encrypted synchronization.
- Widgets: Visual desktop home screen control panels.

## Contributing

Contributions to improve BeatFlow are highly welcome:

1. Fork the repository.
2. Create a new branch dedicated to your changes:
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. Commit your modifications with clear, descriptive notes:
   ```bash
   git commit -m "Add amazing feature description"
   ```
4. Push your branch to the remote fork:
   ```bash
   git push origin feature/amazing-feature
   ```
5. Open a Pull Request on the main branch of the parent repository.

## License

This project is licensed under the MIT License.

```
MIT License

Copyright (c) 2026 Mohan Singh Parmar

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Developer

**Mohan Singh Parmar**  
Android Developer • Cybersecurity & AI Enthusiast  

GitHub: [@monxcode](https://github.com/monxcode)

---

Developed by Mohan Singh Parmar | [BeatFlow](https://github.com/monxcode/BeatFlow) | [Official Website](https://beatflowmusic.vercel.app/) | [GitHub Repository](https://github.com/monxcode/BeatFlow)
