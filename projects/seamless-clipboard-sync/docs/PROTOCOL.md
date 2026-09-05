# Seamless Clipboard Protocol v0

## Discovery

Service type:

```text
_seamclip._tcp.local
```

Advertised TXT fields should stay minimal:

```text
v=0
id=<short device id>
name=<display name>
fp=<public-key fingerprint prefix>
cap=text
```

Discovery information is not trusted authentication data. It only helps locate a peer.

## Identity

Each installation owns a long-term Ed25519 key pair.

```text
deviceId = SHA-256(publicKey)
```

The private key never leaves the device.

## First pairing

For an unknown peer:

1. Discover endpoint via mDNS.
2. Open an encrypted provisional channel.
3. Exchange full identity public keys.
4. Derive and show a short authentication string on both devices.
5. User approves once.
6. Store peer public key in trusted-peers database.

Thereafter no IP/URL/port/code entry is required.

## Reconnection

For a trusted peer:

1. Network becomes available.
2. Query `_seamclip._tcp.local`.
3. Match discovered peer by stored identity fingerprint.
4. Connect to advertised current endpoint.
5. Perform mutual proof of possession of trusted identity keys.
6. Restore clipboard session.

A changed DHCP address does not require re-pairing.

## Clipboard envelope

```json
{
  "protocolVersion": 0,
  "itemId": "uuid-v7",
  "originDeviceId": "sha256:...",
  "sequence": 42,
  "createdAt": 1788580800000,
  "kind": "text",
  "mimeTypes": ["text/plain"],
  "sha256": "...",
  "payload": "hello"
}
```

## ACK

```json
{
  "type": "ack",
  "itemId": "uuid-v7",
  "receivedAt": 1788580800100
}
```

## Heartbeat

Connected peers exchange ping/pong periodically. Missing heartbeats move the peer into reconnecting state. Reconnection is automatic and never clears the trust relationship.

## Conflict policy

For V1 text synchronization:

- accept new local clipboard events;
- deduplicate by `itemId` and payload hash;
- do not echo content back to its origin;
- when two devices copy nearly simultaneously, retain both in local history but expose the latest accepted item as active clipboard content.

## Payload limits

V1 should initially support plain text only. Images/files/HTML can be introduced after reliability is proven. This keeps the first implementation small and makes Android/Linux behavior easier to verify.

## Remote sync extension

A future relay mode may route ciphertext when devices are not on the same LAN. The relay must not become an identity provider and must not possess plaintext clipboard content. LAN P2P remains the default.