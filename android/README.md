# NFC Wallet — Android

Native Android app built with **Kotlin + Jetpack Compose**. Mirrors the iOS (SwiftUI) version exactly — same backend, same key-split scheme.

---

## Requirements

| Tool | Version | Download |
|------|---------|----------|
| Android Studio | Hedgehog (2023.1.1) or newer | https://developer.android.com/studio |
| JDK | 17 (bundled with Android Studio) | — |
| Android SDK | API 34 | via Android Studio SDK Manager |
| Physical device OR emulator | Android 8.0+ (API 26+) | — |

> **NFC note:** NFC only works on a real physical device. Emulators do not support NFC hardware.

---

## 1. Clone the repo

```bash
git clone https://github.com/rythmn1111/nfc-wallet.git
cd nfc-wallet
```

---

## 2. Open in Android Studio

1. Open **Android Studio**
2. Click **File → Open**
3. Navigate to `nfc-wallet/android/` and click **Open**
4. Wait for Gradle to sync — first sync downloads ~300 MB of dependencies (web3j is large), takes 2–5 minutes

If Gradle sync fails with a JDK error:
- Go to **File → Project Structure → SDK Location**
- Set **Gradle JDK** to the JDK bundled with Android Studio (`/Applications/Android Studio.app/Contents/jbr`)

---

## 3. Run on an emulator

1. In Android Studio open **Device Manager** (right toolbar or `View → Tool Windows → Device Manager`)
2. Click **Create Device**
3. Choose any **Pixel** model → click **Next**
4. Select a system image with **API 26 or higher** (API 34 recommended) → Download if needed → Next → Finish
5. Press the green **Run ▶** button (or `Shift+F10`)
6. Select your emulator from the list

> Emulators work for everything **except NFC**. You can test login, wallet creation (without NFC write step), balances, and QR codes.

---

## 4. Run on a physical Android device

### Enable Developer Mode on your phone
1. Open **Settings → About Phone**
2. Tap **Build Number** 7 times until "You are now a developer" appears
3. Go to **Settings → Developer Options**
4. Enable **USB Debugging**

### Connect and run
1. Connect your phone via USB cable
2. Accept the "Allow USB Debugging" dialog on the phone
3. In Android Studio, your device appears in the device dropdown (top toolbar)
4. Press **Run ▶**

The app installs and launches automatically.

---

## 5. NFC setup on the device

The app uses NFC to write/read private key halves to/from NFC cards.

- Make sure **NFC is enabled** on your device: `Settings → Connections → NFC → ON`
- When writing or reading, hold the NFC card flat against the **back of the phone** near the NFC antenna (usually center or top-back)
- Keep the card still until the operation completes (1–2 seconds)

### Compatible NFC cards
Any NDEF-compatible NFC card works:
- NTAG213 / NTAG215 / NTAG216 (most common, widely available)
- Mifare Ultralight
- Any card with at least 144 bytes of NDEF capacity

---

## 6. App flow

```
Register / Login
       ↓
Create Wallet
  ├─ Generates BIP39 12-word mnemonic
  ├─ Derives ETH key  (BIP44: m/44'/60'/0'/0/0)
  ├─ Derives SOL key  (SLIP-0010: m/44'/501'/0'/0')
  ├─ Encrypts keys with your password (AES-256-GCM + PBKDF2)
  ├─ XOR-splits ciphertext → ½ to server, ½ to NFC card
  └─ Writes ETH key half → NFC card, then SOL key half → NFC card
       ↓
Home (Bottom Nav)
  ├─ Balance  — live ETH + SOL balances
  ├─ Receive  — QR codes for your addresses
  └─ Pay      — tap NFC card → enter password → send transaction
```

---

## 7. Making a payment

1. Go to the **Pay** tab
2. Enter the recipient address and amount
3. Tap **Tap NFC Card to Authorize**
4. Hold your NFC card to the phone — the app reads the key half
5. Enter your wallet password
6. Tap **Send Transaction** — the app reconstructs your private key and broadcasts the transaction

---

## 8. Project structure

```
android/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/nfcwallet/app/
│           ├── MainActivity.kt          ← single activity, NFC dispatch
│           ├── AppViewModel.kt          ← app state, NFC tag events
│           ├── types/Types.kt           ← data classes
│           ├── services/
│           │   ├── CryptoService.kt     ← AES-GCM, PBKDF2, XOR split
│           │   ├── WalletService.kt     ← BIP39, BIP32, SLIP-0010
│           │   ├── NFCService.kt        ← NDEF read/write
│           │   └── NetworkService.kt   ← HTTP calls to backend
│           └── ui/
│               ├── theme/Theme.kt
│               └── screens/
│                   ├── OnboardingScreen.kt
│                   ├── CreateWalletScreen.kt
│                   ├── BalanceScreen.kt
│                   ├── ReceiveScreen.kt
│                   └── SendScreen.kt
├── build.gradle
└── settings.gradle
```

---

## 9. Key dependencies

| Library | Purpose |
|---------|---------|
| `org.web3j:core:4.8.7-android` | BIP39 mnemonic, BIP32 HD derivation, ETH signing |
| `net.i2p.crypto:eddsa:0.3.0` | Ed25519 keypair for Solana |
| `javax.crypto` (built-in) | AES-256-GCM encryption, PBKDF2WithHmacSHA256 |
| `android.nfc` (built-in) | NFC NDEF read/write |
| `com.squareup.okhttp3:okhttp:4.12.0` | HTTP networking |
| `com.journeyapps:zxing-android-embedded:4.3.0` | QR code generation |
| Jetpack Compose BOM 2024.02.00 | UI framework |

---

## 10. Backend

The backend is already running on a VPS — no setup needed.

- **URL:** `http://62.146.173.162:3004`
- **Stack:** Node.js + Express + PostgreSQL
- **Managed by:** pm2 (process id 18, name: `nfc-wallet`)

To check backend status:
```bash
ssh root@62.146.173.162 "pm2 logs nfc-wallet --lines 50"
```

---

## 11. Troubleshooting

**Gradle sync fails: "Could not resolve org.web3j:core:4.8.7-android"**
- Check internet connection
- Go to **File → Invalidate Caches → Invalidate and Restart**

**Build error: "Duplicate class" or "Multiple dex files"**
- This is a Bouncy Castle conflict. Add to `app/build.gradle`:
  ```gradle
  configurations.all {
      exclude group: 'org.bouncycastle', module: 'bcprov-jdk15to18'
  }
  ```

**App crashes on launch: "NFC not supported"**
- NFC is optional (`required="false"` in manifest) — the app still runs, NFC features just won't work on non-NFC devices

**NFC write fails: "Tag lost"**
- Keep the card completely still against the phone
- Try a different spot on the back of the phone (antenna position varies)
- Make sure the NFC card has enough storage (NTAG213 = 144 bytes, NTAG216 = 888 bytes — either works)

**"Failed to connect to 62.146.173.162:3004"**
- Check you have mobile data or WiFi
- The VPS allows cleartext HTTP (`usesCleartextTraffic="true"` is set in the manifest)
