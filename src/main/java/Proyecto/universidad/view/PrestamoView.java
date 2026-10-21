package Proyecto.universidad.view;

import Proyecto.universidad.enums.EstadoEquipo;
import Proyecto.universidad.model.Equipo;
import Proyecto.universidad.model.Prestamo;
import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.service.EquipoService;
import Proyecto.universidad.service.PrestamoService;
import Proyecto.universidad.service.UsuarioService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class PrestamoView extends JFrame {

    private JPanel panelPrincipal;
    private JComboBox<Usuario> cmbUsuario;
    private JComboBox<Equipo> cmbEquipo;
    private JSpinner spnFechaDevolucion; // Componente JSpinner enlazado con el .form
    private JTextField txtObservaciones;
    private JButton btnRegistrarPrestamo;
    private JButton btnLimpiar;
    private JButton btnCerrar;
    private JTable tblPrestamos;
    private JTextField txtObsDevolucion;
    private JButton btnProcesarDevolucion;

    private final PrestamoService prestamoService;
    private final EquipoService equipoService;
    private final UsuarioService usuarioService;

    private DefaultTableModel tableModel;
    private Integer idPrestamoSeleccionado;
    private Integer idEquipoSeleccionado;

    public PrestamoView() {
        prestamoService = new PrestamoService();
        equipoService = new EquipoService();
        usuarioService = new UsuarioService();

        setTitle("Gestión de Préstamos y Devoluciones");
        setSize(950, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        if (panelPrincipal != null) {
            setContentPane(panelPrincipal);
        }

        configurarSpinnerFecha();
        cargarCombos();
        configurarTabla();
        cargarDatosTabla();

        if (btnRegistrarPrestamo != null) btnRegistrarPrestamo.addActionListener(e -> registrarPrestamo());
        if (btnProcesarDevolucion != null) btnProcesarDevolucion.addActionListener(e -> procesarDevolucion());
        if (btnLimpiar != null) btnLimpiar.addActionListener(e -> limpiarFormulario());
        if (btnCerrar != null) btnCerrar.addActionListener(e -> dispose());

        if (tblPrestamos != null) {
            tblPrestamos.getSelectionModel().addListSelectionListener(e -> seleccionarFila());
        }
    }

    private void configurarSpinnerFecha() {
        if (spnFechaDevolucion != null) {
            // Configurar el modelo de fecha del JSpinner
            SpinnerDateModel dateModel = new SpinnerDateModel();
            spnFechaDevolucion.setModel(dateModel);

            // Establecer el formato visual: YYYY-MM-DD HH:mm
            JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(spnFechaDevolucion, "yyyy-MM-dd");
            spnFechaDevolucion.setEditor(dateEditor);

            // Valor por defecto: 24 horas después del momento actual
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            spnFechaDevolucion.setValue(cal.getTime());
        }
    }

    private void cargarCombos() {
        if (cmbUsuario != null) {
            cmbUsuario.removeAllItems();
            List<Usuario> usuarios = usuarioService.obtenerTodos();
            for (Usuario u : usuarios) cmbUsuario.addItem(u);
        }

        if (cmbEquipo != null) {
            cmbEquipo.removeAllItems();
            List<Equipo> equipos = equipoService.obtenerTodos();
            for (Equipo eq : equipos) {
                if (eq.getEstado() == EstadoEquipo.DISPONIBLE) {
                    cmbEquipo.addItem(eq);
                }
            }
        }
    }

    private void configurarTabla() {
        tableModel = new DefaultTableModel(
                new String[]{"ID Préstamo", "ID Equipo", "Solicitante", "Equipo", "Código", "Fecha Préstamo", "Estado"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        if (tblPrestamos != null) {
            tblPrestamos.setModel(tableModel);
        }
    }

    private void cargarDatosTabla() {
        tableModel.setRowCount(0);
        List<Prestamo> lista = prestamoService.obtenerPrestamosActivos();
        for (Prestamo p : lista) {
            tableModel.addRow(new Object[]{
                    p.getIdPrestamo(),
                    p.getIdEquipo(),
                    p.getNombreUsuario(),
                    p.getNombreEquipo(),
                    p.getCodigoEquipo(),
                    p.getFechaPrestamo(),
                    p.getEstado()
            });
        }
    }

    private void registrarPrestamo() {
        try {
            Usuario u = (Usuario) cmbUsuario.getSelectedItem();
            Equipo eq = (Equipo) cmbEquipo.getSelectedItem();
            String obs = txtObservaciones.getText();

            String solicitante = u != null ? u.getStrNombre() + " " + u.getStrApellido() : "";
            Integer idEquipo = eq != null ? eq.getIdEquipo() : null;
            Integer idUsuarioRegistra = u != null ? u.getIdUsuario() : 1;

            // Obtener la fecha seleccionada del JSpinner
            Date fechaSeleccionada = (Date) spnFechaDevolucion.getValue();
            Timestamp fechaEsperada = new Timestamp(fechaSeleccionada.getTime());

            prestamoService.realizarPrestamo(solicitante, idEquipo, idUsuarioRegistra, fechaEsperada, obs);

            JOptionPane.showMessageDialog(this, "Préstamo registrado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            cargarCombos();
            cargarDatosTabla();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void procesarDevolucion() {
        if (idPrestamoSeleccionado == null || idEquipoSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un préstamo activo de la tabla para recibir el equipo.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String obs = txtObsDevolucion.getText();
            Usuario u = (Usuario) cmbUsuario.getSelectedItem();
            int idUsuarioRecibe = u != null ? u.getIdUsuario() : 1;

            prestamoService.procesarDevolucion(idPrestamoSeleccionado, idEquipoSeleccionado, idUsuarioRecibe, "BUENO", obs);

            JOptionPane.showMessageDialog(this, "Devolución registrada correctamente. El equipo vuelve a estar DISPONIBLE.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            cargarCombos();
            cargarDatosTabla();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void seleccionarFila() {
        int fila = tblPrestamos.getSelectedRow();
        if (fila >= 0) {
            idPrestamoSeleccionado = (Integer) tableModel.getValueAt(fila, 0);
            idEquipoSeleccionado = (Integer) tableModel.getValueAt(fila, 1);
        }
    }

    private void limpiarFormulario() {
        idPrestamoSeleccionado = null;
        idEquipoSeleccionado = null;
        if (txtObservaciones != null) txtObservaciones.setText("");
        if (txtObsDevolucion != null) txtObsDevolucion.setText("");
        if (tblPrestamos != null) tblPrestamos.clearSelection();

        // Resetear la fecha a +24h
        if (spnFechaDevolucion != null) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            spnFechaDevolucion.setValue(cal.getTime());
        }
    }
}