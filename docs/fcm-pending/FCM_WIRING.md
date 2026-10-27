# FCM wiring — pending on the user's Firebase project

Status: code drafted only (FloraMessagingService.kt in this folder).
Nothing in this folder is compiled or shipped in Flora v0.1.

Steps still needed (all need the user's Firebase console, not the agent):
1. Firebase console me Flora Android app banao (package com.flora.app).
2. google-services.json download karke app/ me rakho.
3. Firebase server key backend sender ke liye surakshit rakho (vault).
4. Tabhi ye wiring karo: firebase-messaging dependency + manifest me
   FloraMessagingService entry + token sync code ko FloraPreferences se jodo.
5. Phir naya Actions build + phone par live push test (notification aaye
   tabhi FCM done mana jayega — server-side claim se nahi).

Rules: push se kabhi silent action nahi hoga; notification sirf dikhega.
Token kabhi log/file/backup/message me nahi jayega.
