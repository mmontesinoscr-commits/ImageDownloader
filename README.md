# ImageDownloader

Aplicación desarrollada en Java (sin bibliotecas externas) para la automatización de la recuperación de documentos HTML, análisis de referencias mediante expresiones regulares, resolución de rutas relativas y descarga binaria segura de imágenes desde un servidor web.

## Descripción del Proyecto
Este proyecto implementa un cliente web en consola que procesa una URL de entrada, establece una conexión HTTP, lee el contenido de la página, localiza todas las etiquetas `<img>` y sus atributos `src`, resuelve las rutas de manera absoluta, valida tipos MIME y descarga las imágenes respetando límites de tamaño y seguridad.

---

##  Requisitos del Sistema
* **Java Development Kit (JDK):** Versión 17 o superior.
* **Entorno de Desarrollo:** IntelliJ IDEA (recomendado) o cualquier terminal con soporte para Java.
* **Conexión a Internet:** Requerida para las pruebas de conexión y descarga de recursos.

---

## Instrucciones de Compilación y Ejecución

### 1. Compilación
Desde la raíz del proyecto (donde se encuentra la carpeta `src`), compila el archivo fuente ejecutando:
```bash
javac -d out src/main/java/ImageDownloader.java


¿Qué responsabilidad desempeña la aplicación dentro de una arquitectura cliente-servidor?
Actúa estrictamente como el cliente. Su rol consiste en formular solicitudes HTTP (método GET) hacia un servidor remoto, recibir la respuesta (código de estado, cabeceras y cuerpo HTML/binario), procesar la información de forma local y gestionar el almacenamiento de los recursos multimedia.

¿Qué diferencias existen entre URL, URLConnection y HttpURLConnection?
    URL: Es una clase que representa la dirección de un recurso en la web (protocolo, host, puerto, ruta).
    URLConnection: Es una clase abstracta que representa una conexión general orientada a cualquier protocolo soportado (HTTP, FTP, etc.).
    HttpURLConnection: Extiende a URLConnection añadiendo funcionalidades específicas y nativas para el protocolo HTTP (como manipulación de códigos de estado, métodos GET/POST y cabeceras de respuesta).

¿Qué sucede al invocar openConnection()?
Se crea un objeto de conexión vinculado a la URL, pero no se establece comunicación de red todavía. Solo prepara la configuración inicial de la solicitud (tiempos de espera, métodos, cabeceras).

¿Qué operaciones provocan que la conexión se establezca realmente?
La conexión de red efectiva y el intercambio de paquetes con el servidor ocurren al invocar métodos que requieren acceso inmediato al recurso, tales como connect(), getResponseCode(), getContentType() o getInputStream().

¿Por qué una página HTML puede requerir múltiples conexiones?
Porque el documento HTML principal solo contiene texto estructurado y referencias (enlaces) a recursos externos como hojas de estilo, scripts y, en este caso, imágenes (<img>). El cliente debe abrir una conexión independiente por cada recurso gráfico que necesite descargar.

¿Cuál es la diferencia entre getInputStream() y getContentType()?
    getContentType() devuelve una cadena con el tipo MIME del recurso (por ejemplo, text/html o image/png) obtenida de las cabeceras HTTP antes de leer el cuerpo.
    getInputStream() retorna un flujo de entrada (InputStream) que permite leer los datos binarios o textuales crudos transmitidos por el servidor.

¿Por qué distinguir recursos textuales y binarios?
Los recursos textuales (HTML) se procesan interpretando codificaciones de caracteres (como UTF-8) mediante lectores de texto (BufferedReader). Los recursos binarios (imágenes JPG, PNG, WebP) deben manipularse estrictamente a nivel de bytes (InputStream y arreglos de byte[]) para evitar alteraciones o corrupción en su estructura digital.

¿Para qué sirven los tiempos máximos de conexión y lectura?setConnectTimeout e setReadTimeout evitan que el hilo de ejecución de la aplicación se quede bloqueado indefinidamente (colgado) si el servidor remoto está caído, responde con extrema lentitud o la red experimenta fallas.

¿Qué limitaciones tiene analizar HTML mediante expresiones regulares?
El HTML no es un lenguaje regular estricto, por lo que las expresiones regulares son aproximaciones didácticas. No manejan de forma robusta etiquetas anidadas complejas, atributos sin comillas, comentarios con etiquetas falsas o código generado dinámicamente mediante JavaScript.

¿Cómo podría implementarse la descarga concurrente usando únicamente la API estándar de Java?
Utilizando la interfaz ExecutorService (por ejemplo, Executors.newFixedThreadPool()) junto con objetos Callable o Runnable para asignar cada URL de imagen a un hilo de ejecución independiente, permitiendo descargas paralelas sin librerías externas.