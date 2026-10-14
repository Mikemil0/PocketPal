-- ============================================================================
-- Módulo 2 · Fase 2 — Base de datos (SDD_Modulo2.md §6)
-- Ejecutar en el SQL Editor de Supabase, bloque por bloque, en orden.
-- Cada bloque es autónomo y seguro de ejecutar por separado.
-- El SQL Editor usa la clave de servicio: las políticas RLS no afectan al editor,
-- solo a la app (claves anon/authenticated). Verificación en §10 del chat/SDD.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- BLOQUE 1 — Tabla `profiles` + RLS
-- Perfil 1 a 1 con auth.users: nombre, país, moneda, idioma, formato de fecha,
-- inicio de semana, tema y avatar. RLS + política "own profile": cada usuario
-- solo lee y escribe SU fila (auth.uid() = id). Sin RLS cualquier cliente
-- autenticado leería todos los perfiles.
-- ----------------------------------------------------------------------------
create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null default '',
  country text not null default 'CO',              -- ISO 3166-1 alpha-2
  currency text not null default 'COP',            -- ISO 4217
  language text not null default 'es' check (language in ('es','en','pt','fr')),
  date_format text not null default 'DD/MM/YYYY'
    check (date_format in ('DD/MM/YYYY','MM/DD/YYYY','YYYY-MM-DD')),
  week_start smallint not null default 0 check (week_start between 0 and 6), -- 0 = domingo
  theme text not null default 'system' check (theme in ('light','dark','system')),
  avatar_path text,
  password_changed_at timestamptz,
  updated_at timestamptz not null default now()
);

alter table public.profiles enable row level security;

drop policy if exists "own profile" on public.profiles;
create policy "own profile" on public.profiles
  for all using (auth.uid() = id) with check (auth.uid() = id);

-- ----------------------------------------------------------------------------
-- BLOQUE 2 — Tabla `categories` + RLS + índice único (RN-02)
-- Categorías de gastos/ingresos por usuario. El índice único por (usuario, tipo,
-- nombre sin mayúsculas ni espacios) es la autoridad detrás del mensaje
-- "Ya tienes una categoría con este nombre". Política "own categories": cada
-- usuario solo ve/edita/borra las suyas.
-- ----------------------------------------------------------------------------
create table public.categories (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  kind text not null check (kind in ('expense','income')),
  name text not null check (char_length(trim(name)) between 1 and 30),
  icon text not null,
  color text not null,
  monthly_limit numeric(14,2) check (monthly_limit is null or monthly_limit > 0),
  position int not null default 0,
  created_at timestamptz not null default now()
);

create unique index categories_unique_name
  on public.categories (user_id, kind, lower(trim(name)));

alter table public.categories enable row level security;

drop policy if exists "own categories" on public.categories;
create policy "own categories" on public.categories
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ----------------------------------------------------------------------------
-- BLOQUE 3 — Tabla `notification_settings` + RLS
-- Preferencias de notificación por usuario (una fila por cuenta). El umbral por
-- defecto es 80 (RN-08, el valor que muestra el Figma). Política "own
-- notification settings": solo tu fila es visible o modificable por ti.
-- ----------------------------------------------------------------------------
create table public.notification_settings (
  user_id uuid primary key references auth.users(id) on delete cascade,
  daily_reminder boolean not null default false,
  weekly_summary boolean not null default false,
  budget_alert boolean not null default false,
  budget_threshold smallint not null default 80 check (budget_threshold between 50 and 100),
  goals_progress boolean not null default false,
  channel_email boolean not null default true,
  channel_sms boolean not null default false
);

alter table public.notification_settings enable row level security;

drop policy if exists "own notification settings" on public.notification_settings;
create policy "own notification settings" on public.notification_settings
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ----------------------------------------------------------------------------
-- BLOQUE 4 — Tabla `user_devices` + RLS (sesiones activas, D-01 opción B)
-- Una fila por dispositivo con sesión: nombre, ubicación aproximada y último uso.
-- Se llena desde la app en la Fase 4. Política "own devices": solo tu lista de
-- dispositivos es visible o modificable por ti.
-- ----------------------------------------------------------------------------
create table public.user_devices (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  session_id uuid,                                  -- claim session_id del JWT
  device_name text not null,
  platform text not null default 'android',
  approx_location text,
  last_active_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  revoked_at timestamptz
);

alter table public.user_devices enable row level security;

drop policy if exists "own devices" on public.user_devices;
create policy "own devices" on public.user_devices
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ----------------------------------------------------------------------------
-- BLOQUE 5 — Función + trigger `handle_new_user`
-- Cada vez que se crea una cuenta, crea su fila en `profiles` y su fila en
-- `notification_settings` (evita filas huérfanas). `security definer` porque el
-- trigger escribe en nombre del sistema y debe poder hacerlo pese a las políticas.
-- El `drop trigger if exists` hace el bloque repetible.
-- ----------------------------------------------------------------------------
create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id, full_name)
  values (new.id, coalesce(new.raw_user_meta_data->>'full_name', new.raw_user_meta_data->>'name', ''));
  insert into public.notification_settings (user_id) values (new.id);
  return new;
end; $$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users for each row execute function public.handle_new_user();

-- ----------------------------------------------------------------------------
-- BLOQUE 6 — Backfill de las cuentas existentes (Módulo 1)
-- Tus cuentas de prueba se crearon ANTES del trigger, así que no tienen fila en
-- `profiles`/`notification_settings`; este bloque les crea una. Idempotente
-- (`on conflict do nothing`): repetirlo no duplica nada.
-- ----------------------------------------------------------------------------
insert into public.profiles (id, full_name)
  select id, coalesce(raw_user_meta_data->>'full_name', raw_user_meta_data->>'name', '')
  from auth.users on conflict do nothing;

insert into public.notification_settings (user_id)
  select id from auth.users on conflict do nothing;

-- ----------------------------------------------------------------------------
-- BLOQUE 7 — Storage: bucket `avatars` + límites + 4 políticas por carpeta (D-09)
-- Bucket PÚBLICO: la foto se sirve por URL pública (simple y suficiente; la
-- seguridad está en que solo puedes escribir/borrar en TU carpeta
-- `avatars/{user_id}/`, comprobado con storage.foldername()).
-- La lectura pública no NECESITA la política select (el bucket es público), pero se
-- declara porque coincide con las 4 políticas del panel y porque la operación de
-- borrado (remove) puede necesitar SELECT para localizar el objeto antes de
-- eliminarlo. La select usa (SELECT auth.uid())::text, como en el panel.
-- Límites de defensa en profundidad (RNF-04): la app sube JPEG de ~512 px comprimido,
-- muy por debajo de 1 MB, y el bucket es solo para avatares, nunca para otros tipos.
-- ----------------------------------------------------------------------------
insert into storage.buckets (id, name, public) values ('avatars', 'avatars', true)
  on conflict do nothing;

-- Máximo 1 MB por archivo y solo imágenes (JPEG/PNG/WebP).
update storage.buckets
  set file_size_limit = 1048576,
      allowed_mime_types = array['image/jpeg', 'image/png', 'image/webp']
  where id = 'avatars';

drop policy if exists "avatar insert own folder" on storage.objects;
create policy "avatar insert own folder" on storage.objects for insert to authenticated
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "avatar update own folder" on storage.objects;
create policy "avatar update own folder" on storage.objects for update to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "avatar delete own folder" on storage.objects;
create policy "avatar delete own folder" on storage.objects for delete to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "avatar select own folder" on storage.objects;
create policy "avatar select own folder" on storage.objects for select to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = (SELECT auth.uid())::text);
