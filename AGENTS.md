# AGENTS.md

Guía corta para quien (persona o asistente de IA) vaya a modificar el código de PocketPal. La descripción completa del proyecto está en el [README](README.md).

## Dónde está cada cosa

- El proyecto de Android Studio está en **`PocketPal01/Pocketpal/`**. Los comandos de Gradle se corren desde ahí, no desde la raíz.
- Hay un solo módulo, `:app`, con el paquete `com.pocketpal.app`.
- El esquema de la base de datos está en `supabase/migrations/`: un `.sql` por módulo, con sus políticas RLS.

## Comandos

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'   # si gradlew no encuentra Java
cd PocketPal01\Pocketpal
.\gradlew.bat :app:testDebugUnitTest    # pruebas unitarias
.\gradlew.bat :app:assembleDebug        # APK de depuración
.\gradlew.bat :app:lintDebug            # análisis estático
```

## Reglas del proyecto

- **MVVM y flujo unidireccional.**
  - Las pantallas son *stateless*: reciben `state` y lambdas.
  - El estado vive en el ViewModel (`StateFlow`).
  - El grafo de navegación del módulo es el componente padre que conecta los dos.
- **Navegación.**
  - Las rutas están en `navigation/Routes.kt`.
  - Cada módulo tiene su grafo (`AuthNavGraph`, `HomeNavGraph`, `ProfileNavGraph`, `GoalsNavGraph`, `MovementsNavGraph`, `BudgetsNavGraph`, `DaiNavGraph`, `NewMovementNavGraph`).
  - `PocketNavHost` solo los junta y decide la navegación según el estado de la sesión.
- **Datos.**
  - Los ViewModels hablan con **interfaces** de repositorio. La implementación con Supabase se crea en `di/AppContainer.kt`.
  - Las reglas de negocio van en objetos de Kotlin puro, sin Android, por ejemplo `BudgetRules` o `ReceiptParser`, para poder probarlas con JUnit.
- **Textos.**
  - Nada de texto visible escrito en el código: todo va en `res/values*/strings*.xml`, en **4 idiomas** (es, en, pt, fr).
  - Los ViewModels emiten `ErrorType`, nunca frases.
- **Secretos.**
  - Las llaves solo van en `local.properties`, que está en `.gitignore`.
  - Jamás se usa la `service_role` en la app.
- **Dai nunca guarda solo.** Todo lo que viene de la IA (OCR o voz) pasa por una pantalla de confirmación antes de escribir en la base.
- **Diseño.** Si el código y el Figma no coinciden, manda el Figma. Hay que reutilizar los colores (`ui/theme`) y los componentes (`ui/components`).
- **Pruebas.** Todo ViewModel o regla nueva lleva su prueba en `app/src/test`, con un repositorio falso (`Fake…Repository`).
