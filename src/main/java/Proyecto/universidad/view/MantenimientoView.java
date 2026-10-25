package Proyecto.universidad.view;

import Proyecto.universidad.enums.EstadoEquipo;
import Proyecto.universidad.model.Equipo;
import Proyecto.universidad.model.Mantenimiento;
import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.service.EquipoService;
import Proyecto.universidad.service.MantenimientoService;
import Proyecto.universidad.service.UsuarioService;

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
    private final UsuarioService usuarioService;

    private DefaultTableModel tableModel;
    private Integer idMantenimientoSeleccionado;
    private Integer idEquipoSeleccionado;

    public MantenimientoView() {
        mantenimientoService = new MantenimientoService();
        equipoService = new EquipoService();
        usuarioService = new UsuarioService();

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
        if (cmbEquipo != null) {
            cmbEquipo.removeAllItems();
            List<Equipo> equipos = equipoService.obtenerTodos();
            for (Equipo eq : equipos) {
                if (eq.getEstado() == EstadoEquipo.DANADO || eq.getEstado() == EstadoEquipo.DISPONIBLE) {
                    cmbEquipo.addItem(eq);
                }
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

            // Obtiene un ID de usuario válido de la base de datos
            List<Usuario> usuarios = usuarioService.obtenerTodos();
            int idUsuarioRegistra = (usuarios != null && !usuarios.isEmpty()) ? usuarios.get(0).getIdUsuario() : 1;

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
        if (idMantenimientoSeleccionado == null || idEquipoSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un mantenimiento en proceso de la tabla.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String resultado = cmbResultadoCierre != null ? (String) cmbResultadoCierre.getSelectedItem() : "REPARADO";
            String obs = txtObservacionesSalida != null ? txtObservacionesSalida.getText() : "";

            mantenimientoService.cerrarMantenimiento(idMantenimientoSeleccionado, idEquipoSeleccionado, resultado, obs);

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
            idEquipoSeleccionado = (Integer) tableModel.getValueAt(fila, 1);
        }
    }

    private void limpiarFormulario() {
        idMantenimientoSeleccionado = null;
        idEquipoSeleccionado = null;
        if (txtResponsable != null) txtResponsable.setText("");
        if (txtDescripcion != null) txtDescripcion.setText("");
        if (txtObservacionesSalida != null) txtObservacionesSalida.setText("");
        if (tblMantenimientos != null) tblMantenimientos.clearSelection();
    }
}