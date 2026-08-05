"""
Generador de dataset sintetico para FinanceAI
================================================
Genera dos datasets relacionados por user_id:

1. dataset_transacciones.csv
   Nivel transaccion -> usado para entrenar el CLASIFICADOR DE GASTOS
   (descripcion de texto -> categoria)

2. dataset_usuarios_perfil.csv
   Nivel usuario (agregado) -> usado para entrenar el CLASIFICADOR DE
   PERFIL FINANCIERO (features numericas/categoricas -> perfil)

Ejecutar con: python3 generar_dataset_financeai.py
"""

import random
from datetime import datetime, timedelta

import numpy as np
import pandas as pd

# ==========================================================
# CONFIGURACION GENERAL
# ==========================================================
SEED = 42
np.random.seed(SEED)
random.seed(SEED)

N_USUARIOS = 800
MIN_TRANSACCIONES = 8
MAX_TRANSACCIONES = 26

# ==========================================================
# CATALOGO DE CATEGORIAS: comercios tipicos + rango de valores
# Ajusta libremente nombres de comercios y rangos segun tu contexto
# ==========================================================
CATEGORIAS = {
    "Alimentacion": {
        "comercios": ["Exito", "Carulla", "Supermercado D1", "Ara Tienda", "Jumbo",
                      "Walmart Supercenter", "Soriana", "Chedraui", "OXXO",
                      "Bodega Aurrera", "Superama"],
        "rango_valor": (10, 270),
    },
    "Transporte": {
        "comercios": ["Combustible Terpel", "Estacion Servicio Primax", "Cabify",
                      "TransMilenio", "Gasolinera Pemex", "Uber", "Didi",
                      "Metrobus CDMX", "Peaje Autopista"],
        "rango_valor": (10, 210),
    },
    "Salud": {
        "comercios": ["Farmacia Cruz Verde", "Farmatodo", "EPS Sura Copago",
                      "Coomeva Medicina Prepagada", "Farmacias del Ahorro",
                      "Farmacias Guadalajara", "Farmacias Similares", "Consultorio IMSS"],
        "rango_valor": (15, 300),
    },
    "Vivienda": {
        "comercios": ["Arriendo Apartamento", "Administracion Conjunto Residencial",
                      "Homecenter", "Renta Departamento", "Administracion Condominio",
                      "Home Depot Mexico", "Pago Infonavit"],
        "rango_valor": (150, 650),
    },
    "Educacion": {
        "comercios": ["Matricula Universidad Nacional", "Icetex Credito Educativo",
                      "Platzi Curso Online", "Matricula UNAM",
                      "Colegiatura Colegio Privado", "Coursera Curso Online"],
        "rango_valor": (25, 400),
    },
    "Ocio": {
        "comercios": ["Netflix", "Spotify Premium", "Disney Plus", "Cine Colombia",
                      "Cinepolis", "Steam Videojuegos", "Bar Restaurante Amigos",
                      "Agencia de Viajes"],
        "rango_valor": (5, 130),
    },
    "Servicios": {
        "comercios": ["Factura Energia Enel Codensa", "Factura Acueducto",
                      "Claro Hogar Internet", "Movistar Plan Celular", "Factura CFE",
                      "Telmex Internet Hogar", "Telcel Plan Celular", "Izzi Telecom"],
        "rango_valor": (15, 150),
    },
    "Otros": {
        "comercios": ["Retiro Cajero Automatico", "Transferencia Nequi", "Daviplata",
                      "Pago Multa Transito", "Transferencia Mercado Pago",
                      "Recarga OXXO", "Deposito Banco Azteca"],
        "rango_valor": (10, 180),
    },
}

FRECUENCIAS_AHORRO = ["Baja", "Media", "Alta"]

# Probabilidad de que una transaccion pertenezca a cada categoria.
# Los gastos esenciales/pequenos (alimentacion, transporte, ocio) son mas
# frecuentes; los gastos grandes (vivienda, educacion) son mas esporadicos,
# tal como ocurriria en datos reales de un mes.
PESOS_CATEGORIA = {
    "Alimentacion": 0.26,
    "Transporte": 0.20,
    "Ocio": 0.15,
    "Servicios": 0.12,
    "Salud": 0.09,
    "Otros": 0.08,
    "Vivienda": 0.06,
    "Educacion": 0.04,
}
_CATS = list(PESOS_CATEGORIA.keys())
_PESOS = list(PESOS_CATEGORIA.values())

