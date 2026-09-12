# Mixora

Mixora is a modern, feature-packed music streaming and playback application for Android, built with Jetpack Compose, Material 3, and Media3 ExoPlayer.

---

## ✨ Features

- **Online Streaming**: Stream millions of tracks via YouTube Music (InnerTube integration).
- **Synchronized Lyrics**: Real-time synced and plain lyrics support powered by LRCLIB, Kugou, and more.
- **Modern Material 3 UI**: Dynamic color theming, sleek animations, and responsive player sheets.
- **Audio Effects & Processing**: Audio normalization, gapless playback, silence skipping, and custom equalizer support.
- **Offline & Cache Support**: Cache your favorite songs and download tracks for offline listening.
- **Together Mode**: Listen to music simultaneously with friends over the local network (LAN Discovery).
- **Flavors**: Available in **FOSS** (fully open-source) and **GMS** (Google Cast support) variants.

---

## 🛠️ Building the Project

### Prerequisites
- JDK 21 (Temurin or Microsoft OpenJDK recommended)
- Android SDK (API 34+)

### Build Commands
```bash
# Build Debug APKs
./gradlew :app:assembleFossDebug
./gradlew :app:assembleGmsDebug

# Build Release APKs
./gradlew :app:assembleFossRelease
./gradlew :app:assembleGmsRelease
```

---

## 🚀 Release Workflow (Automated via GitHub Actions)

Releases are fully automated via GitHub Actions using git tags. Whenever a new version tag is pushed to the repository, the **Build & Publish Release** workflow is triggered automatically.

### How to Publish a New Release

1. **Update Version**: Update `versionCode` and `versionName` in `app/build.gradle.kts`:
   ```kotlin
   versionCode = 6
   versionName = "1.5.1"
   ```

2. **Commit Changes**:
   ```bash
   git add app/build.gradle.kts
   git commit -m "chore: bump version to 1.5.1"
   git push origin main
   ```

3. **Create and Push Tag**:
   Create a git tag starting with `v` matching the new version and push it to GitHub:
   ```bash
   git tag v1.5.1
   git push origin v1.5.1
   ```

### What Happens Automatically
Once the tag is pushed, the GitHub Actions workflow (`.github/workflows/release.yml`) will:
- Check out the codebase at the tagged commit.
- Set up JDK 21 and configure the Gradle build environment.
- Decode the signing keystore (if `KEYSTORE_BASE64` secret is provided).
- Build both **GMS** and **FOSS** release APKs.
- Rename output APKs cleanly (e.g. `Mixora-v1.5.1-gms.apk`, `Mixora-v1.5.1-foss.apk`).
- Generate `checksums-sha256.txt` for integrity verification.
- Publish a new GitHub Release with auto-generated release notes and attach all APK artifacts.

> **Note**: You can also trigger the workflow manually anytime from the **Actions** tab in GitHub by choosing **Build & Publish Release** > **Run workflow**.

---

## 📄 License
This project is open-source and licensed under the GPL-3.0 License.
