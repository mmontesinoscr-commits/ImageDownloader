import java.net.URI;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ImageDownloader {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java ImageDownloader <URL>");
            return;
        }
        HttpURLConnection connection = null;
        try {
            URI uri = URI.create(args[0]);
            URL url = uri.toURL();

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);

            int codigo = connection.getResponseCode();
            System.out.println("Codigo HTTP: " + codigo);
            System.out.println("Tipo de contenido: " + connection.getContentType());

            if (codigo >= 200 && codigo < 300) {

                StringBuilder htmlContent = new StringBuilder();

                try (InputStream input = connection.getInputStream();
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(input, StandardCharsets.UTF_8))) {

                    String linea;
                    while ((linea = reader.readLine()) != null) {
                        htmlContent.append(linea).append("\n");
                    }
                }

                String htmlCompleto = htmlContent.toString();

                // Mostrar los primeros 500 caracteres del HTML
                System.out.println("\n--- Primeros 500 caracteres del HTML ---");
                if (htmlCompleto.length() <= 500) {
                    System.out.println(htmlCompleto);
                } else {
                    System.out.println(htmlCompleto.substring(0, 500) + "...");
                }

                // Extraer y mostrar las imágenes sin duplicados
                Set<String> listaImágenes = extraerImagenes(htmlCompleto);
                System.out.println("\n--- Imágenes encontradas (sin duplicados) ---");
                for (String img : listaImágenes) {
                    System.out.println(img);
                }

            } else {
                System.err.println("El servidor respondió con un código de estado no exitoso: " + codigo);
            }

        } catch (IllegalArgumentException e) {
            System.err.println("URL inválida: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error de conexión (el servidor no responde): " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

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
}