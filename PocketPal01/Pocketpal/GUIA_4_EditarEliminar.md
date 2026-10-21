# Paquete 4 · Flujo "Editar y eliminar meta"

**Commit sugerido:** `Módulo 3 · Flujo 4: editar y eliminar meta con confirmación`

**Requiere:** paquetes 0 a 3 ya subidos (`git pull` antes). Es el último.

## Qué hace

- En el detalle de la meta aparece el **lápiz ✏️** arriba a la derecha.
- Abre el **mismo formulario** de crear, pero prellenado: título **"Editar meta"** y botón
  **"Guardar cambios"**. Se puede cambiar nombre, monto, ícono, color y fecha límite
  (o quitarla).
- No aparece "Ya tengo ahorrado" (lo ahorrado solo cambia con aportes); en su lugar se
  muestra "Llevas ahorrado $X. El monto no puede ser menor."
- Al final, **"Eliminar meta"** (rojo). Pide confirmación en una hoja inferior:
  **"¿Eliminar "Viaje"? Se borrarán también sus N aportes. No podrás recuperarla."**
  Al confirmar, vuelve a la lista.

## Recorrido de los datos

```
lápiz ✏️ ──prepareEdit(id)──▶ GoalFormViewModel ──get(id) + contributions(id)──▶ prellena el formulario
Guardar cambios ──update(draft)──▶ GoalRules.validateGoal(..., excludeId = id, currentSaved = ahorrado)
                                   repository.update(id, draft)  → update goals set name, icon, color, target_amount, deadline
                                   (trigger set_goal_completed: si el monto bajó hasta lo ahorrado, queda completada;
                                    si subió, vuelve a activa)
                                   Event.Updated → vuelve al detalle, que se refresca
Eliminar ──delete()──▶ repository.delete(id) → delete from goals where id = …
                                               (on delete cascade borra sus goal_contributions)
                       Event.Deleted → vuelve a la lista
```

## Archivos

| Archivo | Nuevo/Modificado | Qué hace |
|---|---|---|
| `ui/goals/GoalFormViewModel.kt` | Modificado | Agrega el modo edición: `editing`, `contributionsCount`, `prepareEdit`, `update`, `delete` y los eventos `Updated` / `Deleted`. Al editar, la meta no cuenta como "repetida de sí misma". |
| `ui/goals/GoalFormScreen.kt` | Modificado | Si recibe `editing`, se prellena, cambia títulos, oculta "Ya tengo ahorrado" y muestra "Eliminar meta" + `ConfirmSheet` (la misma hoja de "¿Cerrar sesión?" del módulo 2). |
| `navigation/PocketNavHost.kt` | Modificado | Ruta `goals/detail/{id}/edit`; el lápiz del detalle la abre. |
| `res/values*/strings_goals_edit.xml` | Nuevo | Textos en 4 idiomas (incluye plural "1 aporte / N aportes"). |
| `test/.../GoalEditViewModelTest.kt` | Nuevo | 10 pruebas. |

## Cómo probar

1. Detalle de "Viaje" ($1.000.000, $500.000 ahorrados) → ✏️.
2. Cambiar el nombre a "Viaje a Cartagena" y el monto a $2.000.000 → Guardar cambios.
   Supabase → `goals`: cambió `name` y `target_amount`; `saved_amount` sigue igual.
3. Intentar monto $400.000 → "El monto no puede ser menor a lo que ya ahorraste".
4. Poner el nombre de otra meta → aviso de nombre repetido.
5. "Eliminar meta" → hoja con el número de aportes → Eliminar.
   Supabase: desaparece la fila de `goals` **y** sus filas de `goal_contributions`.
6. Pruebas: `GoalEditViewModelTest` → 10 en verde.

## Preguntas que puede hacer el profesor

- **¿Por qué reutilizar el formulario de crear?** Mismos campos y validaciones; se evita
  duplicar código. Solo cambia el modo (`editing != null`).
- **¿Por qué no se puede editar lo ahorrado?** Porque es la suma de los aportes (trigger).
  Si se pudiera escribir, el total dejaría de cuadrar con el historial.
- **¿Cómo se borran los aportes?** No los borra la app: la llave foránea tiene
  `on delete cascade`, la base los borra al borrar la meta.
- **¿Por qué pide confirmación?** Es una acción que no se puede deshacer; la hoja dice
  cuántos aportes se pierden.
- **¿Qué pasa si se edita el monto justo a lo ahorrado?** El trigger la marca completada
  y pasa a la pestaña Completadas.
