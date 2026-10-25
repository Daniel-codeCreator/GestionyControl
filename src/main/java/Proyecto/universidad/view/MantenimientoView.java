package Proyecto.universidad.view;

import Proyecto.universidad.enums.EstadoEquipo;
import Proyecto.universidad.model.Equipo;
import Proyecto.universidad.model.Mantenimiento;
import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.service.EquipoService;
import Proyecto.universidad.service.MantenimientoService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class MantenimientoView extends JFrame {

    private JPanel panelPrincipal;
    private JComboBox<Equipo> cmbEquipo;
    private JComboBox<String> cmbTipoMantenimiento;
    private JTextField txtResponsable;
    private JTextField txtDescripcion;
    private JButton btnRegistrarIngreso;
    private JButton btnLimpiar;
    private JButton btnCerrar;
    private JTable tblMantenimientos;
    private JComboBox<String> cmbResultadoCierre;
    private JTextField txtObservacionesSalida;
    private JButton btnFinalizarMantenimiento;

    private final MantenimientoService mantenimientoService;
    private final EquipoService equipoService;

    private DefaultTableModel tableModel;
    private Integer idMantenimientoSeleccionado;

    // Usuario con sesión iniciada: es quien registra el ingreso y el cierre
    private final Usuario usuarioLogueado;

    public MantenimientoView(Usuario usuarioLogueado) {
        this.usuarioLogueado = usuarioLogueado;

        mantenimientoService = new MantenimientoService();
        equipoService = new EquipoService();

        setTitle("Gestion de Mantenimiento de Equipos - SIGELAB");
        setSize(950, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        if (panelPrincipal != null) {
            setContentPane(panelPrincipal);
        }

        configurarCombosIniciales();
        configurarTabla();
        cargarCombos();
        cargarDatosTabla();

        if (btnRegistrarIngreso != null) btnRegistrarIngreso.addActionListener(e -> registrarIngreso());
        if (btnFinalizarMantenimiento != null) btnFinalizarMantenimiento.addActionListener(e -> finalizarMantenimiento());
        if (btnLimpiar != null) btnLimpiar.addActionListener(e -> limpiarFormulario());
        if (btnCerrar != null) btnCerrar.addActionListener(e -> dispose());

        if (tblMantenimientos != null) {
            tblMantenimientos.getSelectionModel().addListSelectionListener(e -> seleccionarFila());
        }
    }

    private void configurarCombosIniciales() {
        if (cmbTipoMantenimiento != null) {
            cmbTipoMantenimiento.removeAllItems();
            cmbTipoMantenimiento.addItem("CORRECTIVO");
            cmbTipoMantenimiento.addItem("PREVENTIVO");
        }

        if (cmbResultadoCierre != null) {
            cmbResultadoCierre.removeAllItems();
            cmbResultadoCierre.addItem("REPARADO");
            cmbResultadoCierre.addItem("NO_REPARABLE");
        }
    }

    private void cargarCombos() {
        if (cmbEquipo == null) return;

        cmbEquipo.removeAllItems();

        String tipo = cmbTipoMantenimiento != null
                ? (String) cmbTipoMantenimiento.getSelectedItem()
                : "CORRECTIVO";

        // CORRECTIVO -> equipos dañados | PREVENTIVO -> equipos disponibles
        EstadoEquipo estadoRequerido = "PREVENTIVO".equals(tipo)
                ? EstadoEquipo.DISPONIBLE
                : EstadoEquipo.DANADO;

        List<Equipo> equipos = equipoService.obtenerTodos();
        for (Equipo eq : equipos) {
            if (eq.getEstado() == estadoRequerido) {
                cmbEquipo.addItem(eq);
            }
        }
    }

    private void configurarTabla() {
        tableModel = new DefaultTableModel(
                new String[]{"ID Mant.", "ID Equipo", "Código", "Equipo", "Tipo", "Fecha Ingreso", "Técnico"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        if (tblMantenimientos != null) {
            tblMantenimientos.setModel(tableModel);
        }
    }

    private void cargarDatosTabla() {
        tableModel.setRowCount(0);
        List<Mantenimiento> lista = mantenimientoService.obtenerEnProceso();
        for (Mantenimiento m : lista) {
            tableModel.addRow(new Object[]{
                    m.getIdMantenimiento(),
                    m.getIdEquipo(),
                    m.getCodigoEquipo(),
                    m.getNombreEquipo(),
                    m.getTipoMantenimiento(),
                    m.getFechaIngreso(),
                    m.getResponsable()
            });
        }
    }

    private void registrarIngreso() {
        try {
            Equipo eq = (Equipo) cmbEquipo.getSelectedItem();
            Integer idEquipo = eq != null ? eq.getIdEquipo() : null;
            String tipo = cmbTipoMantenimiento != null ? (String) cmbTipoMantenimiento.getSelectedItem() : "CORRECTIVO";
            String resp = txtResponsable != null ? txtResponsable.getText() : "";
            String desc = txtDescripcion != null ? txtDescripcion.getText() : "";
            Integer idUsuarioRegistra = usuarioLogueado != null ? usuarioLogueado.getIdUsuario() : null;

            mantenimientoService.ingresarAMantenimiento(idEquipo, tipo, desc, resp, idUsuarioRegistra);

            JOptionPane.showMessageDialog(this, "Equipo ingresado a mantenimiento correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            cargarCombos();
            cargarDatosTabla();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Atención", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void finalizarMantenimiento() {
        if (idMantenimientoSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un mantenimiento en proceso de la tabla.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String resultado = cmbResultadoCierre != null ? (String) cmbResultadoCierre.getSelectedItem() : "REPARADO";
            String obs = txtObservacionesSalida != null ? txtObservacionesSalida.getText() : "";
            Integer idUsuario = usuarioLogueado != null ? usuarioLogueado.getIdUsuario() : null;

            mantenimientoService.cerrarMantenimiento(idMantenimientoSeleccionado, resultado, obs, idUsuario);

            String estadoFinal = "REPARADO".equalsIgnoreCase(resultado) ? "DISPONIBLE" : "BAJA";
            JOptionPane.showMessageDialog(this, "Mantenimiento finalizado. El equipo pasa al estado: " + estadoFinal, "Éxito", JOptionPane.INFORMATION_MESSAGE);

            limpiarFormulario();
            cargarCombos();
            cargarDatosTabla();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void seleccionarFila() {
        if (tblMantenimientos == null) return;
        int fila = tblMantenimientos.getSelectedRow();
        if (fila >= 0) {
            idMantenimientoSeleccionado = (Integer) tableModel.getValueAt(fila, 0);
        }
    }

    private void limpiarFormulario() {
        idMantenimientoSeleccionado = null;
        if (txtResponsable != null) txtResponsable.setText("");
        if (txtDescripcion != null) txtDescripcion.setText("");
        if (txtObservacionesSalida != null) txtObservacionesSalida.setText("");
        if (tblMantenimientos != null) tblMantenimientos.clearSelection();
    }
}