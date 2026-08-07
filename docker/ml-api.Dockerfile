# Imagen del servicio de Machine Learning (API FastAPI que corre los modelos de data-science)
FROM python:3.11-slim

ENV PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

# Dependencias (cache optimo: primero copiamos solo los requirements)
COPY data-science/ml-api/requirements.txt /app/ml-api/requirements.txt
RUN pip install --upgrade pip \
    && pip install -r /app/ml-api/requirements.txt

# Codigo de la API y modelos/artefactos
COPY data-science/ml-api/ /app/ml-api/
COPY data-science/ /app/data-science/

# El codigo carga los artefactos desde MODEL_DIR (raiz de data-science)
ENV MODEL_DIR=/app/data-science \
    PORT=8000

EXPOSE 8000

CMD ["uvicorn", "ml-api.app:app", "--host", "0.0.0.0", "--port", "8000"]