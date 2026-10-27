-- Maya rebuild — Supabase schema (NEW system, no old data)
-- Tables user-confirmed: Users, Settings, Plans, Usage
-- Ye sirf schema hai — koi key, password, ya secret is file me nahi hai.
-- RLS: har user sirf apna row dekh/badal sake; licence check hamesha
-- server-side function se hoga, app ke client check par bharosa nahi.

create table if not exists public.users (
  id uuid primary key default gen_random_uuid(),
  created_at timestamptz not null default now(),
  device_id text not null unique,
  display_name text,
  is_owner boolean not null default false
);

create table if not exists public.plans (
  id text primary key,
  name text not null,
  price_paise integer not null,
  billing text not null default 'one_time',
  features jsonb not null default '{}'::jsonb
);

create table if not exists public.settings (
  user_id uuid primary key references public.users(id) on delete cascade,
  voice_engine text not null default 'system',
  language text not null default 'hi',
  persona text not null default 'professional',
  wake_word_enabled boolean not null default true,
  guardian_enabled boolean not null default false,
  updated_at timestamptz not null default now()
);

create table if not exists public.usage (
  id bigint generated always as identity primary key,
  user_id uuid not null references public.users(id) on delete cascade,
  day date not null default current_date,
  talk_seconds integer not null default 0,
  tool_calls integer not null default 0,
  unique (user_id, day)
);

-- Licence table: server-authoritative entitlement
create table if not exists public.licences (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.users(id) on delete cascade,
  plan_id text not null references public.plans(id),
  status text not null default 'active' check (status in ('active','revoked','expired')),
  device_id text not null,
  activated_at timestamptz not null default now(),
  expires_at timestamptz,
  unique (user_id, device_id)
);

alter table public.users enable row level security;
alter table public.plans enable row level security;
alter table public.settings enable row level security;
alter table public.usage enable row level security;
alter table public.licences enable row level security;

-- Seed plans (USER DECISION 2026-10-27 ~20:25 IST, supersedes website/video figures):
-- Flora app, ₹199/month subscription. Payment abhi UPI deep link se,
-- baad me full payment integration hogi. Free tier alag scope hai.
-- Licence scopes alag hain — har plan ke hisaab se service use hogi.
insert into public.plans (id, name, price_paise, billing, features) values
  ('free', 'Flora Free', 0, 'free', '{"talk_seconds_per_day": 600, "tools": "basic_15", "characters": ["default"]}'::jsonb),
  ('pro', 'Flora Pro', 19900, 'monthly', '{"talk": "unlimited", "tools": "all", "characters": "all_plus_future", "live_wallpaper": true, "conversation_mode": true, "fcm_push": true}'::jsonb)
on conflict (id) do nothing;
on conflict (id) do nothing;
