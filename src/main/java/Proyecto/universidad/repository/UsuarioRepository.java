package Proyecto.universidad.repository;

import Proyecto.universidad.Config.connection;
import Proyecto.universidad.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioRepository {

    public Usuario buscarPorUsuario(String strUsuario) {

        String strSql = """
                SELECT
                    IdUsuario, Nombre,Apellido,NombreUsuario,
                    Contrasena,Correo, IdRol, Estado, FechaCreacion
                FROM Usuario
                WHERE NombreUsuario = ?
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
            throw new RuntimeException(
                    "Error al buscar usuario: " + e.getMessage(), e
            );
        }

        return null;
    }

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
                    Nombre,
                    Apellido,
                    NombreUsuario,
                    Contrasena,
                    Correo,
                    IdRol,
                    Estado
                )
                VALUES (?, ?, ?, ?, ?, ?, 'ACTIVO')
                """;

        try (
                Connection conn = connection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        strSql,
                        java.sql.Statement.RETURN_GENERATED_KEYS
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
            throw new RuntimeException(
                    "Error al crear usuario: " + e.getMessage(), e
            );
        }

        return null;
    }
}