# ------------------------------------------------------------------
# Ambiguedad de texto: en la banca real, muchas descripciones NO revelan
# la categoria por si solas ("COMPRA", "PAGO", "TRANSFERENCIA"...). Sin
# esto, el vocabulario queda cerrado y perfectamente separable por
# categoria, lo que produce clasificadores con accuracy irrealmente alto
# (100%). Se agregan descripciones genericas + errores tipograficos
# ocasionales para que el problema sea un desafio de clasificacion real.
# ------------------------------------------------------------------
DESCRIPCIONES_GENERICAS = [
    "COMPRA", "PAGO", "TRANSFERENCIA", "RETIRO", "SERVICIO",
    "RECIBO DE PAGO", "COMPRA VARIOS", "PAGO EN LINEA", "TRANSACCION",
    "ABONO", "CONSUMO", "COBRO AUTOMATICO",
]
PROB_DESCRIPCION_GENERICA = 0.12
PROB_TYPO = 0.10

# RNG dedicado solo a la generacion de texto (independiente de `random`),
# para que la ambiguedad de texto no altere el orden de los sorteos que
# ya definen ingreso, endeudamiento, categoria, valor y perfil financiero
# (mantiene esas distribuciones ya validadas exactamente iguales).
rng_texto = random.Random(SEED + 100)


def introducir_typo(texto):
    """Simula un error tipografico simple (swap, borrado o duplicado de un
    caracter), como pasaria en datos reales de banco."""
    if len(texto) < 4:
        return texto
    tipo = rng_texto.choice(["swap", "delete", "duplicate"])
    pos = rng_texto.randint(0, len(texto) - 2)
    if tipo == "swap":
        letras = list(texto)
        letras[pos], letras[pos + 1] = letras[pos + 1], letras[pos]
        return "".join(letras)
    if tipo == "delete":
        return texto[:pos] + texto[pos + 1:]
    return texto[:pos] + texto[pos] + texto[pos:]


def generar_descripcion_ruidosa(nombre_comercio: str) -> str:
    """Agrega ruido realista (mayusculas, codigos, ciudad) para que la limpieza
    de texto en el EDA tenga sentido, tal como pasaria con datos reales de banco.
    Ademas, con cierta probabilidad reemplaza el nombre del comercio por una
    descripcion generica (COMPRA, PAGO, etc.) que no revela la categoria por
    si sola, y ocasionalmente introduce un error tipografico — esto evita que
    el vocabulario quede perfectamente separado por categoria."""
    es_generica = rng_texto.random() < PROB_DESCRIPCION_GENERICA
    base = rng_texto.choice(DESCRIPCIONES_GENERICAS) if es_generica else nombre_comercio

    texto = base.upper()
    if rng_texto.random() < 0.5:
        texto += f" #{rng_texto.randint(100, 9999)}"
    if rng_texto.random() < 0.3:
        ciudad = rng_texto.choice([
            "BOGOTA", "MEDELLIN", "CALI", "CUCUTA", "BARRANQUILLA", "CARTAGENA",
            "CIUDAD DE MEXICO", "GUADALAJARA", "MONTERREY", "PUEBLA", "TIJUANA", "QUERETARO",
        ])
        texto += f" {ciudad}"
    if rng_texto.random() < 0.2:
        texto += f" REF{rng_texto.randint(100000, 999999)}"

    if not es_generica and rng_texto.random() < PROB_TYPO:
        texto = introducir_typo(texto)

    return texto


def generar_valor(rango: tuple) -> float:
    minimo, maximo = rango
    return round(np.random.uniform(minimo, maximo), 2)


def generar_fecha_aleatoria() -> str:
    hoy = datetime(2026, 7, 31)
    dias_atras = random.randint(0, 180)
    return (hoy - timedelta(days=dias_atras)).strftime("%Y-%m-%d")


