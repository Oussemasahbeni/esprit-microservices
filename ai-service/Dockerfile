# Build context is this directory (esprit-microservices/ai-service).
#   docker build -f ai-service/Dockerfile -t esprit-ai-service ai-service

FROM python:3.12-slim
WORKDIR /app

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Bake the pretrained model into the image so the container doesn't need
# internet access or a slow cold-start download on first request.
RUN python -c "from transformers import pipeline; pipeline('sentiment-analysis', model='distilbert-base-uncased-finetuned-sst-2-english', framework='pt')"

COPY app ./app

EXPOSE 8090
# --http httptools: the gateway's proxied requests use HTTP/1.1 framing that
# h11 (uvicorn's default parser) rejects as malformed; httptools is more
# lenient and handles it correctly.
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090", "--http", "httptools"]
