package Proyecto.universidad.repository;

import Proyecto.universidad.Config.connection;
import Proyecto.universidad.model.ReporteTabla;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReporteRepository {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // RN-17: un préstamo está vencido si su fecha esperada ya pasó y aún tiene equipos pendientes
    private static final String HAY_PENDIENTES =
            "EXISTS (SELECT 1 FROM DetallePrestamo dp WHERE dp.IdPrestamo = p.IdPrestamo AND dp.Estado = 'PENDIENTE')";

    // 1. Equipos por estado (estado == null significa todos)
    public ReporteTabla equiposPorEstado(String estado) {
        StringBuilder sql = new StringBuilder(
                "SELECT e.CodigoInventario AS [Código], e.Nombre AS [Equipo], t.Nombre AS [Tipo], " +
                        "l.Nombre AS [Laboratorio], e.Estado AS [Estado] " +
                        "FROM Equipo e " +
                        "INNER JOIN TipoEquipo t ON e.IdTipoEquipo = t.IdTipoEquipo " +
                        "INNER JOIN Laboratorio l ON e.IdLaboratorio = l.IdLaboratorio ");
        List<Object> params = new ArrayList<>();

        if (estado != null) {
            sql.append("WHERE e.Estado = ? ");
            params.add(estado);
        }
        sql.append("ORDER BY e.Estado, e.CodigoInventario");

        return ejecutar(sql.toString(), params);
    }

    // Cantidad de equipos por estado (COUNT + GROUP BY)
    public Map<String, Integer> conteoEquiposPorEstado() {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        String sql = "SELECT Estado, COUNT(*) AS Cantidad FROM Equipo GROUP BY Estado";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                conteo.put(rs.getString("Estado").trim().toUpperCase(), rs.getInt("Cantidad"));
            }
        } catch (SQLException e) {
            System.err.println("Error al contar equipos por estado: " + e.getMessage());
            throw new IllegalStateException("No se pudo generar el reporte. Intente de nuevo.", e);
        }
        return conteo;
    }

    // 2 y 3. Préstamos por filtro (TODOS, ACTIVOS, FINALIZADOS, VENCIDOS) y, opcionalmente, entre fechas (BETWEEN)
    public ReporteTabla prestamos(String filtro, Timestamp desde, Timestamp hasta) {
        String f = filtro == null ? "TODOS" : filtro;
        String vencido = "p.Estado = 'ACTIVO' AND p.FechaEsperadaDevolucion < SYSDATETIME() AND " + HAY_PENDIENTES;

        StringBuilder sql = new StringBuilder(
                "SELECT p.IdPrestamo AS [ID], p.Solicitante AS [Solicitante], u.NombreUsuario AS [Registrado por], " +
                        "p.FechaPrestamo AS [Fecha préstamo], p.FechaEsperadaDevolucion AS [Devolución esperada], " +
                        "(SELECT COUNT(*) FROM DetallePrestamo d WHERE d.IdPrestamo = p.IdPrestamo) AS [Equipos], " +
                        "(SELECT COUNT(*) FROM DetallePrestamo d WHERE d.IdPrestamo = p.IdPrestamo AND d.Estado = 'PENDIENTE') AS [Pendientes], " +
                        "CASE WHEN " + vencido + " THEN 'VENCIDO' ELSE p.Estado END AS [Estado] " +
                        "FROM Prestamo p " +
                        "INNER JOIN Usuario u ON p.IdUsuarioRegistra = u.IdUsuario " +
                        "WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        switch (f) {
            case "ACTIVOS" -> sql.append("AND p.Estado = 'ACTIVO' ");
            case "FINALIZADOS" -> sql.append("AND p.Estado = 'FINALIZADO' ");
            case "VENCIDOS" -> sql.append("AND ").append(vencido).append(" ");
            default -> { }
        }

        if (desde != null && hasta != null) {
            sql.append("AND p.FechaPrestamo BETWEEN ? AND ? ");
            params.add(desde);
            params.add(hasta);
        }

        sql.append("ORDER BY p.FechaPrestamo DESC");
        return ejecutar(sql.toString(), params);
    }

    // 4. Busca un equipo por su código: devuelve {idEquipo, nombre, estado} o null si no existe
    public String[] buscarEquipo(String codigo) {
        String sql = "SELECT IdEquipo, Nombre, Estado FROM Equipo WHERE CodigoInventario = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, codigo);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                            String.valueOf(rs.getInt("IdEquipo")),
                            rs.getString("Nombre"),
                            rs.getString("Estado")
                    };
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar el equipo: " + e.getMessage());
            throw new IllegalStateException("No se pudo generar el reporte. Intente de nuevo.", e);
        }
        return null;
    }

    // 4. Préstamos de un equipo con la condición en que fue devuelto
    public ReporteTabla historialPrestamosEquipo(int idEquipo) {
        String sql = "SELECT p.IdPrestamo AS [Préstamo], p.Solicitante AS [Solicitante], " +
                "p.FechaPrestamo AS [Fecha préstamo], p.FechaEsperadaDevolucion AS [Devolución esperada], " +
                "d.Estado AS [Estado], dv.FechaDevolucion AS [Devuelto el], dv.Condicion AS [Condición], " +
                "dv.Observacion AS [Observación] " +
                "FROM DetallePrestamo d " +
                "INNER JOIN Prestamo p ON d.IdPrestamo = p.IdPrestamo " +
                "LEFT JOIN Devolucion dv ON dv.IdDetallePrestamo = d.IdDetallePrestamo " +
                "WHERE d.IdEquipo = ? " +
                "ORDER BY p.FechaPrestamo DESC";

        List<Object> params = new ArrayList<>();
        params.add(idEquipo);
        return ejecutar(sql, params);
    }

    // 4 y 5. Mantenimientos, opcionalmente de un equipo y/o por estado (EN_PROCESO, FINALIZADO)
    public ReporteTabla mantenimientos(Integer idEquipo, String estado) {
        StringBuilder sql = new StringBuilder(
                "SELECT m.IdMantenimiento AS [ID], e.CodigoInventario AS [Código], e.Nombre AS [Equipo], " +
                        "m.Tipo AS [Tipo], m.FechaIngreso AS [Ingreso], m.FechaSalida AS [Salida], " +
                        "m.Responsable AS [Responsable], m.Resultado AS [Resultado], m.Estado AS [Estado], " +
                        "m.Descripcion AS [Descripción] " +
                        "FROM Mantenimiento m " +
                        "INNER JOIN Equipo e ON m.IdEquipo = e.IdEquipo " +
                        "WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        if (idEquipo != null) {
            sql.append("AND m.IdEquipo = ? ");
            params.add(idEquipo);
        }
        if (estado != null) {
            sql.append("AND m.Estado = ? ");
            params.add(estado);
        }

        sql.append("ORDER BY m.FechaIngreso DESC");
        return ejecutar(sql.toString(), params);
    }

    // 6. Auditoría (últimos 500 eventos), opcionalmente de una acción
    public ReporteTabla auditoria(String accion) {
        StringBuilder sql = new StringBuilder(
                "SELECT TOP 500 a.FechaHora AS [Fecha y hora], ISNULL(u.NombreUsuario, '(sin usuario)') AS [Usuario], " +
                        "a.Accion AS [Acción], a.Entidad AS [Entidad], a.IdRegistro AS [ID registro], " +
                        "a.Descripcion AS [Descripción] " +
                        "FROM Auditoria a " +
                        "LEFT JOIN Usuario u ON a.IdUsuario = u.IdUsuario " +
                        "WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        if (accion != null) {
            sql.append("AND a.Accion = ? ");
            params.add(accion);
        }

        sql.append("ORDER BY a.FechaHora DESC");
        return ejecutar(sql.toString(), params);
    }

    public List<String> acciones() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT DISTINCT Accion FROM Auditoria ORDER BY Accion";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(rs.getString("Accion"));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar acciones de auditoría: " + e.getMessage());
            throw new IllegalStateException("No se pudo generar el reporte. Intente de nuevo.", e);
        }
        return lista;
    }

    // Ejecuta una consulta y devuelve sus columnas y filas ya listas para mostrar
    private ReporteTabla ejecutar(String sql, List<Object> params) {
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int n = meta.getColumnCount();

                String[] columnas = new String[n];
                for (int i = 0; i < n; i++) {
                    columnas[i] = meta.getColumnLabel(i + 1);
                }

                List<Object[]> filas = new ArrayList<>();
                while (rs.next()) {
                    Object[] fila = new Object[n];
                    for (int i = 0; i < n; i++) {
                        fila[i] = formatear(rs.getObject(i + 1));
                    }
                    filas.add(fila);
                }
                return new ReporteTabla(columnas, filas);
            }

        } catch (SQLException e) {
            System.err.println("Error al ejecutar el reporte: " + e.getMessage());
            throw new IllegalStateException("No se pudo generar el reporte. Intente de nuevo.", e);
        }
    }

    private Object formatear(Object valor) {
        if (valor == null) return "-";
        if (valor instanceof Timestamp ts) return ts.toLocalDateTime().format(FORMATO_FECHA);
        if (valor instanceof LocalDateTime ldt) return ldt.format(FORMATO_FECHA);
        if ("DANADO".equals(valor)) return "DAÑADO";
        return valor;
    }
}