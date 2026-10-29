# B5-02 CI step — PENDING WEB-UI ITEM (API se edit FORBIDDEN)

Token me `workflows` scope NAHI hai, isliye
`.github/workflows/android.yml` ko API se kabhi mat chhuo
(createCommitOnBranch FORBIDDEN, trees API 404, contents API
404 — teeno ek hi wajah se fail hote hain). Ye step sirf
GitHub web UI se Edit + paste + Commit se jud sakta hai (ya
workflows-scope token se).

## Pending step (exact)

- Workflow file: `.github/workflows/android.yml`
- Job: `build`
- Position: `Build debug APK` step ke BAAD (upload-artifact se
  pehle ya baad — koi farq nahi, build ke baad hona chahiye).
- Step name: `Self-map drift check`
- Step description (web UI me yehi daalna hai):
  - Host-side generator chalao jo SelfMapGenerator ke rules ka
    port hai: repo docs + code index se map JSON banao,
    SelfMapSafety denylist lagao (koi secret/token/key value
    nahi, koi token-shape line nahi, koi path-with-secret
    nahi — sirf pattern-level + structural info).
  - Bane hue JSON ke sanitized entry sets ko shipped asset
    `app/src/main/assets/flora_self_map.json` ke sanitized
    entry sets se milao (semantic compare — key order /
    formatting se farq nahi padna chahiye).
  - Drift mile (koi screen/feature/service/data-flow/
    permission/dependency judi, hati ya badli, bina asset
    update ke) to step FAIL ho — asset ke bina code aage
    nahi badhega.
- Fail hone par kya karna hai: map asset ko naye code ke
  hisaab se dobara generate karke (SelfMapGenerator.initialMap
  + asset dono ek saath) commit karo, phir CI dobara chalao.

## B5-02 ki sthiti is pending step ke saath

- Generator code: repo me hai
  (`app/src/main/java/com/flora/app/planner/SelfMap.kt` —
  SelfMapGenerator + SelfMapSafety).
- Initial shipped map asset: repo me hai
  (`app/src/main/assets/flora_self_map.json`) — device par
  read/use abhi se kaam karta hai, CI step ke bina bhi.
- CI step: PENDING (web-UI) — isliye B5-02 ko "code-complete,
  CI-step pending (web-UI)" mana jayega. "CI step live" ka
  jhootha daava kabhi nahi karna hai.
