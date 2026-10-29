// public-settings — NEW, admin panel full build 2026-10-29 (repo copy;
// deploy dashboard editor se hoga; pehle admin_panel_tables.sql
// chalana zaroori hai, warna upi_vpa null milega).
// Kaam: pay page ko owner UPI ID dena. GET ya POST, NO auth
// check — by design. UPI public payment address hai, secret
// nahi, isliye isme auth gate nahi hai. Pay page anon-key
// Bearer bhejta hai taaki gateway JWT verification ON ho to
// bhi call pass ho jaye; function khud koi auth check nahi
// karta.
// Response me SIRF { upi_vpa } hota hai (set na ho to null).
// Isme kabhi aur kuch mat bhejo (keys, settings, users nahi).

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

Deno.serve(async (req) => {
  if (req.method !== "GET" && req.method !== "POST") {
    return new Response("GET/POST only", { status: 405 });
  }
  const json = (obj: unknown, status = 200) =>
    new Response(JSON.stringify(obj), { status, headers: { "Content-Type": "application/json" } });
  try {
    const svc = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
    );
    const { data, error } = await svc.from("admin_settings").select("value").eq("key", "upi_vpa").maybeSingle();
    if (error) return json({ upi_vpa: null });
    return json({ upi_vpa: data?.value || null });
  } catch (_e) {
    return json({ upi_vpa: null });
  }
});