def generar_dataset(n_usuarios: int = N_USUARIOS):
    transacciones_rows = []
    usuarios_rows = []

    for user_id in range(1, n_usuarios + 1):
        # ----- Atributos base del usuario -----
        ingreso_mensual = float(np.clip(np.random.lognormal(mean=8.2, sigma=0.4), 1000, 15000))
        ingreso_mensual = round(ingreso_mensual, 2)

        nivel_endeudamiento = round(np.random.beta(2, 3) * 90, 1)

        # La frecuencia de ahorro esta levemente influenciada por el ingreso (mas realista)
        if ingreso_mensual > 6000:
            pesos = [0.20, 0.35, 0.45]
        elif ingreso_mensual > 3000:
            pesos = [0.30, 0.45, 0.25]
        else:
            pesos = [0.50, 0.40, 0.10]
        frecuencia_ahorro = np.random.choice(FRECUENCIAS_AHORRO, p=pesos)

        # ----- Transacciones del usuario -----
        n_trans = random.randint(MIN_TRANSACCIONES, MAX_TRANSACCIONES)
        gasto_por_categoria = {cat: 0.0 for cat in CATEGORIAS}

        for _ in range(n_trans):
            categoria = random.choices(_CATS, weights=_PESOS, k=1)[0]
            comercio = random.choice(CATEGORIAS[categoria]["comercios"])
            descripcion = generar_descripcion_ruidosa(comercio)
            valor = generar_valor(CATEGORIAS[categoria]["rango_valor"])
            fecha = generar_fecha_aleatoria()

            transacciones_rows.append({
                "user_id": user_id,
                "fecha": fecha,
                "descripcion": descripcion,
                "valor": valor,
                "categoria": categoria,
            })
            gasto_por_categoria[categoria] += valor

        gasto_total = round(sum(gasto_por_categoria.values()), 2)
        ratio_gasto_ingreso = round(gasto_total / ingreso_mensual, 3)

        # ----- Regla base para generar el perfil financiero -----
        # (sirve como "verdad de terreno" simulada; se agrega ruido para que
        # el problema sea un desafio real de clasificacion y no 100% deterministico)
        score = 0
        if ratio_gasto_ingreso > 0.9:
            score += 2
        elif ratio_gasto_ingreso > 0.7:
            score += 1

        if nivel_endeudamiento > 50:
            score += 2
        elif nivel_endeudamiento > 30:
            score += 1

        if frecuencia_ahorro == "Baja":
            score += 1
        elif frecuencia_ahorro == "Alta":
            score -= 1

        if score >= 3:
            perfil = "En riesgo"
        elif score >= 1:
            perfil = "En observacion"
        else:
            perfil = "Saludable"

        # Ruido de etiqueta (~7%) para simular imperfeccion del mundo real
        if random.random() < 0.07:
            perfil = random.choice(["Saludable", "En observacion", "En riesgo"])

        fila_usuario = {
            "user_id": user_id,
            "ingreso_mensual": ingreso_mensual,
            "nivel_endeudamiento": nivel_endeudamiento,
            "frecuencia_ahorro": frecuencia_ahorro,
            "num_transacciones": n_trans,
            "gasto_total": gasto_total,
            "ratio_gasto_ingreso": ratio_gasto_ingreso,
            "perfil_financiero": perfil,
        }
        for cat in CATEGORIAS:
            fila_usuario[f"gasto_{cat.lower()}"] = round(gasto_por_categoria[cat], 2)

        usuarios_rows.append(fila_usuario)

    df_transacciones = pd.DataFrame(transacciones_rows)
    df_usuarios = pd.DataFrame(usuarios_rows)
    return df_transacciones, df_usuarios


if __name__ == "__main__":
    df_transacciones, df_usuarios = generar_dataset()

    df_transacciones.to_csv("dataset_transacciones.csv", index=False)
    df_usuarios.to_csv("dataset_usuarios_perfil.csv", index=False)

    print("Dataset de transacciones:", df_transacciones.shape)
    print(df_transacciones.head(), "\n")

    print("Dataset de usuarios / perfil financiero:", df_usuarios.shape)
    print(df_usuarios.head(), "\n")

    print("Distribucion de categorias de gasto:")
    print(df_transacciones["categoria"].value_counts(), "\n")

    print("Distribucion de perfiles financieros:")
    print(df_usuarios["perfil_financiero"].value_counts())
