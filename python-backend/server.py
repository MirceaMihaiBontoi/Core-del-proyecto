import csv
import joblib
from difflib import get_close_matches
from pathlib import Path
from fastapi import FastAPI, UploadFile, File, Form
from fastapi.responses import Response
from pydantic import BaseModel
from spellchecker import SpellChecker

# Importar nuevos servicios
from system_utils import system_config
from llm_service import llm_service
from stt_service import stt_service
import tts_service

MODEL_PATH = Path(__file__).parent / "models" / "emergency_classifier.pkl"
DATA_PATH = Path(__file__).parent / "data" / "emergencies_dataset.csv"
CONFIG_PATH = Path(__file__).parent / "data" / "emergency_config.json"

app = FastAPI(title="Emergency Classifier API")

model = joblib.load(MODEL_PATH)
spell = SpellChecker(language="es")

import json
import logging

# Configuración de logging profesional
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

with open(CONFIG_PATH, encoding="utf-8") as f:
    config = json.load(f)

PRIORITY_MAP = config["priority_map"]
LABEL_NAMES = config["label_names"]
CONTEXTUAL_INSTRUCTIONS = config["contextual_instructions"]
CONTEXT_DESCRIPTIONS = config["context_descriptions"]

# Extraer palabras del dataset como vocabulario principal de correccion
with open(DATA_PATH, encoding="utf-8") as f:
    reader = csv.DictReader(f)
    dataset_words = set()
    for row in reader:
        for word in row["text"].lower().split():
            if len(word) > 2:
                dataset_words.add(word)
VOCABULARY = list(dataset_words)
spell.word_frequency.load_words(VOCABULARY)


def get_instructions(label: str, text: str) -> list[str]:
    category = CONTEXTUAL_INSTRUCTIONS.get(label, {})
    text_lower = text.lower()
    for keywords, instructions in category.items():
        if keywords == "_default":
            continue
        if any(kw in text_lower for kw in keywords.split("|")):
            return instructions
    return category.get("_default", ["Mantenga la calma y llame al 112"])


def get_context(label: str, text: str) -> str:
    contexts = CONTEXT_DESCRIPTIONS.get(label, {})
    text_lower = text.lower()
    for keywords, desc in contexts.items():
        if keywords == "_default":
            continue
        if any(kw in text_lower for kw in keywords.split("|")):
            return desc
    return contexts.get("_default", "una emergencia")


class ClassifyRequest(BaseModel):
    text: str


class EmergencyDetail(BaseModel):
    type: str
    type_name: str
    confidence: float
    context: str
    instructions: list[str]


class ClassifyResponse(BaseModel):
    priority: int
    corrected_text: str
    emergencies: list[EmergencyDetail]


def correct_text(text: str) -> str:
    words = text.lower().split()
    corrected = []
    for word in words:
        if len(word) <= 2 or word in VOCABULARY:
            corrected.append(word)
            continue
        # Paso 1: si es palabra valida en español, no tocar
        if word in spell:
            corrected.append(word)
            continue
        # Paso 2: buscar la mas parecida en el CSV (prioridad sobre pyspellchecker)
        matches = get_close_matches(word, VOCABULARY, n=1, cutoff=0.7)
        if matches:
            corrected.append(matches[0])
            continue
        # Paso 3: fallback a pyspellchecker para palabras que no matchean el CSV
        correction = spell.correction(word)
        corrected.append(correction if correction else word)
    return " ".join(corrected)


SECONDARY_THRESHOLD = 0.20


def has_relevant_keywords(label: str, text: str) -> bool:
    """Comprueba si el texto contiene keywords relevantes para esa categoria."""
    # Revisar keywords en instrucciones contextuales
    for keywords in CONTEXTUAL_INSTRUCTIONS.get(label, {}):
        if keywords == "_default":
            continue
        if any(kw in text.lower() for kw in keywords.split("|")):
            return True
    # Revisar keywords en descripciones de contexto
    for keywords in CONTEXT_DESCRIPTIONS.get(label, {}):
        if keywords == "_default":
            continue
        if any(kw in text.lower() for kw in keywords.split("|")):
            return True
    return False


