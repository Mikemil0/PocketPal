-- ============================================================================
-- Módulo 5 · Presupuestos — Base de datos
-- Ejecutar en el SQL Editor de Supabase, bloque por bloque y en orden.
-- Requiere los Módulos 2 (categories) y 4 (movements) ya instalados.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- BLOQUE 1 — Tabla `budgets` (presupuestos)
-- Un presupuesto pone un LÍMITE de gasto a UNA categoría de gastos, por mes o
-- por semana. Lo gastado NO se guarda aquí: se calcula sumando los gastos de
-- `movements` de esa categoría en el período (así nunca queda desactualizado).
-- * category_id ... on delete cascade: si se borra la categoría, se borra su presupuesto.
-- * Borrar un presupuesto NO toca los movimientos (los gastos siguen ahí).
-- ----------------------------------------------------------------------------
create table if not exists public.budgets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  category_id uuid not null references public.categories(id) on delete cascade,
  name text not null check (char_length(trim(name)) between 1 and 30),
  amount_limit numeric(14,2) not null check (amount_limit > 0),
  period text not null default 'monthly' check (period in ('monthly','weekly')),
  alert_enabled boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- Un solo presupuesto por categoría y usuario. Es la AUTORIDAD detrás del mensaje
-- "Ya tienes un presupuesto con este nombre en la misma categoría".
create unique index if not exists budgets_one_per_category
  on public.budgets (user_id, category_id);

-- ----------------------------------------------------------------------------
-- BLOQUE 2 — Seguridad (RLS)
-- Cada usuario solo ve, crea, edita y borra SUS presupuestos. Además, al escribir,
-- la categoría tiene que ser suya y de tipo gasto (no se presupuesta un ingreso).
-- ----------------------------------------------------------------------------
alter table public.budgets enable row level security;

drop policy if exists "own budgets" on public.budgets;
create policy "own budgets" on public.budgets
  for all
  using (auth.uid() = user_id)
  with check (
    auth.uid() = user_id
    and exists (
      select 1 from public.categories c
      where c.id = category_id and c.user_id = auth.uid() and c.kind = 'expense'
    )
  );

-- updated_at se actualiza solo en cada edición.
create or replace function public.touch_budget_updated_at() returns trigger
language plpgsql as $$
begin
  new.updated_at := now();
  return new;
end; $$;

drop trigger if exists budgets_touch_updated_at on public.budgets;
create trigger budgets_touch_updated_at
  before update on public.budgets
  for each row execute function public.touch_budget_updated_at();

-- ----------------------------------------------------------------------------
-- BLOQUE 3 — Datos de prueba `seed_demo_budgets(correo)`
-- Crea 4 presupuestos mensuales sobre las categorías de prueba del Módulo 4
-- (Comida, Transporte, Entretenimiento y Compras). "Hogar" queda SIN presupuesto
-- para poder crear uno desde la app. Repetible: borra antes los de esas categorías.
-- Primero hay que haber corrido seed_demo_movements con el mismo correo.
-- security definer + revoke: solo se usa desde el SQL Editor, nunca desde la app.
-- ----------------------------------------------------------------------------
create or replace function public.seed_demo_budgets(p_email text) returns integer
language plpgsql security definer set search_path = public as $$
declare
  uid uuid;
  total integer;
begin
  select id into uid from auth.users where lower(email) = lower(trim(p_email));
  if uid is null then
    raise exception 'No existe un usuario con el correo %', p_email;
  end if;

  delete from public.budgets b
   using public.categories c
   where b.category_id = c.id and b.user_id = uid
     and lower(trim(c.name)) in ('comida','transporte','entretenimiento','compras');

  insert into public.budgets (user_id, category_id, name, amount_limit, period, alert_enabled)
  select uid, c.id, c.name, v.amount, 'monthly', v.alert
    from public.categories c
    join (values
      ('comida',          500000, true),
      ('transporte',      250000, false),
      ('entretenimiento', 100000, true),
      ('compras',          80000, false)
    ) as v(name, amount, alert) on lower(trim(c.name)) = v.name
   where c.user_id = uid and c.kind = 'expense';

  select count(*) into total from public.budgets where user_id = uid;
  return total;
end; $$;

revoke execute on function public.seed_demo_budgets(text) from public, anon, authenticated;

-- ----------------------------------------------------------------------------
-- BLOQUE 4 — Cargar los datos de prueba (cada integrante, con SU correo)
-- Quitar los dos guiones y ejecutar SOLO esa línea. Responde cuántos presupuestos tienes (4).
-- ----------------------------------------------------------------------------
-- select public.seed_demo_budgets('correo-con-el-que-inicias-sesion@gmail.com');

-- ----------------------------------------------------------------------------
-- VERIFICACIÓN (para mostrarle al profesor)
-- select b.name, c.name as categoria, b.amount_limit, b.period, b.alert_enabled,
--        (select coalesce(sum(m.amount),0) from public.movements m
--          where m.category_id = b.category_id and m.kind = 'expense'
--            and m.occurred_at >= date_trunc('month', now() at time zone 'America/Bogota') at time zone 'America/Bogota') as gastado_mes
--   from public.budgets b join public.categories c on c.id = b.category_id
--  order by b.created_at;
-- ----------------------------------------------------------------------------
