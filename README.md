# Nova Music

Nova Music is an Android player for the songs on your device, plus online search, an equalizer, and a Gemini assistant that can find tracks and build playlists from a mood.

The app addresses you by the name you set. A new profile is only a photo and a preferred name. There is no demo catalog: the library is what a scan finds on the device, plus songs you play from search.

## Playing music

- Scan from Settings, or allow storage access from Home.
- Tap a song to play it. The bar at the bottom is the current song.
- Swipe that bar left or right to stop playback and dismiss it.
- Open the full player and swipe the top bar down, or tap the chevron, to return to the app.
- Share sends the current title and artist through the system share sheet.
- The equalizer bands are vertical faders. Drag a band up or down; the change is applied as you move it.

## Permissions

| Permission | Why |
| --- | --- |
| Read audio | Scan and play songs stored on the device |
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
