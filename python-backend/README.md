# Backend Python - SoterIA (LLM + STT + TTS)

Backend avanzado basado en FastAPI que proporciona servicios de inteligencia artificial para la gestión de emergencias, incluyendo conversación fluida, síntesis de voz y reconocimiento de voz local.

## Arquitectura

```
Java (JavaFX) --HTTP--> FastAPI ---> [ LLM (OpenRouter) / Kokoro TTS / Vosk STT ]
```

El sistema integra múltiples servicios de IA:
1. **LLM Conversacional**: Interacción fluida con contexto de usuario e historial de chat (vía OpenRouter).
2. **TTS (Text-to-Speech)**: Generación de voz de alta calidad con **Kokoro-82M** (local).
3. **STT (Speech-to-Text)**: Transcripción de voz local con **Vosk** y detección de emociones con **emotion2vec**(beta).
4. **Clasificador Local**: Modelo Scikit-learn para clasificación rápida y offline de emergencias.

## Características Principales

- **Conversación Inteligente**: Soteria recuerda quién eres, tu historial médico y los últimos mensajes del chat.
- **Streaming de Audio**: El texto y la voz se sincronizan para una respuesta inmediata.
- **Geolocalización Real**: Detección de ubicación por IP y búsqueda de los 3 centros de salud más cercanos.
- **Intercepción de Alertas**: Detección automática de palabras clave (112, socorro, etc.) para activar protocolos de emergencia sin pasar por la IA.

## Requisitos

- Python 3.10 o superior
- Conexión a Internet (para el LLM vía OpenRouter)
- Micrófono y Altavoces (para funciones de voz)

## Instalación

```bash
cd python-backend
pip install -r requirements.txt
```

## Configuración

Crea un archivo `.env` en la carpeta `python-backend/` con tu API Key:
```env
OPENROUTER_API_KEY=tu_clave_aqui
```

## Uso

### Arrancar el servidor

```powershell
cd python-backend; python -m uvicorn server:app --host 0.0.0.0 --port 8000
```

### Endpoints Principales

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/chat` | Conversación fluida con el LLM (incluye contexto) |
| POST | `/tts` | Convierte texto a audio WAV usando Kokoro |
| POST | `/stt` | Transcribe audio a texto localmente |
| GET | `/geolocate` | Obtiene ubicación real y coordenadas |
| POST | `/classify` | Clasificador local (offline) de emergencias |
| GET | `/system-info` | Información de recursos (RAM, GPU, Modelos) |

## Estructura del Proyecto

```
python-backend/
├── data/
│   ├── emergencies_dataset.csv    # Dataset para el clasificador local
│   └── emergency_config.json      # Configuración de prioridades
├── models/
│   ├── emergency_classifier.pkl   # Modelo Scikit-learn entrenado
│   └── vosk-model-...             # Modelo de voz local (auto-descargable)
├── llm_service.py                 # Integración con OpenRouter
├── tts_service.py                 # Motor de voz Kokoro
├── stt_service.py                 # Motor de transcripción Vosk/emotion2vec
├── system_utils.py                # Detección de hardware (GPU/RAM)
├── server.py                      # Servidor FastAPI y Endpoints
└── requirements.txt               # Dependencias del sistema
```
