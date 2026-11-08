# Android GUI for [WireGuard](https://www.wireguard.com/) (with SPA Support)

[![Build](https://github.com/ILYSjp/wireguard-android/actions/workflows/build.yml/badge.svg)](https://github.com/ILYSjp/wireguard-android/actions/workflows/build.yml)

This is a fork of official [wireguard-android](https://github.com/WireGuard/wireguard-android) enhanced with **Single Packet Authorization (SPA)** support designed for [spa-receiver](https://github.com/ILYSjp/spa-receiver).

---

## Single Packet Authorization (SPA) Support

### Overview

This fork integrates Single Packet Authorization directly into the Android WireGuard client. When enabled for a tunnel, the app sends an authenticated authorization UDP packet to the gateway endpoint before initiating the WireGuard handshake. This allows gateway firewalls (such as `nftables` via `spa-receiver`) to dynamically open the WireGuard UDP port only for authorized clients.

### Features

- **Automatic Pre-Up Hook**: Sends an HMAC-SHA256 authenticated UDP packet right before switching the tunnel state to `UP`.
- **GUI Configuration**: Configure SPA options per tunnel directly inside the tunnel editor interface.
- **Config & QR Code Import/Export**: Supports SPA settings embedded as comment annotations (`# SPA_*`) in `.conf` files and QR codes.

### Configuration Parameters

| Parameter | Configuration Key | Default | Description |
| :--- | :--- | :--- | :--- |
| **Enable SPA** | `# SPA_ENABLED` | `false` | Toggles SPA packet transmission prior to tunnel connection. |
| **SPA Port** | `# SPA_PORT` | `62201` | Destination UDP port on the gateway running `spa-receiver`. |
| **SPA Key** | `# SPA_KEY` | *(empty)* | Pre-shared key for HMAC-SHA256 authorization (Base64 string or UTF-8 text). |
| **Wait Delay (ms)** | `# SPA_WAIT_MS` | `100` | Delay after sending the SPA packet before initiating WireGuard handshake (allows firewall rules to take effect). |

### WireGuard Config File Example

When exporting or importing tunnels via `.conf` files or QR codes, the SPA settings are serialized under `[Interface]` as comments:

```ini
[Interface]
# SPA_ENABLED = true
# SPA_PORT = 62201
# SPA_KEY = base64_encoded_preshared_secret==
# SPA_WAIT_MS = 100
PrivateKey = aaaaaaaa...
Address = 10.0.0.2/32, fd00::2/128
DNS = 1.1.1.1

[Peer]
PublicKey = bbbbbbbb...
Endpoint = gw.example.com:51820
AllowedIPs = 0.0.0.0/0, ::/0
```

### Packet Format

The authorization packet sent to the target host (`Endpoint`) has a payload size of 56 bytes:

- **Timestamp (8 bytes)**: 64-bit big-endian epoch seconds (used for replay attack prevention and expiration check).
- **Nonce (16 bytes)**: Cryptographically secure random bytes (`SecureRandom`).
- **HMAC-SHA256 (32 bytes)**: HMAC computed over the first 24 bytes (Timestamp + Nonce) using the pre-shared SPA key.

---

**[Official wireguard-android upstream description below]**

**[Download from the Play Store](https://play.google.com/store/apps/details?id=com.wireguard.android)**

This is an Android GUI for [WireGuard](https://www.wireguard.com/). It [opportunistically uses the kernel implementation](https://git.zx2c4.com/android_kernel_wireguard/about/), and falls back to using the non-root [userspace implementation](https://git.zx2c4.com/wireguard-go/about/).

## Building

```bash
git clone --recurse-submodules https://git.zx2c4.com/wireguard-android
cd wireguard-android
./gradlew assembleRelease
```

macOS users may need [flock(1)](https://github.com/discoteq/flock).

## Embedding

The tunnel library is [on Maven Central](https://search.maven.org/artifact/com.wireguard.android/tunnel), alongside [extensive class library documentation](https://javadoc.io/doc/com.wireguard.android/tunnel).

```groovy
implementation 'com.wireguard.android:tunnel:$wireguardTunnelVersion'
```

The library makes use of Java 8 features, so be sure to support those in your gradle configuration with [desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring):

```groovy
compileOptions {
    sourceCompatibility JavaVersion.VERSION_17
    targetCompatibility JavaVersion.VERSION_17
    coreLibraryDesugaringEnabled = true
}
dependencies {
    coreLibraryDesugaring "com.android.tools:desugar_jdk_libs:2.0.3"
}
```

## Translating

Please help us translate the app into several languages on [our translation platform](https://crowdin.com/project/WireGuard).
