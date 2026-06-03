# 🚀 LNKP V1.3 Beta

---

## ✨ Changelog

### 🔐 Authentication & Security
1. 🔐 Added secure email signup flow with Supabase Auth verification (OTP / email confirmation)
2. 🔐 Fixed login before email verification completion
3. 🔐 Implemented user lifecycle flow (PENDING → VERIFIED → ACTIVE)
4. 🔐 Improved signup stability and authentication consistency
5. 🔐 Fixed OTP/email confirmation mismatch in Supabase configuration
6. 🔐 Improved email verification resend reliability

---

### 👤 User System
7. 👤 Added user account status system (ACTIVE / PENDING / DISABLED)
8. 👤 Implemented real-time last seen tracking
9. 👤 Improved online/offline presence stability
10. 👤 Fixed profile creation timing (now only after verification)
11. 👤 Prevented duplicate user creation during incomplete signup
12. 👤 Fixed username uniqueness conflict during signup

---

### 💬 Messaging System
13. 💬 Improved message sync on login and reconnect
14. 💬 Enhanced realtime chat update reliability
15. 💬 Fixed race conditions in authentication & profile pipeline
16. 💬 Improved messaging error handling and stability

---

### 📸 Media System
17. 📸 Implemented secure media upload system (images + files fully working)
18. 📸 Fixed Supabase Storage signed URL loading in Glide
19. 📸 Resolved 404 bucket/signing issues for image retrieval

---

### ⚙️ Performance & Stability
20. ⚙️ Reduced main-thread load in authentication & chat flows
21. ⚙️ Improved UI responsiveness and overall stability
22. ⚙️ Fixed multiple edge-case crashes in authentication system

---

## 📸 Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/675093ff-3f62-4b53-a07a-1b1122608d6e" width="250"/>
  <img src="https://github.com/user-attachments/assets/27976fa6-f0bb-45ba-b441-ba542546e5a2" width="250"/>
  <img src="https://github.com/user-attachments/assets/7d623529-aadb-40f4-be40-24a0058c821e" width="250"/>
  <img src="https://github.com/user-attachments/assets/c7926f12-fcab-45b0-9c30-49187da4b15f" width="250"/>
</p>

---

## ⚠️ Notes

- Some features marked * are still in progress  
- Email verification is now **required before login**  
- Profile creation happens only after verification  
- Media system uses secure signed URLs  
- Clean install required (do not update over previous builds)  

---
