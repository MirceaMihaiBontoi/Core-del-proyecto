import pytest
from unittest.mock import MagicMock, patch
import numpy as np

# Mocking kokoro
sys_modules = {
    'kokoro': MagicMock(),
    'soundfile': MagicMock()
}

with patch.dict('sys.modules', sys_modules):
    from tts_service import TTSService

def test_tts_initialization():
    service = TTSService()
    with patch('kokoro.KPipeline') as mock_kp:
        success = service.initialize()
        assert success is True
        assert service._initialized is True
        mock_kp.assert_called_once()

def test_tts_synthesize_success():
    service = TTSService()
    service._initialized = True
    service._pipeline = MagicMock()
    
    # Mock pipe output: (gs, ps, audio)
    # audio debe ser un numpy array o algo que tenga .numpy()
    fake_audio = np.zeros(24000, dtype=np.float32)
    service._pipeline.return_value = [("gs", "ps", fake_audio)]
    
    result = service.synthesize("Hola mundo")
    
    assert result["success"] is True
    assert result["model_used"] == "kokoro"
    assert len(result["audio_data"]) > 0
    assert result["sample_rate"] == 24000

def test_tts_text_cleaning():
    service = TTSService()
    service._initialized = True
    service._pipeline = MagicMock()
    service._pipeline.return_value = [] # No genera audio pero queremos ver la limpieza
    
    # Probamos synthesize y vemos si falla o si limpia (aunque aquí el mock absorbe casi todo)
    # Solo verificamos que no explote con caracteres especiales
    result = service.synthesize("¡Hola! ¿Cómo estás?")
    assert result["success"] is False # Porque el mock devolvió lista vacía
    assert result["error"] == "No se pudo generar audio"

def test_tts_voice_management():
    service = TTSService()
    service.set_voice("male")
    assert service._default_voice == "em_alex"
    
    voices = service.get_available_voices()
    assert "female" in voices
    assert "male" in voices
