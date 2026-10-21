package Proyecto.universidad.repository;

import Proyecto.universidad.Config.connection;
import Proyecto.universidad.model.Prestamo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PrestamoRepository {

    public List<Prestamo> listarActivos() {
        List<Prestamo> lista = new ArrayList<>();
        String sql = "SELECT p.IdPrestamo, p.Solicitante, dp.IdEquipo, e.Nombre AS EquipoNombre, " +
                "e.CodigoInventario, p.FechaPrestamo, p.FechaEsperadaDevolucion, p.Estado, p.Observaciones " +
                "FROM Prestamo p " +
                "INNER JOIN DetallePrestamo dp ON p.IdPrestamo = dp.IdPrestamo " +
                "INNER JOIN Equipo e ON dp.IdEquipo = e.IdEquipo " +
                "WHERE p.Estado = 'ACTIVO' AND dp.Estado = 'PENDIENTE'";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Prestamo p = new Prestamo();
                p.setIdPrestamo(rs.getInt("IdPrestamo"));
                p.setNombreUsuario(rs.getString("Solicitante")); // Presentamos al solicitante
                p.setIdEquipo(rs.getInt("IdEquipo"));
                p.setNombreEquipo(rs.getString("EquipoNombre"));
                p.setCodigoEquipo(rs.getString("CodigoInventario"));
                p.setFechaPrestamo(rs.getTimestamp("FechaPrestamo"));
                p.setFechaDevolucionEsperada(rs.getTimestamp("FechaEsperadaDevolucion"));
                p.setEstado(rs.getString("Estado"));
                p.setObservaciones(rs.getString("Observaciones"));
                lista.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar préstamos activos: " + e.getMessage());
        }
        return lista;
    }

    public boolean registrarPrestamo(String solicitante, int idEquipo, int idUsuarioRegistra, Timestamp fechaEsperada, String observaciones) {
        String sqlPrestamo = "INSERT INTO Prestamo (Solicitante, FechaPrestamo, FechaEsperadaDevolucion, IdUsuarioRegistra, Estado, Observaciones) " +
                "VALUES (?, SYSDATETIME(), ?, ?, 'ACTIVO', ?)";
        String sqlDetalle = "INSERT INTO DetallePrestamo (IdPrestamo, IdEquipo, Estado) VALUES (?, ?, 'PENDIENTE')";
        String sqlEquipo = "UPDATE Equipo SET Estado = 'PRESTADO' WHERE IdEquipo = ?";

        Connection conn = null;
        try {
            conn = connection.getConnection();
            conn.setAutoCommit(false); // Transacción atómica

            int idPrestamoGenerado = 0;
            try (PreparedStatement stmtP = conn.prepareStatement(sqlPrestamo, Statement.RETURN_GENERATED_KEYS)) {
                stmtP.setString(1, solicitante);
                stmtP.setTimestamp(2, fechaEsperada);
                stmtP.setInt(3, idUsuarioRegistra);
                stmtP.setString(4, observaciones);
                stmtP.executeUpdate();

                try (ResultSet rs = stmtP.getGeneratedKeys()) {
                    if (rs.next()) {
                        idPrestamoGenerado = rs.getInt(1);
                    }
                }
            }

            if (idPrestamoGenerado == 0) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement stmtD = conn.prepareStatement(sqlDetalle)) {
                stmtD.setInt(1, idPrestamoGenerado);
                stmtD.setInt(2, idEquipo);
                stmtD.executeUpdate();
            }

            try (PreparedStatement stmtE = conn.prepareStatement(sqlEquipo)) {
                stmtE.setInt(1, idEquipo);
                stmtE.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { System.err.println(ex.getMessage()); }
            }
            System.err.println("Error al registrar préstamo: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { System.err.println(ex.getMessage()); }
            }
        }
    }

    public boolean registrarDevolucion(int idPrestamo, int idEquipo, int idUsuarioRecibe, String condicion, String observacion) {
        String sqlBuscarDetalle = "SELECT IdDetallePrestamo FROM DetallePrestamo WHERE IdPrestamo = ? AND IdEquipo = ?";
        String sqlDevolucion = "INSERT INTO Devolucion (IdDetallePrestamo, FechaDevolucion, Condicion, Observacion, IdUsuarioRecibe) " +
                "VALUES (?, SYSDATETIME(), ?, ?, ?)";
        String sqlUpdateDetalle = "UPDATE DetallePrestamo SET Estado = 'DEVUELTO' WHERE IdDetallePrestamo = ?";
        String sqlUpdatePrestamo = "UPDATE Prestamo SET Estado = 'FINALIZADO' WHERE IdPrestamo = ?";
        String sqlUpdateEquipo = "UPDATE Equipo SET Estado = ? WHERE IdEquipo = ?";

        Connection conn = null;
        try {
            conn = connection.getConnection();
            conn.setAutoCommit(false); // Transacción atómica

            int idDetalle = 0;
            try (PreparedStatement stmtB = conn.prepareStatement(sqlBuscarDetalle)) {
                stmtB.setInt(1, idPrestamo);
                stmtB.setInt(2, idEquipo);
                try (ResultSet rs = stmtB.executeQuery()) {
                    if (rs.next()) {
                        idDetalle = rs.getInt("IdDetallePrestamo");
                    }
                }
            }

            if (idDetalle == 0) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement stmtD = conn.prepareStatement(sqlDevolucion)) {
                stmtD.setInt(1, idDetalle);
                stmtD.setString(2, condicion); // 'BUENO' o 'DANADO'
                stmtD.setString(3, observacion);
                stmtD.setInt(4, idUsuarioRecibe);
                stmtD.executeUpdate();
            }

            try (PreparedStatement stmtUD = conn.prepareStatement(sqlUpdateDetalle)) {
                stmtUD.setInt(1, idDetalle);
                stmtUD.executeUpdate();
            }

            try (PreparedStatement stmtUP = conn.prepareStatement(sqlUpdatePrestamo)) {
                stmtUP.setInt(1, idPrestamo);
                stmtUP.executeUpdate();
            }

            try (PreparedStatement stmtE = conn.prepareStatement(sqlUpdateEquipo)) {
                // Si la condición fue DAÑADO cambia el equipo a DANADO, si no a DISPONIBLE
                String nuevoEstadoEquipo = "DANADO".equalsIgnoreCase(condicion) ? "DANADO" : "DISPONIBLE";
                stmtE.setString(1, nuevoEstadoEquipo);
                stmtE.setInt(2, idEquipo);
                stmtE.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { System.err.println(ex.getMessage()); }
            }
            System.err.println("Error al procesar devolución: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { System.err.println(ex.getMessage()); }
            }
        }
    }
}