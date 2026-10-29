-- Flora licence + features management system (LFMS) — dashboard SQL step.
-- Run order: licence_system.sql PEHLE, phir functions deploy karo
-- (licence-verify v2, brain-proxy v5, tts-proxy v3, admin-licences,
-- admin-plan-features, admin-approve minimal edit). SQL ke bina naye
-- functions column-missing error denge (plan_features ke alawa — wo
-- fail-open defaults par chalega, isliye uska missing hona koi
-- function nahi todega).
--
-- DESIGN (user order 2026-10-29 ~09:57 IST + correction):
-- - licences row ka "plan" = plan_id column (pehle se hai: plans.id
--   ko refer karta hai — 'free' / 'pro'). "bound_device_id" =
--   device_id column (pehle se hai). Koi naya plan/device column
--   nahi banaya gaya — naam ka farq sirf docs me saaf kiya hai.
-- - licence_key ADDITIVE hai, replacement nahi: maujooda keyless
--   rows (payment-approved, website-path NULL-claim) bina key ke
--   waise hi chalti rahengi (backward compatible). Nayi key column
--   (unique) sirf admin-created licences + app fill flow ke liye hai.
-- - status me 'superseded' juda: ek user ki ek waqt par sirf EK
--   active licence (nayi licence aate hi pehli active supersede).
-- - 15-DAY TRANSFER RULE (standing rule, toda NAHI gaya): ek phone
--   ki licence ek waqt par sirf ek device par; naye device par
--   15 din poore hone par transfer, usse pehle purani device par
--   hi chalegi (licence-verify v2 me logic hai, ye SQL me nahi).
-- - plan_features: plan -> feature list (licence-verify v2 isse
--   features[] deta hai). Seed ON CONFLICT DO NOTHING — dobara
--   chalane par admin ke badle hue features overwrite NAHI honge.
--
-- SECURITY NOTE (imaandaar, chhupana nahi):
-- RLS ON hai, koi policy NAHI banayi gayi hai — isliye
-- anon/authenticated client se in tables ka koi read/write nahi
-- ho sakta, sirf service-role (functions ke andar, admin/JWT check
-- ke baad) inhe chhu sakta hai. Koi policy MAT banao.
-- licence_key values platform ke alawa at-rest encrypted NAHI
-- hain (provider_keys wali hi sthiti) — isliye key list me kabhi
-- poori nahi aati: sirf masked + last 4; poori key sirf create
-- response me EK BAAR + admin reveal par (audit ke saath) milti
-- hai, aur kabhi log/audit-json me nahi aati.

-- --- licences: naye columns (sirf jo nahi hain, wahi add hon) ---
do $$
begin
  if not exists (select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'licences'
    and column_name = 'licence_key') then
    alter table public.licences add column licence_key text;
  end if;
  if not exists (select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'licences'
    and column_name = 'revoked_reason') then
    alter table public.licences add column revoked_reason text;
  end if;
  if not exists (select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'licences'
    and column_name = 'revoked_by') then
    alter table public.licences add column revoked_by uuid;
  end if;
  if not exists (select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'licences'
    and column_name = 'revoked_at') then
    alter table public.licences add column revoked_at timestamptz;
  end if;
  if not exists (select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'licences'
    and column_name = 'superseded_by') then
    alter table public.licences add column superseded_by uuid;
  end if;
end $$;

-- licence_key unique (NULL keyless rows par koi asar nahi — Postgres
-- unique index me NULL values alag-alag maani jaati hain).
create unique index if not exists licences_licence_key_uidx
  on public.licences (licence_key);

-- (user_id, device_id) unique — admin-approve app-path upsert
-- (onConflict: "user_id,device_id") isi par nirbhar hai (audit
-- A5: repo me constraint kahin nahi tha). NULL device rows
-- isse nahi takrati (NULL alag-alag maane jaate hain), isliye
-- NULL-claim rows par koi asar nahi. NOTE: agar live me pehle
-- se duplicate bound rows hain to ye statement fail hogi (loud
-- failure, silent nahi) — tab pehle duplicates saaf karo, phir
-- dobara chalao. Deploy checklist me ye verify step hai.
create unique index if not exists licences_user_device_uidx
  on public.licences (user_id, device_id);

-- status check: 'superseded' jodo (purana check sirf
-- active/revoked/expired jaanta tha). Drop + add idempotent hai
-- (dobara chalane par bhi wahi constraint dobara banta hai).
alter table public.licences drop constraint if exists licences_status_check;
alter table public.licences add constraint licences_status_check
  check (status in ('active', 'revoked', 'expired', 'superseded'));

-- NOTE (dashboard verify, blind alter nahi): licences.device_id
-- ki nullability yahan NAHI chhedi gayi hai. Website-path
-- NULL-claim rows (admin-approve) ke liye device_id NULL-able
-- hona chahiye; agar live schema me NOT NULL hai to dashboard
-- se ye ek baar chalana hoga:
--   alter table public.licences alter column device_id drop not null;
-- (deploy checklist me verify step hai — bina verify kiye mat chalana.)

-- --- plan_features: plan -> feature list ---
create table if not exists public.plan_features (
  plan text primary key,
  features jsonb not null default '[]'::jsonb,
  updated_at timestamptz not null default now()
);

alter table public.plan_features enable row level security;

-- Seed defaults (dobara chalane par overwrite nahi — admin edits safe).
-- 15 contract flags (flag-keys, panel PLAN_FLAG_LABELS se match):
-- talk, calls, sms, whatsapp, browser, macros, image_gen,
-- screen_vision, camera, overlay_mic, wakeword, home, routines, form_fill, builder.
-- B5 (2026-10-29): "builder" plan-features key hai — ye feature-flags ka
-- toggle NAHI hai (flags me builder ki koi key nahi; effective = plan AND
-- flag, missing key fail-open = true, isliye builder effectively plan-only
-- hai). Entitlement source licence-verify features[] hai (plan_features
-- table se; fallback sirf table/row missing par lagta hai, yahan nahi
-- badla gaya). QUOTA CONTRACT (defaults, user ek shabd me badal sakta
-- hai): Free = max 3 projects + max 5 AI builds/day; Pro = unlimited.
-- 1 AI build = 1 code-generation call (plan-generation steps builds me
-- nahi ginege); day = device local day (local midnight reset). Quota
-- enforcement APP-side/device-side honest gate hai (hard enforcement
-- nahi — server kabhi project code nahi dekhta); server-side Free
-- limiter brain-proxy 600s/day cap ALAG hai aur waisa hi rahega.
-- Seed ON CONFLICT DO NOTHING hai: live DB par pehle se seeded rows
-- par dobara chalane se builder NAHI judega — live DB ko panel Save
-- on Free AND Pro chahiye (allowlist deploy ke BAAD) ya alag UPDATE step.
insert into public.plan_features (plan, features) values
  ('free', '["talk","builder"]'::jsonb),
  ('pro', '["talk","calls","sms","whatsapp","browser","macros","image_gen","screen_vision","camera","overlay_mic","wakeword","home","routines","form_fill","builder"]'::jsonb)
on conflict (plan) do nothing;
