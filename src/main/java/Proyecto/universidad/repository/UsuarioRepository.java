package Proyecto.universidad.repository;

import Proyecto.universidad.Config.connection;
import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.model.Auditoria;
import java.time.LocalDateTime;

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
            stmt.setString(3, strUsuario); // Puede ir la sugerencia o null
            stmt.setString(4, strPassword);
            stmt.setString(5, strCorreo);
            stmt.setInt(6, intRol);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Usuario usuario = new Usuario();
                    usuario.setIdUsuario(rs.getInt("idUsuario"));
                    usuario.setStrNombre(strNombre);
                    usuario.setStrApellido(strApellido);

                    // Lee el usuario definitivo asignado por el Stored Procedure
                    usuario.setStrUsuario(rs.getString("NombreUsuario"));

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

    public void cambiarEstadoUsuario(int idUsuario, String nuevoEstado) {
        String strSQL = "update usuario set Estado  = ? where IdUsuario = ?";

        try (Connection connect = connection.getConnection();
            PreparedStatement stmt = connect.prepareStatement(strSQL)) {

            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idUsuario);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas == 0) {
                throw new RuntimeException("No se encontró el usuario con ID: " + idUsuario);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al cambiar el estado del usuario: " + e.getMessage());
        }
    }

    private final AuditoriaRepository auditoriaRepository = new AuditoriaRepository();

    public Usuario buscarPorId(int idUsuario) {
        String sql = "SELECT IdUsuario, Nombre, Apellido, NombreUsuario, Correo, IdRol, Estado " +
                "FROM Usuario WHERE IdUsuario = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idUsuario);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Usuario usuario = new Usuario();
                    usuario.setIdUsuario(rs.getInt("IdUsuario"));
                    usuario.setStrNombre(rs.getString("Nombre"));
                    usuario.setStrApellido(rs.getString("Apellido"));
                    usuario.setStrUsuario(rs.getString("NombreUsuario"));
                    usuario.setStrCorreo(rs.getString("Correo"));
                    usuario.setIntRol(rs.getInt("IdRol"));
                    usuario.setStrEstado(rs.getString("Estado"));
                    return usuario;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar usuario: " + e.getMessage(), e);
        }
        return null;
    }

    public int contarAdministradoresActivos() {
        String sql = "SELECT COUNT(*) FROM Usuario u " +
                "INNER JOIN Rol r ON u.IdRol = r.IdRol " +
                "WHERE r.Nombre = 'ADMIN' AND u.Estado = 'ACTIVO'";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error al contar administradores: " + e.getMessage(), e);
        }
    }

    // Cambia el estado y registra la auditoría en la misma transacción (RN-18)
    public boolean cambiarEstadoConAuditoria(int idUsuario, String nuevoEstado, int idActor,
                                             String accion, String descripcion) {
        String sql = "UPDATE Usuario SET Estado = ? WHERE IdUsuario = ?";

        Connection conn = null;
        try {
            conn = connection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, nuevoEstado);
                stmt.setInt(2, idUsuario);
                if (stmt.executeUpdate() != 1) {
                    throw new SQLException("No se encontró el usuario con ID " + idUsuario);
                }
            }

            auditoriaRepository.registrar(conn, new Auditoria(
                    idActor,
                    LocalDateTime.now(),
                    accion,
                    "Usuario",
                    idUsuario,
                    descripcion
            ));

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { System.err.println(ex.getMessage()); }
            }
            System.err.println("Error al cambiar el estado del usuario: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { System.err.println(ex.getMessage()); }
            }
        }
    }
}