# Android client

Planned stack: Kotlin + Android NSD/mDNS + Android Keystore + foreground connectivity service.

V1 responsibilities:

- discover trusted Linux peers automatically on the current LAN;
- remember the peer after first approval;
- reconnect automatically after Wi-Fi changes, sleep or process restart;
- receive clipboard text from Linux;
- provide compatible Android→Linux sending paths;
- optionally expose a lightweight InputMethodService enhancement for a more seamless Android→Linux experience.

No Root, no ADB and no manual IP/URL entry are part of the default design.