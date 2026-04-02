# Screen Cast Hub - Smart Display Management System

**简体中文** | [English](./README.zh.md)

> A multi-screen content management system designed for enterprise showrooms, retail displays, and digital signage scenarios

🌐 **Official Website:** [https://www.hdcase.tech/](https://www.hdcase.tech/) — SaaS Service Available

## 💡 Self-hosted vs Cloud Service

| Option | Description |
|--------|-------------|
| **Self-hosted** | Free & open source (MIT), full control over your data and infrastructure |
| **SaaS Cloud** | No server management required — get started instantly at [hdcase.tech](https://www.hdcase.tech/) |

**Don't want to manage servers?** Try our [cloud service](https://www.hdcase.tech/) for a hassle-free experience.

## Overview

A lightweight device management solution built around the principle of **"One Screen, One Code - Scan to Manage"**.

### Key Features

- 📱 **QR Code Binding** - TV displays a QR code; scan from the admin panel to instantly bind the device
- 📤 **Remote Publishing** - Push images, videos, PPTs, PDFs, and other media assets remotely
- 🔄 **Auto-Transcoding** - PPT files automatically convert to PDF for seamless TV playback (Coming Soon)
- 💾 **Local Playback** - Content downloads to device storage; no continuous phone connection required
- 📊 **Multi-Screen Management** - Centralized control of multiple devices with group organization

### Why Not Traditional Casting?

| Comparison | This Solution | Traditional Casting (AirPlay/DLNA) |
|------------|---------------|-----------------------------------|
| Phone Dependency | Works offline after content delivery | Requires constant connection |
| Multi-Device Mgmt | Centralized dashboard | Point-to-point, hard to manage |
| Document Support | PPT auto-converts to PDF | Poor or no support |
| Network Requirements | Only needed during publishing | Required continuously |

---

## Screenshots

### Device Management
<img src="./assets/deviceinfo.png" alt="Device Info" width="600" />

### Asset Management
<img src="./assets/material.png" alt="Material Management" width="600" />

### Content Publishing
<img src="./assets/upload.png" alt="Content Upload" width="600" />

### Sorting Feature
<img src="./assets/sort.png" alt="Sorting" width="600" />

### System Settings
<img src="./assets/setting.png" alt="System Settings" width="600" />

### TV
<img src="./assets/tv_bind.png" alt="绑定码" width="600" />
<img src="./assets/tv_switch.png" alt="播放" width="600" />

---

## Project Structure

```
screen-cast-hub/
├── backend/           # Backend Service (Spring Boot Monolith)
├── android-tv/        # Android TV Client (Java)
├── web-admin/         # PC Admin Dashboard (Vue 3 + Ant Design Vue)
└── README.md
```

---

## Tech Stack

### Backend Service (`backend/`)

| Technology | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.2 | Core framework |
| Spring Security | - | Authentication & authorization |
| JWT | - | Token-based auth |
| MyBatis Plus | - | ORM framework |
| MySQL | 8.0 | Primary database |
| Redis | 7.0 | Caching layer |
| MinIO | - | Object storage |
| EMQX | - | MQTT messaging |
| Gotenberg | - | PPT to PDF conversion |

**Package Structure:**
```
com.opencast.screencast/
├── config/           # Configuration classes
├── constant/         # Constants
├── controller/       # REST controllers
├── dto/              # Data Transfer Objects
├── entity/           # Entity classes
├── enums/            # Enumerations
├── exception/        # Exception handling
├── mapper/           # MyBatis mappers
├── result/           # Response wrappers
├── service/          # Business logic
└── util/             # Utility classes
```

### PC Admin Dashboard (`web-admin/`)

| Technology | Version | Purpose |
|------------|---------|---------|
| Vue | 3.4 | Frontend framework |
| Ant Design Vue | 4.2 | UI component library |
| Pinia | 2.1 | State management |
| Vue Router | 4.2 | Routing |
| Vite | 5.0 | Build tooling |
| TypeScript | 5.6 | Type safety |
| Axios | 1.6 | HTTP client |

**Feature Modules:**
- Authentication & login
- Device management (bind, rename, unbind)
- Asset management (upload, preview, delete)
- Content publishing (create tasks, view history)
- Dashboard (statistics overview)

**Development Port:** `5173`

### Android TV Client (`android-tv/`)

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 8+ | Primary language |
| ExoPlayer | - | Video playback |
| AndroidPdfViewer | - | PDF rendering |
| Glide | - | Image loading |
| Retrofit | - | Network requests |
| Room | - | Local database |

**Feature Modules:**
- Device binding (QR code display)
- Content reception (MQTT)
- Local playback (image slideshow, video, PDF)
- Heartbeat reporting

---

## Quick Start

### 1. Start the Backend

```bash
cd backend

# Configure database
# Edit src/main/resources/application.yml

# Run
mvn spring-boot:run
```

### 2. Start the Admin Dashboard

```bash
cd web-admin
npm install
npm run dev
```

Access at: http://localhost:5173

### 3. Build Android TV App

```bash
cd android-tv
./gradlew assembleDebug
```

---

## Roadmap

### Upcoming Features

| Feature | Status | Description |
|---------|--------|-------------|
| PPT to PDF | 📋 Planned | Integrate Gotenberg service for automatic PPT-to-PDF conversion |

---

## Contact

📧 Email: liang.qfzc@gmail.com

WeChat:

<img src="./assets/wechat-contact.jpg" alt="WeChat Contact" width="200" />

---

## License

MIT
