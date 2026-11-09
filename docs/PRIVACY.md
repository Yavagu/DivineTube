# Privacy and player limitations

YourTube stores category names, parent-provided titles, video IDs, playlist snapshots, ordering, and the protected PIN verifier in app-private local storage. Android backup is disabled. Clearing app data deletes the library and PIN; no cloud recovery or telemetry exists. The app requests only internet and network-state permissions.

The PIN uses a random 16-byte salt and PBKDF2-HMAC-SHA256 (210,000 iterations). The verifier is encrypted with an Android Keystore AES-GCM key; plaintext PINs are not persisted. Attempts are committed before verification. Five failures start exponential lockout from 30 seconds up to one hour. Parent sessions live only in memory, are not restored after process death, and close on activity pause or two minutes of inactivity. Screenshots and recents previews are protected by FLAG_SECURE. A PIN is not an OS/device security boundary.

Network disclosures:

- Thumbnails contact `i.ytimg.com` for approved video IDs; no thumbnail files are persisted by the app.
- Parent searches, playlist snapshots, and playback eligibility checks contact the official `www.googleapis.com/youtube/v3` service. Google receives requested terms/IDs, API key, app package/certificate identity, and network metadata such as IP address.
- The player loads official YouTube JavaScript and privacy-enhanced `youtube-nocookie.com` embeds. YouTube may contact other Google/video/ad infrastructure, process device/network information, and set data when playback occurs. Privacy-enhanced mode is **not zero tracking** and does not remove child-directed-client obligations.
- No YouTube login, app analytics, third-party player, download, stream extraction, or advertising SDK is integrated. YouTube itself controls player advertising.

The app does not cover or rewrite YouTube branding, controls, overlays, or ads. Supported parameters disable autoplay and fullscreen; `rel=0` limits related results to the same channel, **not the approval list**. The app declines arbitrary top-level WebView navigation and automatic external activity launches. YouTube-controlled frame content can still expose unapproved recommendations or other content; neither this restriction nor saved playlist snapshots make the embedded player a complete allowlist. OS controls can leave YourTube.

The native UI links to [YouTube Terms](https://www.youtube.com/t/terms) and [Google Privacy](https://policies.google.com/privacy); first PIN setup requires acknowledgement. This repository-local notice is a development disclosure. Before distribution, the operator must provide an accessible published privacy policy/contact point and complete relevant child privacy/legal review and YouTube self-designation. No such external registration or publication was performed in this session.

Never label this build as guaranteed child-safe. Parent review concerns video suitability, whereas YouTube's Made for Kids designation concerns platform treatment and policy; they are different checks.
