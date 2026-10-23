package Proyecto.universidad.repository;

import Proyecto.universidad.Config.connection;
import Proyecto.universidad.model.Usuario;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class UsuarioRepository {

    /**
     * Obtiene la lista completa de usuarios registrados en la base de datos.
     * Utilizado para poblar combox en vistas como la de Préstamos.
     */
    public List<Usuario> listarTodos() {
        List<Usuario> lista = new ArrayList<>();
        String strSql = """
                SELECT 
                    IdUsuario, Nombre, Apellido, NombreUsuario, 
                    Contrasena, Correo, IdRol, Estado, FechaCreacion
                FROM Usuario
                """;

        try (
                Connection conn = connection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(strSql);
                ResultSet rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                Usuario usuario = new Usuario();

                usuario.setIdUsuario(rs.getInt("IdUsuario"));
                usuario.setStrNombre(rs.getString("Nombre"));
                usuario.setStrApellido(rs.getString("Apellido"));
                usuario.setStrUsuario(rs.getString("NombreUsuario"));
                usuario.setStrPassword(rs.getString("Contrasena"));
                usuario.setStrCorreo(rs.getString("Correo"));
                usuario.setIntRol(rs.getInt("IdRol"));
                usuario.setStrEstado(rs.getString("Estado"));

                if (rs.getTimestamp("FechaCreacion") != null) {
                    usuario.setFechaCreacion(
                            rs.getTimestamp("FechaCreacion").toLocalDateTime()
                    );
                }

                lista.add(usuario);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar usuarios: " + e.getMessage(), e);
        }

        return lista;
    }

    /**
     * Busca un usuario específico por su NombreUsuario.
     */
    public Usuario buscarPorUsuario(String strUsuario) {
        String strSql = """
            SELECT
                IdUsuario, Nombre, Apellido, NombreUsuario,
                Contrasena, Correo, IdRol, Estado, FechaCreacion
            FROM Usuario
            WHERE UPPER(NombreUsuario) = UPPER(?) AND UPPER(Estado) = 'ACTIVO'
            """;

        try (
                Connection conn = connection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(strSql)
        ) {
            stmt.setString(1, strUsuario);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Usuario usuario = new Usuario();

                    usuario.setIdUsuario(rs.getInt("IdUsuario"));
                    usuario.setStrNombre(rs.getString("Nombre"));
                    usuario.setStrApellido(rs.getString("Apellido"));
                    usuario.setStrUsuario(rs.getString("NombreUsuario"));
                    usuario.setStrPassword(rs.getString("Contrasena"));
                    usuario.setStrCorreo(rs.getString("Correo"));
                    usuario.setIntRol(rs.getInt("IdRol"));
                    usuario.setStrEstado(rs.getString("Estado"));

                    if (rs.getTimestamp("FechaCreacion") != null) {
                        usuario.setFechaCreacion(
                                rs.getTimestamp("FechaCreacion").toLocalDateTime()
                        );
                    }

                    return usuario;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar usuario: " + e.getMessage(), e);
        }

        return null;
    }

    /**
     * Crea un usuario ejecutando el procedimiento dbo.sp_CrearUsuario.
     * El procedimiento además registra la auditoría USUARIO_CREADO.
     * strPassword debe llegar ya cifrada con BCrypt.
     */
    public Usuario crearUsuario(
            String strNombre,
            String strApellido,
            String strUsuario,
            String strPassword,
            String strCorreo,
            int intRol
    ) {
        String strSql = "{call dbo.sp_CrearUsuario(?, ?, ?, ?, ?, ?, ?, ?)}";

        try (
                Connection conn = connection.getConnection();
                CallableStatement stmt = conn.prepareCall(strSql)
        ) {
            stmt.setString(1, strNombre);
            stmt.setString(2, strApellido);
            stmt.setString(3, strUsuario);
            stmt.setString(4, strPassword);
            stmt.setString(5, strCorreo);
            stmt.setInt(6, intRol);
            stmt.setString(7, "ACTIVO");
            stmt.registerOutParameter(8, Types.INTEGER);

            stmt.execute();

            int intIdUsuario = stmt.getInt(8);

            Usuario usuario = new Usuario();
            usuario.setIdUsuario(intIdUsuario);
            usuario.setStrNombre(strNombre);
            usuario.setStrApellido(strApellido);
            usuario.setStrUsuario(strUsuario);
            usuario.setStrPassword(strPassword);
            usuario.setStrCorreo(strCorreo);
            usuario.setIntRol(intRol);
            usuario.setStrEstado("ACTIVO");

            return usuario;

        } catch (SQLException e) {
            throw new RuntimeException(mensajeErrorCreacion(e), e);
        }
    }

    /**
     * Convierte los códigos de error del procedimiento en mensajes comprensibles,
     * evitando mostrar SQLException crudas al usuario.
     */
    private String mensajeErrorCreacion(SQLException e) {
        return switch (e.getErrorCode()) {
            case 50010 -> "El rol seleccionado no existe o está inactivo.";
            case 50012 -> "Ya existe un usuario con ese nombre de usuario.";
            case 50013 -> "Ya existe un usuario con ese correo.";
            case 2601, 2627 -> "Ya existe un usuario con ese nombre de usuario o correo.";
            default -> "No se pudo crear el usuario: " + e.getMessage();
        };
    }
    public Usuario crearUsuarioConSP(String strNombre, String strApellido, String strUsuario,
                                     String strPassword, String strCorreo, int intRol)
    {
        String sqlCallable = "{CALL CrearUsuario(?, ?, ?, ?, ?, ?)}";

        try (
                Connection conn = connection.getConnection();
                CallableStatement stmt = conn.prepareCall(sqlCallable)
        ) {
            stmt.setString(1, strNombre);
            stmt.setString(2, strApellido);
            stmt.setString(3, strUsuario);
            stmt.setString(4, strPassword);
            stmt.setString(5, strCorreo);
            stmt.setInt(6, intRol);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Usuario usuario = new Usuario();
                    usuario.setIdUsuario(rs.getInt("IdUsuario"));
                    usuario.setStrNombre(strNombre);
                    usuario.setStrApellido(strApellido);
                    usuario.setStrUsuario(strUsuario);
                    usuario.setStrPassword(strPassword);
                    usuario.setStrCorreo(strCorreo);
                    usuario.setIntRol(intRol);
                    usuario.setStrEstado("ACTIVO");

                    return usuario;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return null;
    }

}