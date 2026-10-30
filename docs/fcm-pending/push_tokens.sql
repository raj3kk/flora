-- SUPERSEDED / DO-NOT-RUN (parent docs pass, 2026-10-30): ye draft canonical
-- supabase/sql/core_tables.sql se diverge karta hai. Canonical version deploy
-- session #1 me chal chuka hai (live trap moot hai) — is draft ko dobara
-- kabhi mat chalao; sirf history/reference ke liye rakha hai.
-- Flora push_tokens table — DRAFT ONLY, deploy nahi hua.
-- Chalane se pehle: Supabase dashboard / SQL editor se chalana
-- hai; is file ko chalane se koi token, key ya secret nahi
-- banta. Token value kabhi log/display nahi hoti.
--
-- Design: ek user ke multiple devices ho sakte hain, isliye
-- (user_id, device_id) unique hai; token rotate hone par
-- upsert se purana token overwrite hoga, duplicate row nahi.

create table if not exists public.push_tokens (
  user_id uuid not null,
  device_id text not null,
  fcm_token text not null,
  platform text not null default 'android',
  updated_at timestamptz not null default now(),
  primary key (user_id, device_id)
);

create index if not exists push_tokens_user_idx
  on public.push_tokens (user_id);

alter table public.push_tokens enable row level security;

-- App sirf apne user_id ki row likh/padh sake; service_role
-- (server send path) RLS ko bypass karta hai, isliye send
-- function me user_id check alag se bhi rahega.
create policy "owner can manage own tokens"
  on public.push_tokens
  for all
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);
