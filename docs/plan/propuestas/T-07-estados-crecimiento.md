# T-07 · Propuesta de textos y presentación de los estados

Propuesta del 29/10/2026 para el issue #61. **Pendiente de aprobación de producto y de
validación médica PVM-10.** Los mensajes son provisorios: no deben interpretarse como
reglas de cálculo ni como una evaluación de las mediciones existentes.

## Alcance de esta propuesta

- Preparar los textos para tutores de RN-CRE-10 y revisar cómo se leerían en el diseño actual.
- Reutilizar `PsCard`, `PsBadge`, `PsTheme`, `PsType` y los tonos existentes, con ícono y texto.
- Probar los tres ejemplos en una pantalla exclusiva de debug, identificada como demostración.
- Mantener la pantalla real de Crecimiento y su aviso de evaluación pendiente.

Los ejemplos visuales fueron solicitados para revisar esta propuesta. No implementan las
tareas futuras de cálculo I2-S1/I2-S2 ni integran resultados en I2-A2/I2-A4. No se agregan
reglas médicas, referencias, endpoints, migraciones, persistencia ni notificaciones.

## Textos para validar

| Estado definido por la SPEC | Texto provisional | Presentación propuesta |
|---|---|---|
| Dentro de lo esperado | El resultado de este indicador está dentro de los criterios de referencia. Podés ver su evolución en el historial. | `PsBadgeTone.Success`, ícono de verificación y nombre completo |
| A controlar | La evolución de este indicador requiere seguimiento. Revisá el historial y compartilo con tu pediatra. | `PsBadgeTone.Warning`, ícono informativo y nombre completo |
| Consultar con el pediatra | Este resultado requiere una revisión con tu pediatra. Podés compartirle las mediciones registradas. | `PsBadgeTone.Error`, ícono de consulta y nombre completo |

Los tres muestran el recordatorio obligatorio, sin modificar su texto:

> Este estado orienta, no diagnostica. Ante cualquier duda, consultá con tu pediatra.

La propuesta muestra el indicador y la fecha de la medición en que se basa el estado.
No usa un diagnóstico ni presenta un color como única forma de identificar el estado.
El mensaje de consulta usa por ahora la forma genérica; la personalización con el nombre del
pediatra vinculado corresponde a la futura integración de RF-CRE-07.

## Ejemplos y comportamiento esperado

- La demostración usa una fecha fija y datos de ejemplo; nunca lee la sesión ni un perfil infantil.
- Permite alternar los tres estados para comparar textos y presentación.
- La selección del ejemplo sobrevive a una rotación. El contenido puede desplazarse con texto grande.
- Con texto ampliado, el selector usa los `PsButton` existentes en columna para evitar etiquetas
  apretadas en el control segmentado. No se modifica el componente compartido.
- Volver cierra la demostración y conserva la sesión y la navegación habitual de Pasitos.
- En las mediciones reales se mantiene el aviso de cálculo pendiente: no hay un estado por defecto.
- El texto propuesto no decide umbrales, cruces ni plausibilidad. Esos criterios requieren el motor
  y las validaciones previstas por T-03/I2-S2.

## Pendientes antes de integrar

- [ ] Validar con el usuario la redacción, el lugar de presentación y la apariencia.
- [ ] Validar con pediatras la claridad y el tono de los mensajes (PVM-10).
- [ ] Confirmar los textos finales y su carga por el administrador, según RN-CRE-10/RF-ADM-11.
- [ ] Planificar la integración con el motor en I2, contemplando ausencia de datos o referencias,
  mediciones no evaluables y datos antiguos según RF-CRE-07.

El PR de propuesta referencia #61 y permanece en borrador; no cierra la validación médica.

## Cómo revisar el prototipo

El código del prototipo está únicamente en `app/app/src/debug/`. No cambia los componentes
compartidos ni los archivos de `src/main`, y el manifest de release no registra esta actividad.
La actividad debug está exportada para abrirla durante la revisión con adb; solo muestra
ejemplos estáticos, ignora los datos del intent y no consulta información del usuario.

Desde la carpeta `app/`, compilar `./gradlew assembleDebug` e instalar el APK de debug en un
emulador de prueba. Después abrir:

```sh
adb shell am start -n com.pasitos.app/.feature.growthchart.proposal.GrowthStatusProposalActivity
```

Usar el selector para comparar los tres mensajes. Probar también rotación, texto grande y
Volver. La demostración no requiere iniciar sesión ni conectarse al backend.

## Referencias

- [Plan transversal T-07](../transversales.md).
- [RF-CRE-07 y RN-CRE-10](../../spec/12-crecimiento.md).
- [PVM-10](../../spec/92-pendientes.md).
- [Plan preliminar de I2](../I2-a-I6-preliminar.md).
