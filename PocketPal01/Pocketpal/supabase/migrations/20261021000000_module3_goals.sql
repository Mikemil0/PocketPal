-- ============================================================================
-- Módulo 3 · Metas de ahorro — Base de datos
-- Ejecutar en el SQL Editor de Supabase, bloque por bloque, en orden.
-- Cada bloque es seguro de repetir (usa "if not exists" / "or replace" / "drop ... if exists").
--
-- Modelo:
--   goals               una fila por meta (nombre, ícono, color, monto, fecha límite…)
--   goal_contributions  una fila por aporte a una meta (monto, fecha, motivo)
--
-- Regla de oro: lo ahorrado de una meta (goals.saved_amount) NUNCA lo escribe la app.
-- Lo calcula la base sumando los aportes (BLOQUE 3). Así el total y el historial
-- siempre cuadran, aunque se borre o se agregue un aporte desde otro dispositivo.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- BLOQUE 1 — Tabla `goals` + RLS + nombre único por usuario
-- ----------------------------------------------------------------------------
create table if not exists public.goals (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null check (char_length(trim(name)) between 1 and 30),
  icon text not null,
  color text not null,
  target_amount numeric(14,2) not null check (target_amount > 0),
  saved_amount numeric(14,2) not null default 0 check (saved_amount >= 0),
  deadline date,                                   -- opcional ("Poner fecha límite")
  position int not null default 0,                 -- orden manual de la lista
  completed_at timestamptz,                        -- null = activa; con fecha = completada
  created_at timestamptz not null default now()
);

-- "Ya tienes una meta con este nombre": ignora mayúsculas y espacios.
create unique index if not exists goals_unique_name
  on public.goals (user_id, lower(trim(name)));

alter table public.goals enable row level security;

drop policy if exists "own goals" on public.goals;
create policy "own goals" on public.goals
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ----------------------------------------------------------------------------
-- BLOQUE 2 — Tabla `goal_contributions` + RLS
-- `kind` distingue el ahorro con el que se creó la meta ('initial') de los
-- aportes posteriores ('manual'). Al borrar una meta se borran sus aportes.
-- La política exige además que la meta sea del mismo usuario (nadie puede
-- aportar a metas ajenas aunque conozca su id).
-- ----------------------------------------------------------------------------
create table if not exists public.goal_contributions (
  id uuid primary key default gen_random_uuid(),
  goal_id uuid not null references public.goals(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  amount numeric(14,2) not null check (amount > 0),
  contributed_on date not null default current_date,
  note text check (note is null or char_length(note) <= 80),
  kind text not null default 'manual' check (kind in ('initial', 'manual')),
  created_at timestamptz not null default now()
);

create index if not exists goal_contributions_by_goal
  on public.goal_contributions (goal_id, contributed_on desc, created_at desc);

alter table public.goal_contributions enable row level security;

drop policy if exists "own goal contributions" on public.goal_contributions;
create policy "own goal contributions" on public.goal_contributions
  for all
  using (auth.uid() = user_id)
  with check (
    auth.uid() = user_id
    and exists (select 1 from public.goals g where g.id = goal_id and g.user_id = auth.uid())
  );

-- ----------------------------------------------------------------------------
-- BLOQUE 3 — Lo ahorrado se recalcula solo
-- Cada vez que se agrega, cambia o borra un aporte, saved_amount de su meta
-- pasa a ser la suma de todos sus aportes.
-- ----------------------------------------------------------------------------
create or replace function public.recalc_goal_saved() returns trigger
language plpgsql set search_path = public as $$
declare
  target_goal uuid := coalesce(new.goal_id, old.goal_id);
begin
  update public.goals g
     set saved_amount = coalesce(
           (select sum(c.amount) from public.goal_contributions c where c.goal_id = target_goal), 0)
   where g.id = target_goal;
  return null;
end; $$;

drop trigger if exists goal_contributions_recalc on public.goal_contributions;
create trigger goal_contributions_recalc
  after insert or update or delete on public.goal_contributions
  for each row execute function public.recalc_goal_saved();

-- ----------------------------------------------------------------------------
-- BLOQUE 4 — Meta completada automáticamente
-- Si lo ahorrado alcanza el monto, la meta se marca completada (completed_at).
-- Si deja de alcanzarlo (por ejemplo, se sube el monto al editar), vuelve a activa.
-- ----------------------------------------------------------------------------
create or replace function public.set_goal_completed() returns trigger
language plpgsql set search_path = public as $$
begin
  if new.saved_amount >= new.target_amount then
    new.completed_at := coalesce(new.completed_at, now());
  else
    new.completed_at := null;
  end if;
  return new;
end; $$;

drop trigger if exists goals_set_completed on public.goals;
create trigger goals_set_completed
  before insert or update on public.goals
  for each row execute function public.set_goal_completed();

-- ----------------------------------------------------------------------------
-- VERIFICACIÓN (opcional, para mostrarle al profesor)
-- select name, target_amount, saved_amount, completed_at from public.goals;
-- select g.name, c.amount, c.contributed_on, c.kind, c.note
--   from public.goal_contributions c join public.goals g on g.id = c.goal_id
--   order by c.contributed_on desc;
-- ----------------------------------------------------------------------------
