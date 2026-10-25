package Proyecto.universidad.repository;

import Proyecto.universidad.Config.connection;
import Proyecto.universidad.model.Auditoria;
import Proyecto.universidad.model.Mantenimiento;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MantenimientoRepository {

    private final AuditoriaRepository auditoriaRepository = new AuditoriaRepository();

    public List<Mantenimiento> listarEnProceso() {
        List<Mantenimiento> lista = new ArrayList<>();
        String sql = "SELECT m.IdMantenimiento, m.IdEquipo, e.CodigoInventario, e.Nombre AS EquipoNombre, " +
                "m.Tipo, m.FechaIngreso, m.Descripcion, m.Responsable " +
                "FROM Mantenimiento m " +
                "INNER JOIN Equipo e ON m.IdEquipo = e.IdEquipo " +
                "WHERE m.Estado = 'EN_PROCESO' AND m.FechaSalida IS NULL";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Mantenimiento m = new Mantenimiento();
                m.setIdMantenimiento(rs.getInt("IdMantenimiento"));
                m.setIdEquipo(rs.getInt("IdEquipo"));
                m.setCodigoEquipo(rs.getString("CodigoInventario"));
                m.setNombreEquipo(rs.getString("EquipoNombre"));
                m.setTipoMantenimiento(rs.getString("Tipo"));
                m.setFechaIngreso(rs.getTimestamp("FechaIngreso"));
                m.setDescripcion(rs.getString("Descripcion"));
                m.setResponsable(rs.getString("Responsable"));
                lista.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar mantenimientos en proceso: " + e.getMessage());
        }
        return lista;
    }

    public Mantenimiento buscarPorId(int idMantenimiento) {
        String sql = "SELECT m.IdMantenimiento, m.IdEquipo, e.CodigoInventario, e.Nombre AS EquipoNombre, " +
                "m.Tipo, m.FechaIngreso, m.Descripcion, m.Responsable, m.FechaSalida, m.Resultado " +
                "FROM Mantenimiento m " +
                "INNER JOIN Equipo e ON m.IdEquipo = e.IdEquipo " +
                "WHERE m.IdMantenimiento = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idMantenimiento);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Mantenimiento m = new Mantenimiento();
                    m.setIdMantenimiento(rs.getInt("IdMantenimiento"));
                    m.setIdEquipo(rs.getInt("IdEquipo"));
                    m.setCodigoEquipo(rs.getString("CodigoInventario"));
                    m.setNombreEquipo(rs.getString("EquipoNombre"));
                    m.setTipoMantenimiento(rs.getString("Tipo"));
                    m.setFechaIngreso(rs.getTimestamp("FechaIngreso"));
                    m.setDescripcion(rs.getString("Descripcion"));
                    m.setResponsable(rs.getString("Responsable"));
                    m.setFechaSalida(rs.getTimestamp("FechaSalida"));
                    m.setResultado(rs.getString("Resultado"));
                    return m;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar mantenimiento: " + e.getMessage());
        }
        return null;
    }

    public boolean registrarIngreso(int idEquipo, String tipoMantenimiento, String descripcion,
                                    String responsable, int idUsuarioRegistra) {
        String sqlInsert = "INSERT INTO Mantenimiento (IdEquipo, FechaIngreso, Tipo, Descripcion, Responsable, Estado, IdUsuarioRegistra) " +
                "VALUES (?, SYSDATETIME(), ?, ?, ?, 'EN_PROCESO', ?)";
        // Solo cambia si el equipo sigue DISPONIBLE o DANADO (evita carreras entre usuarios)
        String sqlUpdateEquipo = "UPDATE Equipo SET Estado = 'MANTENIMIENTO' " +
                "WHERE IdEquipo = ? AND Estado IN ('DISPONIBLE', 'DANADO')";

        Connection conn = null;
        try {
            conn = connection.getConnection();
            conn.setAutoCommit(false); // Transacción atómica

            int idMantenimiento = 0;

            try (PreparedStatement stmtM = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                stmtM.setInt(1, idEquipo);
                stmtM.setString(2, tipoMantenimiento);
                stmtM.setString(3, descripcion);
                stmtM.setString(4, responsable);
                stmtM.setInt(5, idUsuarioRegistra);
                stmtM.executeUpdate();

                try (ResultSet keys = stmtM.getGeneratedKeys()) {
                    if (keys.next()) {
                        idMantenimiento = keys.getInt(1);
                    }
                }
            }

            try (PreparedStatement stmtE = conn.prepareStatement(sqlUpdateEquipo)) {
                stmtE.setInt(1, idEquipo);
                if (stmtE.executeUpdate() != 1) {
                    throw new SQLException("El equipo ya no está DISPONIBLE ni DANADO.");
                }
            }

            auditoriaRepository.registrar(conn, new Auditoria(
                    idUsuarioRegistra,
                    LocalDateTime.now(),
                    "EQUIPO_ENVIADO_MANTENIMIENTO",
                    "Mantenimiento",
                    idMantenimiento,
                    "Equipo ID " + idEquipo + " enviado a mantenimiento " + tipoMantenimiento +
                            ". Responsable: " + responsable
            ));

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { System.err.println(ex.getMessage()); }
            }
            System.err.println("Error al registrar ingreso a mantenimiento: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { System.err.println(ex.getMessage()); }
            }
        }
    }

    public boolean finalizarMantenimiento(int idMantenimiento, int idEquipo, String resultado,
                                          String observaciones, int idUsuario) {
        // Solo cierra un mantenimiento que siga EN_PROCESO
        String sqlUpdateMantenimiento = "UPDATE Mantenimiento SET FechaSalida = SYSDATETIME(), Resultado = ?, " +
                "Estado = 'FINALIZADO', Observaciones = ? " +
                "WHERE IdMantenimiento = ? AND Estado = 'EN_PROCESO'";
        // Solo cambia el equipo si sigue en MANTENIMIENTO (un equipo en BAJA nunca vuelve a DISPONIBLE)
        String sqlUpdateEquipo = "UPDATE Equipo SET Estado = ? WHERE IdEquipo = ? AND Estado = 'MANTENIMIENTO'";

        Connection conn = null;
        try {
            conn = connection.getConnection();
            conn.setAutoCommit(false); // Transacción atómica

            try (PreparedStatement stmtM = conn.prepareStatement(sqlUpdateMantenimiento)) {
                stmtM.setString(1, resultado);
                stmtM.setString(2, observaciones);
                stmtM.setInt(3, idMantenimiento);
                if (stmtM.executeUpdate() != 1) {
                    throw new SQLException("El mantenimiento ya no está EN_PROCESO.");
                }
            }

            // RN-15: REPARADO -> DISPONIBLE, NO_REPARABLE -> BAJA
            String nuevoEstado = "REPARADO".equals(resultado) ? "DISPONIBLE" : "BAJA";

            try (PreparedStatement stmtE = conn.prepareStatement(sqlUpdateEquipo)) {
                stmtE.setString(1, nuevoEstado);
                stmtE.setInt(2, idEquipo);
                if (stmtE.executeUpdate() != 1) {
                    throw new SQLException("El equipo no está en estado MANTENIMIENTO.");
                }
            }

            auditoriaRepository.registrar(conn, new Auditoria(
                    idUsuario,
                    LocalDateTime.now(),
                    "MANTENIMIENTO_FINALIZADO",
                    "Mantenimiento",
                    idMantenimiento,
                    "Mantenimiento finalizado como " + resultado + ". Equipo ID " + idEquipo +
                            " pasa a " + nuevoEstado
            ));

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { System.err.println(ex.getMessage()); }
            }
            System.err.println("Error al finalizar mantenimiento: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { System.err.println(ex.getMessage()); }
            }
        }
    }
}