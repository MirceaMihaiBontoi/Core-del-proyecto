# Backend Python - Clasificador de Emergencias

Backend basado en FastAPI que clasifica emergencias a partir de texto libre usando inteligencia artificial.

## Arquitectura

```
Java (terminal/Android) --HTTP--> FastAPI ---> Modelo IA ---> Respuesta JSON
```

El usuario describe su emergencia en texto libre o por voz (ej: "me duele mucho el pecho"). El backend:
1. Corrige errores ortograficos (doble pasada: vocabulario CSV + pyspellchecker)
2. Clasifica el texto con un modelo de Machine Learning (scikit-learn)
3. Detecta emergencias secundarias si el texto las justifica
4. Devuelve: tipo(s) de emergencia, prioridad, confianza, contexto e instrucciones de actuacion

## Categorias de emergencia

| Tipo | Descripcion | Prioridad |
|------|-------------|-----------|
| MEDICAL | Emergencias medicas | 9 |
| FIRE | Incendios | 8 |
| SECURITY | Seguridad ciudadana | 8 |
| TRAFFIC | Accidentes de trafico | 7 |
| NATURAL | Desastres naturales | 7 |

## Modelo de IA

El modelo usa **TF-IDF con n-gramas de caracteres** + **Regresion Logistica** (scikit-learn).

- **Dataset**: ~150 frases en espanol etiquetadas manualmente (incluye frases cortas para contextos de emergencia real)
- **Precision**: ~86% en validacion cruzada
- **Archivo**: `models/emergency_classifier.pkl` (ya incluido en el repositorio)

> **Nota**: Este modelo es basico, pensado para un ejercicio academico. En un proyecto real se usaria un modelo preentrenado mas potente como BETO (BERT en espanol) o similar, con un dataset mucho mas grande y tecnicas avanzadas de NLP.

## Requisitos

- Python 3.10 o superior

## Instalacion

```bash
cd python-backend
pip install -r requirements.txt
```

## Uso

### Arrancar el servidor

PowerShell:
```powershell
cd python-backend; python -m uvicorn server:app --host 0.0.0.0 --port 8000
```

CMD:
```cmd
cd python-backend && python -m uvicorn server:app --host 0.0.0.0 --port 8000
```

El servidor estara disponible en `http://localhost:8000`

### Probar la clasificacion (opcional)

```bash
curl -X POST http://localhost:8000/classify -H "Content-Type: application/json" -d "{\"text\": \"me duele mucho el pecho\"}"
```

Respuesta (emergencia simple):
```json
{
  "priority": 9,
  "corrected_text": "me duele mucho el pecho",
  "emergencies": [
    {
      "type": "MEDICAL",
      "type_name": "Emergencia Medica",
      "confidence": 0.83,
      "context": "un posible problema cardiaco",
      "instructions": [
        "Siente a la persona en posicion comoda, semi-incorporada",
        "Afloje ropa ajustada (cinturon, corbata, camisa)",
        "Si tiene medicacion para el corazon, ayudele a tomarla"
      ]
    }
  ]
}
```

Respuesta (emergencia mixta):
```json
{
  "priority": 9,
  "corrected_text": "mi casa se ha incendiado y me he quemado la pierna",
  "emergencies": [
    {
      "type": "FIRE",
      "type_name": "Incendio",
      "confidence": 0.68,
      "context": "un incendio en una vivienda",
      "instructions": ["Evacue la zona inmediatamente", "..."]
    },
    {
      "type": "MEDICAL",
      "type_name": "Emergencia Medica",
      "confidence": 0.28,
      "context": "una quemadura",
      "instructions": ["Enfrie la quemadura con agua fria durante al menos 10 minutos", "..."]
    }
  ]
}
```

### Reentrenar el modelo (opcional)

Si se modifica el dataset en `data/emergencies_dataset.csv`:

```bash
python train_model.py
```

Esto regenera el archivo `models/emergency_classifier.pkl`.

### Endpoints

| Metodo | Ruta | Descripcion |
|--------|------|-------------|
| POST | `/classify` | Clasifica texto de emergencia |
| GET | `/geolocate` | Geolocalizacion aproximada por IP |
| POST | `/transcribe` | Graba audio del microfono y transcribe a texto |
| GET | `/health` | Comprueba que el servidor esta activo |

### Reconocimiento de voz

El endpoint `/transcribe` graba audio del microfono del PC y lo transcribe usando Google Speech API gratuita.

Requiere: `sounddevice`, `soundfile`, `SpeechRecognition`

En Android se reemplazaria por `SpeechRecognizer` nativo, sin necesidad de este endpoint.

## Estructura

```
python-backend/
├── data/
│   ├── emergencies_dataset.csv    # Dataset de entrenamiento
│   └── emergency_config.json      # Instrucciones y contextos por tipo
├── models/
│   └── emergency_classifier.pkl   # Modelo entrenado (incluido)
├── train_model.py                 # Script de entrenamiento
├── server.py                      # Servidor FastAPI
├── requirements.txt               # Dependencias Python
└── README.md
```
