import pytest
from unittest.mock import MagicMock, patch
import json
import httpx

from llm_service import LLMService

def test_create_messages():
    service = LLMService()
    messages = service._create_messages("ayuda fuego", "el usuario esta en la cocina")
    
    assert len(messages) >= 2
    assert messages[0]["role"] == "system"
    assert "Soteria" in messages[0]["content"]
    assert messages[1]["role"] == "system"
    assert "cocina" in messages[1]["content"]
    assert messages[-1]["role"] == "user"
    assert "ayuda fuego" in messages[-1]["content"]

@patch("httpx.Client")
def test_call_openrouter_success(mock_client_class):
    # Mocking httpx client context manager
    mock_client = mock_client_class.return_value.__enter__.return_value
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.return_value = {
        "choices": [{"message": {"content": "Test response"}}]
    }
    mock_client.post.return_value = mock_response
    
    service = LLMService()
    config = {"name": "test-model", "api_key": "key", "api_base": "http://test"}
    result = service._call_openrouter(config, [{"role": "user", "content": "hi"}])
    
    assert result == "Test response"

@patch("httpx.Client")
def test_chat_fallback(mock_client_class):
    # Simulamos que el primer modelo falla y el segundo funciona
    mock_client = mock_client_class.return_value.__enter__.return_value
    
    resp_fail = MagicMock()
    resp_fail.status_code = 500
    
    resp_success = MagicMock()
    resp_success.status_code = 200
    resp_success.json.return_value = {
        "choices": [{"message": {"content": "Fallback response"}}]
    }
    
    # Configuramos el mock para que devuelva fallo primero y luego éxito
    mock_client.post.side_effect = [resp_fail, resp_success]
    
    with patch("system_utils.system_config.is_configured", return_value=True):
        service = LLMService()
        result = service.chat("hola")
        
        assert result["success"] is True
        assert result["response"] == "Fallback response"
        assert result["model_used"] == service.fallback_model["name"]

def test_detect_emergency_type_parsing():
    service = LLMService()
    
    # Mockeamos _call_openrouter para que devuelva un JSON en string
    mock_json_resp = '{"is_emergency": true, "type": "fire", "urgency": "high", "needs_112": true}'
    
    with patch.object(service, '_call_openrouter', return_value=mock_json_resp):
        result = service.detect_emergency_type("fuego")
        assert result["is_emergency"] is True
        assert result["type"] == "fire"

def test_detect_emergency_type_markdown_parsing():
    service = LLMService()
    
    # Mockeamos respuesta con markdown
    mock_json_resp = '```json\n{"is_emergency": false, "type": "none", "urgency": "low", "needs_112": false}\n```'
    
    with patch.object(service, '_call_openrouter', return_value=mock_json_resp):
        result = service.detect_emergency_type("hola")
        assert result["is_emergency"] is False
        assert result["type"] == "none"
