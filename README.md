# Flora — Android voice assistant (clean-room rebuild)

Flora ek Android voice assistant app hai (package `com.flora.app`), jo user ki awaaz se kaam karti hai: baat-cheet, planner ke zariye verified-step execution, tools, routines, macros, aur server-side licence/usage/push.

> Ye README current state batata hai. Purana "Maya skeleton" wala README isse replace hua hai (Roadmap Phase 1). Isme koi secret/key/password/token ki value nahi hai.

## Current state (ek nazar me)

- **Version:** versionName `2.22.0-rebuild`, versionCode `45` (app/build.gradle.kts se)
- **Stack:** Kotlin 1.9.24, AGP 8.5.2, Compose, compileSdk/targetSdk 34, minSdk 26
- **Brain:** app → providers direct PRIMARY (Gemini → Groq → provider lanes → BasicTalk); server brain-proxy sirf FALLBACK/OPTIONAL hai, kabhi primary nahi.
- **Voice:** har jagah Sample 2, Lily (system-TTS profile); Kokoro neural voice Gate 6 par HELD hai.
- **Licence:** Pro ₹199/month FINAL; Free = 600 talk sec/day + saare tools; UTR exactly 12-digit numeric; payment auto-confirm kabhi nahi hota (UPI + UTR + admin approve).
- **Signing:** purani signing key LOST hai — isliye Flora hamesha sideload-only hai, Play Store update kabhi nahi hoga. Release unsigned hi banega jab tak signing migration par user decision na ho.

## Repo map

```
raj3kk/flora (PRIVATE, branch sirf main)
├── app/            Android app (Kotlin + Compose)
├── supabase/
│   ├── functions/  Edge Functions (TypeScript/Deno) — har deployed function ki committed copy
│   └── sql/        6 canonical SQL files (deploy order ke liye baseline doc dekho)
├── web/            Vercel website (HTML) + updates/version.json (update channel)
├── docs/           Specs, plans, audits, phone test plans
└── .github/workflows/android.yml   CI: JDK 17 + Gradle 8.7 + assembleDebug + APK upload
```

**Note on legacy naming:** `settings.gradle.kts` me `rootProject.name` ab `"flora"` hai (pehle `"maya-rebuild"` tha; verify karke badla gaya hai). App ka naam, package aur product Flora hi hai.

## Build (APK kahan banta hai)

APK **GitHub Actions ke server par** banta hai (Supabase par nahi; website Vercel par hai). CI steps: checkout → JDK 17 → Gradle 8.7 → assembleDebug → APK ko GitHub Release asset ke roop me upload → unit tests → drift check.

- Flora par commit message me kabhi `[skip ci]` mat lagao — isse build hi nahi hoga.
- Local build + static verify ke liye Android SDK aur JDK 17 chahiye; VM par emulator nahi hai, isliye APK run/test VM par nahi ho sakta.

## Deploy (functions + database)

- **Management API route Flora ke liye dead hai** — Edge Function deploys sirf Supabase dashboard (browser) se hote hain, GitHub push se nahi.
- **Niyam:** dashboard-only code kabhi deploy mat karo jo GitHub par committed na ho. Har deployed function ki committed copy tip par honi chahiye (isiliye `send-push` ko tip par commit kiya gaya — pehle wo sirf local working copy me tha).
- **SQL deploy order:** licence_system.sql → admin_panel_tables.sql → admin_panel_phase2.sql → home_sync_table.sql → core_tables.sql → usage_atomic_increment.sql. `supabase/schema.sql` SUPERSEDED hai — is par "do not run" likha hai, kabhi mat chalao.
- **Live schema note:** `public.usage.day` live me DATE type hai (file me TEXT) — isliye usage RPC me `p_day::date` cast hai; is cast ko hatana nahi hai.

## Server (Supabase)

- Project: Flora, region ap-south-1, Free plan.
- Functions (tip par 23, send-push commit ke baad): licence-verify, usage-count, push-token-sync, send-push, stt-proxy, tts-proxy, brain-proxy, home-sync, admin-* (approve, list, licences, plan-features, keys, payments, settings, flags, instructions, overview), feature-flags, public-settings, heartbeat, licence-key, gmail-proxy (501 DRAFT — Gmail OAuth decision pending, kabhi deploy nahi hoga jab tak decision na ho).
- Database: 15 canonical tables (users, licences, usage, push_tokens, payment_submissions, admin_audit, admin_settings, feature_flags, global_instructions, heartbeats, home_boxes, plan_features, plans, provider_keys, push_log).
- Design: RLS ON + zero policies (service-role-only); live RLS verify dashboard se baaki hai.
- Update channel: `web/updates/version.json` (vCode 45; apkUrl + sha256 khaali = channel pending). Root `/version.json` ka 404 alag path hai.

## Website (Vercel)

- Project `flora`, stable domain `https://flora-henna-alpha.vercel.app` — git-connected auto-deploy.
- Pages: index, signup, login, account, pay, pricing, admin, download, privacy, terms, reset-password, models/manifest.json, updates/version.json. Koi API route nahi hai.

## Product rules (freeze)

- Free: 600 talk sec/day, saare tools. Pro: ₹199/mahina, unlimited talk, sab tools, FCM push.
- Plan features ka single source `plan_features` table hai (licence-verify usi se features[] deta hai); app me PlanFeatures fail-open hai (list kabhi mili hi nahi to sab ON).
- Device: 1 phone, max 1 change / 15 din, admin-only (lost-phone exception abhi decision pending hai).
- Retention: 30 din (chat memory exempt — long-lasting).
- Gmail: disabled (501 draft) jab tak OAuth scopes/consent/token security/revocation complete na ho.
- FCM project: flora-f22a7, package exactly `com.flora.app`; token value kabhi log nahi hoti.

## Recovery (Maya wala haal dobara na ho)

Poora source-code audit: `docs/` me roadmap baseline (`FLORA_ROADMAP_BASELINE.md`) aur user ke paas di gayi audit file (`Flora-Full-Source-Audit-2026-10-31.md`) dekho. Flora ko waisa ka waisa dobara banane ka tareeqa: repo clone → build → SQL order se database → dashboard se functions deploy → secrets (dashboard se) → Vercel site → storage → phone test + admin unblock.

## Excluded (kabhi implement nahi honge bina alag approval ke)

Payment auto-confirm, silent auto-answer/reply, SIM-call TTS injection, WhatsApp call API, hidden accessibility actions, self-granting privileged permissions, Termux bridge, web password autofill/typing, creator voice cloning, purane creator credentials/assets ka reuse, creator code/UI/assets/trademark ki copy (clean-room PERMANENT law).
