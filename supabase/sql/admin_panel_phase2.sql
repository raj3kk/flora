-- Flora admin panel PHASE 2 tables — dashboard SQL step (ek baar chalao).
-- Run order: pehle supabase/sql/admin_panel_tables.sql, PHIR ye file,
-- phir phase 2 functions deploy karo (feature-flags, admin-flags,
-- admin-instructions, heartbeat, admin-overview, brain-proxy v4,
-- send-push local redeploy). Table na ho to functions null/error
-- denge, crash nahi — par deploy se pehle SQL zaroori hai.
--
-- feature_flags: global + per-user feature toggles. scope='global'
--   wali row sab par lagu; scope=<user_id> wali row us user par
--   global se upar (override). Jo key na mile → app client me
--   default ON (fail-open) — table ka missing hona app nahi todega.
-- global_instructions: ek row (id=1) — panel se set ki gayi
--   global brain instructions + knowledge. brain-proxy v4 inhe
--   STYLE ke saath jodta hai (proxy lane par hi asar; app-direct
--   lanes apne prompts use karte hain).
-- push_log: send-push ka send history (template + ok). Token
--   value kabhi isme nahi aati — sirf user_id + template + ok.
-- heartbeats: app se heartbeat POST par last_seen. "Online"
--   status nahi hai — sirf aakhri seen time hai.
--
-- SECURITY NOTE: RLS ON hai, koi policy NAHI banayi gayi hai —
-- isliye anon/authenticated client se in tables ka koi read/write
-- nahi ho sakta, sirf service-role (functions ke andar, admin
-- check ke baad — feature-flags read ke alawa, jo global config
-- hai aur secret nahi) inhe chhu sakta hai. Koi policy MAT banao.

create table if not exists public.feature_flags (
  key text not null,
  enabled boolean not null,
  scope text not null default 'global',
  updated_at timestamptz not null default now(),
  primary key (key, scope)
);

create table if not exists public.global_instructions (
  id int primary key default 1,
  instructions text not null default '',
  knowledge text not null default '',
  updated_at timestamptz not null default now()
);

create table if not exists public.push_log (
  id uuid primary key default gen_random_uuid(),
  user_id uuid,
  template text,
  sent_at timestamptz not null default now(),
  ok boolean
);

create table if not exists public.heartbeats (
  user_id uuid primary key,
  last_seen timestamptz not null default now()
);

alter table public.feature_flags enable row level security;
alter table public.global_instructions enable row level security;
alter table public.push_log enable row level security;
alter table public.heartbeats enable row level security;
