INSERT INTO analisis_financiero (
    id_analisis_financiero, id_usuario, id_salud_financiera, mes, anio, fecha_generacion
) VALUES
    -- Ana (usuario 1)
    (1, 1, 1, 1, 2026, '2026-02-01 10:00:00'),
    (2, 1, 1, 3, 2026, '2026-04-01 10:00:00'),
    (3, 1, 1, 5, 2026, '2026-06-01 10:00:00'),
    (4, 1, 2, 8, 2026, '2026-09-01 10:00:00'),
    -- Carlos (usuario 2)
    (5, 2, 2, 4, 2026, '2026-05-01 10:00:00'),
    (6, 2, 1, 9, 2026, '2026-10-01 10:00:00'),
    -- Sofía (usuario 3)
    (7, 3, 1, 1, 2026, '2026-02-01 10:00:00'),
    (8, 3, 1, 7, 2026, '2026-08-01 10:00:00'),
    (9, 3, 1, 11, 2026, '2026-12-01 10:00:00'),
    -- Diego (usuario 4)
    (10, 4, 1, 2, 2026, '2026-03-01 10:00:00'),
    (11, 4, 1, 4, 2026, '2026-05-01 10:00:00'),
    (12, 4, 2, 9, 2026, '2026-10-01 10:00:00'),
    (13, 4, 1, 11, 2026, '2026-12-01 10:00:00'),
    -- Mariana (usuario 5)
    (14, 5, 2, 1, 2026, '2026-02-01 10:00:00'),
    (15, 5, 1, 3, 2026, '2026-04-01 10:00:00'),
    (16, 5, 1, 8, 2026, '2026-09-01 10:00:00'),
    (17, 5, 1, 11, 2026, '2026-12-01 10:00:00'),
    (18, 5, 1, 12, 2026, '2027-01-01 10:00:00'),
    -- Jorge (usuario 6)
    (19, 6, 1, 2, 2026, '2026-03-01 10:00:00'),
    (20, 6, 1, 5, 2026, '2026-06-01 10:00:00'),
    (21, 6, 2, 7, 2026, '2026-08-01 10:00:00'),
    (22, 6, 1, 10, 2026, '2026-11-01 10:00:00'),
    (23, 6, 1, 12, 2026, '2027-01-01 10:00:00'),
    -- Valeria (usuario 7)
    (24, 7, 1, 1, 2026, '2026-02-01 10:00:00'),
    (25, 7, 1, 3, 2026, '2026-04-01 10:00:00'),
    (26, 7, 1, 6, 2026, '2026-07-01 10:00:00'),
    (27, 7, 2, 8, 2026, '2026-09-01 10:00:00'),
    (28, 7, 3, 11, 2026, '2026-12-01 10:00:00'),
    (29, 7, 2, 12, 2026, '2027-01-01 10:00:00'),
    -- Ricardo (usuario 8)
    (30, 8, 2, 2, 2026, '2026-03-01 10:00:00'),
    (31, 8, 2, 5, 2026, '2026-06-01 10:00:00'),
    (32, 8, 1, 9, 2026, '2026-10-01 10:00:00'),
    (33, 8, 2, 12, 2026, '2027-01-01 10:00:00');

INSERT INTO clasificacion_transaccion (
    id_clasificacion_transaccion, id_analisis_financiero, probabilidad
) VALUES
    (1, 1, 0.850),
    (2, 2, 0.920),
    (3, 3, 0.800),
    (4, 4, 0.660),
    (5, 5, 0.740),
    (6, 6, 0.890),
    (7, 7, 0.820),
    (8, 8, 0.870),
    (9, 9, 0.790),
    (10, 10, 0.860),
    (11, 11, 0.830),
    (12, 12, 0.610),
    (13, 13, 0.900),
    (14, 14, 0.700),
    (15, 15, 0.840),
    (16, 16, 0.880),
    (17, 17, 0.860),
    (18, 18, 0.810),
    (19, 19, 0.850),
    (20, 20, 0.780),
    (21, 21, 0.640),
    (22, 22, 0.830),
    (23, 23, 0.870),
    (24, 24, 0.880),
    (25, 25, 0.850),
    (26, 26, 0.900),
    (27, 27, 0.650),
    (28, 28, 0.520),
    (29, 29, 0.690),
    (30, 30, 0.720),
    (31, 31, 0.670),
    (32, 32, 0.840),
    (33, 33, 0.700);

