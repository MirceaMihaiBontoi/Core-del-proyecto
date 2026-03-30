package com.emergencias.ui;

import com.emergencias.model.UserData;
import com.emergencias.services.AIClassifierClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;
import java.io.FileOutputStream;

import com.emergencias.alert.AlertSender;
import com.emergencias.model.CentroSalud;
import com.emergencias.model.CentroSaludUtils;
import com.emergencias.model.EmergencyEvent;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 Controlador para la pantalla de chat conversacional.
 */
public class ChatController implements Initializable {

    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessages;
    @FXML private TextField messageInput;
    @FXML private Button voiceButton;
    @FXML private Label statusLabel;
    @FXML private Label aiStatusLabel;
    
    private AIClassifierClient aiClient;
    private UserData currentUser; // Guardar datos del usuario
    private boolean aiAvailable = false;
    private boolean isRecording = false;
    private MediaPlayer mediaPlayer; // Referencia fuerte para evitar GC
    private java.util.List<String> chatHistory = new java.util.ArrayList<>(); // Historial de mensajes

    @FXML
    private void handleSendMessage() {
        String message = messageInput.getText().trim();
        if (message.isEmpty()) return;
        
        addUserMessage(message);
        messageInput.clear();
        setStatus("Procesando...");
        processMessage(message);
    }

    @FXML
    private void handleVoiceInput() {
        if (!aiAvailable || isRecording) return;
        
        isRecording = true;
        voiceButton.setText("⏹️");
        voiceButton.setStyle("-fx-background-color: #ef4444;");
        setStatus("🎤 Grabando... (5 seg)");
        
        new Thread(() -> {
            try {
                String text = recordAndTranscribe(5);
                Platform.runLater(() -> {
                    isRecording = false;
                    voiceButton.setText("🎤");
                    voiceButton.setStyle("");
                    setStatus("Listo");
                    
                    if (text != null && !text.isEmpty()) {
                        addUserMessage(text);
                        processMessage(text);
                    } else {
                        addBotMessage("❌ No se entendió. Intenta de nuevo.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    isRecording = false;
                    voiceButton.setText("🎤");
                    voiceButton.setStyle("");
                    setStatus("Error");
                    addBotMessage("❌ Error: " + e.getMessage());
                });
            }
        }).start();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        aiClient = new AIClassifierClient("http://localhost:8000");
        checkAIAvailability();
        
        // Configurar evento de clic para alternar grabación (Toggle)
        voiceButton.setOnAction(event -> toggleRecording());
    }

    public void setUserData(UserData userData) {
        this.currentUser = userData;
        // Mensaje de bienvenida personalizado
        addBotMessage("¡Hola " + userData.getFullName() + "! 👋\n\n" +
                     "Soy **Soteria**, tu asistente de emergencias. Puedo ayudarte a:\n" +
                     "• Clasificar emergencias por texto o voz\n" +
                     "• Dar instrucciones de actuación\n" +
                     "• Enviar alertas al 112\n\n" +
                     "Describe lo que está pasando o presiona 🎤 para hablar.");
    }

    private void checkAIAvailability() {
        new Thread(() -> {
            aiAvailable = aiClient.isAvailable();
            Platform.runLater(() -> {
                if (aiAvailable) {
                    aiStatusLabel.setText("Soteria: ✅ Conectada");
                    aiStatusLabel.setStyle("-fx-text-fill: #10b981;");
                } else {
                    aiStatusLabel.setText("Soteria: ❌ Desconectada");
                    aiStatusLabel.setStyle("-fx-text-fill: #ef4444;");
                    addBotMessage("⚠️ Servidor de Soteria no disponible.\nModo básico activado.\n\n" +
                                 "Para activar Soteria completa:\ncd python-backend && python -m uvicorn server:app --host 0.0.0.0 --port 8000");
                }
            });
        }).start();
    }

    private void toggleRecording() {
        handleVoiceInput();
    }

    private String recordAndTranscribe(int duration) {
        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8000/transcribe?duration=" + duration))
                    .POST(java.net.http.HttpRequest.BodyPublishers.noBody())
                    .timeout(java.time.Duration.ofSeconds(20))
                    .build();

            java.net.http.HttpResponse<String> response = java.net.http.HttpClient.newHttpClient()
                    .send(request, java.net.http.HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));

            String body = response.body();
            if (body.contains("\"error\"")) return null;
            return AIClassifierClient.extractString(body, "text");
        } catch (Exception e) {
            return null;
        }
    }

