/*
   Crear usuario directamente en la base de datos (sin pasar por la aplicacion).

   - La contrasena debe enviarse YA CIFRADA con BCrypt (mismo algoritmo que
     usa la aplicacion: org.mindrot.jbcrypt.BCrypt con coste 12).
   - El usuario queda ACTIVO pero SIN rostro registrado, por lo que todavia no
     podra iniciar sesion hasta que un administrador lo enrolle:
         cd facial-service
         .\enroll-user.ps1 <IdUsuario>
   - La operacion deja auditoria USUARIO_CREADO en la tabla Auditoria.

   HASH BCrypt DE EJEMPLO (cambiala en produccion):
     Admin123!   -> $2a$12$E56gPGzNEYxzlADLuOEnJ.Bx9ouNyJuZRsrxqWhZiFHjWugE4qWhW
     Usuario123*  -> $2a$12$2o7Pq7f4a6ZVpdcwHKShgOIEOjNCNeMXN15mnQtdilTJgXnfbQZeq
*/

USE [PROYCPROGRA2];
GO
SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

CREATE OR ALTER PROCEDURE [dbo].[CrearUsuarioDirecto]
    @Nombre VARCHAR(100),
    @Apellido VARCHAR(100),
    @Usuario VARCHAR(50),
    @ContrasenaHash VARCHAR(255),
    @Correo VARCHAR(150),
    @IdRol INT,
    @IdUsuarioSalida INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @Nombre IS NULL OR LTRIM(RTRIM(@Nombre)) = ''
    BEGIN
        THROW 50020, 'El nombre es obligatorio.', 1;
    END;

    IF @Apellido IS NULL OR LTRIM(RTRIM(@Apellido)) = ''
    BEGIN
        THROW 50021, 'El apellido es obligatorio.', 1;
    END;

    IF @Usuario IS NULL OR LTRIM(RTRIM(@Usuario)) = ''
    BEGIN
        THROW 50022, 'El nombre de usuario es obligatorio.', 1;
    END;

    /* La contrasena debe ser un hash BCrypt de 60 caracteres ($2a$, $2b$ o $2y$). */
    IF @ContrasenaHash IS NULL
       OR LEN(LTRIM(RTRIM(@ContrasenaHash))) <> 60
       OR LEFT(LTRIM(RTRIM(@ContrasenaHash)), 4) NOT IN ('$2a$', '$2b$', '$2y$')
    BEGIN
        THROW 50023, 'La contrasena debe enviarse cifrada con BCrypt (hash de 60 caracteres).', 1;
    END;

    IF @Correo IS NULL OR @Correo NOT LIKE '%_@_%.__%'
    BEGIN
        THROW 50024, 'El correo no tiene un formato valido.', 1;
    END;

    IF @IdRol IS NULL
       OR NOT EXISTS (SELECT 1 FROM dbo.Rol WHERE IdRol = @IdRol AND Estado = 'ACTIVO')
    BEGIN
        THROW 50010, 'El rol indicado no existe o esta inactivo.', 1;
    END;

    IF EXISTS (SELECT 1 FROM dbo.Usuario WHERE NombreUsuario = @Usuario)
    BEGIN
        THROW 50012, 'Ya existe un usuario con ese nombre de usuario.', 1;
    END;

    IF EXISTS (SELECT 1 FROM dbo.Usuario WHERE Correo = @Correo)
    BEGIN
        THROW 50013, 'Ya existe un usuario con ese correo.', 1;
    END;

    BEGIN TRY
        BEGIN TRANSACTION;

        INSERT INTO dbo.Usuario
        (
            Nombre,
            Apellido,
            NombreUsuario,
            Contrasena,
            Correo,
            IdRol,
            Estado
        )
        VALUES
        (
            LTRIM(RTRIM(@Nombre)),
            LTRIM(RTRIM(@Apellido)),
            LTRIM(RTRIM(@Usuario)),
            LTRIM(RTRIM(@ContrasenaHash)),
            LTRIM(RTRIM(@Correo)),
            @IdRol,
            'ACTIVO'
        );

        SET @IdUsuarioSalida = CAST(SCOPE_IDENTITY() AS INT);

        INSERT INTO dbo.Auditoria
        (
            IdUsuario,
            FechaHora,
            Accion,
            Entidad,
            IdRegistro,
            Descripcion
        )
        VALUES
        (
            @IdUsuarioSalida,
            SYSDATETIME(),
            'USUARIO_CREADO',
            'Usuario',
            @IdUsuarioSalida,
            'Usuario creado directamente en la base de datos; falta registrar su rostro para iniciar sesion.'
        );

        COMMIT TRANSACTION;

        SELECT @IdUsuarioSalida AS IdUsuario;

    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
        BEGIN
            ROLLBACK TRANSACTION;
        END;
        THROW;
    END CATCH;
END;
GO

/*
   EJEMPLOS DE USO (ejecutar uno por vez en SQL Server Management Studio):

   -- ADMIN (rol 1) con la contrasena Admin123!
   DECLARE @id INT;
   EXEC dbo.CrearUsuarioDirecto
        'Nombre', 'Apellido', 'usuario.admin',
        '$2a$12$E56gPGzNEYxzlADLuOEnJ.Bx9ouNyJuZRsrxqWhZiFHjWugE4qWhW',
        'usuario.admin@umg.edu.gt', 1, @id OUTPUT;
   SELECT @id AS IdUsuario;

   -- ENCARGADO (rol 2) con la contrasena Usuario123*
   DECLARE @id2 INT;
   EXEC dbo.CrearUsuarioDirecto
        'Nombre', 'Apellido', 'usuario.encargado',
        '$2a$12$2o7Pq7f4a6ZVpdcwHKShgOIEOjNCNeMXN15mnQtdilTJgXnfbQZeq',
        'usuario.encargado@umg.edu.gt', 2, @id2 OUTPUT;
   SELECT @id2 AS IdUsuario;

   -- Como generar el hash de otra contrasena (desde la carpeta del proyecto):
   --   1) Abrir IntelliJ y ejecutar una clase con:
   --        System.out.println(BCrypt.hashpw("MiClave123!", BCrypt.gensalt(12)));
   --   2) Copiar el resultado como parametro @ContrasenaHash.
*/
