-- Home sync table (Phase 8) — Supabase SQL editor me ek baar chalao.
-- Ek device_id = ek row (box badle to upsert se update).
-- RLS ON hai, par koi policy NAHI banayi gayi — isliye anon /
-- authenticated client is table ko seedha padh/likh NAHI sakta.
-- Sirf home-sync Edge Function (service-role key se) upsert karta
-- hai, JWT gate ke baad (JWT user == body user_id, warna 403).
-- Ye design jaanboojh kar hai: relay box code client ke haath
-- me kabhi seedha table access nahi deta.
create table if not exists public.home_boxes (
  device_id text primary key,
  user_id uuid not null,
  box_code text not null,
  updated_at timestamptz not null default now()
);

alter table public.home_boxes enable row level security;
-- NOTE: jaanboojh kar koi policy nahi — service-role-only.
-- Agar kabhi client-side read chahiye ho to pehle user order
-- lo, phir owner-only policy (auth.uid() = user_id) banao;
-- bina order ke policy mat banao.