    private void processMessage(String message) {
        // 1. INTERCEPTAR COMANDOS DE EMERGENCIA CRÍTICOS
        if (isEmergencyCommand(message)) {
            handleEmergencyAlert(message);
            return;
        }

        // 2. Añadir al historial
        chatHistory.add("Usuario: " + message);
        if (chatHistory.size() > 10) chatHistory.remove(0);

        new Thread(() -> {
            try {
                // 3. Construir contexto con datos del usuario e historial
                StringBuilder context = new StringBuilder();
                if (currentUser != null) {
                    context.append("DATOS DEL USUARIO:\n")
                           .append("- Nombre: ").append(currentUser.getFullName()).append("\n")
                           .append("- Teléfono: ").append(currentUser.getPhoneNumber()).append("\n")
                           .append("- Información Médica: ").append(currentUser.getMedicalInfo()).append("\n")
                           .append("- Contacto Emergencia: ").append(currentUser.getEmergencyContact()).append("\n\n");
                }
                
                context.append("HISTORIAL RECIENTE:\n");
                for (String hist : chatHistory) {
                    context.append(hist).append("\n");
                }

                // USAR LLM para conversación avanzada
                String llmResponse = aiClient.chat(message, context.toString());
                
                if (llmResponse != null) {
                    String responseText = AIClassifierClient.extractString(llmResponse, "response");
                    boolean success = llmResponse.contains("\"success\":") && llmResponse.contains("true");
                    
                    if (success && responseText != null && !responseText.isEmpty()) {
                        // Añadir respuesta al historial
                        chatHistory.add("Soteria: " + responseText);
                        
                        String formattedText = responseText
                            .replace("\\n\\n", "\n\n")
                            .replace("\\n", "\n")
                            .replace("**", "");
                        
                        // Iniciar streaming sincronizado de texto y audio
                        streamTextAndAudio(formattedText);
                    } else {
                        // Mensaje de error simple
                        Platform.runLater(() -> {
                            addBotMessage("Lo siento, no pude procesar tu mensaje. Por favor, intenta de nuevo.");
                            setStatus("Listo");
                        });
                    }
                } else {
                    // Sin conexión al servidor
                    Platform.runLater(() -> {
                        addBotMessage("No puedo conectarme al servidor. Verifica que esté ejecutándose.");
                        setStatus("Error");
                    });
                }
                
            } catch (Exception e) {
                System.err.println("[ERROR] Error procesando mensaje: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    addBotMessage("❌ Error: " + e.getMessage());
                    setStatus("Error");
                });
            }
        }).start();
    }

    private void addUserMessage(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_RIGHT);
        messageBox.setPadding(new Insets(5, 0, 5, 50));
        
        VBox bubble = new VBox();
        bubble.setStyle("-fx-background-color: #2563eb; -fx-background-radius: 15; -fx-padding: 10 15;");
        
        Text text = new Text(message);
        text.setStyle("-fx-fill: white; -fx-font-size: 14;");
        text.setWrappingWidth(300);
        
        bubble.getChildren().add(text);
        messageBox.getChildren().add(bubble);
        chatMessages.getChildren().add(messageBox);
        scrollToBottom();
    }

    private void addBotMessage(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5, 50, 5, 0));
        
        VBox bubble = new VBox();
        bubble.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 15; -fx-padding: 10 15;");
        
        TextFlow textFlow = new TextFlow();
        for (String line : message.split("\n")) {
            Text text = new Text(line + "\n");
            text.setStyle("-fx-fill: #1e293b; -fx-font-size: 14;");
            textFlow.getChildren().add(text);
        }
        
        bubble.getChildren().add(textFlow);
        messageBox.getChildren().add(bubble);
        chatMessages.getChildren().add(messageBox);
        scrollToBottom();
    }
    
    private void streamTextAndAudio(String message) {
        new Thread(() -> {
            try {
                // Dividir el mensaje en oraciones
                String[] sentences = message.split("(?<=[.!?])\\s+");
                StringBuilder currentText = new StringBuilder();
                
                for (int i = 0; i < sentences.length; i++) {
                    String sentence = sentences[i];
                    // Limpiar Markdown y caracteres de escape antes del TTS
                    String clean = sentence
                        .replace("**", "")
                        .replace("\\", "")
                        .replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚñÑüÜ0-9\\s.,;:!¿?¡]", " ")
                        .trim();
                    
                    if (clean.length() < 2) continue;

                    // Solicitar audio de esta oración
                    byte[] audioData = aiClient.synthesize(clean, "neutral");
                    
                    if (audioData != null && audioData.length > 0) {
                        // Esperar a que el audio anterior termine
                        synchronized (this) {
                            while (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                                Thread.sleep(50);
                            }
                        }

                        // Mostrar esta parte del texto y reproducir audio
                        final String textToShow = sentence + " ";
                        Platform.runLater(() -> {
                            if (currentText.length() == 0) {
                                addBotMessage(textToShow); // Crear burbuja nueva
                            } else {
                                updateLastBotMessage(textToShow); // Añadir a la burbuja existente
                            }
                            currentText.append(textToShow);
                            
                            try {
                                File temp = File.createTempFile("soteria_stream_", ".wav");
                                temp.deleteOnExit();
                                try (FileOutputStream fos = new FileOutputStream(temp)) {
                                    fos.write(audioData);
                                    fos.flush();
                                }
                                
                                Media media = new Media(temp.toURI().toString());
                                mediaPlayer = new MediaPlayer(media);
                                mediaPlayer.setOnEndOfMedia(() -> {
                                    // Pequeño retraso para asegurar que el hardware terminó de sonar
                                    new Thread(() -> {
                                        try { Thread.sleep(100); } catch (Exception ignored) {}
                                        Platform.runLater(() -> {
                                            if (mediaPlayer != null) {
                                                mediaPlayer.dispose();
                                                mediaPlayer = null;
                                            }
                                            temp.delete();
                                        });
                                    }).start();
                                });
                                mediaPlayer.play();
                            } catch (Exception e) {
                                System.err.println("Error en stream audio: " + e.getMessage());
                            }
                        });
                    }
                }
                Platform.runLater(() -> setStatus("Listo"));
            } catch (Exception e) {
                System.err.println("Error en stream sincronizado: " + e.getMessage());
                Platform.runLater(() -> addBotMessage(message));
            }
        }).start();
    }

    /**
     * Actualiza el último mensaje del bot añadiendo más texto.
     */
    private void updateLastBotMessage(String additionalText) {
        if (chatMessages.getChildren().isEmpty()) return;
        
        HBox lastBox = (HBox) chatMessages.getChildren().get(chatMessages.getChildren().size() - 1);
        VBox bubble = (VBox) lastBox.getChildren().get(0);
        TextFlow textFlow = (TextFlow) bubble.getChildren().get(0);
        
        Text text = new Text(additionalText);
        text.setStyle("-fx-fill: #1e293b; -fx-font-size: 14;");
        textFlow.getChildren().add(text);
        
        scrollToBottom();
    }

    private void scrollToBottom() {
        // Usar un pequeño retraso para asegurar que el layout se complete antes del scroll
        new Thread(() -> {
            try {
                Thread.sleep(50); // Pequeña pausa para que el layout se actualice
                Platform.runLater(() -> {
                    chatScrollPane.layout();
                    chatScrollPane.setVvalue(1.0);
                });
            } catch (InterruptedException e) {
                Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
            }
        }).start();
    }

    private void setStatus(String status) {
        statusLabel.setText(status);
    }

    /**
     * Detecta si el mensaje es un comando de emergencia directo.
     */
    private boolean isEmergencyCommand(String message) {
        String msg = message.toLowerCase();
        return msg.contains("112") || 
               msg.contains("alerta") || 
               msg.contains("emergencia") || 
               msg.contains("socorro") || 
               msg.contains("ayuda") ||
               msg.contains("ambulancia") ||
               msg.contains("policía") ||
               msg.contains("bomberos");
    }

    /**
     * Gestiona el envío de una alerta de emergencia real.
     */
    private void handleEmergencyAlert(String message) {
        setStatus("🚨 ENVIANDO ALERTA...");
        
        new Thread(() -> {
            try {
                // 1. Obtener ubicación real por IP (usando el servidor Python)
                String geoResponse = aiClient.geolocate();
                String locationStr = "Ubicación desconocida";
                double userLat = 0, userLon = 0;
                
                if (geoResponse != null && !geoResponse.contains("\"error\"")) {
                    String city = AIClassifierClient.extractString(geoResponse, "city");
                    String region = AIClassifierClient.extractString(geoResponse, "region");
                    
                    // Extraer coordenadas de forma más robusta (pueden ser números en el JSON)
                    String latStr = "0";
                    String lonStr = "0";
                    
                    if (geoResponse.contains("\"lat\":")) {
                        int start = geoResponse.indexOf("\"lat\":") + 6;
                        int end = geoResponse.indexOf(",", start);
                        if (end == -1) end = geoResponse.indexOf("}", start);
                        latStr = geoResponse.substring(start, end).trim();
                    }
                    
                    if (geoResponse.contains("\"lon\":")) {
                        int start = geoResponse.indexOf("\"lon\":") + 6;
                        int end = geoResponse.indexOf(",", start);
                        if (end == -1) end = geoResponse.indexOf("}", start);
                        lonStr = geoResponse.substring(start, end).trim();
                    }
                    
                    locationStr = city + ", " + region + " (Lat: " + latStr + ", Lon: " + lonStr + ")";
                    try {
                        userLat = Double.parseDouble(latStr);
                        userLon = Double.parseDouble(lonStr);
                    } catch (Exception ignored) {}
                }

                // 2. Crear el evento de emergencia
                EmergencyEvent event = new EmergencyEvent(
                    "EMERGENCIA CHAT: " + message,
                    locationStr,
                    10,
                    currentUser != null ? currentUser.getFullName() : "Usuario Desconocido"
                );

                // 3. Enviar la alerta
                AlertSender sender = new AlertSender();
                boolean success = sender.send(event);

                // 4. Buscar centros de salud cercanos
                final String centrosCercanos = buscarCentrosCercanos(userLat, userLon);
                final String finalLocation = locationStr;

                Platform.runLater(() -> {
                    if (success) {
                        addBotMessage("🚨 **ALERTA ENVIADA AL 112** 🚨\n\n" +
                                     "He enviado tu ubicación y datos de contacto a los servicios de emergencia.\n" +
                                     "📍 **Tu ubicación detectada:** " + finalLocation + "\n\n" +
                                     centrosCercanos + "\n" +
                                     "Mantén la calma. La ayuda está en camino.");
                        setStatus("🚨 ALERTA ACTIVA");
                    } else {
                        addBotMessage("⚠️ Error al enviar la alerta automática.\n" +
                                     "Por favor, llama manualmente al 112 de inmediato.");
                        setStatus("Error");
                    }
                });
            } catch (Exception e) {
                System.err.println("Error enviando alerta: " + e.getMessage());
                Platform.runLater(() -> setStatus("Error"));
            }
        }).start();
    }

    /**
     * Busca los 3 centros de salud más cercanos a las coordenadas dadas.
     */
    private String buscarCentrosCercanos(double lat, double lon) {
        if (lat == 0 && lon == 0) return "No se pudo determinar la ubicación para buscar centros cercanos.";
        
        try {
            List<CentroSalud> centros = CentroSaludUtils.cargarCentros("/CentrosdeSaludMurcia.json");
            if (centros == null || centros.isEmpty()) return "";

            // Calcular distancias y ordenar
            final double uLat = lat;
            final double uLon = lon;
            
            List<CentroSalud> cercanos = centros.stream()
                .filter(c -> {
                    try {
                        return c.getLatitud() != null && c.getLongitud() != null;
                    } catch (Exception e) { return false; }
                })
                .sorted(Comparator.comparingDouble(c -> {
                    try {
                        double cLat = Double.parseDouble(c.getLatitud().replace(",", "."));
                        double cLon = Double.parseDouble(c.getLongitud().replace(",", "."));
                        return calcularDistancia(uLat, uLon, cLat, cLon);
                    } catch (Exception e) { return Double.MAX_VALUE; }
                }))
                .limit(3)
                .collect(Collectors.toList());

            StringBuilder sb = new StringBuilder("🏥 **Centros de salud más cercanos:**\n");
            for (CentroSalud c : cercanos) {
                double dist = 0;
                try {
                    double cLat = Double.parseDouble(c.getLatitud().replace(",", "."));
                    double cLon = Double.parseDouble(c.getLongitud().replace(",", "."));
                    dist = calcularDistancia(uLat, uLon, cLat, cLon);
                } catch (Exception ignored) {}
                
                sb.append("• ").append(c.getNombre())
                  .append(" (").append(String.format("%.2f", dist)).append(" km)\n")
                  .append("  📍 ").append(c.getDireccion()).append("\n");
            }
            return sb.toString();
            
        } catch (Exception e) {
            return "Error al buscar centros de salud cercanos.";
        }
    }

    /**
     * Calcula la distancia en km entre dos puntos (Haversine).
     */
    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Radio de la Tierra en km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}