@app.post("/classify", response_model=ClassifyResponse)
def classify(request: ClassifyRequest):
    corrected = correct_text(request.text)

    label = model.predict([corrected])[0]
    probabilities = model.predict_proba([corrected])[0]
    classes = model.classes_

    # Ordenar por probabilidad descendente
    ranked = sorted(zip(classes, probabilities), key=lambda x: -x[1])

    primary_label, primary_conf = ranked[0]
    primary = EmergencyDetail(
        type=primary_label,
        type_name=LABEL_NAMES[primary_label],
        confidence=round(primary_conf, 2),
        context=get_context(primary_label, corrected),
        instructions=get_instructions(primary_label, corrected),
    )

    emergencies = [primary]

    # Incluir secundaria solo si supera umbral Y tiene keywords relevantes en el texto
    secondary_label, secondary_conf = ranked[1]
    if secondary_conf >= SECONDARY_THRESHOLD and has_relevant_keywords(secondary_label, corrected):
        emergencies.append(EmergencyDetail(
            type=secondary_label,
            type_name=LABEL_NAMES[secondary_label],
            confidence=round(secondary_conf, 2),
            context=get_context(secondary_label, corrected),
            instructions=get_instructions(secondary_label, corrected),
        ))

    # Prioridad maxima entre las emergencias detectadas
    max_priority = max(PRIORITY_MAP[e.type] for e in emergencies)

    return ClassifyResponse(
        priority=max_priority,
        corrected_text=corrected,
        emergencies=emergencies,
    )


@app.get("/geolocate")
def geolocate():
    """Obtiene ubicacion aproximada del usuario por IP publica."""
    import httpx
    try:
        r = httpx.get("http://ip-api.com/json/?lang=es&fields=status,city,regionName,country,lat,lon,query", timeout=5)
        data = r.json()
        if data.get("status") == "success":
            return {
                "city": data.get("city", ""),
                "region": data.get("regionName", ""),
                "country": data.get("country", ""),
                "lat": data.get("lat", 0),
                "lon": data.get("lon", 0),
                "ip": data.get("query", ""),
            }
        return {"error": "No se pudo determinar la ubicacion"}
    except Exception as e:
        return {"error": str(e)}


@app.post("/transcribe")
def transcribe(duration: int = 5):
    """Graba audio del microfono y lo transcribe a texto usando Google Speech API."""
    import tempfile
    import os
    import logging
    logger = logging.getLogger(__name__)

    # Check 1: dependencias instaladas
    try:
        import sounddevice as sd
    except ImportError:
        return {"error": "DEPENDENCIA: sounddevice no instalado. Ejecute: pip install sounddevice"}
    try:
        import soundfile as sf
    except ImportError:
        return {"error": "DEPENDENCIA: soundfile no instalado. Ejecute: pip install soundfile"}
    try:
        import speech_recognition as sr
    except ImportError:
        return {"error": "DEPENDENCIA: SpeechRecognition no instalado. Ejecute: pip install SpeechRecognition"}

    # Check 2: microfono disponible
    try:
        devices = sd.query_devices()
        input_device = sd.default.device[0]
        if input_device is None or input_device < 0:
            return {"error": "MICROFONO: No se detecto ningun microfono. Conecte uno y reinicie el servidor."}
    except Exception:
        return {"error": "MICROFONO: No se pudo acceder a los dispositivos de audio."}

    sample_rate = 16000
    try:
        # Añadir un pequeño margen de 0.5s para compensar latencia de red
        actual_duration = duration
        logger.info(f"Iniciando grabación de {actual_duration} segundos...")
        
        # Grabar con un pequeño margen extra
        audio = sd.rec(int((actual_duration + 0.5) * sample_rate), samplerate=sample_rate, channels=1, dtype="int16")
        sd.wait()
        logger.info("Grabación finalizada.")

        # Guardar en archivo temporal WAV
        tmp = tempfile.NamedTemporaryFile(suffix=".wav", delete=False)
        sf.write(tmp.name, audio, sample_rate)
        tmp.close()

        # Transcribir con Google Speech API
        recognizer = sr.Recognizer()
        with sr.AudioFile(tmp.name) as source:
            audio_data = recognizer.record(source)
            text = recognizer.recognize_google(audio_data, language="es-ES")  # type: ignore

        os.unlink(tmp.name)
        logger.info(f"Transcripción exitosa: {text}")
        return {"text": text}

    except sr.UnknownValueError:
        return {"error": "No se pudo entender el audio. Intente hablar mas claro y cerca del microfono."}
    except sr.RequestError as e:
        return {"error": f"Error del servicio de transcripcion (requiere internet): {e}"}
    except Exception as e:
        return {"error": f"Error al grabar audio: {e}"}


