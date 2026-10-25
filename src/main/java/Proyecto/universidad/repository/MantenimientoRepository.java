package Proyecto.universidad.repository;

import Proyecto.universidad.Config.connection;
import Proyecto.universidad.model.Mantenimiento;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MantenimientoRepository {

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

    public boolean registrarIngreso(int idEquipo, String tipoMantenimiento, String descripcion, String responsable, int idUsuarioRegistra) {
        String sqlInsert = "INSERT INTO Mantenimiento (IdEquipo, FechaIngreso, Tipo, Descripcion, Responsable, Estado, IdUsuarioRegistra) " +
                "VALUES (?, SYSDATETIME(), ?, ?, ?, 'EN_PROCESO', ?)";
        String sqlUpdateEquipo = "UPDATE Equipo SET Estado = 'MANTENIMIENTO' WHERE IdEquipo = ?";

        Connection conn = null;
        try {
            conn = connection.getConnection();
            conn.setAutoCommit(false); // Transacción atómica[cite: 15]

            try (PreparedStatement stmtM = conn.prepareStatement(sqlInsert)) {
                stmtM.setInt(1, idEquipo);
                stmtM.setString(2, tipoMantenimiento); // 'PREVENTIVO' o 'CORRECTIVO'
                stmtM.setString(3, descripcion);
                stmtM.setString(4, responsable);
                stmtM.setInt(5, idUsuarioRegistra);
                stmtM.executeUpdate();
            }

            try (PreparedStatement stmtE = conn.prepareStatement(sqlUpdateEquipo)) {
                stmtE.setInt(1, idEquipo);
                stmtE.executeUpdate();
            }

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

    public boolean finalizarMantenimiento(int idMantenimiento, int idEquipo, String resultado, String observaciones) {
        String sqlUpdateMantenimiento = "UPDATE Mantenimiento SET FechaSalida = SYSDATETIME(), Resultado = ?, Estado = 'FINALIZADO', Observaciones = ? " +
                "WHERE IdMantenimiento = ?";
        String sqlUpdateEquipo = "UPDATE Equipo SET Estado = ? WHERE IdEquipo = ?";

        Connection conn = null;
        try {
            conn = connection.getConnection();
            conn.setAutoCommit(false); // Transacción atómica[cite: 15]

            try (PreparedStatement stmtM = conn.prepareStatement(sqlUpdateMantenimiento)) {
                stmtM.setString(1, resultado); // 'REPARADO' o 'NO_REPARABLE'
                stmtM.setString(2, observaciones);
                stmtM.setInt(3, idMantenimiento);
                stmtM.executeUpdate();
            }

            // RN-15: Si resultado es REPARADO -> DISPONIBLE. Si es NO_REPARABLE -> BAJA[cite: 11, 12]
            String nuevoEstado = "REPARADO".equalsIgnoreCase(resultado) ? "DISPONIBLE" : "BAJA";

            try (PreparedStatement stmtE = conn.prepareStatement(sqlUpdateEquipo)) {
                stmtE.setString(1, nuevoEstado);
                stmtE.setInt(2, idEquipo);
                stmtE.executeUpdate();
            }

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