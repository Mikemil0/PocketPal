-- ============================================================================
-- Módulo 4 · Movimientos — Base de datos
-- Ejecutar en el SQL Editor de Supabase. Se puede correr COMPLETO de una vez y es
-- seguro repetirlo (usa "if not exists" / "or replace" / "drop ... if exists").
--
-- Modelo:
--   movements   una fila por gasto o ingreso (título, monto, categoría, fecha…)
--
-- En este módulo la app SOLO LEE movimientos (ver, buscar, filtrar, detalle).
-- Registrarlos lo hará Dai con fotos en un módulo posterior; mientras tanto, el
-- BLOQUE 4 crea datos de prueba para la cuenta que ustedes elijan.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- BLOQUE 1 — Tabla `movements`
-- kind:        'expense' (gasto) o 'income' (ingreso). Se guardan códigos neutros
--              y la app traduce las etiquetas (igual que en categorías).
-- amount:      siempre POSITIVO. El signo lo da `kind` (un gasto se muestra -$).
-- category_id: si se borra la categoría, el movimiento queda "sin categoría"
--              (on delete set null) en vez de borrarse.
-- recurrence:  null = movimiento normal; 'weekly' | 'monthly' | 'yearly' = recurrente.
-- next_charge_on: próximo cobro de un recurrente (opcional; si falta, la app lo calcula).
-- source:      quién lo creó: 'manual', 'dai' (futuro módulo) o 'demo' (datos de prueba).
-- ----------------------------------------------------------------------------
create table if not exists public.movements (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  kind text not null check (kind in ('expense', 'income')),
  title text not null check (char_length(trim(title)) between 1 and 60),
  amount numeric(14,2) not null check (amount > 0),
  category_id uuid references public.categories(id) on delete set null,
  description text check (description is null or char_length(description) <= 200),
  occurred_at timestamptz not null default now(),
  recurrence text check (recurrence in ('weekly', 'monthly', 'yearly')),
  next_charge_on date,
  source text not null default 'manual' check (source in ('manual', 'dai', 'demo')),
  created_at timestamptz not null default now()
);

-- La lista siempre se pide "de un usuario, entre dos fechas, la más reciente primero".
create index if not exists movements_by_user_date
  on public.movements (user_id, occurred_at desc);

-- ----------------------------------------------------------------------------
-- BLOQUE 2 — Seguridad (RLS)
-- Cada usuario solo ve y toca SUS movimientos. Además, al escribir, la categoría
-- (si tiene) debe ser del mismo usuario: nadie puede colgar un movimiento de una
-- categoría ajena aunque conozca su id.
-- ----------------------------------------------------------------------------
alter table public.movements enable row level security;

drop policy if exists "own movements" on public.movements;
create policy "own movements" on public.movements
  for all
  using (auth.uid() = user_id)
  with check (
    auth.uid() = user_id
    and (
      category_id is null
      or exists (select 1 from public.categories c
                 where c.id = category_id and c.user_id = auth.uid())
    )
  );

-- ----------------------------------------------------------------------------
-- BLOQUE 3 — Función de datos de prueba `seed_demo_movements(correo)`
-- Crea (si no existen) 6 categorías y ~20 movimientos repartidos entre hoy, esta
-- semana, este mes y el mes pasado, para que se vean todos los filtros.
-- * Borra primero SOLO los movimientos 'demo' de esa cuenta: se puede repetir.
-- * security definer: corre con permisos del dueño porque escribe para otra cuenta.
-- * Por eso se le QUITA el permiso de ejecución a la app (anon / authenticated):
--   solo se puede llamar desde el SQL Editor del panel.
-- ----------------------------------------------------------------------------
create or replace function public.seed_demo_movements(p_email text) returns integer
language plpgsql security definer set search_path = public as $$
declare
  uid uuid;
  base timestamp;               -- hoy a las 00:00, hora de Colombia
  c_comida uuid;
  c_transporte uuid;
  c_entretenimiento uuid;
  c_compras uuid;
  c_hogar uuid;
  c_salario uuid;
  total integer;
