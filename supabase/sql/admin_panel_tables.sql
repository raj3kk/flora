-- Flora admin panel tables — dashboard SQL step (ek baar chalao).
-- Run order: pehle ye SQL, phir naye functions deploy karo
-- (admin-keys / admin-settings / public-settings is table par
-- nirbhar hain; table na ho to wo null/error denge, crash nahi).
--
-- provider_keys: panel se set ki gayi provider keys
--   (gemini / groq / deepgram). brain-proxy v3 + tts-proxy v2
--   inhe sirf tab padhte hain jab dashboard secret (env) na ho.
-- admin_settings: admin-only settings (aaj sirf upi_vpa —
--   website pay page par dikhne wala owner UPI address).
--
-- SECURITY NOTE (CEO batch 2026-10-31, D-21 FIX — purana note superseded):
-- admin-keys ab key_value ko AES-GCM encrypt karke store karta hai
-- (format enc:v1:..., key = SHA-256(PROVIDER_KEYS_ENCRYPTION_KEY
-- dashboard secret)). Is file ka table shape wahi hai (key_value text);
-- encryption function-level hai, schema change nahi. Legacy plaintext
-- rows migration ke dauran padhi jayengi, phir panel se re-enter par
-- encrypted ho jayengi (status me providers_encrypted se verify karo).
-- Protection ye hai: (1) RLS ON hai, (2) koi policy NAHI banayi
-- gayi hai — isliye anon/authenticated client se in tables ka
-- koi read/write nahi ho sakta, sirf service-role (functions ke
-- andar, admin check ke baad) inhe chhu sakta hai, (3) key
-- VALUES kabhi panel par display nahi hongi aur kabhi log nahi
-- hongi. Dashboard secrets (env) zyada secure raasta hai aur
-- hamesha pehle jeetenge; DB keys sirf fallback hain.
--
-- Koi policy MAT banao: policy banate hi client-side access
-- khul jayega — service-role-only hi design hai.

create table if not exists public.provider_keys (
  provider text primary key,
  key_value text not null,
  updated_at timestamptz not null default now()
);

create table if not exists public.admin_settings (
  key text primary key,
  value text not null,
  updated_at timestamptz not null default now()
);

alter table public.provider_keys enable row level security;
alter table public.admin_settings enable row level security;
