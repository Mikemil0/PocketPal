# PocketPal

App Android nativa (Kotlin + Jetpack Compose + Material 3 + Supabase) para control de gastos personales.

## Estructura del repositorio

```
PocketPall/                     ← raíz del repositorio (este README)
└── PocketPal01/Pocketpal/      ← proyecto de Android Studio (Gradle)
    ├── local.properties            ← claves REALES (ignorado por Git)
    ├── local.properties.example    ← plantilla versionada: copia y rellena
    └── app/                        ← módulo único, paquete com.pocketpal.app
```

## Requisitos

- Windows con **Android Studio** instalado (trae el JDK necesario: `C:\Program Files\Android\Android Studio\jbr`).
- Un dispositivo o emulador (el proyecto se probó con emulador **Pixel 6, API 37.1**).
- Una cuenta de **Supabase** y un proyecto en **Google Cloud** (ver "Servicios externos" abajo).

## Servicios externos (Supabase y Google Cloud)

**Supabase** (https://supabase.com → New project):
1. En la app solo se usan la **URL del proyecto** y la clave **publishable/anon** (nunca la `service_role`).
2. Authentication → Providers → **Email**: dejar "Confirm email" **activado**.
3. Authentication → Email Templates → **Confirm signup** y **Reset Password**: el cuerpo debe usar `{{ .Token }}` (llega como código de 6 dígitos) en lugar de `{{ .ConfirmationURL }}` (la app verifica OTP, no enlaces).
4. Authentication → Providers → **Google**: activarlo con el **Client ID de tipo Web** y su secreto de Google Cloud.

**Google Cloud** (https://console.cloud.google.com):
1. Crear un proyecto y una **pantalla de consentimiento OAuth** de tipo **Externo**; agregar los correos de prueba como **Test users**.
2. Credentials → Create credentials → **OAuth client ID** de tipo **Android**: package `com.pocketpal.app` + el SHA-1 de depuración (se obtiene con `keytool` o desde Android Studio → Gradle → `signingReport`).
3. Credentials → Create credentials → **OAuth client ID** de tipo **Web**: ese Client ID es el que usa la app (`GOOGLE_WEB_CLIENT_ID` en `local.properties`), no el de Android.

## Configuración (obligatoria para compilar)

1. Abre la carpeta **`PocketPal01/Pocketpal`** con Android Studio (File → Open) y deja que sincronice Gradle.
2. En el explorador de Windows, entra a `PocketPal01\Pocketpal`, **copia `local.properties.example` y renómbrala a `local.properties`** (si `local.properties` ya existe por Android Studio, solo agrega las líneas que faltan).
3. Edita `local.properties` y rellena los tres valores obligatorios:

   | Clave | Dónde sale |
   |---|---|
   | `SUPABASE_URL` | Supabase → Project Settings → API → URL (`https://TU-PROYECTO.supabase.co`) |
   | `SUPABASE_KEY` | Supabase → Project Settings → API → clave **publishable/anon** (`sb_publishable_...`). **Nunca** uses la `service_role`. |
   | `GOOGLE_WEB_CLIENT_ID` | Google Cloud Console → Credentials → cliente de tipo **Web** (no el de Android) |

4. Vuelve a Android Studio y haz **Sync Now**.

Si falta alguna clave obligatoria, la app compila pero **falla al iniciar** y el mensaje del crash indica exactamente cuál falta (`SupabaseProvider.kt`).

> **Importante:** `local.properties` está en `.gitignore`. NUNCA lo subas al repositorio ni compartas sus valores. Tampoco subas keystores (`*.jks`).

## Compilar y ejecutar

Por terminal (desde `PocketPal01/Pocketpal`). Si `gradlew` falla con "JAVA_HOME is not set", primero:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
```

```powershell
cd PocketPal01\Pocketpal
.\gradlew.bat :app:assembleDebug     # genera el APK de depuración
.\gradlew.bat :app:testDebugUnitTest # pruebas unitarias
.\gradlew.bat :app:lintDebug         # análisis estático
```

O simplemente el botón **Run ▶** de Android Studio con el emulador o el celular conectado.

> El proyecto usa configuration cache de Gradle: si editas `local.properties`, el próximo build lo detecta y regenera `BuildConfig` automáticamente (verificado). Ante cualquier síntoma raro, `--no-configuration-cache` siempre funciona.

## Estado del proyecto

- **Módulo 1 (implementado):** registro, verificación de correo con código OTP, inicio de sesión con correo y con Google, recuperación de contraseña, tutorial de bienvenida, solicitud de permisos (notificaciones, cámara, micrófono) y Home con cierre de sesión.
- **Módulo 2 (en implementación por fases):** secretos fuera del código (esta configuración), internacionalización en 4 idiomas (español, inglés, portugués y francés), splash animado, área de perfil (datos personales, foto, teléfono con SMS, seguridad y sesiones activas, notificaciones, categorías de gastos e ingresos, preferencias, exportar datos, ayuda y acerca de) y eliminación de cuenta.
