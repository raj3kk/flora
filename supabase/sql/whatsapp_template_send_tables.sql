-- Item 36 (SD-P2) — whatsapp-template-send tables.
-- Dashboard SQL editor me chalana hai (function deploy ke saath ya pehle).
-- Bina in tables ke function ke gate reads fail honge aur function
-- fail-closed rahega (koi send nahi) — yehi intended behaviour hai.
--
-- Spec: FLORA_ITEM36_TEACHONCE_SERVER_SPEC.md §2.5/§2.7.
--   whatsapp_template_authorizations: user ne jis template ko khud
--     authorize kiya ho (app me explicit authorize action se). Sirf
--     'active' record par hi send hoga; revoke par status 'revoked'.
--   whatsapp_send_confirmations: har send se pehle ki per-send
--     confirmation (masked: poora number kabhi nahi, sirf SHA-256 hash).
--   whatsapp_template_sends: har attempt ka audit record (sent/failed/
--     rejected). Poora number, params values, token kabhi nahi.
--
-- NO foreign keys (jaanboojh kar): auth user ka public.users row na
-- bhi ho to send flow FK par na toote — gates JWT + records se hain.
-- NO RLS policies (baaki tables jaisa): client direct access
-- fail-closed rahega; service-role se hi function access karega.

create table if not exists public.whatsapp_template_authorizations (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null,
  template_name text not null,
  template_language text not null,
  authorized_by uuid not null,
  authorized_at timestamptz not null default now(),
  status text not null default 'active'
    check (status in ('active', 'revoked'))
);
-- Ek user + template + language par ek waqt par sirf EK active record.
create unique index if not exists whatsapp_template_authorizations_active_uidx
  on public.whatsapp_template_authorizations (user_id, template_name, template_language)
  where status = 'active';

create table if not exists public.whatsapp_send_confirmations (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null,
  template_name text not null,
  template_language text not null,
  recipient_hash text not null,
  confirmed_at timestamptz not null default now(),
  consumed boolean not null default false
);
create index if not exists whatsapp_send_confirmations_user_idx
  on public.whatsapp_send_confirmations (user_id, confirmed_at desc);

create table if not exists public.whatsapp_template_sends (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null,
  template_name text not null,
  template_language text not null,
  recipient_hash text not null,
  recipient_last4 text not null,
  confirmation_id uuid,
  authorization_id uuid,
  idempotency_key text not null,
  provider_message_id text,
  status text not null
    check (status in ('sent', 'failed', 'rejected')),
  error_code text,
  created_at timestamptz not null default now(),
  unique (user_id, idempotency_key)
);
create index if not exists whatsapp_template_sends_user_created_idx
  on public.whatsapp_template_sends (user_id, created_at desc);
