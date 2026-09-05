# OpenRepo Store v1

A cross-platform discovery layer for installable GitHub Release assets. This implementation is independent and does not copy GitHub Store/RepoStore UI or code.

## v1 Android product scope

- Android app only shows public repositories whose latest published GitHub Release contains at least one `.apk` asset.
- Primary install action points directly to the selected GitHub Release asset download URL.
- Secondary fallback action opens the release page when direct download cannot be completed or the asset requires manual inspection.
- Source ZIP/TAR archives are never treated as apps.
- Discovery ranking uses repository metadata and Android-oriented signals; every candidate is verified against its latest published release before display.

## Cross-platform asset model

The shared catalog model supports:

- Android: `.apk`
- macOS: `.dmg`, `.pkg`
- Windows: `.exe`, `.msi`
- Linux: `.deb`, `.rpm`, `.AppImage`

The Android client filters this shared model to Android assets only. Future macOS/Windows/Linux clients reuse the same catalog and asset resolver.

## Install modes

### Direct mode (default)
1. Resolve `/repos/{owner}/{repo}/releases/latest`.
2. Select a non-prerelease, non-draft APK asset.
3. Download using the asset's GitHub `browser_download_url`.
4. Save to app-managed Downloads/cache using Android DownloadManager or a streamed HTTP client.
5. Hand the local APK URI to Android's package installer through FileProvider.
6. Never request silent-install/device-owner privileges.

### Release fallback mode
- Open the canonical GitHub release page in the browser.
- Always available from the app detail screen.
- Used when the direct asset is unavailable, download fails, user wants to inspect upstream notes, or GitHub changes asset behavior.

## Stability rules

- HTTP timeouts and retry with bounded exponential backoff.
- Local cache for discovery metadata and last-known good release data.
- Graceful GitHub API rate-limit handling; optional OAuth can be added later.
- Do not scrape GitHub HTML for release assets; use the public GitHub API and documented release metadata.
- Verify Content-Length when available and reject HTML/error responses masquerading as APK files.
- Support cancellation and resumable/retry downloads where platform APIs allow.
- Never automatically execute/install after download without an explicit user action.

## Risk and trust UI

Every detail/install view must show:

- `Source: GitHub Release` and upstream owner/repository.
- License if GitHub exposes one; otherwise `License not detected`.
- Release tag and publication time.
- Asset filename and byte size.
- Hash/signature status: `Verified`, `Not provided by publisher`, or `Could not verify`.
- Clear warning that being hosted on GitHub does **not** mean GitHub or OpenRepo Store has reviewed the binary.
- Recommendation to inspect permissions and publisher reputation before installation.
- No "safe" badge based only on stars, forks, or GitHub hosting.

## Brand/legal boundaries

- Product brand is **OpenRepo Store** (working name), not `GitHub App Store`.
- GitHub is identified only as the upstream hosting/source service.
- Do not imply GitHub sponsorship, certification, endorsement, or security review.
- Do not mirror or re-upload third-party APK/DMG installers by default; downloads point to original publisher release assets.
- Respect each repository's license, trademarks, screenshots, and redistribution terms.

## Independent UX direction

Home: Featured / Trending Android / Recently Updated / New Android Apps.

Card: app/repo name, short description, latest tag, update date, stars, APK size, publisher.

Details: app header, install button, risk panel, release notes, screenshots/README summary where allowed, developer/repo metadata, `View source`, `Open release page`.

Downloads: queued/downloading/downloaded/failed with retry.

Settings: network policy, cache, risk confirmations, optional GitHub sign-in later.

## Next implementation slices

1. Android Kotlin + Jetpack Compose shell.
2. GitHub search/release API client and APK-only filter.
3. Direct downloader + FileProvider package-installer handoff.
4. Store-style home/details/download screens.
5. Risk panel and source/license metadata.
6. GitHub Actions signed debug/release APK build.
7. Shared platform resolver for future DMG/PKG/EXE/MSI/DEB/RPM/AppImage clients.
