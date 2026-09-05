# Linux client

Planned stack: lightweight user daemon with mDNS discovery and platform-native clipboard adapters.

V1 responsibilities:

- advertise `_seamclip._tcp.local`;
- discover paired Android devices automatically;
- authenticate by stored device public key rather than IP address;
- reconnect after suspend/resume and network changes;
- monitor/write Wayland clipboard through `wl-clipboard` where supported;
- monitor/write X11 clipboard through an event-driven watcher;
- start automatically through a user-level service;
- keep UI optional and minimal.

The intended user experience is that, after the first pairing, normal use requires no reconnect button, no address field and no repeated pairing code.