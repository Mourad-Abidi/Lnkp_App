
## 📱 LNKP App

A **FOSS & lightweight Android messaging application** built for learning, experimentation, and real-time communication features.

LNKP App is designed with a single core vision:

> 🔐 True privacy-first messaging with end-to-end encryption (E2E) principles

No ads. No tracking. No data harvesting. Just communication.

---

## 🚀 Latest Version

### 📦 Download APK
👉 [Download Latest Release](https://github.com/Mourad-Abidi/Lnkp_App/releases/latest)

---

## 📸 Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/1a6c8bad-d456-4088-a694-4b9c1697cddf" width="250"/>
  <img src="https://github.com/user-attachments/assets/d118ec7e-39ea-497f-a440-2f269a62117e" width="250"/>
  <img src="https://github.com/user-attachments/assets/3109cc84-c800-4262-b5b2-69e1439206b3" width="250"/>
</p>

---

## 🧠 About This Release System

This link always points to the newest available version of the app.

- 🔄 Automatically updates when a new release is published
- 📌 No need to manually change links in the README
- ⚡ Always redirects to the latest stable version

> This ensures users always download the most recent build without confusion.

---

## 🚀 Latest Release Notes — V1.2 Beta

### 💬 Messaging System Improvements
- Full message sync on first login (auto-fetch old conversations)
- Fixed missing chats until manual navigation trigger (search tab → chats)
- Fixed offline message send queue (messages now persist and send after reconnect)
- Implemented message retry delivery after reconnection
- Stabilized real-time listener in chat system

---

### 🔔 Notifications & Counters
- Fixed unread message counter not resetting after opening chat
- Fixed unread counter duplication loop (prevent infinite increment bug)
- Improved unread message accuracy across sessions
- Added pull-to-refresh (swipe down) to fetch latest messages

---

### 🔐 Security & Encryption UX
- Fixed encrypted/raw message preview showing cipher text in chat list
- Improved message rendering consistency for encrypted content

---

### 👤 Presence & Status System
- Added user presence system (online / offline tracking)
- Added last seen timestamp logic

---

### 📩 Message Status System
- SENT → single grey tick
- DELIVERED → double grey tick
- READ → double blue tick
- Moved real-time status tracking to ConversationHeader only

---

### 🗑️ Chat Management
- Added long-press chat actions
- Delete chat from Supabase + UI sync cleanup

---

### ⚙️ Backend Fixes
- Fixed Supabase query filter parsing error (PGRST100 id.eq issue)
- Fixed duplicate PATCH requests causing state overwrite loops
- Improved real-time sync stability

---

## ⚠️ Notes

- This is an **early beta release**
- Some features may still be unstable or experimental
- Always perform a **clean install** (do NOT overwrite older versions)
- Older builds may conflict with updated backend logic
