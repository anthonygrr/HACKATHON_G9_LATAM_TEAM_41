"""API de servicios de Ciencia de Datos - FinanceAI.

Expone los modelos entrenados como servicio REST consumido por el backend
(Spring Boot) via HTTP JSON:

    POST /clasificar-transaccion    -> clasifica UNA descripcion en categoria de gasto
    POST /clasificar-transacciones  -> clasifica UN LOTE de descripciones (array)
    GET  /clasificar?descripcion=...  -> idem (util para debug)
    POST /analisis-financiero        -> perfil financiero + resumen + recomendaciones
    GET  /health                      -> healthcheck (docker)

Los artefactos (.pkl / .json) se cargan desde MODEL_DIR (por defecto, la raiz
de data-science, carpeta padre de ml-api/).
"""

from __future__ import annotations

import json
import os
import re
import unicodedata
from typing import List

import joblib
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

# ---------------------------------------------------------------------------
# Rutas y carga de artefactos
# ---------------------------------------------------------------------------
BASE_DIR = os.path.normpath(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_MODEL_DIR = os.path.normpath(os.path.join(BASE_DIR, ".."))
MODEL_DIR = os.environ.get("MODEL_DIR", DEFAULT_MODEL_DIR)

# Si MODEL_DIR (p.ej. un valor obsoleto o relativo al cwd) no contiene los artefactos,
# se usa la raiz de data-science (carpeta padre de ml-api/) como fallback.
if not os.path.exists(os.path.join(MODEL_DIR, "modelo_clasificador_gastos.pkl")):
    MODEL_DIR = DEFAULT_MODEL_DIR


def _load(name: str):
    path = os.path.join(MODEL_DIR, name)
    if not os.path.exists(path):
        raise RuntimeError(f"Artefacto no encontrado: {path}")
    return joblib.load(path)


def _load_json(name: str):
    path = os.path.join(MODEL_DIR, name)
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


modelo_gastos = _load("modelo_clasificador_gastos.pkl")
vectorizador_tfidf = _load("tfidf_vectorizer.pkl")
modelo_perfil = _load("modelo_perfil_financiero.pkl")
scaler_perfil = _load("scaler_perfil.pkl")
encoder_ahorro = _load("encoder_frecuencia_ahorro.pkl")

metadata_perfil = _load_json("metadata_modelo_perfil.json")
columnas_features = metadata_perfil["columnas_features"]
columnas_numericas_continuas = metadata_perfil["columnas_numericas_continuas"]
requiere_escalado = metadata_perfil["requiere_escalado"]

reglas_rec = _load_json("reglas_recomendaciones.json")
REGLAS = reglas_rec["reglas"]
NOMBRES_CATEGORIA = reglas_rec["nombres_categoria_display"]
MENSAJES_PERFIL = reglas_rec["mensajes_perfil_general"]

app = FastAPI(
    title="FinanceAI - ML API",
    description="Clasificacion de transacciones y analisis de perfil financiero.",
    version="1.0.0",
)

# ---------------------------------------------------------------------------
# Preprocesamiento (identico al de los notebooks)
# ---------------------------------------------------------------------------
CIUDADES_RUIDO = [
    "ciudad de mexico", "bogota", "medellin", "barranquilla", "cartagena",
    "guadalajara", "monterrey", "queretaro", "cucuta", "cali", "tijuana", "puebla",
]

CATEGORIAS_BASE = [
    "alimentacion", "transporte", "salud", "vivienda",
    "educacion", "ocio", "servicios", "otros",
]


def quitar_tildes(texto: str) -> str:
    texto_norm = unicodedata.normalize("NFKD", texto)
    return "".join(c for c in texto_norm if not unicodedata.combining(c))


def limpiar_descripcion(texto) -> str:
    texto = (texto or "").lower()
    texto = quitar_tildes(texto)
    texto = re.sub(r"ref\d+", " ", texto)
    texto = re.sub(r"#\d+", " ", texto)
    for ciudad in CIUDADES_RUIDO:
        texto = texto.replace(ciudad, " ")
    texto = re.sub(r"[^a-z\s]", " ", texto)
    texto = re.sub(r"\s+", " ", texto).strip()
    return texto


def clasificar_gasto(descripcion_cruda: str) -> dict:
    """Texto crudo -> categoria (slug) + probabilidad."""
    texto_limpio = limpiar_descripcion(descripcion_cruda)
    vector = vectorizador_tfidf.transform([texto_limpio])
    predicha = str(modelo_gastos.predict(vector)[0])
    categoria = predicha.lower()

    prob = None
    if hasattr(modelo_gastos, "predict_proba"):
        clases = [str(c) for c in modelo_gastos.classes_]
        if predicha in clases:
            probas = modelo_gastos.predict_proba(vector)[0]
            prob = round(float(probas[clases.index(predicha)]), 4)

    return {
        "descripcion": descripcion_cruda,
        "categoria": categoria,
        "categoria_nombre": categoria.title(),
        "probabilidad": prob,
    }


def construir_features_usuario(ingreso_mensual, nivel_endeudamiento,
                               frecuencia_ahorro, transacciones):
    gasto_por_categoria = {c: 0.0 for c in CATEGORIAS_BASE}

    for t in transacciones:
        texto_limpio = limpiar_descripcion(t.descripcion)
        vector = vectorizador_tfidf.transform([texto_limpio])
        cat = str(modelo_gastos.predict(vector)[0]).lower()
        gasto_por_categoria[cat] = gasto_por_categoria.get(cat, 0.0) + float(t.monto)

    gasto_total = float(sum(gasto_por_categoria.values()))
    num_tx = len(transacciones)
    ratio = gasto_total / float(ingreso_mensual) if ingreso_mensual else 0.0
    gasto_promedio = gasto_total / num_tx if num_tx else 0.0
    cat_dominante = max(gasto_por_categoria, key=gasto_por_categoria.get)

    fila = {
        "ingreso_mensual": float(ingreso_mensual),
        "nivel_endeudamiento": float(nivel_endeudamiento),
        "num_transacciones": num_tx,
        "gasto_total": gasto_total,
        "ratio_gasto_ingreso": ratio,
        "gasto_promedio_transaccion": gasto_promedio,
    }
    for c in CATEGORIAS_BASE:
        fila[f"gasto_{c}"] = gasto_por_categoria[c]
        fila[f"pct_{c}"] = (gasto_por_categoria[c] / gasto_total) if gasto_total else 0.0
        fila[f"dominante_{c}"] = 1 if c == cat_dominante else 0

    fila["frecuencia_ahorro_cod"] = float(
        encoder_ahorro.transform(
            pd.DataFrame([[frecuencia_ahorro]], columns=["frecuencia_ahorro"])
        )[0][0]
    )

    df_fila = pd.DataFrame([fila]).reindex(columns=columnas_features, fill_value=0)
    return df_fila, gasto_por_categoria


def predecir_perfil(df_fila):
    if requiere_escalado:
        df_modelo = df_fila.copy()
        df_modelo[columnas_numericas_continuas] = scaler_perfil.transform(
            df_fila[columnas_numericas_continuas]
        )
    else:
        df_modelo = df_fila

    perfil = str(modelo_perfil.predict(df_modelo)[0])
    clases = [str(c) for c in modelo_perfil.classes_]
    probas = modelo_perfil.predict_proba(df_modelo)[0]
    prob = round(float(probas[clases.index(perfil)]), 4)
    return perfil, prob


def generar_recomendaciones(perfil, gasto_por_categoria, ingreso_mensual, ratio,
                            nivel_deu, frecuencia_ahorro):
    candidatas = []

    for categoria, umbral in REGLAS["umbral_pct_categoria"].items():
        gasto = gasto_por_categoria.get(categoria, 0.0)
        pct = gasto / float(ingreso_mensual) if ingreso_mensual else 0.0
        if pct > umbral:
            severidad = 2 if pct > umbral * 1.3 else 1
            nombre = NOMBRES_CATEGORIA.get(categoria, categoria)
            candidatas.append((
                severidad,
                f"Tu gasto en {nombre} representa el {pct * 100:.0f}% de tu ingreso "
                f"mensual (recomendado: hasta {umbral * 100:.0f}%); considera "
                f"reducirlo o revisar estos gastos.",
            ))

    if ratio > REGLAS["umbral_ratio_alto"]:
        candidatas.append((3,
            f"Tus gastos totales representan el {ratio * 100:.0f}% de tu ingreso "
            "mensual; estas gastando casi todo (o mas de) lo que ganas. Revisa tu "
            "presupuesto con urgencia."))
    elif ratio > REGLAS["umbral_ratio_medio"]:
        candidatas.append((2,
            f"Tus gastos totales representan el {ratio * 100:.0f}% de tu ingreso "
            "mensual; te queda poco margen. Vigila tus gastos recurrentes."))

    if nivel_deu > REGLAS["umbral_endeudamiento_alto"]:
        candidatas.append((3,
            f"Tu nivel de endeudamiento ({nivel_deu:.0f}%) es alto; prioriza reducir "
            "el uso de credito y evita adquirir nuevas deudas."))
    elif nivel_deu > REGLAS["umbral_endeudamiento_medio"]:
        candidatas.append((1,
            f"Tu nivel de endeudamiento ({nivel_deu:.0f}%) esta en un rango moderado; "
            "evita aumentarlo en los proximos meses."))

    if frecuencia_ahorro == "Baja":
        candidatas.append((2,
            "Tu frecuencia de ahorro es baja; intenta destinar una porcion fija de tu "
            "ingreso al ahorro cada mes, aunque sea pequena."))
    elif frecuencia_ahorro == "Media":
        candidatas.append((1, "Podrias aumentar tu frecuencia de ahorro para fortalecer "
                          "tu reserva financiera."))

    candidatas.sort(key=lambda x: x[0], reverse=True)
    max_rec = REGLAS["max_recomendaciones"]
    especificas = [m for _, m in candidatas[: max_rec - 1]]
    recomendaciones = [MENSAJES_PERFIL.get(perfil, "Revisa tu situacion financiera con un asesor.")]
    recomendaciones.extend(especificas)
    return recomendaciones[:max_rec]


def analisis_financiero_completo(ingreso_mensual, nivel_endeudamiento,
                                 frecuencia_ahorro, transacciones):
    df_fila, gasto_por_categoria = construir_features_usuario(
        ingreso_mensual, nivel_endeudamiento, frecuencia_ahorro, transacciones
    )
    perfil, probabilidad = predecir_perfil(df_fila)

    gasto_total = float(sum(gasto_por_categoria.values()))
    ratio = gasto_total / float(ingreso_mensual) if ingreso_mensual else 0.0

    recomendaciones = generar_recomendaciones(
        perfil, gasto_por_categoria, ingreso_mensual, ratio,
        nivel_endeudamiento, frecuencia_ahorro,
    )
    resumen_gastos = {k: round(v, 2) for k, v in gasto_por_categoria.items() if v > 0}

    return {
        "perfil_financiero": perfil,
        "probabilidad": probabilidad,
        "resumen_gastos": resumen_gastos,
        "recomendaciones": recomendaciones,
    }


# ---------------------------------------------------------------------------
# Schemas
# ---------------------------------------------------------------------------
class ClasifRequest(BaseModel):
    descripcion: str = Field(..., description="Descripcion cruda de la transaccion")


class ClasifBatchRequest(BaseModel):
    transacciones: list[ClasifRequest] = Field(
        ..., min_length=1, description="Lista de descripciones a clasificar"
    )


class TransaccionInput(BaseModel):
    descripcion: str
    monto: float


class AnalisisRequest(BaseModel):
    ingreso_mensual: float
    nivel_endeudamiento: float
    frecuencia_ahorro: str = Field(..., description="Alta | Media | Baja")
    transacciones: list[TransaccionInput]


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------
@app.get("/health")
def health():
    return {"status": "ok", "models_loaded": MODEL_DIR}


@app.post("/clasificar-transaccion")
def post_clasificar(req: ClasifRequest):
    try:
        return clasificar_gasto(req.descripcion)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))


@app.post("/clasificar-transacciones")
def post_clasificar_lote(req: ClasifBatchRequest):
    try:
        resultados = [clasificar_gasto(t.descripcion) for t in req.transacciones]
        return {"transacciones": resultados}
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))


@app.get("/clasificar")
def get_clasificar(descripcion: str):
    try:
        return clasificar_gasto(descripcion)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))


@app.post("/analisis-financiero")
def post_analisis(req: AnalisisRequest):
    try:
        return analisis_financiero_completo(
            req.ingreso_mensual, req.nivel_endeudamiento,
            req.frecuencia_ahorro, req.transacciones,
        )
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))