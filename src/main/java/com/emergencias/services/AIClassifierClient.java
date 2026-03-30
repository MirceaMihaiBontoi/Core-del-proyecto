package com.emergencias.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Cliente HTTP que se comunica con el backend Python de clasificacion de emergencias.
 */
public class AIClassifierClient {
    private final String baseUrl;
    private final HttpClient client;

    public AIClassifierClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Envia texto al backend y devuelve la respuesta JSON cruda.
     * Retorna null si hay error de conexion.
     */
    public String classify(String text) {
        try {
            String jsonBody = "{\"text\": \"" + escapeJson(text) + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/classify"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                return response.body();
            }
            System.err.println("Error del servidor: HTTP " + response.statusCode());
            return null;
        } catch (Exception e) {
            System.err.println("No se pudo conectar con el backend de IA: " + e.getMessage());
            return null;
        }
    }

    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public static String extractString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    public static int extractInt(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return -1;
        start += search.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        return Integer.parseInt(json.substring(start, end));
    }

    public static double extractDouble(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return -1;
        start += search.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) {
            end++;
        }
        return Double.parseDouble(json.substring(start, end));
    }

    public static String[] extractStringArray(String json, String key) {
        String search = "\"" + key + "\":[";
        int start = json.indexOf(search);
        if (start == -1) return new String[0];
        start += search.length();
        int end = json.indexOf("]", start);
        String arrayContent = json.substring(start, end).trim();
        if (arrayContent.isEmpty()) return new String[0];

        java.util.List<String> items = new java.util.ArrayList<>();
        boolean inQuotes = false;
        int itemStart = -1;
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '"' && !inQuotes) {
                inQuotes = true;
                itemStart = i + 1;
            } else if (c == '"' && inQuotes) {
                inQuotes = false;
                items.add(arrayContent.substring(itemStart, i));
            }
        }
        return items.toArray(new String[0]);
    }

    /**
     * Extrae los objetos JSON del array "emergencies" como Strings individuales.
     */
    public static String[] extractEmergencies(String json) {
        String search = "\"emergencies\":[";
        int start = json.indexOf(search);
        if (start == -1) return new String[0];
        start += search.length();

        java.util.List<String> objects = new java.util.ArrayList<>();
        int depth = 0;
        int objStart = -1;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart != -1) {
                    objects.add(json.substring(objStart, i + 1));
                    objStart = -1;
                }
            } else if (c == ']' && depth == 0) {
                break;
            }
        }
        return objects.toArray(new String[0]);
    }

    /**
     * Obtiene la ubicacion aproximada del usuario por IP.
     * Retorna null si hay error.
     */
    public String geolocate() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/geolocate"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                String body = response.body();
                if (body.contains("\"error\"")) return null;
                return body;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    // ============================================================
    // NUEVOS MÉTODOS - LLM Chat, TTS, STT Avanzado
    // ============================================================

    /**
     * Envía un mensaje al LLM para conversación.
     * Usa fallback: stepfun/step-3.5-flash:free → openrouter/free
     */
    public String chat(String message, String context) {
        try {
            String jsonBody = "{\"message\": \"" + escapeJson(message) + 
                             "\", \"context\": \"" + escapeJson(context) + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat"))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, java.nio.charset.StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = client.send(request, 
                    HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                return response.body();
            }
            System.err.println("Error del servidor LLM: HTTP " + response.statusCode());
            return null;
        } catch (Exception e) {
            System.err.println("No se pudo conectar con el servicio LLM: " + e.getMessage());
            return null;
        }
    }

    /**
     * Sintetiza texto a audio (TTS).
     * Usa fallback: EmotiVoice → Piper
     * Retorna los bytes del audio WAV, o null si hay error.
     */
    public byte[] synthesize(String text, String emotion) {
        try {
            String jsonBody = "{\"text\": \"" + escapeJson(text) + 
                             "\", \"emotion\": \"" + escapeJson(emotion) + "\"}";
            // Logs de depuracion: tamaño y prefijo del body que vamos a enviar
            byte[] reqBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            System.out.println("TTS -> POST " + baseUrl + "/tts");
            System.out.println("TTS -> Request-Bytes: " + reqBytes.length);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/tts"))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, java.nio.charset.StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            System.out.println("TTS -> Response status: " + response.statusCode());
            System.out.println("TTS -> Response headers: " + response.headers().map());

            if (response.statusCode() == 200) {
                byte[] resp = response.body();
                int len = resp == null ? 0 : resp.length;
                System.out.println("TTS -> Response-Bytes: " + len);

                // Detectar si el servidor devolvió JSON en lugar de WAV
                String ct = response.headers().firstValue("content-type").orElse("");
                if (ct.contains("application/json")) {
                    String json = resp == null ? "" : new String(resp, StandardCharsets.UTF_8);
                    System.err.println("TTS -> Server returned JSON instead of WAV: " + json);
                    return null;
                }

                if (len > 0) {
                    int show = Math.min(len, 16);
                    System.out.println("TTS -> Response-first-bytes: " + Arrays.toString(Arrays.copyOf(resp, show)));
                    try {
                        if (len >= 4) {
                            String sig = new String(resp, 0, 4, StandardCharsets.US_ASCII);
                            if (!"RIFF".equals(sig)) {
                                System.err.println("TTS -> WARNING: response does not start with RIFF (" + sig + ")");
                            } else {
                                System.out.println("TTS -> WAV signature OK (RIFF)");
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("TTS -> Error leyendo signature: " + ex.getMessage());
                    }
                }
                return resp;
            }
            System.err.println("Error del servidor TTS: HTTP " + response.statusCode());
            return null;
        } catch (Exception e) {
            System.err.println("No se pudo conectar con el servicio TTS: " + e.getMessage());
            return null;
        }
    }

    /**
     * Transcribe audio a texto (STT avanzado).
     * Usa fallback: emotion2vec → Vosk
     */
    public String transcribeAdvanced(byte[] audioData, int sampleRate) {
        try {
            // Crear multipart form data
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            
            // Agregar archivo de audio
            baos.write(("--" + boundary + "\r\n").getBytes());
            baos.write("Content-Disposition: form-data; name=\"audio\"; filename=\"audio.wav\"\r\n".getBytes());
            baos.write("Content-Type: audio/wav\r\n\r\n".getBytes());
            baos.write(audioData);
            baos.write("\r\n".getBytes());
            
            // Agregar sample_rate
            baos.write(("--" + boundary + "\r\n").getBytes());
            baos.write("Content-Disposition: form-data; name=\"sample_rate\"\r\n\r\n".getBytes());
            baos.write(String.valueOf(sampleRate).getBytes());
            baos.write("\r\n".getBytes());
            
            // Cerrar boundary
            baos.write(("--" + boundary + "--\r\n").getBytes());
            
            byte[] body = baos.toByteArray();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/stt"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = client.send(request, 
                    HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                return response.body();
            }
            System.err.println("Error del servidor STT: HTTP " + response.statusCode());
            return null;
        } catch (Exception e) {
            System.err.println("No se pudo conectar con el servicio STT: " + e.getMessage());
            return null;
        }
    }

    /**
     * Analiza emoción del audio.
     */
    public String analyzeEmotion(byte[] audioData, int sampleRate) {
        try {
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            
            baos.write(("--" + boundary + "\r\n").getBytes());
            baos.write("Content-Disposition: form-data; name=\"audio\"; filename=\"audio.wav\"\r\n".getBytes());
            baos.write("Content-Type: audio/wav\r\n\r\n".getBytes());
            baos.write(audioData);
            baos.write("\r\n".getBytes());
            
            baos.write(("--" + boundary + "\r\n").getBytes());
            baos.write("Content-Disposition: form-data; name=\"sample_rate\"\r\n\r\n".getBytes());
            baos.write(String.valueOf(sampleRate).getBytes());
            baos.write("\r\n".getBytes());
            
            baos.write(("--" + boundary + "--\r\n").getBytes());
            
            byte[] body = baos.toByteArray();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/analyze-emotion"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = client.send(request, 
                    HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                return response.body();
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error analizando emoción: " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene información del sistema y modelos cargados.
     */
    public String getSystemInfo() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/system-info"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = client.send(request, 
                    HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                return response.body();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
