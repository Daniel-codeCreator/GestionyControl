-- Creacion de las BD
CREATE DATABASE PROYCPROGRA2;
GO

USE PROYCPROGRA2;
GO


-- creacion de la tablas
CREATE TABLE Rol
(
    IdRol INT IDENTITY(1,1) PRIMARY KEY,

    Nombre VARCHAR(50) NOT NULL,

    Descripcion VARCHAR(200) NULL,

    Estado VARCHAR(10) NOT NULL
        CONSTRAINT DF_Rol_Estado DEFAULT 'ACTIVO',

    CONSTRAINT UQ_Rol_Nombre
        UNIQUE (Nombre),

    CONSTRAINT CK_Rol_Estado
        CHECK (Estado IN ('ACTIVO', 'INACTIVO'))
);
GO

CREATE TABLE Usuario
(
    IdUsuario INT IDENTITY(1,1) PRIMARY KEY,

    Nombre VARCHAR(100) NOT NULL,

    Apellido VARCHAR(100) NOT NULL,

    NombreUsuario VARCHAR(50) NOT NULL,

    Contrasena VARCHAR(255) NOT NULL,

    Correo VARCHAR(150) NOT NULL,

    IdRol INT NOT NULL,

    Estado VARCHAR(10) NOT NULL
        CONSTRAINT DF_Usuario_Estado DEFAULT 'ACTIVO',

    FechaCreacion DATETIME2 NOT NULL
        CONSTRAINT DF_Usuario_FechaCreacion DEFAULT SYSDATETIME(),

    CONSTRAINT UQ_Usuario_NombreUsuario
        UNIQUE (NombreUsuario),

    CONSTRAINT UQ_Usuario_Correo
        UNIQUE (Correo),

    CONSTRAINT CK_Usuario_Estado
        CHECK (Estado IN ('ACTIVO', 'INACTIVO')),

    CONSTRAINT FK_Usuario_Rol
        FOREIGN KEY (IdRol)
            REFERENCES Rol(IdRol)
);
GO

CREATE TABLE Laboratorio
(
    IdLaboratorio INT IDENTITY(1,1) PRIMARY KEY,

    Codigo VARCHAR(20) NOT NULL,

    Nombre VARCHAR(100) NOT NULL,

    Ubicacion VARCHAR(200) NOT NULL,

    Capacidad INT NOT NULL,

    Estado VARCHAR(10) NOT NULL
        CONSTRAINT DF_Laboratorio_Estado DEFAULT 'ACTIVO',

    CONSTRAINT UQ_Laboratorio_Codigo
        UNIQUE (Codigo),

    CONSTRAINT CK_Laboratorio_Capacidad
        CHECK (Capacidad > 0),

    CONSTRAINT CK_Laboratorio_Estado
        CHECK (Estado IN ('ACTIVO', 'INACTIVO'))
);
GO

CREATE TABLE TipoEquipo
(
    IdTipoEquipo INT IDENTITY(1,1) PRIMARY KEY,

    Nombre VARCHAR(100) NOT NULL,

    Descripcion VARCHAR(200) NULL,

    Estado VARCHAR(10) NOT NULL
        CONSTRAINT DF_TipoEquipo_Estado DEFAULT 'ACTIVO',

    CONSTRAINT UQ_TipoEquipo_Nombre
        UNIQUE (Nombre),

    CONSTRAINT CK_TipoEquipo_Estado
        CHECK (Estado IN ('ACTIVO', 'INACTIVO'))
);
GO

CREATE TABLE Equipo
(
    IdEquipo INT IDENTITY(1,1) PRIMARY KEY,

    CodigoInventario VARCHAR(50) NOT NULL,

    Nombre VARCHAR(100) NOT NULL,

    IdTipoEquipo INT NOT NULL,

    Marca VARCHAR(100) NULL,

    Modelo VARCHAR(100) NULL,

    NumeroSerie VARCHAR(100) NULL,

    IdLaboratorio INT NOT NULL,

    Estado VARCHAR(20) NOT NULL
        CONSTRAINT DF_Equipo_Estado DEFAULT 'DISPONIBLE',

    Observaciones VARCHAR(500) NULL,

    FechaRegistro DATETIME2 NOT NULL
        CONSTRAINT DF_Equipo_FechaRegistro DEFAULT SYSDATETIME(),

    CONSTRAINT UQ_Equipo_CodigoInventario
        UNIQUE (CodigoInventario),

    CONSTRAINT CK_Equipo_Estado
        CHECK
            (
            Estado IN
            (
             'DISPONIBLE',
             'PRESTADO',
             'DANADO',
             'MANTENIMIENTO',
             'BAJA'
                )
            ),

    CONSTRAINT FK_Equipo_TipoEquipo
        FOREIGN KEY (IdTipoEquipo)
            REFERENCES TipoEquipo(IdTipoEquipo),

    CONSTRAINT FK_Equipo_Laboratorio
        FOREIGN KEY (IdLaboratorio)
            REFERENCES Laboratorio(IdLaboratorio)
);
GO

