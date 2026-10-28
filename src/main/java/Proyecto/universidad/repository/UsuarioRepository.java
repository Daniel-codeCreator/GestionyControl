package Proyecto.universidad.repository;

import Proyecto.universidad.Config.connection;
import Proyecto.universidad.model.Usuario;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UsuarioRepository {

    /**
     * Obtiene la lista completa de usuarios registrados en la base de datos.
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
     * Busca un usuario específico por su NombreUsuario sin fallos por espacios o estados.
     */
    public Usuario buscarPorUsuario(String strUsuario) {
        if (strUsuario == null || strUsuario.isBlank()) {
            return null;
        }

        String strSql = """
            SELECT
                IdUsuario, Nombre, Apellido, NombreUsuario,
                Contrasena, Correo, IdRol, Estado, FechaCreacion
            FROM Usuario
            WHERE LOWER(RTRIM(LTRIM(NombreUsuario))) = LOWER(RTRIM(LTRIM(?)))
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
     * Registra un nuevo usuario en el sistema.
     */
    public Usuario crearUsuario(
            String strNombre,
            String strApellido,
            String strUsuario,
            String strPassword,
            String strCorreo,
            int intRol
    ) {
        String strSql = """
                INSERT INTO Usuario
                (
                    Nombre, Apellido, NombreUsuario,
                    Contrasena, Correo, IdRol, Estado
                )
                VALUES (?, ?, ?, ?, ?, ?, 'ACTIVO')
                """;

        try (
                Connection conn = connection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        strSql,
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {
            stmt.setString(1, strNombre);
            stmt.setString(2, strApellido);
            stmt.setString(3, strUsuario);
            stmt.setString(4, strPassword);
            stmt.setString(5, strCorreo);
            stmt.setInt(6, intRol);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Usuario usuario = new Usuario();

                    usuario.setIdUsuario(rs.getInt(1));
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
            throw new RuntimeException("Error al crear usuario: " + e.getMessage(), e);
        }

        return null;
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