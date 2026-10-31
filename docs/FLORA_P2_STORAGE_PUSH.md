# Flora Part 2 — Storage Policy + Push/FCM (P2-08 / P2-09, 2F)

> Status: CODE-SIDE VERIFY + RULING OPTIONS ONLY. Koi policy dashboard se
> nahi lagi hai, koi bucket nahi bana hai, koi live push test nahi chala
> hai. ZERO-policies ka live claim dashboard se verify hoga
> (parent-delegation) — yahan uska daava nahi hai.

## P2-08 — Storage policy (code-side check + CORRECTION)

### Tip tree se upload paths

- Tip tree me upload se juda ek hi app file path dikha: backup ka
  DriveClient (Google Drive backup route).
- **CORRECTION (plan ke shorthand par):** "sirf DriveClient" poori tasveer
  nahi hai. Tip par `WebsitePublisher` bhi maujood hai (local se match) —
  wo flora-sites bucket par POST se HTML upload karta hai (anon key ke
  saath, FloraApp se wired hai, planner ToolRegistry me bhi darj hai).
  Uske apne comment me likha hai: bucket public-read + upload policy
  dashboard se lagti hai.
- Baaki storage istemaal sirf public READ hai: flora-models manifest,
  flora-sites public link, update check — upload nahi.
- Natija: P2-08 ruling me flora-sites bucket bhi shamil hona chahiye;
  sirf flora-apk bucket par ruling kafi nahi hai.

### Ruling options (D-14 ke liye)

- Vikalp (a) Public-by-design document karo: jo buckets public-read hain
  (models, sites, apk download), unko public-by-design likhkar darj karo;
  upload sirf authenticated/service-role se ho.
- Vikalp (b) Scoped policy: har bucket par seemit policy — public read
  sirf zaroori buckets par, upload sirf auth/service-role se, anonymous
  write kisi bucket par nahi.
- Hard rule (dono vikalp me): ANONYMOUS WRITE KABHI NAHI.
- Backup scope bhi D-14 me hai: plans backup me hon ya nahi (ZP-G5).
- Policy sirf tab lagegi jab upload feature aaye / ruling ho — ruling ke
  bina dashboard par policy mat lagao.
- Parent-delegation: dashboard se ZERO-policies claim verify karo + jo
  policies maujood hain unki list lo (generic agent se nahi hoga).

## P2-09 — Push/FCM lifecycle (code-side verify)

- Send-push tip-landed confirm hai (2A ledger me bhi darj): 401 gate +
  `{ ok: true, message_id }` shape.
- FCM config presence only: `app/google-services.json` tip par maujood
  hai aur usme app package match karta hai. Koi value is doc/report me
  print NAHI ki gayi hai.
- Live E2E + panel test (ZP-F13) phone/parent-delegation me hain:
  token refresh / logout / reinstall / multi-device tests + panel se push
  (message_id mile). Live deploy state disputed hai (P2-01 re-verify me
  cover) — isliye live E2E ka daava yahan nahi hai.
- Token lifecycle note: push-token-sync function tip par maujood hai;
  token-sync fail ka Catcher hook Part 3 ka hissa hai (yahan overlap
  nahi — sirf note hai).