CREATE TABLE Prestamo
(
    IdPrestamo INT IDENTITY(1,1) PRIMARY KEY,

    Solicitante VARCHAR(150) NOT NULL,

    FechaPrestamo DATETIME2 NOT NULL
        CONSTRAINT DF_Prestamo_FechaPrestamo DEFAULT SYSDATETIME(),

    FechaEsperadaDevolucion DATETIME2 NOT NULL,

    IdUsuarioRegistra INT NOT NULL,

    Estado VARCHAR(20) NOT NULL
        CONSTRAINT DF_Prestamo_Estado DEFAULT 'ACTIVO',

    Observaciones VARCHAR(500) NULL,

    CONSTRAINT CK_Prestamo_Estado
        CHECK (Estado IN ('ACTIVO', 'FINALIZADO')),

    CONSTRAINT CK_Prestamo_Fechas
        CHECK (FechaEsperadaDevolucion >= FechaPrestamo),

    CONSTRAINT FK_Prestamo_Usuario
        FOREIGN KEY (IdUsuarioRegistra)
            REFERENCES Usuario(IdUsuario)
);
GO

CREATE TABLE DetallePrestamo
(
    IdDetallePrestamo INT IDENTITY(1,1) PRIMARY KEY,

    IdPrestamo INT NOT NULL,

    IdEquipo INT NOT NULL,

    Estado VARCHAR(20) NOT NULL
        CONSTRAINT DF_DetallePrestamo_Estado DEFAULT 'PENDIENTE',

    CONSTRAINT CK_DetallePrestamo_Estado
        CHECK (Estado IN ('PENDIENTE', 'DEVUELTO')),

    /*
       Evita agregar dos veces el mismo equipo
       al mismo prestamo.
    */
    CONSTRAINT UQ_DetallePrestamo_PrestamoEquipo
        UNIQUE (IdPrestamo, IdEquipo),

    CONSTRAINT FK_DetallePrestamo_Prestamo
        FOREIGN KEY (IdPrestamo)
            REFERENCES Prestamo(IdPrestamo),

    CONSTRAINT FK_DetallePrestamo_Equipo
        FOREIGN KEY (IdEquipo)
            REFERENCES Equipo(IdEquipo)
);
GO

CREATE TABLE Devolucion
(
    IdDevolucion INT IDENTITY(1,1) PRIMARY KEY,

    IdDetallePrestamo INT NOT NULL,

    FechaDevolucion DATETIME2 NOT NULL
        CONSTRAINT DF_Devolucion_Fecha DEFAULT SYSDATETIME(),

    Condicion VARCHAR(10) NOT NULL,

    Observacion VARCHAR(500) NULL,

    IdUsuarioRecibe INT NOT NULL,

    /*
       Un detalle solamente puede devolverse una vez.
       Esto ayuda a cumplir RN-11.
    */
    CONSTRAINT UQ_Devolucion_Detalle
        UNIQUE (IdDetallePrestamo),

    CONSTRAINT CK_Devolucion_Condicion
        CHECK (Condicion IN ('BUENO', 'DANADO')),

    CONSTRAINT FK_Devolucion_DetallePrestamo
        FOREIGN KEY (IdDetallePrestamo)
            REFERENCES DetallePrestamo(IdDetallePrestamo),

    CONSTRAINT FK_Devolucion_Usuario
        FOREIGN KEY (IdUsuarioRecibe)
            REFERENCES Usuario(IdUsuario)
);
GO

