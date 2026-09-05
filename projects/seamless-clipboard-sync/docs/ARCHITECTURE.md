# Architecture

## Product target

The product should feel persistent after the first pairing: whenever trusted devices return to the same reachable LAN, they discover each other and restore the secure session automatically.

## Components

### Shared protocol

- Stable device ID derived from a long-term public key.
- mDNS/DNS-SD service: `_seamclip._tcp.local`.
- Secure session negotiation after discovery.
- Clipboard message envelope with origin, sequence, hash and timestamp.
- Deduplication and loop prevention.
- Heartbeat and reconnect state machine.

### Android

- Kotlin app.
- Foreground/background connectivity service within Android platform limits.
- Android Keystore for long-term private key.
- NSD/mDNS discovery.
- Normal mode plus optional InputMethodService enhancement.
- Local database for trusted devices and last-seen state.

### Linux

- Lightweight user daemon + optional tray UI.
- Avahi/mDNS advertisement and discovery.
- Wayland via wl-clipboard; X11 via event-driven clipboard watcher.
- systemd --user startup.
- Local secret storage (Secret Service/keyring where available; protected fallback config).

## Connection state machine

```text
OFFLINE
  ↓ network available
DISCOVERING
  ↓ trusted peer found
AUTHENTICATING
  ↓ mutual authentication succeeds
CONNECTED
  ↓ transport fails / sleep / Wi-Fi switch
RECONNECTING
  ↘ exponential backoff + mDNS rediscovery
  ↘ trusted peer found → AUTHENTICATING → CONNECTED
```

The UI should not ask the user to reconnect manually after a previously trusted peer disappears and returns.

## Pairing model

1. Both devices generate a long-term Ed25519 identity key pair on first launch.
2. Discovery announces only non-sensitive metadata and a short public-key fingerprint.
3. For an untrusted peer, both devices display a human-readable confirmation once.
4. On approval, each side stores the peer public key as trusted.
5. Future sessions authenticate against the stored key; IP address and port may change freely.

## Reliability rules

- TCP/WebSocket/QUIC transport may disconnect; this is expected.
- Heartbeat detects dead sessions.
- Reconnect attempts use bounded exponential backoff with jitter.
- A network-change event triggers immediate rediscovery instead of waiting for the old address.
- Suspend/resume triggers rediscovery.
- The last known endpoint is only a fast-path hint, never the identity anchor.

## Clipboard loop prevention

Each clipboard item carries:

- `itemId`
- `originDeviceId`
- `sequence`
- `sha256`
- `createdAt`

A receiver records recent IDs/hashes. Content written to the local clipboard by the sync engine is marked in local state so the local watcher does not send it straight back to the origin.

## Security boundary

- Never trust a device solely because it is on the same Wi-Fi.
- Never identify a peer solely by IP/MAC address.
- Never use plaintext HTTP for clipboard payloads.
- Long-term secrets remain in OS-backed secure storage when available.
- Pairing can be revoked locally at any time.

## Future platforms

The transport/protocol layer remains platform-neutral. iOS, macOS and Windows clients implement the same discovery, trust and message envelope while using platform-native clipboard APIs and background capabilities.