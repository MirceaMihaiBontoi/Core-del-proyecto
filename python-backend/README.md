# Backend Python - Clasificador de Emergencias

Backend basado en FastAPI que clasifica emergencias a partir de texto libre usando inteligencia artificial.

## Arquitectura

```
Java (terminal/Android) --HTTP POST--> FastAPI ---> Modelo IA ---> Respuesta JSON
```

El usuario describe su emergencia en texto libre (ej: "me duele mucho el pecho"). El backend:
1. Corrige errores ortograficos con `pyspellchecker`
2. Clasifica el texto con un modelo de Machine Learning (scikit-learn)
3. Devuelve: tipo de emergencia, prioridad, confianza e instrucciones de actuacion

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

- **Dataset**: 100 frases en espanol etiquetadas manualmente (20 por categoria)
- **Precision**: ~82% en validacion cruzada (5-fold)
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

```bash
cd python-backend
python -m uvicorn server:app --host 0.0.0.0 --port 8000
```

El servidor estara disponible en `http://localhost:8000`

### Probar la clasificacion

```bash
curl -X POST http://localhost:8000/classify -H "Content-Type: application/json" -d "{\"text\": \"me duele mucho el pecho\"}"
```

Respuesta:
```json
{
  "type": "MEDICAL",
  "type_name": "Emergencia Medica",
  "priority": 9,
  "confidence": 0.80,
  "corrected_text": "me duele mucho el pecho",
  "instructions": [
    "Mantenga la calma y no mueva al paciente",
    "Compruebe si respira y tiene pulso",
    "Si no respira, inicie RCP si sabe hacerlo",
    "Mantenga al paciente abrigado y comodo",
    "No le de comida ni bebida"
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
| GET | `/health` | Comprueba que el servidor esta activo |

## Estructura

```
python-backend/
├── data/
│   └── emergencies_dataset.csv   # Dataset de entrenamiento
├── models/
│   └── emergency_classifier.pkl  # Modelo entrenado (incluido)
├── train_model.py                # Script de entrenamiento
├── server.py                     # Servidor FastAPI
├── requirements.txt              # Dependencias Python
└── README.md
```
