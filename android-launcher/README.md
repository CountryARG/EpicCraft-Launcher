# EpicCraft Launcher — código de integración Android

Estado: implementación inicial SIN APK compilado y SIN prueba en dispositivo.
Este paquete contiene el código propio, controles y un preparador que obtiene el motor
Amethyst en una revisión fija. No es un proyecto Android autónomo hasta ejecutar prepare.py.
No incluye Minecraft ni los binarios de Java para Android.

## Implementado en el código

- Inicio con nombre local obligatorio y botón Iniciar sesión; Home.java separado.
- Tarjeta gris, avatar cuadrado de MCHeads y nombre local guardado.
- Consola de eventos de preparación y descarga, y lectura del último registro real del juego.
- JUGAR consulta AsyncVersionList del motor; selector dinámico con versiones locales y remotas.
- Cuadro de progreso conectado a los contadores de bytes del descargador, con ruta del archivo.
- El total del motor incluye archivos existentes verificados: la interfaz lo aclara.
- Si falta el tamaño, la barra queda indeterminada; no se inventa un porcentaje de bytes.
- Durante preparación/extracción se muestra el estado del motor sin porcentaje artificial.
- Lanzamiento mediante el flujo original LauncherActivity → MinecraftDownloader → MainActivity.
- Diseño de controles nativo: joystick abajo a la izquierda; Esc, Chat/T, inventario/E,
  cámara/F5, teclado, salto, agacharse, correr, minar y usar. Menú abre opciones del motor.
- Entrada táctil, multitáctil, mouse y teclado conservadas del motor.

## Límites importantes de esta entrega

No se compiló ni se verificó que Minecraft abra. Se comprobó la aplicación de los cambios sobre los archivos de la revisión fijada y la estructura de controles.
La prueba Java del cálculo de progreso se ejecutó con el módulo jdk.compiler: 7 comprobaciones correctas. Esto no verifica la app Android ni el motor.

El nombre local es la identidad del launcher; la cuenta de Minecraft se gestiona en la barra
superior. Amethyst exige una cuenta de Microsoft para descargar versiones nuevas y para
habilitar su flujo de alta de cuentas locales. Sus comprobaciones se mantienen intactas.
Escribir un nombre no autentica una cuenta de Minecraft ni garantiza una skin propia.
La API MCHeads recibe ese nombre para buscar un avatar público; si falla se conserva un icono.

El joystick mueve al personaje; la cámara se gira deslizando por el área del juego.
No se agregó giro automático de cámara al joystick. Los gestos heredados permiten minar
manteniendo pulsado y usar con un toque, orientándose con la mira; no reproducen exactamente
la selección directa de bloques de Bedrock. Hay botones Minar y Usar adicionales.

El motor ejecuta Minecraft en otro proceso y cierra la actividad del launcher al abrirlo.
Por eso la consola Home muestra descarga/preparación y permite leer el último registro;
los logs en vivo del juego se ven en la consola incorporada del motor, desde su menú.

La lista de versiones no garantiza compatibilidad con cada versión/GPU/mod. La instalación
 de Java adicional y los renderizadores se gestionan con el motor y sus ajustes.
El workflow obtiene JRE8 como el upstream; las versiones modernas requieren el runtime adecuado.
Los artefactos de JRE de terceros pueden caducar: si ese paso falla, revisar el workflow upstream.

## Preparar en Mac, Linux o Windows

Necesitás Git, Python 3, JDK 21 y Android Studio con SDK/NDK de la revisión fijada.
Desde la carpeta de este paquete:

```bash
python3 prepare.py --destination EpicCraft-Android
```

El script clona Amethyst, fija el commit 3ad1100904fef8e3aaa7f50f1b6a05cef918b29c,
obtiene sus submódulos y aplica los cambios. Se niega a sobrescribir una carpeta existente.
Abrí EpicCraft-Android en Android Studio.

Dependencias de la base: Android SDK 37, NDK 27.3.13750724, Gradle 9.6.1, AGP 9.3.1.
También requiere los binarios JRE8 que descarga el flujo incluido. Android Studio por sí solo
no sustituye ese paso. Consultá el workflow de la base para sus artefactos y estructura.

## Compilar mediante GitHub Actions

1. Colocá el contenido de esta carpeta en la raíz de un repositorio tuyo, incluyendo .github.
2. Abrí Actions → Compilar EpicCraft APK → Run workflow.
3. Si todos los pasos terminan correctamente, descargá el artefacto EpicCraft-APK.
4. Descomprimilo: contendrá el APK de prueba firmado por la configuración debug del motor.

El flujo está preparado, pero NO fue ejecutado desde esta conversación.
No requiere claves de publicación. No debe distribuirse un APK debug como una versión final.
Para publicar se necesita una clave propia y conservar los avisos/licencias de las dependencias.

## Verificación pendiente en Android

- Instalar y abrir; rechazar usuario vacío y validar entrada por teclado.
- Consultar catálogo en línea y probar error de red sin mostrar progreso inventado.
- Entrar con una cuenta autorizada y descargar una versión compatible.
- Comprobar barra, ruta, verificación y apertura efectiva de Minecraft.
- Descargar de nuevo y confirmar reutilización de archivos verificados.
- Probar mover + saltar + mirar simultáneamente y soltar todas las entradas al perder foco.
- Probar chat, Esc, gestos, mouse, teclado y retorno tras cerrar Minecraft.
- Probar rotación, minimizar durante descarga, memoria baja y error de red a mitad de descarga.

## Créditos y licencia

Motor: Amethyst Android (derivado de PojavLauncher), LGPL-3.0.
https://github.com/AngelAuraMC/Amethyst-Android
Origen fijado: https://github.com/AngelAuraMC/Amethyst-Android/tree/3ad1100904fef8e3aaa7f50f1b6a05cef918b29c
Los cambios de integración de este paquete se proporcionan bajo LGPL-3.0.
Conservar LICENSE y las licencias del motor y sus componentes al redistribuir.
Avatares: https://mc-heads.net/
Minecraft es de sus respectivos titulares; EpicCraft es un proyecto no oficial.
