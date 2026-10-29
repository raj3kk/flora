# Flora-wide core migration audit — B5 (surface-wise checklist)

Plan source: FLORA_BUILDER_PLAN.md — FLORA-WIDE ARCHITECTURE section
(migration B4 ke baad, B5 ke saath; har surface ki migration audit
B5 gate me darj hogi) + B5 section (tools consolidation).

Niyam: koi surface chupchaap planner me nahi ghusi, aur koi surface
bina wajah ke bahar nahi rakhi gayi. Neeche har surface ki sthiti +
wajah hai. "OUT" ka matlab "kabhi nahi" nahi — matlab B5 me
migration nahi hui, wajah ke saath, aur planner tak ka raasta
(adapter) phir bhi maujood hai jahan likha hai.

## IN (planner-native, B5 tak)

- Builder (BuilderScreen + ProjectStore + PlannerTools): IN — plan
  steps se project save/read, repo push, deploy sab planner se
  chalte hain; B5 me entitlement gate (B5-08) + quota gate (B5-06)
  + tool audit (B5-07) isi raaste par lage hain.
- Planner (PlannerScreen): IN — plan banao/dekho/chalao/amend sab
  PlannerEngine se (B4).
- Doctor (DoctorScreen): IN (planner-linked) — plan<->project link
  + pre-deploy gate PlannerTools VERCEL_DEPLOY branch me (B4-DOCTOR).
- Chat Plan route (IntentRouter Plan): IN — sirf explicit plan
  phrases ("plan banao / plan bana kar / plan karke karo") par
  Plan route par jata hai (B4). Baaki chat BrainChat hi rahega —
  har baat ko plan banana conflict hai, isliye router B5 me
  badla NAHI gaya (BrainChat hijack nahi).

## OUT (B5 me migration nahi — wajah ke saath)

- App-open (IntentRouter OpenApp, "<naam> kholo"): OUT — app-open
  accessibility service + VerifiedAppLauncher se chalta hai, apne
  confirm flows ke saath. Har app-open ko planner me daalna
  confirm semantics badal deta aur service-binding rework
  maangta. Planner tak ka raasta: MACRO_UI_STEP adapter (B4) —
  user Routines confirm se chalata hai, planner khud
  accessibility drive nahi karta.
- Browser (OpenBrowser + BrowserScreen + BrowserController
  singleton + form fill): OUT — B1 ke read/fill/classify raaste
  aur FormFiller guards safety-critical hain (password field
  kabhi type nahi; login-like -> stop #3, submit-like ->
  stop #2). Planner migration ke liye guard-preserving execution
  path chahiye, jo B5 ka scope nahi hai. Planner tak ka raasta:
  macro adapter hi. B5 me BrowserController ko registry me
  descriptor-only rakha gaya hai (naya controller nahi, doosra
  execution path nahi) — duplicate raasta mana hai.
- Voice commands (aam voice, wake/mic/TTS pipeline): PARTIAL —
  voice -> chat -> Plan route explicit plan phrases par kaam
  karta hai (upar IN). Aam voice commands OUT — voice pipeline
  (wake, barge-in, mic service, TTS) ke apne guards/confirm
  flows hain; planner migration interrupt/confirm semantics
  badal deti. Planner tak ka raasta: voice text chat me aakar
  Plan route par ja sakta hai (explicit phrase par).
- Settings / baaki surfaces: OUT — inhe planner ki zaroorat
  nahi; Settings recovery path hai (kabhi flag/gate se band
  nahi hoti). Direct raasta hi sahi hai.

## Guards barkaraar (B5 ke baad bhi)

- FormFiller guards: chhue NAHI gaye (FormFiller files is push
  me nahi hain) — password field kabhi type nahi; login-like
  -> stop #3, submit-like -> stop #2.
- Clipboard-key rule: chhua NAHI gaya — prompt pehle, phir
  capture, phir auto-clear; koi naya silent raasta nahi bana.
- BuilderTokens / KeyStore storage: chhua NAHI gaya.
- Flora-own-code silent edit + deploy: kabhi nahi — self-map
  update != code self-edit (map sirf read/use, naya map naye
  version ke saath publish route se).
- Server / web / supabase / functions: chhue NAHI gaye (F2
  server hissa server-team flag note me hai, audit log me).

## Phase 10 me prove hoga (is cycle me daava nahi)

- Har IN surface ka E2E phone test (plan -> deploy, builder
  gate message, quota gate message).
- OUT surfaces ka regression: app-open / browser / voice ke
  purane flows B5 ke baad bhi waise hi chalte hain (koi
  planner hijack nahi).
