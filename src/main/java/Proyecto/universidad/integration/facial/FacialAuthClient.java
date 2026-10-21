package Proyecto.universidad.integration.facial;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Cliente HTTP pequeño para el servicio facial local. Nunca transporta passwords ni imágenes. */
public class FacialAuthClient {

    private static Process pythonProcess = null;
    private final HttpClient httpClient;
    private final String baseUrl;

    public FacialAuthClient() {
        this(
                System.getProperty(
                        "universidad.facial.url",
                        System.getenv().getOrDefault(
                                "FACIAL_API_URL",
                                System.getenv().getOrDefault("FACIAL_SERVICE_URL", "http://127.0.0.1:8765")
                        )
                ),
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(4))
                        .build()
        );
    }

    public FacialAuthClient(String baseUrl, HttpClient httpClient) {
        asegurarServicioPython(); // Inicia el microservicio en Python automáticamente
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.httpClient = httpClient;
    }

    /**
     funcion que levanta automaticamente servidor de Python si no está activo.
     */
    public static synchronized void asegurarServicioPython() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            return;
        }

        try {
            File directorioFacial = new File("facial-service");
            File ejecutablePython = new File(directorioFacial, ".venv/Scripts/python.exe");

            if (!ejecutablePython.exists()) {
                directorioFacial = new File(".");
                ejecutablePython = new File(".venv/Scripts/python.exe");
            }

            if (ejecutablePython.exists()) {
                ProcessBuilder pb = new ProcessBuilder(
                        ejecutablePython.getAbsolutePath(),
                        "-m",
                        "scripts.start"
                );
                pb.directory(directorioFacial);
                pb.inheritIO();

                pythonProcess = pb.start();
                System.out.println(">>> Microservicio Facial en Python iniciado automáticamente.");

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    if (pythonProcess != null && pythonProcess.isAlive()) {
                        pythonProcess.destroy();
                        System.out.println(">>> Microservicio Facial detenido.");
                    }
                }));

                Thread.sleep(2000);
            } else {
                System.err.println("No se encontró el ejecutable de Python en: " + ejecutablePython.getAbsolutePath());
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Error al iniciar el microservicio de Python automáticamente: " + e.getMessage());
        }
    }

    public FacialVerificationResult verify(int userId) {
        String body = send("/face/verify", "POST", "{\"userId\":" + userId + "}", Duration.ofSeconds(30));
        return new FacialVerificationResult(
                booleanField(body, "success"),
                booleanField(body, "matched"),
                intField(body, "userId"),
                nullableDoubleField(body, "score"),
                doubleField(body, "threshold"),
                booleanField(body, "livenessPassed"),
                intField(body, "framesEvaluated"),
                stringField(body, "message")
        );
    }

    public FacialEnrollmentResult enroll(int userId, boolean replace) {
        String body = send("/face/enroll", "POST",
                "{\"userId\":" + userId + ",\"replace\":" + replace + "}",
                Duration.ofSeconds(90));
        return new FacialEnrollmentResult(
                booleanField(body, "success"),
                intField(body, "userId"),
                stringField(body, "model"),
                intField(body, "dimension"),
                intField(body, "samples"),
                nullableDoubleField(body, "qualityScore"),
                booleanField(body, "livenessPassed"),
                stringField(body, "message")
        );
    }

    public FacialStatus status(int userId) {
        String body = send("/face/status/" + userId, "GET", null, Duration.ofSeconds(8));
        return new FacialStatus(
                intField(body, "userId"),
                booleanField(body, "enrolled"),
                stringField(body, "registeredAt"),
                stringField(body, "updatedAt"),
                stringField(body, "model"),
                intField(body, "dimension"),
                intField(body, "samples")
        );
    }

    public void deleteProfile(int userId, int adminUserId) {
        send("/face/profile/" + userId, "DELETE", "{\"adminUserId\":" + adminUserId + "}", Duration.ofSeconds(8));
    }

    private String send(String path, String method, String body, Duration timeout) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(timeout)
                    .header("Accept", "application/json");
            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(body));
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new FacialClientException(extraerMensaje(response.body()), response.statusCode());
            }
            return response.body();
        } catch (FacialClientException e) {
            throw e;
        } catch (IOException e) {
            throw new FacialClientException("No se pudo comunicar con el servicio facial.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FacialClientException("La verificación facial fue interrumpida.", e);
        }
    }

    private String extraerMensaje(String body) {
        String detail = stringField(body, "detail");
        return detail == null || detail.isBlank()
                ? "El servicio facial rechazó la solicitud (HTTP)."
                : detail;
    }

    private static String rawField(String json, String field) {
        String key = Pattern.quote(field);
        Pattern pattern = Pattern.compile("\\\"" + key
                + "\\\"\\s*:\\s*(null|true|false|-?\\d+(?:\\.\\d+)?|\"(?:\\\\.|[^\"\\\\])*\")");
        Matcher matcher = pattern.matcher(json == null ? "" : json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static boolean booleanField(String json, String field) {
        return "true".equals(rawField(json, field));
    }

    private static int intField(String json, String field) {
        String value = rawField(json, field);
        return value == null || "null".equals(value) ? 0 : Integer.parseInt(value);
    }

    private static double doubleField(String json, String field) {
        String value = rawField(json, field);
        return value == null || "null".equals(value) ? 0.0 : Double.parseDouble(value);
    }

    private static Double nullableDoubleField(String json, String field) {
        String value = rawField(json, field);
        return value == null || "null".equals(value) ? null : Double.valueOf(value);
    }

    private static String stringField(String json, String field) {
        String value = rawField(json, field);
        if (value == null || "null".equals(value)) return null;
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r");
        }
        return value;
    }
}