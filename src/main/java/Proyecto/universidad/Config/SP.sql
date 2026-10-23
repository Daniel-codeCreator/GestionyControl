Sotred Procedure para poder crear el usuario

USE [PROYCPROGRA2]
GO
/****** Object:  StoredProcedure [dbo].[CrearUsuario]    Script Date: 22/10/2026 22:13:54 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
create or ALTER procedure [dbo].[CrearUsuario]
	@Nombre varchar (100),
	@Apellido varchar (100),
	@Usuario varchar (100),
	@Contrasena varchar (500),
	@Correo varchar (150),
	@idRol int

	--BITACORA
	--Autor; Daniel Boc
	--21102026; Se crea la primera version de sp para crear usuario

	as
begin
		set nocount on;

		if exists (select 1 from Usuario where NombreUsuario = @Usuario)
begin
			raiserror('el nombre de usuario ya existe', 16, 1);
			return
end

		if exists (select 1 from Usuario where Correo = @Correo)
begin
			raiserror('el correo ya tiene un usuario registrado', 16, 1);
			return
end

insert into Usuario (Nombre, Apellido, NombreUsuario, Contrasena, Correo, IdRol, Estado, FechaCreacion)
values (@Nombre, @Apellido, @Usuario, @Contrasena, @Correo, @idRol,'Activo', SYSDATETIME())

select SCOPE_IDENTITY() as idUsuario
end