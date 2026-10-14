// Edge Function "revoke-device" (D-01 opción B, §8.5: sesiones activas).
//
// Recibe { "device_id": "<uuid de user_devices>" } con el JWT del usuario en
// Authorization. Usa la clave service_role (secreto de la función, NUNCA en la app) para:
//   1. Verificar que el dispositivo pertenece al usuario del JWT.
//   2. Intentar revocar la sesión en Supabase Auth (Admin API).
//   3. Marcar revoked_at en public.user_devices.
//
// Responde { "revoked": true|false }: si es false, el cliente aplica el fallback A
// de D-01 (SignOutScope.OTHERS). Despliegue: supabase functions deploy revoke-device
// (o Dashboard > Edge Functions). Los secretos SUPABASE_URL y SUPABASE_SERVICE_ROLE_KEY
// están configurados automáticamente por la plataforma.

import { createClient } from "npm:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

Deno.serve(async (req) => {
  try {
    const authHeader = req.get("Authorization") ?? "";
    const jwt = authHeader.replace("Bearer ", "");
    if (!jwt) return json({ error: "unauthorized" }, 401);

    const admin = createClient(SUPABASE_URL, SERVICE_ROLE);

    // 1. Usuario del JWT (auth.getUser valida el token).
    const { data: userData, error: userError } = await admin.auth.getUser(jwt);
    if (userError || !userData?.user) return json({ error: "unauthorized" }, 401);
    const userId = userData.user.id;

    const body = await req.json();
    const deviceId = body?.device_id;
    if (!deviceId) return json({ error: "missing device_id" }, 400);

    // 2. El dispositivo debe pertenecer al usuario (además de RLS).
    const { data: row, error: rowError } = await admin
      .from("user_devices")
      .select("id, user_id, session_id")
      .eq("id", deviceId)
      .maybeSingle();
    if (rowError || !row || row.user_id !== userId) {
      return json({ error: "not_found" }, 404);
    }

    // 3. Revocación server-side de la sesión (mejor esfuerzo).
    let revoked = false;
    if (row.session_id) {
      const res = await fetch(
        `${SUPABASE_URL}/auth/v1/admin/users/${userId}/sessions/${row.session_id}`,
        {
          method: "DELETE",
          headers: {
            apikey: SERVICE_ROLE,
            Authorization: `Bearer ${SERVICE_ROLE}`,
          },
        },
      );
      revoked = res.ok || res.status === 204;
    }

    // 4. La fila queda marcada en cualquier caso (visibilidad para la lista).
    await admin
      .from("user_devices")
      .update({ revoked_at: new Date().toISOString() })
      .eq("id", deviceId);

    return json({ revoked });
  } catch (_e) {
    return json({ error: "unknown" }, 500);
  }
});

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
