"""
Servicio LLM con sistema de fallback para OpenRouter.
"""
import httpx
import logging
import json
from typing import Optional, List, Dict
from system_utils import system_config

logger = logging.getLogger(__name__)


class LLMService:
    """
    Servicio para interactuar con LLMs de OpenRouter con fallback.
    """
    
    def __init__(self):
        self.primary_model = system_config.get_llm_config()
        self.fallback_model = system_config.get_llm_fallback_config()
        self.timeout = 30.0
        
    def _create_messages(self, user_message: str, context: str = "") -> List[Dict]:
        """
        Crea los mensajes para el LLM con contexto de emergencias.
        """
        system_prompt = """Eres Soteria, un asistente de IA especializado en gestión de emergencias críticas. 
Tu objetivo es salvar vidas mediante instrucciones precisas, calmadas y rápidas.

PROTOCOLO DE ACTUACIÓN PRIORITARIO:
1. EVALUACIÓN: Determina la gravedad. No pidas llamar al 112 para accidentes domésticos leves (quemaduras pequeñas, cortes superficiales, torceduras).
2. PRIMEROS AUXILIOS: Da instrucciones inmediatas (ej: "Pon la quemadura bajo agua fría 20 minutos").
3. DERIVACIÓN: Solo ordena llamar al 112 si hay: pérdida de conciencia, dificultad para respirar, hemorragia masiva, fuego descontrolado o dolor torácico intenso.

REGLAS DE COMUNICACIÓN:
- Sé extremadamente CONCISO.
- Usa frases CORTAS y claras.
- NO uses listas numeradas (1., 2., etc.) ni viñetas.
- NO uses negritas (**) ni caracteres especiales.
- Redacta las instrucciones en párrafos seguidos o frases separadas por puntos.
- NO sugieras comandos de voz.
- Si es un accidente doméstico, prioriza qué hacer AHORA mismo.

LIMITACIONES CRÍTICAS:
- Si el usuario bromea, responde con brevedad y sin protocolos.
- No repitas "Llama al 112" si ya lo has dicho o si la situación no lo requiere claramente.

Responde siempre en español puro. """

        messages = [
            {"role": "system", "content": system_prompt}
        ]
        
        if context:
            messages.append({"role": "system", "content": f"Contexto adicional: {context}"})
        
        messages.append({"role": "user", "content": user_message})
        
        return messages
    
    def _call_openrouter(self, model_config: dict, messages: List[Dict]) -> Optional[str]:
        """
        Llama a la API de OpenRouter.
        """
        try:
            headers = {
                "Authorization": f"Bearer {model_config['api_key']}",
                "Content-Type": "application/json",
                "HTTP-Referer": "https://soteria-emergency.app",
                "X-Title": "Soteria Emergency Assistant"
            }
            
            payload = {
                "model": model_config["name"],
                "messages": messages,
                "max_tokens": 1024,
                "temperature": 0.7,
                "top_p": 0.9,
                "stream": False
            }
            
            with httpx.Client(timeout=self.timeout) as client:
                response = client.post(
                    f"{model_config['api_base']}/chat/completions",
                    headers=headers,
                    json=payload
                )
                
                if response.status_code == 200:
                    data = response.json()
                    content = data.get("choices", [{}])[0].get("message", {}).get("content", "")
                    return content
                else:
                    logger.error(f"Error OpenRouter ({model_config['name']}): {response.status_code} - {response.text}")
                    return None
                    
        except Exception as e:
            logger.error(f"Error llamando a OpenRouter ({model_config['name']}): {e}")
            return None
    
    def chat(self, user_message: str, context: str = "") -> dict:
        """
        Envía un mensaje al LLM con fallback automático.
        
        Returns:
            dict con:
            - success: bool
            - response: str (mensaje del asistente)
            - model_used: str (modelo utilizado)
            - error: str (mensaje de error si falló)
        """
        if not system_config.is_configured():
            return {
                "success": False,
                "response": "",
                "model_used": "none",
                "error": "API key de OpenRouter no configurada"
            }
        
        messages = self._create_messages(user_message, context)
        
        # Intentar modelo primario
        logger.info(f"Intentando modelo primario: {self.primary_model['name']}")
        response = self._call_openrouter(self.primary_model, messages)
        
        if response:
            return {
                "success": True,
                "response": response,
                "model_used": self.primary_model["name"],
                "error": ""
            }
        
        # Fallback a modelo secundario
        logger.warning(f"Modelo primario falló, usando fallback: {self.fallback_model['name']}")
        response = self._call_openrouter(self.fallback_model, messages)
        
        if response:
            return {
                "success": True,
                "response": response,
                "model_used": self.fallback_model["name"],
                "error": ""
            }
        
        # Ambos modelos fallaron
        logger.error("Ambos modelos LLM fallaron")
        return {
            "success": False,
            "response": "",
            "model_used": "none",
            "error": "No se pudo conectar con ningún modelo LLM"
        }
    
    def detect_emergency_type(self, text: str) -> dict:
        """
        Usa el LLM para detectar el tipo de emergencia (sin clasificar para el usuario).
        Solo para uso interno del sistema.
        """
        detection_prompt = f"""Analiza este mensaje y determina si describe una emergencia real.
Si es una emergencia, indica el tipo (medical, fire, accident, assault, natural_disaster, drowning, intoxication, electrical, other).
Si NO es una emergencia, indica "none".

Mensaje: {text}

Responde SOLO con JSON:
{{"is_emergency": true/false, "type": "tipo", "urgency": "high/medium/low", "needs_112": true/false}}"""
        
        messages = [{"role": "user", "content": detection_prompt}]
        
        response = self._call_openrouter(self.primary_model, messages)
        
        if response:
            try:
                # Extraer JSON de la respuesta
                json_str = response.strip()
                if "```json" in json_str:
                    json_str = json_str.split("```json")[1].split("```")[0]
                elif "```" in json_str:
                    json_str = json_str.split("```")[1].split("```")[0]
                
                result = json.loads(json_str)
                return result
            except Exception as e:
                logger.error(f"Error parseando detección de emergencia: {e}")
                return {"is_emergency": False, "type": "unknown", "urgency": "low", "needs_112": False}
        
        return {"is_emergency": False, "type": "unknown", "urgency": "low", "needs_112": False}


# Instancia global del servicio
llm_service = LLMService()