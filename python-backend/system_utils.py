"""
Utilidades del sistema para detección de recursos y configuración.
"""
import os
import psutil
import logging
from pathlib import Path
from dotenv import load_dotenv

# Cargar variables de entorno
load_dotenv(Path(__file__).parent / ".env")

# Configurar logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def get_system_ram_gb() -> float:
    """
    Detecta la RAM total del sistema en GB.
    Returns: RAM total en GB (float)
    """
    try:
        ram_bytes = psutil.virtual_memory().total
        ram_gb = ram_bytes / (1024 ** 3)
        logger.info(f"RAM detectada: {ram_gb:.2f} GB")
        return ram_gb
    except Exception as e:
        logger.warning(f"No se pudo detectar RAM: {e}. Asumiendo 4 GB")
        return 4.0


def has_gpu() -> bool:
    """
    Detecta si hay GPU disponible.
    Returns: True si hay GPU CUDA disponible
    """
    try:
        import torch
        # Verificar CUDA (NVIDIA)
        if torch.cuda.is_available():
            gpu_name = torch.cuda.get_device_name(0)
            logger.info(f"GPU CUDA detectada: {gpu_name}")
            return True
        
        # Verificar DirectML (AMD/Intel en Windows)
        try:
            import torch_directml # type: ignore
            if hasattr(torch_directml, 'is_available') and torch_directml.is_available():
                logger.info("GPU DirectML detectada (AMD/Intel)")
                return True
        except (ImportError, Exception):
            pass
        
        # Verificar ROCm (AMD en Linux)
        rocm = getattr(torch.backends, 'rocm', None)
        if rocm is not None and hasattr(rocm, 'is_available') and rocm.is_available():
            gpu_name = torch.cuda.get_device_name(0) if torch.cuda.is_available() else "AMD GPU"
            logger.info(f"GPU ROCm detectada: {gpu_name}")
            return True
        
        logger.info("No se detectó GPU compatible (CUDA/DirectML/ROCm)")
        return False
        
    except ImportError:
        logger.info("PyTorch no instalado, sin GPU")
        return False
    except Exception as e:
        logger.warning(f"Error detectando GPU: {e}")
        return False


class SystemConfig:
    """
    Configuración del sistema basada en recursos disponibles.
    """
    RAM_THRESHOLD_GB = 8.0
    
    def __init__(self):
        self.ram_gb = get_system_ram_gb()
        self.has_gpu = has_gpu()
        self.use_heavy_models = self.ram_gb >= self.RAM_THRESHOLD_GB
        
        # Cargar API keys
        self.openrouter_api_key = os.getenv("OPENROUTER_API_KEY", "")
        
        # Configurar modelos según recursos
        self._configure_models()
        
        logger.info(f"Configuración del sistema:")
        logger.info(f"  - RAM: {self.ram_gb:.2f} GB")
        logger.info(f"  - GPU: {'Sí' if self.has_gpu else 'No'}")
        logger.info(f"  - Modelos pesados: {'Sí' if self.use_heavy_models else 'No'}")
    
    def _configure_models(self):
        """Configura los modelos según los recursos disponibles."""
        if self.use_heavy_models:
            # Modelos pesados (mejor calidad, más recursos)
            self.stt_model = "emotion2vec"
            self.tts_model = "emotivoice"
            logger.info("Usando modelos pesados: emotion2vec + EmotiVoice")
        else:
            # Modelos ligeros (fallback)
            self.stt_model = "vosk"
            self.tts_model = "piper"
            logger.info("Usando modelos ligeros: Vosk + Piper")
        
        # Configurar LLM con fallback
        self.llm_models = [
            {
                "name": "stepfun/step-3.5-flash:free",
                "api_base": "https://openrouter.ai/api/v1",
                "api_key": self.openrouter_api_key,
            },
            {
                "name": "openrouter/free",
                "api_base": "https://openrouter.ai/api/v1",
                "api_key": self.openrouter_api_key,
            }
        ]
    
    def get_llm_config(self) -> dict:
        """Retorna la configuración del LLM principal."""
        return self.llm_models[0]
    
    def get_llm_fallback_config(self) -> dict:
        """Retorna la configuración del LLM de fallback."""
        return self.llm_models[1]
    
    def is_configured(self) -> bool:
        """Verifica si el sistema está configurado correctamente."""
        if not self.openrouter_api_key:
            logger.error("API key de OpenRouter no configurada en .env")
            return False
        return True


# Instancia global de configuración
system_config = SystemConfig()