INSERT INTO resumen_gasto (
    id_clasificacion_transaccion, id_categoria_gasto, monto_total
) VALUES
    -- Ana
    (1, 1, 410.00), (1, 4, 1100.00), (1, 7, 6540.00), (1, 5, 1330.00),
    (2, 1, 4270.00), (2, 4, 5650.00), (2, 7, 12950.00), (2, 5, 6110.00),
    (3, 1, 1970.00), (3, 4, 1100.00), (3, 7, 6060.00), (3, 5, 1330.00),
    (4, 1, 4010.00), (4, 4, 1100.00), (4, 7, 6540.00), (4, 5, 1330.00),
    -- Carlos
    (5, 1, 348.50), (5, 4, 935.00), (5, 7, 7251.50), (5, 5, 1130.50),
    (6, 1, 3977.50), (6, 4, 4802.50), (6, 7, 11077.50), (6, 5, 5194.00),
    -- Sofía
    (7, 1, 451.00), (7, 4, 1210.00), (7, 7, 9966.00), (7, 5, 1463.00),
    (8, 1, 13137.00), (8, 4, 6215.00), (8, 7, 15162.00), (8, 5, 6721.00),
    (9, 1, 451.00), (9, 4, 1210.00), (9, 7, 9966.00), (9, 5, 1463.00),
    -- Diego
    (10, 1, 512.50), (10, 4, 1375.00), (10, 7, 10112.50), (10, 5, 1662.50),
    (11, 1, 5337.50), (11, 4, 7062.50), (11, 7, 15652.50), (11, 5, 7637.50),
    (12, 1, 512.50), (12, 4, 1375.00), (12, 7, 10112.50), (12, 5, 1662.50),
    (13, 1, 512.50), (13, 4, 1375.00), (13, 7, 13200.00), (13, 5, 1662.50),
    -- Mariana
    (14, 1, 369.00), (14, 4, 990.00), (14, 7, 6504.00), (14, 5, 1197.00),
    (15, 1, 369.00), (15, 4, 990.00), (15, 7, 11412.00), (15, 5, 1197.00),
    (16, 1, 4215.00), (16, 4, 5085.00), (16, 7, 14338.00), (16, 5, 5499.00),
    (17, 1, 369.00), (17, 4, 990.00), (17, 7, 6504.00), (17, 5, 1197.00),
    (18, 1, 369.00), (18, 4, 990.00), (18, 7, 10272.00), (18, 5, 1197.00),
    -- Jorge
    (19, 1, 471.50), (19, 4, 1265.00), (19, 7, 9279.50), (19, 5, 1529.50),
    (20, 1, 471.50), (20, 4, 1265.00), (20, 7, 9279.50), (20, 5, 1529.50),
    (21, 1, 10962.00), (21, 4, 6502.50), (21, 7, 16124.50), (21, 5, 7027.00),
    (22, 1, 471.50), (22, 4, 1265.00), (22, 7, 9279.50), (22, 5, 1529.50),
    (23, 1, 471.50), (23, 4, 1265.00), (23, 7, 13245.50), (23, 5, 1529.50),
    -- Valeria
    (24, 1, 574.00), (24, 4, 1540.00), (24, 7, 12390.00), (24, 5, 1862.00),
    (25, 1, 574.00), (25, 4, 1540.00), (25, 7, 18192.00), (25, 5, 1862.00),
    (26, 1, 14792.00), (26, 4, 7910.00), (26, 7, 21386.00), (26, 5, 8554.00),
    (27, 1, 574.00), (27, 4, 1540.00), (27, 7, 12390.00), (27, 5, 1862.00),
    (28, 1, 574.00), (28, 4, 1540.00), (28, 7, 18690.00), (28, 5, 1862.00),
    (29, 1, 1470.00), (29, 4, 1540.00), (29, 7, 4410.00), (29, 5, 1862.00),
    -- Ricardo
    (30, 1, 430.50), (30, 4, 1155.00), (30, 7, 8899.50), (30, 5, 1396.50),
    (31, 1, 6510.00), (31, 4, 5950.00), (31, 7, 13745.50), (31, 5, 6414.00),
    (32, 1, 430.50), (32, 4, 1155.00), (32, 7, 8899.50), (32, 5, 1396.50),
    (33, 1, 430.50), (33, 4, 1155.00), (33, 7, 13857.50), (33, 5, 1396.50);

