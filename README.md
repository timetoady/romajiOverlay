# RomajiOverlay

RomajiOverlay is a modern, lightweight Android Accessibility Service designed to detect Japanese text (Kanji, Hiragana, Katakana) inside **Google Messages** and dynamically render translucent Romaji pronunciation overlays.

It helps Japanese language learners read incoming and outgoing messages fluently without leaving the chat interface.

---

## 🚀 Key Features

* **Targeted Translation**: Automatically scans, captures, and maps text elements inside Google Messages (`com.google.android.apps.messaging`).
* **Offline NLP**: Powered by the **Kuromoji IPADIC** Japanese analyzer to perform tokenization locally on your device with zero network calls and clean memory usage.
* **Hepburn Transliteration Engine**: Custom Katakana-to-Romaji transliterator supporting Modified Hepburn rules (e.g. sokuon `っ`/`ッ` double consonants, macrons for long vowels `ā`/`ū`/`ē`/`ō`/`ī`, and syllabic `n'` boundaries).
* **Grammar-Aware Spacing**: Systematically separates Japanese particles (`は` $\rightarrow$ `wa`, `を` $\rightarrow$ `o`, `へ` $\rightarrow$ `e`, `に` $\rightarrow$ `ni`) and counters while leaving prefixes, suffixes, and helper auxiliary verbs naturally attached.
* **Zero-Permission Drawing**: Uses the secure system window type `TYPE_ACCESSIBILITY_OVERLAY`. You **do not** need to grant the dangerous "Display over other apps" (`SYSTEM_ALERT_WINDOW`) permission.
* **Adaptive Theming**: Detects system Dark/Light mode in real-time and applies high-contrast, rounded translucent cards.
* **Dual Rendering Modes**: 
  - **Furigana-Style** (Default): Places small Romaji tags directly above or below message bubble bounds.
  - **Overlay-Style**: Completely covers the native Japanese text with a translucent card drawing the centered Romaji equivalent.

---

## 📦 Download & Installation

The compiled package is available as an early alpha APK:

1. Download the latest APK asset from our GitHub Releases page:
   [**Download RomajiOverlay v0.1.0-alpha APK**](https://github.com/timetoady/romajiOverlay/releases/tag/v0.1.0-alpha)
2. Tap the downloaded `.apk` file on your Android device to install it. (If prompted, allow installation from unknown sources).

---

## 🛠️ How to Activate & Use

Android Accessibility Services must be manually enabled in your system settings before they can process screen layouts:

### Step 1: Grant Accessibility Permission
1. Open your Android device **Settings**.
2. Navigate to **Accessibility**.
3. Select **Installed Services** (or *Installed Apps* / *Downloaded Apps* depending on your OS version).
4. Tap **RomajiOverlay**.
5. Switch the toggle **Use RomajiOverlay** to **ON**.
6. Tap **Allow** on the system permission request dialog.

> [!IMPORTANT]
> **Android 13+ "Restricted Setting" Bypass:**
> Since this APK is sideloaded (downloaded outside the Google Play Store), Android will initially block the toggle switch and show a warning stating **"Restricted setting"**.
>
> To unlock it:
> 1. Open your device's main **Settings** and go to **Apps** (or **See all apps**).
> 2. Search for and select **RomajiOverlay**.
> 3. Tap the **three-dot menu icon** in the top-right corner of the *App Info* page.
> 4. Select **Allow restricted settings** and authenticate (PIN, pattern, or fingerprint).
> 5. Return to **Settings > Accessibility > RomajiOverlay**; the switch will now be clickable.

### Step 2: Configure Render Settings (Optional)
1. Open the **RomajiOverlay** app from your home screen launcher.
2. Under **Overlay Render Mode**, select either:
   - **Furigana-Style** (small tag above text bubble)
   - **Overlay-Style** (covers and replaces the native Japanese block)
3. The accessibility service will dynamically refresh and apply this configuration in real-time without needing a reboot!

### Step 3: Test inside Google Messages
1. Open **Google Messages**.
2. Open any conversation.
3. Type or view a message containing Japanese characters (e.g., `こんにちは`, `東京に行きます`, or `お酒を飲みます`).
4. The translation overlay will instantly align itself with the text bubble bounds!

---

## 🔧 Developer Notes & Compilation

To build the APK locally from source:

1. Clone this repository:
   ```bash
   git clone https://github.com/timetoady/romajiOverlay.git
   cd romajiOverlay
   ```
2. Compile the debug build using the Gradle wrapper:
   ```bash
   # Windows PowerShell
   .\gradlew.bat assembleDebug

   # macOS / Linux
   ./gradlew assembleDebug
   ```
3. Locate the generated APK at:
   `app/build/outputs/apk/debug/app-debug.apk`

To run local unit tests for Hepburn grammar rules and particle spacing:
```bash
# Windows
.\gradlew.bat test

# macOS / Linux
./gradlew test
```
