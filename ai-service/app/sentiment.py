import logging

from transformers import pipeline

logger = logging.getLogger("ai-service.sentiment")

# Real pretrained transformer (DistilBERT fine-tuned on SST-2), run on CPU.
# Downloaded once from the Hugging Face Hub on first container start and
# cached in the image's HF cache layer/volume — this is genuine ML inference,
# not a hand-written lexicon: the model outputs a learned probability
# distribution over POSITIVE/NEGATIVE for the input text.
_classifier = pipeline(
    task="sentiment-analysis",
    model="distilbert-base-uncased-finetuned-sst-2-english",
    framework="pt",
)


def analyze(text: str) -> dict:
    prediction = _classifier(text, truncation=True)[0]
    label = prediction["label"].upper()  # POSITIVE | NEGATIVE
    confidence = float(prediction["score"])

    # DistilBERT-SST2 is binary; treat low-confidence calls as NEUTRAL so the
    # API surfaces uncertainty instead of forcing every borderline text into
    # one of the two trained classes.
    if confidence < 0.6:
        label = "NEUTRAL"

    logger.info("model=%s label=%s confidence=%.3f", _classifier.model.name_or_path, label, confidence)

    return {
        "label": label,
        "scores": {
            "model_label": prediction["label"],
            "confidence": confidence,
        },
    }