INSERT INTO recomendacion (
    id_analisis_financiero, descripcion
) VALUES
    (1, 'Tu salud financiera es saludable. Considera incrementar tu ahorro mensual un 5% para fortalecer tu fondo de emergencia.'),
    (1, 'Realiza un solo viaje al supermercado por semana y planifica un menú para reducir gastos de alimentación.'),
    (2, 'Excelente mes! Tus ingresos superaron ampliamente tus gastos. Evalúa destinar el excedente a inversiones de bajo riesgo.'),
    (2, 'Revisa tus servicios básicos y compara proveedores para reducir costos fijos mensuales.'),
    (3, 'Tus gastos se mantienen estables. Mantén la disciplina y revisa las suscripciones que menos utilizas.'),
    (3, 'Considera agrupar pequeños gastos recurrentes para negociar un mejor precio.'),
    (4, 'Tu salud financiera disminuyó este mes. Analiza los incrementos en gastos fijos y busca optimizarlos.'),
    (5, 'Mantén un presupuesto por categoría de gasto y da seguimiento semanal para evitar desviaciones.'),
    (6, 'Buen comportamiento financiero. Sigue registrando tus ingresos y gastos para mantener la tendencia positiva.'),
    (7, 'Disminuye los gastos en ocio y destina al menos un 10% de tus ingresos al ahorro.'),
    (8, 'Eleva la meta de ahorro mensual. Tu capacidad de ahorro actual te permite un margen mayor.'),
    (9, 'Revisa tus gastos de vivienda y transportes, podrían estar sobre el promedio recomendado.'),
    (10, 'Tu salud financiera es sólida. Considera crear metas de inversión a mediano plazo.'),
    (11, 'Mantiene el equilibrio entre ingresos y gastos. Evita financiar consumos con ingreso anticipado.'),
    (12, 'Los gastos subieron ligeramente. Identifica las categorías con mayor incremento para controlarlas.'),
    (13, 'Refuerza tu fondo de ahorro e invierte el superávit en instrumentos de bajo riesgo.'),
    (14, 'Establece un límite a tus gastos variables y da prioridad al ahorro desde el inicio del mes.'),
    (15, 'Continúa con la buena administración. Revisa las tarifas bancarias para optimizar tu flujo.'),
    (16, 'Excelente mes! Destina un porcentaje mayor de tus ingresos al ahorro e inversión.'),
    (17, 'Mantiene el control sobre sus gastos. Evalúa automatizar el ahorro mensual.'),
    (18, 'Buen cierre de año. Considera consolidar tus deudas para iniciar el próximo año con mejor liquidez.'),
    (19, 'Cumple con tu presupuesto. Revisa tus categorías de gasto para mantener el equilibrio.'),
    (20, 'Mantiene un perfil estable. Sugiere aumentar ahorro e iniciar un plan de inversión.'),
    (21, 'Tus gastos subieron ante el análisis. Reduce gastos no esenciales para recuperar salud financiera.'),
    (22, 'Equilibrio financiero adecuado. Sigue monitoreando tus gastos recurrentes.'),
    (23, 'Buen fin de año. Refuerza tu ahorro y establece nuevas metas financieras.'),
    (24, 'Tu capacidad de ahorro es alta. Aprovecha para invertir en activos de mediano plazo.'),
    (25, 'Mantiene el buen desempeño. Automatiza tus aportaciones al ahorro.'),
    (26, 'Excelente administración mensual. Considera diversificar tus inversiones.'),
    (27, 'Tus gastos presentan un incremento. Revisa el presupuesto por categorías y recorta lo innecesario.'),
    (28, 'Tu salud financiera está en riesgo. Prioriza reducir deudas y gastos variables de forma inmediata.'),
    (29, 'Mejoró respecto al mes anterior, pero mantén la disciplina y evita nuevos gastos fijos.'),
    (30, 'Mantiene una salud moderada. Reduce gastos en ocio y servicios para mejorar tu margen.'),
    (31, 'Los gastos superan lo esperado. Elabora un plan de reducción de gastos por categoría.'),
    (32, 'Buen mes financiero. Sigue la tendencia y consolida tus ahorros.'),
    (33, 'Mantiene superávit. Refuerza tu fondo de emergencia ante la variabilidad de inicio de año.');