package Proyecto.universidad.repository;

import Proyecto.universidad.Config.connection;
import Proyecto.universidad.model.Dashboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardRepository {

    public Dashboard obtenerResumen() {
        String sqlEquipos = "SELECT Estado, COUNT(*) AS Cantidad FROM Equipo GROUP BY Estado";

        String sqlActivos = "SELECT COUNT(*) FROM Prestamo WHERE Estado = 'ACTIVO'";

        // RN-17: vencido = fecha esperada ya pasó y todavía hay equipos pendientes
        String sqlVencidos = "SELECT COUNT(*) FROM Prestamo p " +
                "WHERE p.Estado = 'ACTIVO' " +
                "AND p.FechaEsperadaDevolucion < SYSDATETIME() " +
                "AND EXISTS (SELECT 1 FROM DetallePrestamo dp " +
                "            WHERE dp.IdPrestamo = p.IdPrestamo AND dp.Estado = 'PENDIENTE')";

        try (Connection conn = connection.getConnection()) {

            int disponibles = 0, prestados = 0, danados = 0, enMantenimiento = 0, deBaja = 0;

            try (PreparedStatement stmt = conn.prepareStatement(sqlEquipos);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String estado = rs.getString("Estado").trim().toUpperCase();
                    int cantidad = rs.getInt("Cantidad");
                    switch (estado) {
                        case "DISPONIBLE" -> disponibles = cantidad;
                        case "PRESTADO" -> prestados = cantidad;
                        case "DANADO" -> danados = cantidad;
                        case "MANTENIMIENTO" -> enMantenimiento = cantidad;
                        case "BAJA" -> deBaja = cantidad;
                        default -> { }
                    }
                }
            }

            int activos = contar(conn, sqlActivos);
            int vencidos = contar(conn, sqlVencidos);
            int total = disponibles + prestados + danados + enMantenimiento + deBaja;

            return new Dashboard(total, disponibles, prestados, danados,
                    enMantenimiento, deBaja, activos, vencidos);

        } catch (SQLException e) {
            System.err.println("Error al consultar el dashboard: " + e.getMessage());
            throw new IllegalStateException("No se pudo consultar el resumen del sistema.", e);
        }
    }

    private int contar(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}