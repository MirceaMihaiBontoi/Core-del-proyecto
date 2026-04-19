import pytest
from unittest.mock import MagicMock, patch
import numpy as np
import io

# Mocking heavy libraries before import
sys_modules = {
    'transformers': MagicMock(),
    'torch': MagicMock(),
    'vosk': MagicMock(),
    'librosa': MagicMock(),
    'soundfile': MagicMock()
}

with patch.dict('sys.modules', sys_modules):
    from stt_service import STTService

def test_stt_initialization_vosk():
    service = STTService()
    service.model_type = "vosk"
    
    with patch('vosk.Model'), patch('vosk.KaldiRecognizer'), patch('pathlib.Path.exists', return_value=True):
        success = service.initialize()
        assert success is True
        assert service._initialized is True

def test_stt_transcribe_vosk():
    service = STTService()
    service.model_type = "vosk"
    service._initialized = True
    service.recognizer = MagicMock()
    service.recognizer.FinalResult.return_value = '{"text": "fuego en la cocina", "confidence": 0.98}'
    
    audio_data = b"\x00" * 3200 # 0.1s of silence
    result = service.transcribe(audio_data, 16000)
    
    assert result["success"] is True
    assert result["text"] == "fuego en la cocina"
    assert result["model_used"] == "vosk"

def test_emotion_to_urgency():
    service = STTService()
    service.model_type = "emotion2vec" # Para que no devuelva medium por defecto
    
    # Mocking transcribe to return different emotions
    with patch.object(service, 'transcribe') as mock_transcribe:
        # Caso Urgencia Alta
        mock_transcribe.return_value = {"success": True, "emotion": "fear", "confidence": 0.9}
        res = service.get_emotion(b"fake")
        assert res["urgency"] == "high"
        
        # Caso Urgencia Baja
        mock_transcribe.return_value = {"success": True, "emotion": "neutral", "confidence": 0.9}
        res = service.get_emotion(b"fake")
        assert res["urgency"] == "low"
        
        # Caso desconocido
        mock_transcribe.return_value = {"success": False}
        res = service.get_emotion(b"fake")
        assert res["urgency"] == "medium"

def test_stt_fallback_on_init_fail():
    service = STTService()
    service.model_type = "emotion2vec"
    
    # Simulamos que emotion2vec falla al cargar (ImportError) y Vosk funciona
    with patch.object(service, '_init_emotion2vec', side_effect=Exception("CUDA error")), \
         patch.object(service, '_init_vosk', return_value=True):
        
        success = service.initialize()
        assert success is True
        assert service.model_type == "vosk"
