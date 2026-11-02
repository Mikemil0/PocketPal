-- ============================================================================
-- Módulo 6 · Dai (Daikoku), el asistente financiero — Base de datos
-- Ejecutar en el SQL Editor de Supabase, completo y una sola vez.
-- Requiere los módulos 2 (categories), 3 (goals), 4 (movements) y 5 (budgets).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- BLOQUE 1 — Tabla `dai_messages` (historial del chat con Dai)
-- Cada fila es UN mensaje: lo que escribió el usuario (role = 'user') o lo que
-- respondió Dai (role = 'dai').
-- * `card` (jsonb, opcional): la tarjeta que acompaña la respuesta de Dai
--   (resumen de gastos, metas, alerta, lección, factura guardada…). Se guarda
--   como JSON para que, al volver a abrir el chat, se dibuje igual.
-- * "Borrar historial" (Configuración de Dai) borra SOLO estas filas: los
--   movimientos, presupuestos y metas no se tocan.
-- ----------------------------------------------------------------------------
create table if not exists public.dai_messages (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  role text not null check (role in ('user','dai')),
  text text not null check (char_length(text) between 1 and 2000),
  card jsonb,
  created_at timestamptz not null default now()
);

-- La consulta del chat es "mis mensajes, del más viejo al más nuevo".
create index if not exists dai_messages_by_user_date
  on public.dai_messages (user_id, created_at);

-- ----------------------------------------------------------------------------
-- BLOQUE 2 — Seguridad (RLS)
-- Cada usuario solo ve, escribe y borra SU conversación con Dai.
-- ----------------------------------------------------------------------------
alter table public.dai_messages enable row level security;

drop policy if exists "own dai messages" on public.dai_messages;
create policy "own dai messages" on public.dai_messages
  for all
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

-- ----------------------------------------------------------------------------
-- NOTA · Facturas leídas con la cámara
-- Cuando el usuario APRUEBA los datos de una factura, la app crea una fila en
-- `public.movements` (Módulo 4) con source = 'dai'. Ese valor ya estaba
-- permitido en el check de la columna `source`, así que no hay que cambiar nada:
-- la misma política RLS "own movements" deja insertar solo a nombre propio y
-- con una categoría propia.
-- ----------------------------------------------------------------------------

-- ----------------------------------------------------------------------------
-- VERIFICACIÓN (para mostrarle al profesor)
-- select role, text, card, created_at from public.dai_messages order by created_at;
-- select title, amount, occurred_at, source from public.movements where source = 'dai';
-- ----------------------------------------------------------------------------