# ============================================================
# NUEVOS ENDPOINTS - LLM, STT avanzado, TTS
# ============================================================

class ChatRequest(BaseModel):
    message: str
    context: str = ""

class ChatResponse(BaseModel):
    success: bool
    response: str
    model_used: str
    error: str = ""

@app.middleware("http")
async def log_requests(request, call_next):
    import logging
    logger = logging.getLogger(__name__)
    
    # Log del cuerpo de la petición
    body = await request.body()
    logger.info(f"Request: {request.method} {request.url}")
    logger.info(f"Headers: {dict(request.headers)}")
    logger.info(f"Body: {body}")
    
    response = await call_next(request)
    return response


@app.post("/chat", response_model=ChatResponse)
def chat(request: ChatRequest):
    """
    Endpoint para conversación con LLM.
    Usa fallback automático: stepfun/step-3.5-flash:free → openrouter/free
    """
    import logging
    import traceback
    logger = logging.getLogger(__name__)
    logger.info(f"Chat request recibido: message='{request.message}', context='{request.context}'")
    logger.info(f"Request object: {request}")
    
    try:
        result = llm_service.chat(request.message, request.context)
        logger.info(f"LLM result: {result}")
        
        if not isinstance(result, dict) or 'success' not in result:
            logger.error(f"Resultado inválido del LLM: {result}")
            return ChatResponse(success=False, response="Error interno del servidor", model_used="none", error="Resultado inválido del LLM")
        
        return ChatResponse(**result)
    except Exception as e:
        logger.error(f"Error en chat endpoint: {e}")
        logger.error(traceback.format_exc())
        return ChatResponse(success=False, response="Error interno del servidor", model_used="none", error=str(e))


class TTSRequest(BaseModel):
    text: str
    emotion: str = "neutral"

@app.post("/tts")
def text_to_speech(request: TTSRequest):
    """
    Endpoint para síntesis de voz.
    Usa fallback automático: EmotiVoice → Piper
    """
    result = tts_service.tts_service.synthesize(request.text, request.emotion)
    
    if result["success"]:
        return Response(
            content=result["audio_data"],
            media_type="audio/wav",
            headers={
                "X-Model-Used": result["model_used"],
                "X-Sample-Rate": str(result["sample_rate"])
            }
        )
    else:
        return {"error": result["error"], "model_used": result["model_used"]}


@app.post("/stt")
async def speech_to_text_advanced(
    audio: UploadFile = File(...),
    sample_rate: int = Form(16000)
):
    """
    Endpoint avanzado para reconocimiento de voz.
    Usa fallback automático: emotion2vec → Vosk
    """
    try:
        audio_data = await audio.read()
        result = stt_service.transcribe(audio_data, sample_rate)
        return result
    except Exception as e:
        return {
            "success": False,
            "text": "",
            "emotion": "unknown",
            "confidence": 0.0,
            "model_used": "none",
            "error": str(e)
        }


@app.post("/analyze-emotion")
async def analyze_emotion(
    audio: UploadFile = File(...),
    sample_rate: int = Form(16000)
):
    """
    Endpoint para detectar emoción en audio.
    Útil para detectar urgencia del usuario.
    """
    try:
        audio_data = await audio.read()
        result = stt_service.get_emotion(audio_data, sample_rate)
        return result
    except Exception as e:
        return {"emotion": "unknown", "confidence": 0.0, "urgency": "medium"}


@app.get("/system-info")
def system_info():
    """
    Retorna información del sistema y modelos cargados.
    """
    return {
        "ram_gb": system_config.ram_gb,
        "has_gpu": system_config.has_gpu,
        "use_heavy_models": system_config.use_heavy_models,
        "stt_model": system_config.stt_model,
        "tts_model": system_config.tts_model,
        "llm_primary": system_config.get_llm_config()["name"],
        "llm_fallback": system_config.get_llm_fallback_config()["name"],
        "api_configured": system_config.is_configured()
    }


@app.get("/health")
def health():
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn
    print("Iniciando servidor Soteria en http://0.0.0.0:8000")
    print("Endpoints disponibles:")
    print("  - POST /chat (conversación con LLM)")
    print("  - POST /tts (síntesis de voz)")
    print("  - POST /stt (reconocimiento de voz)")
    print("  - GET  /system-info (información del sistema)")
    print("  - GET  /health (verificar estado)")
    uvicorn.run(app, host="0.0.0.0", port=8000)
