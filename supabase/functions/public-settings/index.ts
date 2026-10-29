// public-settings — NEW, admin panel full build 2026-10-29 (repo copy;
// deploy dashboard editor se hoga; pehle admin_panel_tables.sql
// chalana zaroori hai, warna upi_vpa null milega).
// Kaam: pay page ko owner UPI ID dena. GET ya POST, NO auth
// check — by design. UPI public payment address hai, secret
// nahi, isliye isme auth gate nahi hai. Pay page anon-key
// Bearer bhejta hai taaki gateway JWT verification ON ho to
// bhi call pass ho jaye; function khud koi auth check nahi
// karta.
// Response me { upi_vpa } + ADDITIVE status hota hai (S1-38,
// Cycle 1 FIX): "set" (value maujood), "unset" (row nahi /
// khaali value), "table_missing" (admin_settings table hi nahi
// hai — SQL step baaki hai), "error" (koi aur failure).
// upi_vpa field waisi hi hai (pay page ka fallback na toote).
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
    if (error) {
      // S1-38: table-missing vs unset ka farq — pehle dono par
      // sirf upi_vpa:null milta tha (silent). 42P01 = Postgres
      // "relation does not exist".
      const msg = (error as any).message || "";
      const tableMissing = (error as any).code === "42P01" ||
        (msg.includes("does not exist") && msg.includes("admin_settings"));
      return json({ upi_vpa: null, status: tableMissing ? "table_missing" : "error" });
    }
    if (!data?.value) return json({ upi_vpa: null, status: "unset" });
    return json({ upi_vpa: data.value, status: "set" });
  } catch (_e) {
    return json({ upi_vpa: null, status: "error" });
  }
});
