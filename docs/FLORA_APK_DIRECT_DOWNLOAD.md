# Flora direct APK download — website se, latest, auto-update (2026-10-28)

**Maqsad:** Flow99 ki tarah — Flora ka APK seedha website se
download ho, har naya PASS build khud-b-khud latest link par aa
jaye. Koi expiry nahi, koi 250-download limit nahi.

## Design (final)

- **Storage:** Supabase Storage public bucket `flora-apk`
  (flora-models bucket jaisa hi pattern — public read).
- **Stable link (pehli upload ke baad live):**
  `https://keomyweghrmhqglaxmyv.supabase.co/storage/v1/object/public/flora-apk/flora-latest.apk`
- **Auto-update:** GitHub Actions (`.github/workflows/android.yml`,
  step "Upload direct APK to Supabase Storage") har main-push ke
  PASS build ka APK usi naam se upsert karta hai — page ka link
  kabhi badalna nahi padega.
- **Page flip:** pehli direct upload dikhte hi `web/download.html`
  ka primary button SwissTransfer se stable link par badal diya
  jayega (ek chhota web push).

## One-time setup (user/dashboard kaam — sirf ek baar)

1. **Bucket banao** — Supabase dashboard > Storage > Create bucket:
   naam `flora-apk`, Public ON. Ya SQL editor me:
   ```sql
   insert into storage.buckets (id, name, public)
   values ('flora-apk', 'flora-apk', true);
   ```
2. **Public read policy** (bucket public hone par bhi zaroori):
   ```sql
   create policy "flora-apk public read" on storage.objects
     for select using (bucket_id = 'flora-apk');
   ```
   (Write sirf service_role se hota hai — koi public-write
   policy nahi banani.)
3. **GitHub secrets** — repo Settings > Secrets and variables >
   Actions: `SUPABASE_URL` =
   `https://keomyweghrmhqglaxmyv.supabase.co`,
   `SUPABASE_SERVICE_ROLE_KEY` = dashboard se (service_role key
   kabhi repo/chat me nahi).
4. **Schema note (approve option ii ke liye):** agar
   `licences.device_id` NOT NULL ho to website-path licence ke
   liye ek baar:
   ```sql
   alter table public.licences alter column device_id drop not null;
   ```
   Ye tabhi chalana jab admin-approve (ii) deploy karte waqt
   NULL insert fail ho — pehle check karo, blind alter nahi.

## Deploy checklist (edge functions, dashboard editor se)

- `admin-approve` (option ii website-path) + `licence-verify`
  (NULL-device claim) — dono repo me taiyaar hain; GitHub push
  se deploy NAHI hota, dashboard code editor se paste + deploy
  karna hai, phir ek website-only payer se test karna hai.
- `tts-proxy` 404 abhi bhi khula hai (repo me hai, deploy nahi)
  — S3 deploy ke saath hi deploy hoga.

## Limits (imaandari se)

- Jab tak bucket + secrets nahi hain, CI step chupchaap skip
  hoga (continue-on-error) — build gate kabhi is par fail nahi
  hoga, par direct link tab tak live nahi hoga.
- APK ~111 MB ka hai — Storage se download par koi expiry ya
  count limit nahi hai, par pehli baar upload me waqt lagega.
- Debug-signed APK hi rahega (release signing key nahi hai) —
  install par "unknown sources" permission abhi bhi chahiye hogi.
