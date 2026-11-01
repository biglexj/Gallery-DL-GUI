# Diagnóstico de resolución — colección Kanokari

Fecha: 2026-10-26. Motor probado: bin/gallery-dl.exe, versión 1.31.7.
Origen: https://www.deviantart.com/sdugoten/gallery/86558154/kanokari
Carpeta analizada: D:\Imágenes\Descargas\Kanokari.

## Resultado

Los 35 archivos tienen 1280 píxeles de ancho. La mayoría son 1280 × 720; también hay imágenes verticales y otras proporciones. No se verificó la resolución de los originales remotos porque el acceso probado devuelve un límite de descargas.

La GUI fuerza `original=false` en el JSON y en dos argumentos de máxima prioridad. Esta decisión previa evita solicitar los originales de DeviantArt y utiliza la imagen de visualización. No hay un redimensionado posterior en el servicio de descarga: la URL seleccionada ya contiene `w_1280`.

## Pruebas realizadas

| Prueba | Resultado |
| --- | --- |
| Leer dimensiones de los 35 archivos locales | Todos tienen ancho 1280; inventario en temp/resolution-diagnostic/local-dimensions.csv. |
| Motor sin configuración externa, original=false, primer elemento | Selecciona 1280 × 715, JPEG q_100. |
| Motor sin configuración externa, configuración predeterminada, primer elemento | Solicita original; metadatos devuelven `Free download limit reached.` y conserva URL de 1280 × 715. |
| Motor sin configuración externa, original=true, primer elemento | Mismo error remoto y resolución reducida. |
| Motor con original=true, elemento vertical 949919267 | Mismo error remoto; conserva 1280 × 1810. |
| Descarga real con original=true, primer elemento | Termina con código 0 y guarda 1280 × 715, 159968 bytes. |
| Descarga real con original=false, primer elemento | SHA-256 idéntico al archivo existente: 086337F2CFA60CE7B4CEA855C529FBD9E810FC01F044FE85A92FD9A05BB307C3. |

Las descargas de prueba se guardaron en temp/resolution-diagnostic, sin reemplazar los archivos de la colección ni modificar el historial o la configuración de usuario.

## Interpretación

Existen dos problemas distintos: la GUI desactiva globalmente los originales de DeviantArt, y el motor probado puede ocultar el rechazo del servidor guardando una imagen reducida con salida exitosa. En 1.31.7, `_update_content_default` mezcla la respuesta de la API con los metadatos existentes y marca `is_original=true` incluso en la respuesta observada con error. Por ello, ese indicador y el código de salida no prueban que se haya obtenido el original.

`quality=100` controla la compresión JPEG, no la resolución. Eliminar únicamente `original=false` no resuelve este caso en el acceso público probado. El mensaje remoto no demuestra que se haya agotado la cuota de la cuenta personal del usuario: estas pruebas no utilizaron sus cookies ni un token privado.

## Siguiente corrección propuesta

Solicitar la mejor calidad disponible y hacer visible cuándo el proveedor entrega una versión reducida o rechaza el original. Validar el comportamiento de una versión posterior del motor antes de reemplazar el binario. No afirmar que una descarga es original solo por `is_original` o por salida 0. Cualquier comprobación con una cuenta debe respetar sus límites de descarga.

## Alcance de esta sesión

Diagnóstico y pruebas reales. No se modificó el código de la aplicación, no se actualizó el motor y no se publicó ni instaló una versión.

## Fuentes

- Implementación local: composeApp/src/desktopMain/kotlin/com/biglexj/gallerydl/platform/DesktopDownloadService.kt.
- Código del motor probado: https://github.com/mikf/gallery-dl/blob/v1.31.7/gallery_dl/extractor/deviantart.py.
- Configuración oficial actual (sus valores predeterminados pueden diferir de 1.31.7): https://gdl-org.github.io/docs/configuration.html#extractor-deviantart-original.
- Evidencias temporales: temp/resolution-diagnostic/ (JSON de respuestas, registros, descargas y dimensiones).

## Reprueba directa solicitada por Biglex

Se ejecutó el motor con su configuración habitual, sin `--config-ignore`, sin JSON de la GUI y sin opciones `original`:

```powershell
.\bin\gallery-dl.exe --range 1-2 -D temp/resolution-diagnostic/cli-default --verbose 'https://www.deviantart.com/sdugoten/gallery/86558154/kanokari'
```

El registro indica versión 1.31.7 y `Configuration Files []`. Los dos archivos guardados miden 1280 × 715 (159968 bytes) y 1280 × 720 (162250 bytes). El servidor devuelve HTTP 400 con `Free download limit reached.` al solicitar los originales, pero el proceso termina con salida 0. Evidencia: temp/resolution-diagnostic/cli-default.log y cli-default/.

Existe además un reporte público del mismo rechazo de DeviantArt en gallery-dl, abierto el 18 de mayo de 2026: https://github.com/mikf/gallery-dl/issues/9537. La documentación actual del motor recoge límites semanales del proveedor. No se determinó cuándo se introdujo el cambio ni si afectó a las descargas anteriores de Biglex, ni se verificó la cuota de su cuenta personal.

## Prueba del ZIP Python 1.32.18 aportado por Biglex

Se extrajo gallery-dl-1.32.18.zip en temp/resolution-diagnostic/source-1.32.18. Se ejecutó con Python 3.12 del runtime disponible y se instaló requests con sus dependencias únicamente en temp/resolution-diagnostic/python-deps. No se reemplazó bin/gallery-dl.exe ni se modificó la GUI.

Pruebas reales sobre los dos primeros elementos de la colección, sin configuración externa ni sesión personal:

| Modo de 1.32.18 | Resultado |
| --- | --- |
| Predeterminado | Guarda 1280 × 715 (867681 bytes) y 1280 × 720 (827282 bytes). Salida 0. |
| original=true | La API devuelve `Free download limit reached.`; guarda 1280 × 715 (159968 bytes) y 1280 × 720 (162250 bytes). Salida 0. |

El cambio de bytes entre modos corresponde a URLs JPEG con distinta calidad, sin mejora de resolución. La versión nueva tampoco resuelve la obtención de originales en el acceso probado.

El CHANGELOG incluido en el ZIP sitúa en 1.31.10, con fecha 2026-03-19, la desactivación por defecto de originales de DeviantArt. Confirmado también en el código de 1.32.18: `self.original = self.config("original", False)`.

Ejecutor local reutilizable: temp/resolution-diagnostic/Run-GalleryDl-1.32.18.ps1. Acepta argumentos del motor; comprobar con `--version`. El código y las dependencias deben permanecer en estas carpetas temporales para usarlo. Los registros de las pruebas son python-1.32.18-default.log y python-1.32.18-original.log.
