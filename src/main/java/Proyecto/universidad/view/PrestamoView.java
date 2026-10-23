package Proyecto.universidad.view;

import Proyecto.universidad.enums.EstadoEquipo;
import Proyecto.universidad.model.DetallePrestamo;
import Proyecto.universidad.model.Equipo;
import Proyecto.universidad.model.Prestamo;
import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.service.EquipoService;
import Proyecto.universidad.service.PrestamoService;
import Proyecto.universidad.service.UsuarioService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class PrestamoView extends JFrame {

    // Componentes mapeados desde PrestamoView.form
    private JPanel panelPrincipal;
    private JComboBox<Usuario> cmbUsuario;
    private JComboBox<Equipo> cmbEquipo;
    private JSpinner spnFechaDevolucion;
    private JTextField txtObservaciones;
    private JButton btnRegistrarPrestamo;
    private JButton btnLimpiar;
    private JButton btnCerrar;
    private JTable tblPrestamos;
    private JTextField txtObsDevolucion;
    private JButton btnProcesarDevolucion;

    // Componentes adicionales para préstamo múltiple y condición de devolución
    private JButton btnAgregarCarrito;
    private JButton btnQuitarCarrito;
    private JTable tblCarrito;
    private JComboBox<String> cmbCondicionDevolucion;

    private final PrestamoService prestamoService;
    private final EquipoService equipoService;
    private final UsuarioService usuarioService;

    private DefaultTableModel tableModelPrestamos;
    private DefaultTableModel tableModelCarrito;

    private final List<DetallePrestamo> listaCarrito = new ArrayList<>();
    private Integer idPrestamoSeleccionado;
    private Integer idEquipoSeleccionado;

    public PrestamoView() {
        prestamoService = new PrestamoService();
        equipoService = new EquipoService();
        usuarioService = new UsuarioService();

        setTitle("Gestion de Prestamos y Devoluciones - SIGELAB");
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        if (panelPrincipal != null) {
            setContentPane(panelPrincipal);
        }

        configurarComponentesForm();
        configurarSpinnerFecha();
        cargarCombos();
        configurarTablas();
        cargarDatosTabla();

        if (btnRegistrarPrestamo != null) btnRegistrarPrestamo.addActionListener(e -> registrarPrestamo());
        if (btnProcesarDevolucion != null) btnProcesarDevolucion.addActionListener(e -> procesarDevolucion());
        if (btnLimpiar != null) btnLimpiar.addActionListener(e -> limpiarFormulario());
        if (btnCerrar != null) btnCerrar.addActionListener(e -> dispose());
        if (btnAgregarCarrito != null) btnAgregarCarrito.addActionListener(e -> agregarAlCarrito());
        if (btnQuitarCarrito != null) btnQuitarCarrito.addActionListener(e -> quitarDelCarrito());

        if (tblPrestamos != null) {
            tblPrestamos.getSelectionModel().addListSelectionListener(e -> seleccionarFila());
        }
    }

    private void configurarComponentesForm() {
        if (cmbCondicionDevolucion != null) {
            cmbCondicionDevolucion.removeAllItems();
            cmbCondicionDevolucion.addItem("BUENO");
            cmbCondicionDevolucion.addItem("DANADO");
        }
    }

    private void configurarSpinnerFecha() {
        if (spnFechaDevolucion != null) {
            SpinnerDateModel dateModel = new SpinnerDateModel();
            spnFechaDevolucion.setModel(dateModel);
            JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(spnFechaDevolucion, "yyyy-MM-dd");
            spnFechaDevolucion.setEditor(dateEditor);

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

    private void configurarTablas() {
        tableModelPrestamos = new DefaultTableModel(
                new String[]{"ID Prestamo", "ID Equipo", "Solicitante", "Equipo", "Codigo", "Fecha Prestamo", "Estado"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        if (tblPrestamos != null) {
            tblPrestamos.setModel(tableModelPrestamos);
        }

        tableModelCarrito = new DefaultTableModel(
                new String[]{"ID Equipo", "Codigo", "Nombre"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        if (tblCarrito != null) {
            tblCarrito.setModel(tableModelCarrito);
        }
    }

    private void cargarDatosTabla() {
        tableModelPrestamos.setRowCount(0);
        List<Prestamo> lista = prestamoService.obtenerPrestamosActivos();
        for (Prestamo p : lista) {
            tableModelPrestamos.addRow(new Object[]{
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

    private void agregarAlCarrito() {
        Equipo eq = (Equipo) cmbEquipo.getSelectedItem();
        if (eq == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un equipo disponible.", "Atencion", JOptionPane.WARNING_MESSAGE);
            return;
        }

        for (DetallePrestamo det : listaCarrito) {
            if (det.getIdEquipo().equals(eq.getIdEquipo())) {
                JOptionPane.showMessageDialog(this, "El equipo ya esta agregado a la lista.", "Atencion", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        DetallePrestamo det = new DetallePrestamo(eq.getIdEquipo(), eq.getNombre(), eq.getCodigoInventario());
        listaCarrito.add(det);
        tableModelCarrito.addRow(new Object[]{eq.getIdEquipo(), eq.getCodigoInventario(), eq.getNombre()});
    }

    private void quitarDelCarrito() {
        if (tblCarrito == null) return;
        int fila = tblCarrito.getSelectedRow();
        if (fila >= 0) {
            listaCarrito.remove(fila);
            tableModelCarrito.removeRow(fila);
        } else {
            JOptionPane.showMessageDialog(this, "Seleccione un equipo de la tabla de lista para quitarlo.", "Atencion", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void registrarPrestamo() {
        try {
            Usuario u = (Usuario) cmbUsuario.getSelectedItem();
            String obs = txtObservaciones != null ? txtObservaciones.getText() : "";

            String solicitante = u != null ? u.getStrNombre() + " " + u.getStrApellido() : "";
            Integer idUsuarioRegistra = u != null ? u.getIdUsuario() : 1;

            Date fechaSeleccionada = (Date) spnFechaDevolucion.getValue();
            Timestamp fechaEsperada = new Timestamp(fechaSeleccionada.getTime());

            prestamoService.realizarPrestamoMultiple(solicitante, listaCarrito, idUsuarioRegistra, fechaEsperada, obs);

            JOptionPane.showMessageDialog(this, "Prestamo registrado exitosamente.", "Exito", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            cargarCombos();
            cargarDatosTabla();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error de Validacion", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void procesarDevolucion() {
        if (idPrestamoSeleccionado == null || idEquipoSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un prestamo activo de la tabla para recibir el equipo.", "Atencion", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String obs = txtObsDevolucion != null ? txtObsDevolucion.getText() : "";
            Usuario u = (Usuario) cmbUsuario.getSelectedItem();
            int idUsuarioRecibe = u != null ? u.getIdUsuario() : 1;
            String condicion = cmbCondicionDevolucion != null ? (String) cmbCondicionDevolucion.getSelectedItem() : "BUENO";

            prestamoService.procesarDevolucion(idPrestamoSeleccionado, idEquipoSeleccionado, idUsuarioRecibe, condicion, obs);

            JOptionPane.showMessageDialog(this, "Devolucion registrada en condicion '" + condicion + "'.", "Exito", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            cargarCombos();
            cargarDatosTabla();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void seleccionarFila() {
        if (tblPrestamos == null) return;
        int fila = tblPrestamos.getSelectedRow();
        if (fila >= 0) {
            idPrestamoSeleccionado = (Integer) tableModelPrestamos.getValueAt(fila, 0);
            idEquipoSeleccionado = (Integer) tableModelPrestamos.getValueAt(fila, 1);
        }
    }

    private void limpiarFormulario() {
        idPrestamoSeleccionado = null;
        idEquipoSeleccionado = null;
        listaCarrito.clear();
        if (tableModelCarrito != null) tableModelCarrito.setRowCount(0);

        if (txtObservaciones != null) txtObservaciones.setText("");
        if (txtObsDevolucion != null) txtObsDevolucion.setText("");
        if (tblPrestamos != null) tblPrestamos.clearSelection();

        if (spnFechaDevolucion != null) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            spnFechaDevolucion.setValue(cal.getTime());
        }
    }
}