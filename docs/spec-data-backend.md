# Spec — Data + Backend (Room/DataStore + naya Supabase backend)

Source: behaviour only — negative finding (app ki apni tables Room me
NAHI mili; sirf system tables mile), user-confirmed naya design, master
plan, supabase-schema.sql (EXISTS). Folder: data/ (ABHI KHAALI HAI).

## Files (honi chahiye)
- data/MayaDatabase.kt — Room database (memories, chat).
- data/entity/MemoryEntity.kt, ChatEntity.kt — local memories + chat;
  edit/delete kaam kare; Incognito on ho to kuch save nahi, purani
  memory use nahi.
- data/MayaPreferences.kt — DataStore keys: wake word on/off, theme,
  orb size, persona, STT/TTS choice, battery saver, echo guard, media
  mode, typing mode (anuman list — final sirf behaviour se).
- data/SupabaseClient.kt — naya backend client; service key KABHI app
  me nahi, sirf server par.
- data/LicenceRepository.kt — licence check HAR protected kaam par
  server se (client-only check patchable hai, isliye server hi faisla
  karega); free mode 10 min/day usage server par count.
- data/BackupRepository.kt — encrypted backup/restore (Yaadein + chats),
  sirf user ke kehne par, sirf uske account se.
- rebuilt/supabase-schema.sql (EXISTS) — users, settings, plans, usage,
  licences (server-authoritative: plan, status active/revoked/expired,
  device_id, activated_at, expires_at; unique user+device); RLS on;
  seed plans website figure se (app ₹699 one-time, pro_max ₹800 one-time)
  — video ke ₹99/month se discrepancy abhi bhi note me hai, payment
  phase par final confirmation chahiye, build ko rokta nahi.

## Backend + licence server (app ke bahar, master plan section 1B/1C)
- Website login → plan → payment (test mode pehle, live ke liye alag
  approval) → server licence generate kare → app me key daalo → server
  verify → device bind → deactivate / device change (max 1 / 15 din) →
  revoke / ban (contact route se).
- Control panel: users, licences, plans, usage, devices, versions,
  feature flags, notices, rules. Update system: version check → user
  approval se download/install (INSTALL_PACKAGES flow, YM0 evidence se
  mila self-update pattern — code dobara likhna hai, copy nahi).

## Acceptance (Verify → Test → Audit)
1. Local + server sync test: delete ek jagah, doosri jagah bhi hatna
   chahiye jahan design kahe.
2. Koi crash / hidden exception nahi; licence bypass test fail hona
   chahiye (client-only tamper se kuch unlock nahi).
3. Incognito / delete kaam kare.
4. Koi purani key, service key, ya secret code/file me nahi hai.
5. Test log docs me darj hai.
