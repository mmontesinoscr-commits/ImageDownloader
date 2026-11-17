import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ImageDownloader {

    public static void main(String[] args) {
        // 1. Recibir URL por argumentos
        if (args.length != 1) {
            System.err.println("Uso: java ImageDownloader <URL>");
            return;
        }

        try {
            // 2. Validar dirección
            URL url = validarURL(args[0]);
            System.out.println("Conectando y procesando: " + url);

            // 3 y 4. Abrir conexión y recuperar HTML completo
            String htmlCompleto = obtenerHTML(url);

            // Mostrar los primeros 500 caracteres del HTML recuperado
            System.out.println("\n--- Primeros 500 caracteres del HTML ---");
            if (htmlCompleto.length() <= 500) {
                System.out.println(htmlCompleto);
            } else {
                System.out.println(htmlCompleto.substring(0, 500) + "...");
            }

            // 5. Extraer referencias de imágenes (<img> y src) sin duplicados
            Set<String> referencias = extraerImagenes(htmlCompleto);
            System.out.println("\n--- Iniciando descarga de imágenes ---");

            Path directorio = Path.of("imagenes");
            int contador = 1;

            // 6 y 7. Resolver URLs absolutas, validar conexión y descargar recursos
            for (String ref : referencias) {
                try {
                    URL urlAbsoluta = resolverImagen(url, ref);
                    boolean exito = descargarImagen(urlAbsoluta, directorio, contador);
                    if (exito) {
                        contador++;
                    }
                } catch (Exception e) {
                    System.err.println("No se pudo procesar la referencia (" + ref + "): " + e.getMessage());
                }
            }

            System.out.println("\n¡Proceso de descarga finalizado con éxito!");

        } catch (Exception e) {
            System.err.println("Error en la ejecución de la aplicación: " + e.getMessage());
        }
    }

    // Método para validar la dirección URL y su esquema (HTTP/HTTPS)
    public static URL validarURL(String argumento) throws Exception {
        if (argumento == null || argumento.trim().isEmpty()) {
            throw new IllegalArgumentException("El argumento de la URL está vacío.");
        }
        URI uri = URI.create(argumento);
        URL url = uri.toURL();

        String protocolo = url.getProtocol().toLowerCase();
        if (!protocolo.equals("http") && !protocolo.equals("https")) {
            throw new IllegalArgumentException("Solo se admiten protocolos HTTP y HTTPS: " + protocolo);
        }
        return url;
    }

    // Método para abrir conexión HTTP y recuperar el HTML como String
    public static String obtenerHTML(URL url) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);

            int codigo = connection.getResponseCode();
            System.out.println("Código HTTP: " + codigo);
            System.out.println("Tipo de contenido: " + connection.getContentType());

            if (codigo < 200 || codigo >= 300) {
                throw new IOException("El servidor respondió con un código no exitoso: " + codigo);
            }

            StringBuilder htmlContent = new StringBuilder();
            try (InputStream input = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(input, StandardCharsets.UTF_8))) {

                String linea;
                while ((linea = reader.readLine()) != null) {
                    htmlContent.append(linea).append("\n");
                }
            }
            return htmlContent.toString();

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // Método para identificar etiquetas <img> y extraer el atributo src usando expresiones regulares
    public static Set<String> extraerImagenes(String html) {
        Set<String> imagenesUnicas = new HashSet<>();

        Pattern imgPattern = Pattern.compile("<img\\b[^>]*>", Pattern.CASE_INSENSITIVE);
        Pattern srcPattern = Pattern.compile("\\bsrc\\s*=\\s*([\"'])(.*?)\\1", Pattern.CASE_INSENSITIVE);

        Matcher imgMatcher = imgPattern.matcher(html);
        while (imgMatcher.find()) {
            String etiqueta = imgMatcher.group();
            Matcher srcMatcher = srcPattern.matcher(etiqueta);
            if (srcMatcher.find()) {
                String src = srcMatcher.group(2);
                imagenesUnicas.add(src);
            }
        }
        return imagenesUnicas;
    }

    // Método para resolver direcciones relativas o absolutas a partir de la URL base
    public static URL resolverImagen(URL base, String referencia) throws Exception {
        if (referencia == null || referencia.trim().isEmpty()) {
            throw new IllegalArgumentException("La referencia está vacía");
        }

        String refLower = referencia.toLowerCase();
        if (refLower.startsWith("data:") || refLower.startsWith("javascript:") || refLower.startsWith("vbscript:")) {
            throw new IllegalArgumentException("Esquema no admitido: " + referencia);
        }

        URL urlResuelta = new URL(base, referencia);

        String protocolo = urlResuelta.getProtocol().toLowerCase();
        if (!protocolo.equals("http") && !protocolo.equals("https")) {
            throw new IllegalArgumentException("Protocolo no admitido: " + protocolo);
        }

        return urlResuelta;
    }

    // Método para descargar y almacenar el recurso binario de forma segura
    public static boolean descargarImagen(URL url, Path directorio, int numero) {
        HttpURLConnection connectionImg = null;
        Path destino = null;
        try {
            connectionImg = (HttpURLConnection) url.openConnection();
            connectionImg.setRequestMethod("GET");
            connectionImg.setConnectTimeout(10000);
            connectionImg.setReadTimeout(15000);

            int codigo = connectionImg.getResponseCode();
            if (codigo < 200 || codigo >= 300) {
                return false;
            }

            String contentType = connectionImg.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                return false;
            }

            // Determinar extensión mediante tipo MIME
            String extension = ".jpg";
            String mimeLower = contentType.toLowerCase();
            if (mimeLower.contains("png")) {
                extension = ".png";
            } else if (mimeLower.contains("gif")) {
                extension = ".gif";
            } else if (mimeLower.contains("webp")) {
                extension = ".webp";
            } else if (mimeLower.contains("jpeg") || mimeLower.contains("jpg")) {
                extension = ".jpg";
            }

            // Crear nombre seguro y consecutivo (ej. imagen_001.png)
            String nombreArchivo = String.format("imagen_%03d%s", numero, extension);
            Files.createDirectories(directorio);
            destino = directorio.resolve(nombreArchivo);

            long maxBytes = 10 * 1024 * 1024; // Límite máximo de 10 MB por imagen
            try (InputStream input = connectionImg.getInputStream();
                 OutputStream out = Files.newOutputStream(destino)) {

                byte[] buffer = new byte[8192];
                long totalBytes = 0;
                int leidos;
                while ((leidos = input.read(buffer)) != -1) {
                    totalBytes += leidos;
                    if (totalBytes > maxBytes) {
                        throw new IOException("La imagen excede el límite máximo de 10 MB.");
                    }
                    out.write(buffer, 0, leidos);
                }
            }

            System.out.println("Imagen descargada con éxito: " + destino);
            return true;

        } catch (Exception e) {
            // Eliminar archivo parcial si ocurre algún error durante la descarga
            if (destino != null) {
                try {
                    Files.deleteIfExists(destino);
                } catch (Exception ignored) {}
            }
            System.err.println("Error al descargar " + url + ": " + e.getMessage());
            return false;
        } finally {
            if (connectionImg != null) {
                connectionImg.disconnect();
            }
        }
    }
}