CREATE TABLE Mantenimiento
(
    IdMantenimiento INT IDENTITY(1,1) PRIMARY KEY,

    IdEquipo INT NOT NULL,

    FechaIngreso DATETIME2 NOT NULL
        CONSTRAINT DF_Mantenimiento_FechaIngreso DEFAULT SYSDATETIME(),

    Tipo VARCHAR(20) NOT NULL,

    Descripcion VARCHAR(500) NOT NULL,

    Responsable VARCHAR(150) NOT NULL,

    FechaSalida DATETIME2 NULL,

    Resultado VARCHAR(20) NULL,

    Estado VARCHAR(20) NOT NULL
        CONSTRAINT DF_Mantenimiento_Estado DEFAULT 'EN_PROCESO',

    Observaciones VARCHAR(500) NULL,

    IdUsuarioRegistra INT NOT NULL,

    CONSTRAINT CK_Mantenimiento_Tipo
        CHECK (Tipo IN ('PREVENTIVO', 'CORRECTIVO')),

    CONSTRAINT CK_Mantenimiento_Estado
        CHECK (Estado IN ('EN_PROCESO', 'FINALIZADO')),

    CONSTRAINT CK_Mantenimiento_Resultado
        CHECK
            (
            Resultado IS NULL
                OR Resultado IN ('REPARADO', 'NO_REPARABLE')
            ),

    CONSTRAINT CK_Mantenimiento_Fechas
        CHECK
            (
            FechaSalida IS NULL
                OR FechaSalida >= FechaIngreso
            ),

    CONSTRAINT FK_Mantenimiento_Equipo
        FOREIGN KEY (IdEquipo)
            REFERENCES Equipo(IdEquipo),

    CONSTRAINT FK_Mantenimiento_Usuario
        FOREIGN KEY (IdUsuarioRegistra)
            REFERENCES Usuario(IdUsuario)
);
GO

CREATE TABLE Auditoria
(
    IdAuditoria BIGINT IDENTITY(1,1) PRIMARY KEY,

    /*
       Puede ser NULL porque un LOGIN_FALLIDO puede ocurrir
       sin que exista un usuario autenticado.
    */
    IdUsuario INT NULL,

    FechaHora DATETIME2 NOT NULL
        CONSTRAINT DF_Auditoria_FechaHora DEFAULT SYSDATETIME(),

    Accion VARCHAR(100) NOT NULL,

    Entidad VARCHAR(100) NOT NULL,

    IdRegistro INT NULL,

    Descripcion VARCHAR(500) NULL,

    CONSTRAINT FK_Auditoria_Usuario
        FOREIGN KEY (IdUsuario)
            REFERENCES Usuario(IdUsuario)
);
GO

CREATE INDEX IX_Equipo_Estado
    ON Equipo(Estado);
GO

/*
   Facilita buscar equipos por laboratorio.
*/
CREATE INDEX IX_Equipo_Laboratorio
    ON Equipo(IdLaboratorio);
GO

/*
   Facilita buscar prestamos por estado.
*/
CREATE INDEX IX_Prestamo_Estado
    ON Prestamo(Estado);
GO

/*
   Facilita consultas por fecha.
*/
CREATE INDEX IX_Prestamo_FechaPrestamo
    ON Prestamo(FechaPrestamo);
GO

/*
   Facilita consultar los mantenimientos de un equipo.
*/
CREATE INDEX IX_Mantenimiento_Equipo
    ON Mantenimiento(IdEquipo);
GO

/*
   Facilita consultar auditoria por usuario.
*/
CREATE INDEX IX_Auditoria_Usuario
    ON Auditoria(IdUsuario);
GO


-- ingresa los tipos de roles
INSERT INTO Rol
(
    Nombre,
    Descripcion
)
VALUES
(
    'ADMIN',
    'Administrador con acceso completo al sistema'
),
(
    'ENCARGADO',
    'Encargado de equipos, prestamos, devoluciones y mantenimiento'
),
(
    'DOCENTE',
    'Usuario docente para consultas o solicitudes'
);
GO


-- ingresa los tipos de equipos

INSERT INTO TipoEquipo
(
    Nombre,
    Descripcion
)
VALUES
('Laptop', 'Computadoras portatiles'),
('Camara', 'Camaras utilizadas en laboratorios'),
('Proyector', 'Proyectores multimedia'),
('Router', 'Equipos de comunicacion de red'),
('Switch', 'Equipos de conmutacion de red'),
('Tablet', 'Dispositivos tablet'),
('Monitor', 'Monitores'),
('Kit Electronico', 'Kits utilizados en practicas de electronica');
GO


SELECT
    TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;
GO