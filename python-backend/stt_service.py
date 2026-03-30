"""
Servicio STT (Speech-to-Text) con sistema de fallback.
emotion2vec (pesado) → Vosk (ligero)
"""
import logging
import tempfile
import os
from pathlib import Path
from typing import Optional, Any
import numpy as np

from system_utils import system_config

logger = logging.getLogger(__name__)


class STTService:
    """
    Servicio de reconocimiento de voz con fallback automático.
    """
    
    def __init__(self):
        self.model_type = system_config.stt_model
        self.model: Any = None
        self.processor: Any = None
        self.recognizer: Any = None
        self.feature_extractor: Any = None
        self._initialized = False
        
    def initialize(self):
        """Inicializa el modelo STT según recursos disponibles."""
        if self._initialized:
            return True
            
        try:
            if self.model_type == "emotion2vec":
                return self._init_emotion2vec()
            else:
                return self._init_vosk()
        except Exception as e:
            logger.error(f"Error inicializando STT: {e}")
            # Intentar fallback
            if self.model_type == "emotion2vec":
                logger.warning("Fallback a Vosk por error en emotion2vec")
                self.model_type = "vosk"
                return self._init_vosk()
            return False
    
    def _init_emotion2vec(self) -> bool:
        """
        Inicializa emotion2vec para STT con detección de emociones.
        Requiere ~2-4 GB RAM.
        """
        try:
            from transformers import AutoModelForAudioClassification, AutoFeatureExtractor
            import torch
            
            logger.info("Cargando modelo emotion2vec...")
            
            # Modelo emotion2vec para detección de emociones en voz
            model_name = "emotion2vec/emotion2vec_plus_base"
            
            self.model = AutoModelForAudioClassification.from_pretrained(model_name)
            self.feature_extractor = AutoFeatureExtractor.from_pretrained(model_name)
            
            # Mover a GPU si está disponible
            if system_config.has_gpu:
                self.model = self.model.cuda()
                logger.info("emotion2vec movido a GPU")
            
            self._initialized = True
            logger.info("emotion2vec inicializado correctamente")
            return True
            
        except ImportError as e:
            logger.error(f"Dependencias faltantes para emotion2vec: {e}")
            return False
        except Exception as e:
            logger.error(f"Error cargando emotion2vec: {e}")
            return False
    
    def _init_vosk(self) -> bool:
        """
        Inicializa Vosk para STT ligero.
        Requiere ~50-100 MB RAM.
        """
        try:
            import vosk
            import json
            
            logger.info("Cargando modelo Vosk...")
            
            # Descargar modelo si no existe
            model_path = Path(__file__).parent / "models" / "vosk-model-small-es-0.42"
            
            if not model_path.exists():
                logger.info("Modelo Vosk no encontrado. Descargando...")
                self._download_vosk_model(model_path)
            
            self.model = vosk.Model(str(model_path))
            self.recognizer = vosk.KaldiRecognizer(self.model, 16000)
            
            self._initialized = True
            logger.info("Vosk inicializado correctamente")
            return True
            
        except ImportError as e:
            logger.error(f"Dependencias faltantes para Vosk: {e}")
            return False
        except Exception as e:
            logger.error(f"Error cargando Vosk: {e}")
            return False
    
    def _download_vosk_model(self, model_path: Path):
        """Descarga el modelo Vosk para español."""
        import urllib.request
        import zipfile
        
        url = "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip"
        zip_path = model_path.parent / "vosk-model.zip"
        
        model_path.parent.mkdir(parents=True, exist_ok=True)
        
        logger.info(f"Descargando modelo Vosk desde {url}...")
        urllib.request.urlretrieve(url, zip_path)
        
        logger.info("Extrayendo modelo...")
        with zipfile.ZipFile(zip_path, 'r') as zip_ref:
            zip_ref.extractall(model_path.parent)
        
        os.remove(zip_path)
        logger.info("Modelo Vosk descargado y extraído")
    
    def transcribe(self, audio_data: bytes, sample_rate: int = 16000) -> dict:
        """
        Transcribe audio a texto.
        
        Args:
            audio_data: Datos de audio en bytes (PCM 16-bit)
            sample_rate: Frecuencia de muestreo
            
        Returns:
            dict con:
            - success: bool
            - text: str (texto transcrito)
            - emotion: str (emoción detectada, solo con emotion2vec)
            - confidence: float (confianza)
            - model_used: str
            - error: str
        """
        if not self._initialized:
            if not self.initialize():
                return {
                    "success": False,
                    "text": "",
                    "emotion": "unknown",
                    "confidence": 0.0,
                    "model_used": "none",
                    "error": "Modelo STT no inicializado"
                }
        
        try:
            if self.model_type == "emotion2vec":
                return self._transcribe_emotion2vec(audio_data, sample_rate)
            else:
                return self._transcribe_vosk(audio_data, sample_rate)
        except Exception as e:
            logger.error(f"Error en transcripción: {e}")
            return {
                "success": False,
                "text": "",
                "emotion": "unknown",
                "confidence": 0.0,
                "model_used": self.model_type,
                "error": str(e)
            }
    
    def _transcribe_emotion2vec(self, audio_data: bytes, sample_rate: int) -> dict:
        """Transcribe con emotion2vec (detecta emociones)."""
        try:
            import torch
            import soundfile as sf
            import io
            
            # Convertir bytes a numpy array
            audio_array = np.frombuffer(audio_data, dtype=np.int16).astype(np.float32) / 32768.0
            
            # Guardar temporalmente para procesar
            with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
                sf.write(tmp.name, audio_array, sample_rate, subtype='PCM_16')
                tmp_path = tmp.name
            
            try:
                # Procesar con emotion2vec
                import librosa
                
                # Cargar audio
                waveform, sr = librosa.load(tmp_path, sr=16000, mono=True)
                
                if self.feature_extractor is None or self.model is None:
                    raise ValueError("Modelo emotion2vec no inicializado correctamente")

                # Preparar input
                inputs = self.feature_extractor(waveform, sampling_rate=16000, return_tensors="pt")
                
                if system_config.has_gpu:
                    inputs = {k: v.cuda() for k, v in inputs.items()}
                
                # Inferencia
                with torch.no_grad():
                    # Al ser self.model de tipo Any, Pylance permitirá la llamada
                    model_output = self.model(**inputs)
                    logits = getattr(model_output, "logits")
                    predictions = torch.nn.functional.softmax(logits, dim=-1)
                
                # Obtener emoción detectada
                emotion_labels = ["angry", "disgust", "fear", "happy", "neutral", "sad", "surprise"]
                
                # Asegurar que el índice sea un entero
                predicted_idx_tensor = torch.argmax(predictions, dim=-1)
                predicted_idx = int(predicted_idx_tensor.item())
                
                # Extraer confianza asegurando tipos nativos
                confidence = float(predictions[0][predicted_idx].item())
                
                emotion = emotion_labels[predicted_idx] if predicted_idx < len(emotion_labels) else "unknown"
                
                # Para STT real, necesitaríamos un modelo adicional
                # Por ahora retornamos la emoción y un texto placeholder
                # En producción, combinar con Whisper o similar
                
                return {
                    "success": True,
                    "text": f"[Emoción detectada: {emotion}]",  # Placeholder
                    "emotion": emotion,
                    "confidence": round(confidence, 2),
                    "model_used": "emotion2vec",
                    "error": ""
                }
                
            finally:
                os.unlink(tmp_path)
                
        except Exception as e:
            logger.error(f"Error en emotion2vec: {e}")
            raise
    
    def _transcribe_vosk(self, audio_data: bytes, sample_rate: int) -> dict:
        """Transcribe con Vosk (ligero)."""
        try:
            import json
            
            # Vosk espera audio en 16kHz mono PCM
            if sample_rate != 16000:
                # Resamplear si es necesario
                import librosa
                audio_array = np.frombuffer(audio_data, dtype=np.int16).astype(np.float32) / 32768.0
                audio_array = librosa.resample(audio_array, orig_sr=sample_rate, target_sr=16000)
                audio_data = (audio_array * 32768).astype(np.int16).tobytes()
            
            # Procesar con Vosk
            if self.recognizer is None:
                raise ValueError("Reconocedor Vosk no inicializado")

            self.recognizer.AcceptWaveform(audio_data)
            result = json.loads(self.recognizer.FinalResult())
            
            text = result.get("text", "")
            confidence = result.get("confidence", 0.0)
            
            return {
                "success": True,
                "text": text,
                "emotion": "neutral",  # Vosk no detecta emociones
                "confidence": round(confidence, 2),
                "model_used": "vosk",
                "error": ""
            }
            
        except Exception as e:
            logger.error(f"Error en Vosk: {e}")
            raise
    
    def get_emotion(self, audio_data: bytes, sample_rate: int = 16000) -> dict:
        """
        Solo detecta emoción del audio (sin transcribir).
        Útil para detectar urgencia del usuario.
        """
        if self.model_type != "emotion2vec":
            return {
                "emotion": "unknown",
                "confidence": 0.0,
                "urgency": "medium"
            }
        
        try:
            result = self.transcribe(audio_data, sample_rate)
            
            if result["success"]:
                emotion = result["emotion"]
                # Mapear emociones a urgencia
                urgency_map = {
                    "angry": "high",
                    "fear": "high",
                    "sad": "medium",
                    "surprise": "medium",
                    "disgust": "medium",
                    "happy": "low",
                    "neutral": "low"
                }
                urgency = urgency_map.get(emotion, "medium")
                
                return {
                    "emotion": emotion,
                    "confidence": result["confidence"],
                    "urgency": urgency
                }
            
            return {"emotion": "unknown", "confidence": 0.0, "urgency": "medium"}
            
        except Exception as e:
            logger.error(f"Error detectando emoción: {e}")
            return {"emotion": "unknown", "confidence": 0.0, "urgency": "medium"}


# Instancia global del servicio
stt_service = STTService()