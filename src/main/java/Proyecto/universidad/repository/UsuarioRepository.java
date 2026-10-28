package Proyecto.universidad.repository;

import Proyecto.universidad.Config.connection;
import Proyecto.universidad.model.Auditoria;
import Proyecto.universidad.model.Usuario;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
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

    /**
     * Desactiva lógicamente un usuario (baja lógica: conserva el historial).
     * Deja la auditoría USUARIO_DESACTIVADO en la misma transacción (RN-18).
     *
     * @return true si el usuario estaba ACTIVO y pasó a INACTIVO.
     */
    public boolean desactivarUsuario(int intIdUsuario, int intIdUsuarioAccion) {
        String strSql = "UPDATE Usuario SET Estado = 'INACTIVO' WHERE IdUsuario = ? AND Estado = 'ACTIVO'";

        Connection conn = null;
        try {
            conn = connection.getConnection();
            conn.setAutoCommit(false); // Transacción atómica

            int filas = 0;
            try (PreparedStatement stmt = conn.prepareStatement(strSql)) {
                stmt.setInt(1, intIdUsuario);
                filas = stmt.executeUpdate();
            }

            if (filas == 0) {
                conn.rollback();
                return false;
            }

            // RN-18: rastro de auditoría de la desactivación
            new AuditoriaRepository().registrar(conn, new Auditoria(
                    intIdUsuarioAccion,
                    LocalDateTime.now(),
                    "USUARIO_DESACTIVADO",
                    "Usuario",
                    intIdUsuario,
                    "Usuario desactivado (baja lógica), se conserva el historial."
            ));

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { System.err.println(ex.getMessage()); }
            }
            System.err.println("Error al desactivar usuario: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { System.err.println(ex.getMessage()); }
            }
        }
    }
}