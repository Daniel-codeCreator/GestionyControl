USE PROYCPROGRA2;
GO

/* ============================================================
   PERFIL FACIAL
   Un usuario puede tener un único perfil facial activo.
   ============================================================ */

CREATE TABLE PerfilFacial
(
    IdPerfilFacial INT IDENTITY(1,1) PRIMARY KEY,

    IdUsuario INT NOT NULL,

    Embedding VARBINARY(MAX) NOT NULL,

    Modelo VARCHAR(100) NOT NULL,

    Dimension INT NOT NULL,

    FechaRegistro DATETIME2 NOT NULL
        CONSTRAINT DF_PerfilFacial_FechaRegistro
        DEFAULT SYSDATETIME(),

    FechaActualizacion DATETIME2 NOT NULL
        CONSTRAINT DF_PerfilFacial_FechaActualizacion
        DEFAULT SYSDATETIME(),

    Activo BIT NOT NULL
        CONSTRAINT DF_PerfilFacial_Activo
        DEFAULT 1,

    CantidadMuestras INT NOT NULL
        CONSTRAINT DF_PerfilFacial_CantidadMuestras
        DEFAULT 1,

    QualityScore FLOAT NULL,

    VersionAlgoritmo VARCHAR(50) NOT NULL
        CONSTRAINT DF_PerfilFacial_VersionAlgoritmo
        DEFAULT '1.0',

    CONSTRAINT UQ_PerfilFacial_Usuario
        UNIQUE (IdUsuario),

    CONSTRAINT FK_PerfilFacial_Usuario
        FOREIGN KEY (IdUsuario)
            REFERENCES Usuario(IdUsuario),

    CONSTRAINT CK_PerfilFacial_Dimension
        CHECK (Dimension > 0),

    CONSTRAINT CK_PerfilFacial_Muestras
        CHECK (CantidadMuestras > 0)
);
GO


/* ============================================================
   AUDITORÍA DEL RECONOCIMIENTO FACIAL
   ============================================================ */

CREATE TABLE AuditoriaFacial
(
    IdAuditoria BIGINT IDENTITY(1,1) PRIMARY KEY,

    IdUsuario INT NULL,

    FechaHora DATETIME2 NOT NULL
        CONSTRAINT DF_AuditoriaFacial_FechaHora
        DEFAULT SYSDATETIME(),

    Accion VARCHAR(30) NOT NULL,

    Resultado VARCHAR(20) NULL,

    Score FLOAT NULL,

    Threshold FLOAT NULL,

    Detalle VARCHAR(1000) NULL,

    Dispositivo VARCHAR(200) NULL,

    CONSTRAINT FK_AuditoriaFacial_Usuario
        FOREIGN KEY (IdUsuario)
            REFERENCES Usuario(IdUsuario)
            ON DELETE SET NULL,

    CONSTRAINT CK_AuditoriaFacial_Accion
        CHECK
            (
            Accion IN
            (
             'ENROLL_INICIADO',
             'ENROLL_EXITOSO',
             'ENROLL_FALLIDO',
             'VERIFY_INICIADO',
             'VERIFY_EXITOSO',
             'VERIFY_FALLIDO',
             'LIVENESS_FALLIDO',
             'CAMARA_NO_DISPONIBLE',
             'PERFIL_ELIMINADO'
                )
            )
);
GO


CREATE INDEX IX_AuditoriaFacial_UsuarioFecha
    ON AuditoriaFacial(IdUsuario, FechaHora DESC);
GO