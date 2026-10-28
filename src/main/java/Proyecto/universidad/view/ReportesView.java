package Proyecto.universidad.view;

import Proyecto.universidad.Config.connection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ReportesView extends JFrame {

    private JPanel panelPrincipal;
    private JTabbedPane tabbedPaneMain;

    // Pestaña 1: Equipos
    private JPanel panelEquipos;
    private JComboBox<String> cmbEstadoEquipo;
    private JButton btnFiltrarEquipos;
    private JTable tblEquiposReporte;

    // Pestaña 2: Préstamos
    private JPanel panelPrestamos;
    private JButton btnCargarVencidos;
    private JTable tblPrestamosReporte;

    // Pestaña 3: Historial Mantenimiento
    private JPanel panelMantenimientos;
    private JTextField txtCodigoBusqueda;
    private JButton btnBuscarHistorial;
    private JTable tblMantenimientoReporte;

    private JButton btnCerrar;

    private DefaultTableModel modelEquipos;
    private DefaultTableModel modelPrestamos;
    private DefaultTableModel modelMantenimiento;

    public ReportesView() {
        setTitle("Módulo de Reportes y Consultas - SIGELAB");
        setSize(1000, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        if (panelPrincipal != null) {
            setContentPane(panelPrincipal);
        }

        configurarCombosYTablas();

        if (btnFiltrarEquipos != null) btnFiltrarEquipos.addActionListener(e -> cargarEquiposPorEstado());
        if (btnCargarVencidos != null) btnCargarVencidos.addActionListener(e -> cargarPrestamosVencidos());
        if (btnBuscarHistorial != null) btnBuscarHistorial.addActionListener(e -> cargarHistorialMantenimiento());
        if (btnCerrar != null) btnCerrar.addActionListener(e -> dispose());

        // Cargas iniciales
        cargarEquiposPorEstado();
        cargarPrestamosVencidos();
    }

    private void configurarCombosYTablas() {
        if (cmbEstadoEquipo != null) {
            cmbEstadoEquipo.removeAllItems();
            cmbEstadoEquipo.addItem("TODOS");
            cmbEstadoEquipo.addItem("DISPONIBLE");
            cmbEstadoEquipo.addItem("PRESTADO");
            cmbEstadoEquipo.addItem("DANADO");
            cmbEstadoEquipo.addItem("MANTENIMIENTO");
            cmbEstadoEquipo.addItem("BAJA");
        }

        modelEquipos = new DefaultTableModel(new String[]{"ID", "Código", "Nombre", "Tipo", "Laboratorio", "Estado"}, 0);
        if (tblEquiposReporte != null) tblEquiposReporte.setModel(modelEquipos);

        modelPrestamos = new DefaultTableModel(new String[]{"ID Préstamo", "Solicitante", "Fecha Préstamo", "Fecha Esperada", "Estado"}, 0);
        if (tblPrestamosReporte != null) tblPrestamosReporte.setModel(modelPrestamos);

        modelMantenimiento = new DefaultTableModel(new String[]{"ID Mant.", "Código Equipo", "Equipo", "Tipo", "Ingreso", "Salida", "Resultado", "Técnico"}, 0);
        if (tblMantenimientoReporte != null) tblMantenimientoReporte.setModel(modelMantenimiento);
    }

    private void cargarEquiposPorEstado() {
        modelEquipos.setRowCount(0);
        String estado = cmbEstadoEquipo != null ? (String) cmbEstadoEquipo.getSelectedItem() : "TODOS";

        String sql = "SELECT e.IdEquipo, e.CodigoInventario, e.Nombre, te.Nombre AS Tipo, l.Nombre AS Laboratorio, e.Estado " +
                "FROM Equipo e " +
                "INNER JOIN TipoEquipo te ON e.IdTipoEquipo = te.IdTipoEquipo " +
                "INNER JOIN Laboratorio l ON e.IdLaboratorio = l.IdLaboratorio " +
                ("TODOS".equals(estado) ? "" : "WHERE e.Estado = ?");

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (!"TODOS".equals(estado)) {
                stmt.setString(1, estado);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    modelEquipos.addRow(new Object[]{
                            rs.getInt("IdEquipo"),
                            rs.getString("CodigoInventario"),
                            rs.getString("Nombre"),
                            rs.getString("Tipo"),
                            rs.getString("Laboratorio"),
                            rs.getString("Estado")
                    });
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al obtener reporte de equipos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarPrestamosVencidos() {
        modelPrestamos.setRowCount(0);
        String sql = "SELECT IdPrestamo, Solicitante, FechaPrestamo, FechaEsperadaDevolucion, Estado " +
                "FROM Prestamo " +
                "WHERE Estado = 'ACTIVO' AND FechaEsperadaDevolucion < SYSDATETIME()";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                modelPrestamos.addRow(new Object[]{
                        rs.getInt("IdPrestamo"),
                        rs.getString("Solicitante"),
                        rs.getTimestamp("FechaPrestamo"),
                        rs.getTimestamp("FechaEsperadaDevolucion"),
                        rs.getString("Estado")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al obtener préstamos vencidos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarHistorialMantenimiento() {
        modelMantenimiento.setRowCount(0);
        String codigo = txtCodigoBusqueda != null ? txtCodigoBusqueda.getText().trim() : "";

        String sql = "SELECT m.IdMantenimiento, e.CodigoInventario, e.Nombre AS EquipoNombre, m.Tipo, " +
                "m.FechaIngreso, m.FechaSalida, m.Resultado, m.Responsable " +
                "FROM Mantenimiento m " +
                "INNER JOIN Equipo e ON m.IdEquipo = e.IdEquipo " +
                (codigo.isEmpty() ? "" : "WHERE e.CodigoInventario LIKE ? ") +
                "ORDER BY m.FechaIngreso DESC";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (!codigo.isEmpty()) {
                stmt.setString(1, "%" + codigo + "%");
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    modelMantenimiento.addRow(new Object[]{
                            rs.getInt("IdMantenimiento"),
                            rs.getString("CodigoInventario"),
                            rs.getString("EquipoNombre"),
                            rs.getString("Tipo"),
                            rs.getTimestamp("FechaIngreso"),
                            rs.getTimestamp("FechaSalida"),
                            rs.getString("Resultado"),
                            rs.getString("Responsable")
                    });
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al obtener historial de mantenimiento: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}