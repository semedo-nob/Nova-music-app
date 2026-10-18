# Nova Music

Nova Music is an Android player for the songs on your device, plus online search, an equalizer, and a Gemini assistant that can find tracks and build playlists from a mood.

The app addresses you by the name you set. A new profile is only a photo and a preferred name. There is no demo catalog: the library is what a scan finds on the device, plus songs you play from search.

## Playing music

- Scan from Settings, or allow storage access from Home.
- The app opens on a short splash. Tap it to skip.
- Swipe left or right to move between Home, Library, AI DJ, and Settings. The dots above the bottom bar are the same pages, and the tabs jump to a page directly.
- Tap a song to play it. The bar at the bottom is the current song.
- Swipe that bar left or right to stop playback and dismiss it.
- Open the full player and swipe the top bar down, or tap the chevron, to return to the app.
- The equalizer bands are vertical faders. Drag a band up or down; the change is applied as you move it.
- Crossfade, in Settings, fades the next song in when the current one ends.

## Library

Long-press a song, or tap the check button, to select one or more songs.

- **Share** sends the audio file for songs stored on the device. Streams share the title and artist. The share button on the full player does the same for the current song.
- **Queue** adds the selection to the current queue. If nothing is playing, the selection starts playing.
- **Playlist** adds the selection to a playlist you already made, or creates a new playlist and adds the songs. Playlists can also be created from the Playlists tab.
- **Remove** appears inside a playlist you created and takes the songs out of that playlist only.
- **Delete** removes the songs. On-device files are deleted only after the system confirmation. Songs that are not files on the device are removed from the library.

## DJ mixer

Open the mixer from the waveform button in the Library header. Load a song on deck A and deck B, then slide the crossfader from A toward B. Each deck has its own gain, play/pause, and cue (back to the start). Deck B does not replace the notification for the main player. Closing the mixer stops deck B and restores deck A to full volume.

## Permissions

| Permission | Why |
| --- | --- |
| Read audio | Scan and play songs stored on the device |
| Write storage (Android 10 and older) | Delete songs stored on the device |
| Microphone | Identify a song playing nearby |
| Internet | Search, previews, and the Gemini assistant |
| Notifications | Playback controls while the app is in the background |
| Foreground media playback | Keep audio playing with a media notification |

## Gemini

Copy `.env.example` to `.env` and set `GEMINI_API_KEY`. The file stays on your machine and is not committed. Without a key, local playback still works; the assistant does not.

Firebase is optional. `google-services.json` is not in this repository. The build warns and continues when that file is missing. Add it only if you enable Firebase features.

## Release build

Requirements: JDK 17 and Android SDK 36. This project expects the SDK at `ANDROID_HOME`.

The release APK is signed with an upload keystore (`alias` `upload`). Pass the store and key passwords when you build. Do not commit the keystore.

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export STORE_PASSWORD="your-store-password"
export KEY_PASSWORD="your-key-password"
./gradlew :app:assembleRelease
```

The signed APK is written to `app/build/outputs/apk/release/app-release.apk`.

`versionCode` is 1 and `versionName` is `1.0`. Raise both before each Play upload.

## Google Play

Before the first production release:

- Create an app in Play Console and upload the signed APK or an Android App Bundle (`./gradlew :app:bundleRelease`).
- Add a privacy policy. The app reads on-device audio, uses the microphone for recognition, and sends assistant prompts to Gemini when that feature is used.
- Declare the microphone, storage, and foreground-service permissions in the Play data-safety and permission forms.
- Complete content rating, target audience, and the media/foreground-service declarations.

The upload keystore password used for local signing must be kept with the keystore. Losing it means you cannot update the same Play listing with that key.
