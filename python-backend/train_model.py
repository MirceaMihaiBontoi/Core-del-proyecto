import pandas as pd
import joblib
from pathlib import Path
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.model_selection import cross_val_score

DATA_PATH = Path(__file__).parent / "data" / "emergencies_dataset.csv"
MODEL_PATH = Path(__file__).parent / "models" / "emergency_classifier.pkl"

def train():
    df = pd.read_csv(DATA_PATH)
    X = df["text"]
    y = df["label"]

    pipeline = Pipeline([
        ("tfidf", TfidfVectorizer(
            analyzer="char_wb",
            ngram_range=(2, 5),
            min_df=1,
            sublinear_tf=True
        )),
        ("clf", LogisticRegression(
            max_iter=1000,
            C=5.0,
            class_weight="balanced"
        ))
    ])

    scores = cross_val_score(pipeline, X, y, cv=5, scoring="accuracy")
    print(f"Accuracy (cross-validation 5-fold): {scores.mean():.2%} (+/- {scores.std():.2%})")

    pipeline.fit(X, y)

    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(pipeline, MODEL_PATH)
    print(f"Modelo guardado en: {MODEL_PATH}")

if __name__ == "__main__":
    train()
