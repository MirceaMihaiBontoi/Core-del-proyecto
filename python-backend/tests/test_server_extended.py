import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock
import server

client = TestClient(server.app)

def test_system_info_extended():
    resp = client.get("/system-info")
    assert resp.status_code == 200
    body = resp.json()
    assert "llm_fallback" in body
    assert "api_configured" in body

@patch("llm_service.llm_service.chat")
def test_chat_endpoint(mock_chat):
    mock_chat.return_value = {
        "success": True,
        "response": "Mantenga la calma, la ayuda va en camino.",
        "model_used": "test-model"
    }
    
    resp = client.post("/chat", json={"message": "ayuda!", "context": "fuego"})
    assert resp.status_code == 200
    assert resp.json()["response"] == "Mantenga la calma, la ayuda va en camino."

@patch("tts_service.tts_service.synthesize")
def test_tts_endpoint_success(mock_synthesize):
    mock_synthesize.return_value = {
        "success": True,
        "audio_data": b"fake_wav_data",
        "sample_rate": 24000,
        "model_used": "kokoro"
    }
    
    resp = client.post("/tts", json={"text": "hola", "emotion": "neutral"})
    assert resp.status_code == 200
    assert resp.content == b"fake_wav_data"
    assert resp.headers["X-Model-Used"] == "kokoro"

@patch("stt_service.stt_service.transcribe")
def test_stt_endpoint(mock_transcribe):
    mock_transcribe.return_value = {
        "success": True,
        "text": "fuego detectado",
        "emotion": "fear",
        "confidence": 0.95,
        "model_used": "vosk"
    }
    
    # Simulamos envío de archivo
    files = {'audio': ('test.wav', b"fake_audio_bytes", 'audio/wav')}
    resp = client.post("/stt", files=files, data={"sample_rate": 16000})
    
    assert resp.status_code == 200
    assert resp.json()["text"] == "fuego detectado"

@patch("stt_service.stt_service.get_emotion")
def test_analyze_emotion_endpoint(mock_get_emotion):
    mock_get_emotion.return_value = {
        "emotion": "angry",
        "confidence": 0.8,
        "urgency": "high"
    }
    
    files = {'audio': ('test.wav', b"fake_audio_bytes", 'audio/wav')}
    resp = client.post("/analyze-emotion", files=files)
    
    assert resp.status_code == 200
    assert resp.json()["urgency"] == "high"

@patch("httpx.get")
def test_geolocate_endpoint(mock_get):
    mock_resp = MagicMock()
    mock_resp.json.return_value = {
        "status": "success",
        "city": "Madrid",
        "regionName": "Madrid",
        "country": "Spain",
        "lat": 40.41,
        "lon": -3.7,
        "query": "1.2.3.4"
    }
    mock_get.return_value = mock_resp
    
    resp = client.get("/geolocate")
    assert resp.status_code == 200
    assert resp.json()["city"] == "Madrid"
