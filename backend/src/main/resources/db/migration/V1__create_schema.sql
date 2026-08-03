CREATE TABLE rol (
    id_rol INT NOT NULL,
    nombre VARCHAR(20) NOT NULL,
    CONSTRAINT pk_rol PRIMARY KEY (id_rol),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre),
    CONSTRAINT chk_rol_nombre CHECK (nombre IN ('ROLE_ADMIN' , 'ROLE_USER'))
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

CREATE TABLE usuario (
    id_usuario INT NOT NULL AUTO_INCREMENT,
    id_rol INT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellido_paterno VARCHAR(100) NOT NULL,
    apellido_materno VARCHAR(100) NULL,
    fecha_nacimiento DATE NULL,
    correo VARCHAR(255) NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_correo UNIQUE (correo),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol)
        REFERENCES rol (id_rol)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_usuario_rol (id_rol)
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

CREATE TABLE tipo_transaccion (
    id_tipo_transaccion INT NOT NULL,
    nombre VARCHAR(20) NOT NULL,
    CONSTRAINT pk_tipo_transaccion PRIMARY KEY (id_tipo_transaccion),
    CONSTRAINT uq_tipo_transaccion_nombre UNIQUE (nombre),
    CONSTRAINT chk_tipo_transaccion_nombre CHECK (nombre IN ('INGRESO' , 'GASTO'))
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

CREATE TABLE salud_financiera (
    id_salud_financiera INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL,
    CONSTRAINT pk_salud_financiera PRIMARY KEY (id_salud_financiera),
    CONSTRAINT uq_salud_financiera_nombre UNIQUE (nombre)
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

CREATE TABLE categoria_gasto (
    id_categoria_gasto INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    CONSTRAINT pk_categoria_gasto PRIMARY KEY (id_categoria_gasto),
    CONSTRAINT uq_categoria_gasto_nombre UNIQUE (nombre),
    CONSTRAINT chk_categoria_gasto_nombre CHECK (nombre IN ('Alimentación' , 'Transporte',
        'Salud',
        'Vivienda',
        'Educación',
        'Ocio',
        'Servicios',
        'Otras'))
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

CREATE TABLE transaccion (
    id_transaccion INT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    id_tipo_transaccion INT NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    monto DECIMAL(12 , 2 ) NOT NULL,
    fecha DATE NOT NULL,
    CONSTRAINT pk_transaccion PRIMARY KEY (id_transaccion),
    CONSTRAINT chk_transaccion_monto CHECK (monto > 0),
    CONSTRAINT fk_transaccion_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_transaccion_tipo_transaccion FOREIGN KEY (id_tipo_transaccion)
        REFERENCES tipo_transaccion (id_tipo_transaccion)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_transaccion_usuario_fecha (id_usuario , fecha),
    INDEX idx_transaccion_tipo (id_tipo_transaccion)
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

CREATE TABLE analisis_financiero (
    id_analisis_financiero INT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    id_salud_financiera INT NOT NULL,
    mes INTEGER NOT NULL,
    anio INTEGER NOT NULL,
    fecha_generacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_analisis_financiero PRIMARY KEY (id_analisis_financiero),
    CONSTRAINT chk_analisis_mes CHECK (mes BETWEEN 1 AND 12),
    CONSTRAINT chk_analisis_anio CHECK (anio > 0),
    CONSTRAINT fk_analisis_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_analisis_salud_financiera FOREIGN KEY (id_salud_financiera)
        REFERENCES salud_financiera (id_salud_financiera)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_analisis_usuario_periodo (id_usuario , anio , mes),
    INDEX idx_analisis_salud_financiera (id_salud_financiera)
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

CREATE TABLE clasificacion_transaccion (
    id_clasificacion_transaccion INT NOT NULL AUTO_INCREMENT,
    id_analisis_financiero INT NOT NULL,
    probabilidad DECIMAL(4 , 3 ) NOT NULL,
    CONSTRAINT pk_clasificacion_transaccion PRIMARY KEY (id_clasificacion_transaccion),
    CONSTRAINT uq_clasificacion_analisis UNIQUE (id_analisis_financiero),
    CONSTRAINT chk_clasificacion_probabilidad CHECK (probabilidad BETWEEN 0 AND 1),
    CONSTRAINT fk_clasificacion_analisis FOREIGN KEY (id_analisis_financiero)
        REFERENCES analisis_financiero (id_analisis_financiero)
        ON DELETE CASCADE ON UPDATE CASCADE
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

CREATE TABLE resumen_gasto (
    id_resumen_gasto INT NOT NULL AUTO_INCREMENT,
    id_clasificacion_transaccion INT NOT NULL,
    id_categoria_gasto INT NOT NULL,
    monto_total DECIMAL(12 , 2 ) NOT NULL,
    CONSTRAINT pk_resumen_gasto PRIMARY KEY (id_resumen_gasto),
    CONSTRAINT uq_resumen_clasificacion_categoria UNIQUE (id_clasificacion_transaccion , id_categoria_gasto),
    CONSTRAINT chk_resumen_monto CHECK (monto_total >= 0),
    CONSTRAINT fk_resumen_clasificacion FOREIGN KEY (id_clasificacion_transaccion)
        REFERENCES clasificacion_transaccion (id_clasificacion_transaccion)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_resumen_categoria FOREIGN KEY (id_categoria_gasto)
        REFERENCES categoria_gasto (id_categoria_gasto)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_resumen_categoria (id_categoria_gasto)
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;

CREATE TABLE recomendacion (
    id_recomendacion INT NOT NULL AUTO_INCREMENT,
    id_analisis_financiero INT NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    CONSTRAINT pk_recomendacion PRIMARY KEY (id_recomendacion),
    CONSTRAINT fk_recomendacion_analisis FOREIGN KEY (id_analisis_financiero)
        REFERENCES analisis_financiero (id_analisis_financiero)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_recomendacion_analisis (id_analisis_financiero)
)  ENGINE=INNODB DEFAULT CHARSET=UTF8MB4 COLLATE = UTF8MB4_UNICODE_CI;
