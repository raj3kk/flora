-- Cycle 122 (Lane B server): usage-count atomic increment (S1-20).
-- Deploy: Supabase dashboard SQL editor me chalao (GitHub push se
-- SQL deploy NAHI hota). Order: PEHLE is file ko chalao, PHIR
-- usage-count function ka naya version deploy karo. Function me
-- RPC-na-mile par purane read-modify-write path ka fallback hai,
-- isliye order galat ho to bhi function nahi tootega (sirf race
-- tab tak barkaraar rahegi jab tak ye SQL na chale).
--
-- Step 1: maujooda duplicate (user_id, day) rows merge karo —
-- sabse purani row me counters jodo, baaki hatao. Koi duplicate
-- na ho to ye statements no-op hain (kuch badalte nahi).
-- Step 2: unique index — is ke baad naye duplicate ban hi nahi sakte.
-- Step 3: increment_usage RPC — atomic INSERT ... ON CONFLICT DO UPDATE.

-- Step 1a: keeper row ke counters = group ka jod.
UPDATE public.usage u
SET talk_seconds = s.total_secs, tool_calls = s.total_tools
FROM (
  SELECT user_id, day, min(id) AS keep_id,
    sum(talk_seconds) AS total_secs, sum(tool_calls) AS total_tools
  FROM public.usage GROUP BY user_id, day HAVING count(*) > 1
) s
WHERE u.id = s.keep_id;

-- Step 1b: keeper ke alawa duplicate rows hatao.
DELETE FROM public.usage u
USING (
  SELECT user_id, day, min(id) AS keep_id
  FROM public.usage GROUP BY user_id, day HAVING count(*) > 1
) s
WHERE u.user_id = s.user_id AND u.day = s.day AND u.id <> s.keep_id;

-- Step 2: unique index.
CREATE UNIQUE INDEX IF NOT EXISTS usage_user_day_uidx
  ON public.usage (user_id, day);

-- Step 3: atomic increment RPC (service-role se call hota hai,
-- isliye alag RLS policy ki zaroorat nahi).
-- NOTE (deploy session #2 fix): live usage.day column DATE type
-- ka hai, jabki function 'YYYY-MM-DD' text bhejta hai — isliye
-- yahan p_day::date cast hai; RPC signature (p_day text) wahi
-- rakha hai taaki usage-count function ka call na badle.
CREATE OR REPLACE FUNCTION public.increment_usage(
  p_user_id uuid, p_day text, p_secs integer, p_tools integer
)
RETURNS integer
LANGUAGE sql
AS $$
  INSERT INTO public.usage (user_id, day, talk_seconds, tool_calls)
  VALUES (p_user_id, p_day::date, p_secs, p_tools)
  ON CONFLICT (user_id, day)
  DO UPDATE SET talk_seconds = public.usage.talk_seconds + EXCLUDED.talk_seconds,
                tool_calls = public.usage.tool_calls + EXCLUDED.tool_calls
  RETURNING talk_seconds;
$$;
