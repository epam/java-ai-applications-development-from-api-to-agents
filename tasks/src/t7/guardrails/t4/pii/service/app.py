from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.responses import JSONResponse
from presidio_analyzer import AnalyzerEngine
from presidio_analyzer.nlp_engine import NlpEngineProvider
from presidio_anonymizer import AnonymizerEngine
from pydantic import BaseModel


# ── Shared engine instances (loaded once at startup) ───────────────────────────

analyzer: AnalyzerEngine
anonymizer: AnonymizerEngine


@asynccontextmanager
async def lifespan(app: FastAPI):
    global analyzer, anonymizer

    nlp_configuration = {
        "nlp_engine_name": "spacy",
        "models": [{"lang_code": "en", "model_name": "en_core_web_sm"}],
    }
    provider = NlpEngineProvider(nlp_configuration=nlp_configuration)
    analyzer = AnalyzerEngine(nlp_engine=provider.create_engine())
    anonymizer = AnonymizerEngine()

    print("PII service ready — Presidio + spaCy en_core_web_sm loaded.")
    yield


app = FastAPI(title="PII Redaction Service", lifespan=lifespan)


# ── Request / response models ──────────────────────────────────────────────────

class RedactRequest(BaseModel):
    text: str


class RedactResponse(BaseModel):
    redacted: str


# ── Endpoints ──────────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/redact", response_model=RedactResponse)
def redact(req: RedactRequest) -> RedactResponse:
    """Analyse `text` for PII and return the anonymised version."""
    if not req.text:
        return RedactResponse(redacted=req.text)

    results = analyzer.analyze(text=req.text, language="en")
    anonymized = anonymizer.anonymize(text=req.text, analyzer_results=results)

    return RedactResponse(redacted=anonymized.text)
