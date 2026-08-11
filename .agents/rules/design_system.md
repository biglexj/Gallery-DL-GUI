# 🎨 Regla de Lenguaje de Diseño — Material Expressive System

> [!CAUTION]
> Esta regla es **CRÍTICA y no negociable** para todos los proyectos con interfaz gráfica (Compose Multiplatform, Android, Web). El agente DEBE utilizar el sistema de diseño **Material 3 Expressive (Material Expressive)** en lugar de Material UI clásico o componentes Material 3 obsoletos.

## Principios de Material Expressive

1. **Expresividad Visual & Tipografía Dinámica**:
   - Usar tipografías modernas (ej. Inter, Outfit, Roboto Flex) y contrastes expresivos en encabezados y títulos.
   - Jerarquías tipográficas claras sin fuentes genéricas o por defecto del navegador.

2. **Componentes Expresivos**:
   - Botones con bordes redondeados adaptativos, micro-animaciones suaves y estados de presión/hover interactivos.
   - Tarjetas (Cards) con bordes suaves, contenedores elevados con tonos tonales HSL / Material You armónicos.
   - Selectores y diálogos modulares con transiciones fluidas.

3. **Interacciones y Notificaciones**:
   - Banners de notificación e indicadores de estado no intrusivos integrados en la UI.
   - Indicadores de carga expresivos (barras de progreso lineales dinámicas y spinners adaptativos).

4. **Dimensionamiento de Diálogos y Modales en Móvil [OBLIGATORIO]**:
   - En Android y dispositivos móviles, los diálogos modales DEBEN configurarse con `DialogProperties(usePlatformDefaultWidth = false)` y `fillMaxWidth(0.80f)` (ancho exacto del 80% de pantalla).
   - Se prohíbe el uso de anchos angostos por defecto que causen compresión o saltos de línea no deseados en botones de acción.

5. **Prohibición de Estilos Desactualizados**:
   - **Prohibido** utilizar estilos planos de Material UI desfasados o layouts rígidos sin elevación ni padding dinámico.
   - **Prohibido** utilizar colores primarios planos y rígidos (rojo puro #FF0000, azul puro #0000FF). Usar paletas tonales derivadas de Material 3 Expressive (`primaryContainer`, `onPrimaryContainer`, `surfaceContainerHighest`).
