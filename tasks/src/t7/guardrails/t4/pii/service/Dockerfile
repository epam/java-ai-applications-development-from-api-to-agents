FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
RUN python -m spacy download en_core_web_sm
COPY app.py .
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8060"]
