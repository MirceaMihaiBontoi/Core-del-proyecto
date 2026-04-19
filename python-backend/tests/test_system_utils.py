import sys
from unittest.mock import MagicMock, patch
import pytest

# Mocking psutil and torch before importing system_utils to avoid actual hardware detection
with patch('psutil.virtual_memory') as mock_vm, \
     patch('torch.cuda.is_available', return_value=False), \
     patch('torch.cuda.get_device_name', return_value="None"):
    
    import system_utils

def test_get_system_ram_gb():
    with patch('psutil.virtual_memory') as mock_vm:
        mock_vm.return_value.total = 16 * (1024 ** 3)
        ram = system_utils.get_system_ram_gb()
        assert ram == 16.0

def test_get_system_ram_gb_exception():
    with patch('psutil.virtual_memory', side_effect=Exception("Error")):
        ram = system_utils.get_system_ram_gb()
        assert ram == 4.0

def test_has_gpu_cuda():
    with patch('torch.cuda.is_available', return_value=True), \
         patch('torch.cuda.get_device_name', return_value="NVIDIA RTX 3080"):
        assert system_utils.has_gpu() is True

def test_has_gpu_none():
    # Simulamos que no hay torch instalado o no hay cuda
    with patch('torch.cuda.is_available', return_value=False), \
         patch('importlib.import_module', side_effect=ImportError):
        assert system_utils.has_gpu() is False

def test_system_config_heavy():
    with patch('system_utils.get_system_ram_gb', return_value=16.0), \
         patch('system_utils.has_gpu', return_value=True), \
         patch('os.getenv', return_value="test_key"):
        
        config = system_utils.SystemConfig()
        assert config.use_heavy_models is True
        assert config.stt_model == "emotion2vec"
        assert config.tts_model == "emotivoice"
        assert config.is_configured() is True

def test_system_config_light():
    with patch('system_utils.get_system_ram_gb', return_value=4.0), \
         patch('system_utils.has_gpu', return_value=False), \
         patch('os.getenv', return_value=""):
        
        config = system_utils.SystemConfig()
        assert config.use_heavy_models is False
        assert config.stt_model == "vosk"
        assert config.tts_model == "piper"
        assert config.is_configured() is False

def test_get_llm_configs():
    config = system_utils.SystemConfig()
    primary = config.get_llm_config()
    fallback = config.get_llm_fallback_config()
    
    assert "name" in primary
    assert "api_key" in primary
    assert primary != fallback
