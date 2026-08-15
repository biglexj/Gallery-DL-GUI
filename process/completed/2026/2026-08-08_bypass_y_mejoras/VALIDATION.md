# Aplanamiento de directorios y mejoras de descarga — Validación

- Estado: `PENDING_FUNCTIONAL_VALIDATION`

## Comprobaciones

- [x] V01 — Agente — Compilación del proyecto sin errores mediante `./gradlew :composeApp:compileKotlinDesktop`. Esperado: Build exitosa.
- [ ] V02 — Tester — Descargar una galería pública. Esperado: descarga correcta mediante el acceso público predeterminado.
- [ ] V03 — Tester — Probar "Solo Carpeta de Colección" y pulsar "Abrir". Esperado: creación y apertura de la carpeta real de la colección bajo el directorio elegido.
- [x] V04 — Agente — Verificar que la interfaz se mantiene responsiva durante la descarga y que no hay bloqueos de UI (ANR). Esperado: La app responde a interacciones y los logs fluyen libremente.
- [x] V05 — Agente — Probar la carga de miniaturas en el historial. Esperado: Al finalizar la descarga, la primera imagen se muestra como miniatura en el panel del historial.
- [x] V06 — Agente — Comprobar redirección al hacer clic en el enlace del historial. Esperado: Abre el navegador nativo del sistema con la URL correspondiente.
- [x] V07 — Agente — Comprobar creación y eliminación limpia del JSON de configuración en el directorio temporal del sistema. Esperado: no depende de permisos de escritura en la carpeta de instalación y se elimina al finalizar o cancelar.
- [x] V08 — Agente — Verificar comportamiento de la "Carpeta de Guardado" al hacer clic. Esperado: no muestra cursor de selección de texto ni menús contextuales de copiar/pegar.
- [x] V09 — Agente — Comprobar el tema del menú contextual al hacer clic derecho en la barra de URL. Esperado: Muestra el menú de Copiar/Pegar/Cortar con colores y fondo oscuros que combinan con el tema de la aplicación.
- [x] V10 — Agente — Comprobar la opción "Autor / Colección". Esperado: Al descargar una galería con esta opción, se crea la estructura "Usuario/Nombre de Colección" bajo la ruta elegida.
- [x] V13 — Agente — Ejecutar `:composeApp:desktopTest` y `:composeApp:compileKotlinDesktop` tras simplificar el flujo público. Resultado: compilación exitosa y 5 pruebas aprobadas, 0 fallos.
- [x] V14 — Agente — Verificar la configuración generada. Resultado: flujo público, `original=false` para evitar WixMP 400 y estructura de colección preservada.
- [ ] V16 — Biglex — Verificar que Descargar permanezca desactivado sin URL y se active al escribir una URL válida.

## Limitación de esta validación

Biglex realizará las pruebas funcionales finales. Por ello, V02, V03 y V16 quedan pendientes antes de cerrar el proceso.