begin
  select id into uid from auth.users where lower(email) = lower(trim(p_email));
  if uid is null then
    raise exception 'No existe un usuario con el correo %', p_email;
  end if;

  -- Categorías: si ya existe una con el mismo nombre (índice único), se reutiliza.
  insert into public.categories (user_id, kind, name, icon, color, monthly_limit, position) values
    (uid, 'expense', 'Comida',          'restaurant', '#FF9B9B', 700000, 100),
    (uid, 'expense', 'Transporte',      'dashboard',  '#55A5EF', 250000, 101),
    (uid, 'expense', 'Entretenimiento', 'star',       '#EAB760', null,   102),
    (uid, 'expense', 'Compras',         'favorite',   '#C90E0E', null,   103),
    (uid, 'expense', 'Hogar',           'home',       '#D194E5', null,   104),
    (uid, 'income',  'Salario',         'savings',    '#55A5EF', null,   100)
  on conflict do nothing;

  select id into c_comida from public.categories
    where user_id = uid and kind = 'expense' and lower(trim(name)) = 'comida';
  select id into c_transporte from public.categories
    where user_id = uid and kind = 'expense' and lower(trim(name)) = 'transporte';
  select id into c_entretenimiento from public.categories
    where user_id = uid and kind = 'expense' and lower(trim(name)) = 'entretenimiento';
  select id into c_compras from public.categories
    where user_id = uid and kind = 'expense' and lower(trim(name)) = 'compras';
  select id into c_hogar from public.categories
    where user_id = uid and kind = 'expense' and lower(trim(name)) = 'hogar';
  select id into c_salario from public.categories
    where user_id = uid and kind = 'income' and lower(trim(name)) = 'salario';

  delete from public.movements where user_id = uid and source = 'demo';

  base := date_trunc('day', now() at time zone 'America/Bogota');

  insert into public.movements
    (user_id, kind, title, amount, category_id, description, occurred_at, recurrence, next_charge_on, source)
  values
    -- Hoy
    (uid, 'expense', 'Supermercado Carulla', 86450,  c_comida,          'Compra en Carulla',       (base + interval '10 hours 24 minutes') at time zone 'America/Bogota', null, null, 'demo'),
    (uid, 'expense', 'Spotify',              24900,  c_entretenimiento, 'Plan Premium',            (base + interval '8 hours')             at time zone 'America/Bogota', 'monthly', (base + interval '1 month')::date, 'demo'),
    (uid, 'income',  'Salario',              1200000, c_salario,        'Pago quincena',           (base + interval '7 hours')             at time zone 'America/Bogota', 'monthly', (base + interval '1 month')::date, 'demo'),
    (uid, 'expense', 'Transporte',           12000,  c_transporte,      'Taxi a la oficina',       (base + interval '7 hours 40 minutes')  at time zone 'America/Bogota', null, null, 'demo'),
    (uid, 'expense', 'Café',                 8500,   c_comida,          null,                      (base + interval '9 hours 5 minutes')   at time zone 'America/Bogota', null, null, 'demo'),
    -- Esta semana
    (uid, 'expense', 'Mercado Central',      42800,  c_comida,          'Frutas y verduras',       (base - interval '1 day' + interval '18 hours')  at time zone 'America/Bogota', null, null, 'demo'),
    (uid, 'expense', 'Transmilenio',         5900,   c_transporte,      null,                      (base - interval '2 days' + interval '6 hours')  at time zone 'America/Bogota', null, null, 'demo'),
    (uid, 'expense', 'Cine Colombia',        32000,  c_entretenimiento, 'Boletas fin de semana',   (base - interval '3 days' + interval '20 hours') at time zone 'America/Bogota', null, null, 'demo'),
    (uid, 'expense', 'Supermercado Norte',   76200,  c_comida,          null,                      (base - interval '5 days' + interval '17 hours') at time zone 'America/Bogota', null, null, 'demo'),
    -- Antes en el mes (o finales del mes pasado si hoy es muy temprano en el mes)
    (uid, 'expense', 'Arriendo',             650000, c_hogar,           'Arriendo apartamento',    (base - interval '9 days' + interval '9 hours')  at time zone 'America/Bogota', 'monthly', (base - interval '9 days' + interval '1 month')::date, 'demo'),
    (uid, 'expense', 'Zara',                 98500,  c_compras,         'Chaqueta',                (base - interval '11 days' + interval '16 hours') at time zone 'America/Bogota', null, null, 'demo'),
    (uid, 'expense', 'Mercado del Puerto',   18900,  c_comida,          'Almuerzo',                (base - interval '13 days' + interval '13 hours') at time zone 'America/Bogota', null, null, 'demo'),
    (uid, 'expense', 'Gasolina',             120000, c_transporte,      null,                      (base - interval '15 days' + interval '8 hours')  at time zone 'America/Bogota', null, null, 'demo'),
    (uid, 'income',  'Venta en línea',       150000, c_salario,         'Venta de audífonos',      (base - interval '16 days' + interval '19 hours') at time zone 'America/Bogota', null, null, 'demo'),
    (uid, 'expense', 'Netflix',              26900,  c_entretenimiento, null,                      (base - interval '18 days' + interval '21 hours') at time zone 'America/Bogota', 'monthly', (base - interval '18 days' + interval '1 month')::date, 'demo'),
    (uid, 'expense', 'Farmacia',             35400,  null,              'Sin categoría',           (base - interval '20 days' + interval '12 hours') at time zone 'America/Bogota', null, null, 'demo'),
    -- Mes pasado (para el "% vs. mes anterior")
    (uid, 'income',  'Salario',              1200000, c_salario,        'Pago quincena',           (base - interval '1 month' + interval '7 hours')  at time zone 'America/Bogota', null, null, 'demo'),
    (uid, 'expense', 'Arriendo',             650000, c_hogar,           'Arriendo apartamento',    (base - interval '1 month' - interval '9 days' + interval '9 hours') at time zone 'America/Bogota', null, null, 'demo'),
    (uid, 'expense', 'Supermercado Carulla', 210300, c_comida,          null,                      (base - interval '1 month' - interval '3 days' + interval '11 hours') at time zone 'America/Bogota', null, null, 'demo'),
    (uid, 'expense', 'Uber',                 45000,  c_transporte,      null,                      (base - interval '1 month' - interval '6 days' + interval '22 hours') at time zone 'America/Bogota', null, null, 'demo');

  select count(*) into total from public.movements where user_id = uid and source = 'demo';
  return total;
end; $$;

-- La función solo se puede usar desde el panel (SQL Editor), nunca desde la app.
revoke execute on function public.seed_demo_movements(text) from public, anon, authenticated;

-- ----------------------------------------------------------------------------
-- BLOQUE 4 — Cargar los datos de prueba (cada integrante, con SU correo)
-- Quitar los dos guiones del inicio, poner el correo con el que inician sesión en
-- la app y ejecutar SOLO esa línea. Responde cuántos movimientos quedaron (≈20).
-- ----------------------------------------------------------------------------
-- select public.seed_demo_movements('correo-con-el-que-inicias-sesion@gmail.com');

-- ----------------------------------------------------------------------------
-- VERIFICACIÓN (opcional, para mostrarle al profesor)
-- select m.title, m.kind, m.amount, c.name as categoria, m.occurred_at, m.recurrence
--   from public.movements m left join public.categories c on c.id = m.category_id
--   order by m.occurred_at desc;
-- ----------------------------------------------------------------------------
