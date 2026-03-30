"""
Servicio TTS con Kokoro - Local, neural, alta calidad.
Licencia Apache 2.0.
"""
import logging
import tempfile
import os
import io
import wave
from typing import Optional, Any

logger = logging.getLogger(__name__)

# Voces disponibles en español (lang_code='e')
SPANISH_VOICES = {
    "default": "ef_dora",
    "female": "ef_dora",
    "male": "em_alex",
}


class TTSService:
    
    def __init__(self):
        self._pipeline: Optional[Any] = None
        self._initialized = False
        self._default_voice = "ef_dora"
        self._speed = 1.33
        
    def initialize(self):
        if self._initialized:
            return True
            
        try:
            import warnings
            from kokoro import KPipeline  # type: ignore
            
            with warnings.catch_warnings():
                warnings.simplefilter("ignore")
                # Pre-cargar pipeline
                self._pipeline = KPipeline(lang_code='e', repo_id='hexgrad/Kokoro-82M')
            
            self._initialized = True
            logger.info("Kokoro TTS inicializado correctamente")
            return True
            
        except ImportError as e:
            logger.error(f"Kokoro no instalado: {e}")
            logger.error("Instala con: pip install kokoro soundfile")
            return False
        except Exception as e:
            logger.error(f"Error inicializando Kokoro: {e}")
            return False
    
    def synthesize(self, text: str, emotion: str = "neutral") -> dict:
        """Genera audio WAV con Kokoro."""
        logger.info(f"Iniciando síntesis TTS. Texto: {text[:50]}...")
        
        if not self._initialized:
            if not self.initialize():
                logger.error("Fallo al inicializar Kokoro")
                return {
                    "success": False,
                    "audio_data": b"",
                    "sample_rate": 24000,
                    "model_used": "none",
                    "error": "Kokoro no inicializado"
                }
        
        try:
            import numpy as np
            import re
            
            # 1. Limpieza de texto para el fonetizador de Kokoro
            text = text.replace("¡", "").replace("¿", "").replace("!", ".").replace("?", ".")
            text = text.replace(":", ",").replace(";", ",")
            
            # 2. Dividir por saltos de línea para respetar la estructura del mensaje
            lines = [line.strip() for line in text.split("\n") if line.strip()]
            
            # 3. Dividir cada línea por puntuación si es necesario
            sentences = []
            for line in lines:
                parts = re.split(r'(?<=[.!?])\s+', line)
                sentences.extend([p.strip() for p in parts if len(p.strip()) > 1])
            
            logger.info(f"Texto estructurado en {len(sentences)} oraciones/líneas")
            
            if not sentences:
                logger.warning("No se detectaron oraciones válidas tras la limpieza")
                sentences = [text] if text.strip() else []
            
            all_audio = []
            silence = np.zeros(int(24000 * 0.2), dtype=np.float32) # 0.2s de silencio entre líneas
            
            if self._pipeline is None:
                logger.error("Pipeline es None en synthesize")
                return {
                    "success": False,
                    "audio_data": b"",
                    "sample_rate": 24000,
                    "model_used": "kokoro",
                    "error": "Pipeline no inicializado"
                }

            for i, sentence in enumerate(sentences):
                logger.info(f"Procesando oración {i+1}/{len(sentences)}: '{sentence[:50]}...'")
                
                try:
                    # Forzar generación de cada oración
                    generator = self._pipeline(sentence, voice=self._default_voice, speed=self._speed)
                    
                    fragments_in_sentence = 0
                    for _, _, audio in generator:
                        if audio is not None:
                            if hasattr(audio, 'numpy'):
                                audio = audio.numpy()
                            
                            # Aplicar un pequeño fade-out al final del fragmento para evitar clics
                            if len(audio) > 300:
                                fade_len = 300
                                fade = np.linspace(1.0, 0.0, fade_len)
                                audio[-fade_len:] *= fade
                            
                            # Añadir un silencio real (150ms) al final de cada fragmento
                            # Esto da margen al reproductor de Java para no cortar la última palabra
                            padding = np.zeros(int(24000 * 0.15), dtype=np.float32)
                            all_audio.append(audio)
                            all_audio.append(padding)
                            fragments_in_sentence += 1
                    
                    if fragments_in_sentence > 0:
                        logger.info(f"Oración {i+1} completada con {fragments_in_sentence} fragmentos")
                        all_audio.append(silence)
                    else:
                        logger.warning(f"Oración {i+1} no generó audio: '{sentence}'")
                        
                except Exception as sent_err:
                    logger.error(f"Error en oración {i+1}: {sent_err}")
                    continue
            
            if not all_audio:
                logger.error("No se generó ningún audio tras procesar todas las oraciones")
                return {
                    "success": False,
                    "audio_data": b"",
                    "sample_rate": 24000,
                    "model_used": "kokoro",
                    "error": "No se pudo generar audio"
                }
            
            # Concatenar todos los fragmentos
            full_audio = np.concatenate(all_audio)
            duration = len(full_audio) / 24000
            logger.info(f"Audio total generado: {duration:.2f} segundos")
            
            # Normalización suave
            max_val = np.max(np.abs(full_audio))
            if max_val > 0.01:
                full_audio = full_audio / max_val
                logger.info(f"Audio normalizado (pico: {max_val:.4f})")

            # Convertir a WAV
            buf = io.BytesIO()
            with wave.open(buf, "wb") as wf:
                wf.setnchannels(1)
                wf.setsampwidth(2)
                wf.setframerate(24000)
                
                # Escalar a int16 con margen de seguridad
                samples_int16 = (full_audio * 32000).astype(np.int16)
                wf.writeframes(samples_int16.tobytes())
            
            audio_bytes = buf.getvalue()
            logger.info(f"WAV finalizado: {len(audio_bytes)} bytes")
            
            return {
                "success": True,
                "audio_data": audio_bytes,
                "sample_rate": 24000,
                "model_used": "kokoro",
                "error": ""
            }
            
        except Exception as e:
            logger.error(f"Error crítico en synthesize: {e}", exc_info=True)
            return {
                "success": False,
                "audio_data": b"",
                "sample_rate": 24000,
                "model_used": "kokoro",
                "error": str(e)
            }
    
    def set_voice(self, voice_id: str):
        """Configurar voz por defecto."""
        if voice_id in SPANISH_VOICES:
            self._default_voice = SPANISH_VOICES[voice_id]
            logger.info(f"Voz configurada a: {self._default_voice}")
    
    def set_speed(self, speed: float):
        """Configurar velocidad de habla."""
        self._speed = max(0.5, min(2.0, speed))
        logger.info(f"Velocidad configurada a: {self._speed}")
    
    def get_available_voices(self):
        """Retorna voces disponibles en español."""
        return list(SPANISH_VOICES.keys())


# Instancia singleton
tts_service = TTSService()