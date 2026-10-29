package Proyecto.universidad.view;

import Proyecto.universidad.enums.EstadoEquipo;
import Proyecto.universidad.model.DetallePrestamo;
import Proyecto.universidad.model.Equipo;
import Proyecto.universidad.model.Prestamo;
import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.service.EquipoService;
import Proyecto.universidad.service.PrestamoService;
import Proyecto.universidad.service.UsuarioService;
import Proyecto.universidad.service.QrService;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
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
    private JButton btnEscanearQR;
    private JButton getBtnCerrar;
    private final QrService qrService = new QrService();

    private JButton btnAgregarCarrito;
    private JButton btnQuitarCarrito;
    private JTable tblCarrito;
    private JComboBox<String> cmbCondicionDevolucion;

    private final PrestamoService prestamoService;
    private final EquipoService equipoService;
    private final UsuarioService usuarioService;

    private DefaultTableModel tableModelPrestamos;
    private DefaultTableModel tableModelDisponibles;

    private Integer idPrestamoSeleccionado;
    private Integer idEquipoSeleccionado;

    private final Usuario usuarioLogueado;

    public PrestamoView(Usuario usuarioLogueado) {
        this.usuarioLogueado = usuarioLogueado;

        prestamoService = new PrestamoService();
        equipoService = new EquipoService();
        usuarioService = new UsuarioService();

        setTitle("Gestión de Préstamos y Devoluciones - SIGELAB");
        setSize(1100, 700);
        setMinimumSize(new java.awt.Dimension(950, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        if (panelPrincipal != null) {
            setContentPane(panelPrincipal);
        }

        configurarComponentesForm();
        configurarSpinnerFecha();
        cargarCombos();
        configurarTablas();
        cargarEquiposDisponibles();
        cargarDatosTabla();

        if (btnRegistrarPrestamo != null) btnRegistrarPrestamo.addActionListener(e -> registrarPrestamo());
        if (btnProcesarDevolucion != null) btnProcesarDevolucion.addActionListener(e -> procesarDevolucion());
        if (btnLimpiar != null) btnLimpiar.addActionListener(e -> limpiarFormulario());
        if (btnCerrar != null) btnCerrar.addActionListener(e -> dispose());
        if (btnAgregarCarrito != null) btnAgregarCarrito.addActionListener(e -> tblCarrito.selectAll());
        if (btnQuitarCarrito != null) btnQuitarCarrito.addActionListener(e -> tblCarrito.clearSelection());
        if (btnEscanearQR != null) btnEscanearQR.addActionListener(e -> escanearQRDevolucion());
        if (btnCerrar != null) btnCerrar.addActionListener(e -> dispose());

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

        if (cmbEquipo != null) cmbEquipo.setVisible(false);

        if (btnAgregarCarrito != null) btnAgregarCarrito.setText("Seleccionar todos");
        if (btnQuitarCarrito != null) btnQuitarCarrito.setText("Quitar selección");
    }

    private void configurarSpinnerFecha() {
        if (spnFechaDevolucion != null) {
            SpinnerDateModel dateModel = new SpinnerDateModel(
                    fechaSugerida(), inicioDeHoy(), null, Calendar.DAY_OF_MONTH);
            spnFechaDevolucion.setModel(dateModel);
            spnFechaDevolucion.setEditor(new JSpinner.DateEditor(spnFechaDevolucion, "dd/MM/yyyy"));
        }
    }

    private Date inicioDeHoy() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date fechaSugerida() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        return cal.getTime();
    }

    private void cargarCombos() {
        if (cmbUsuario != null) {
            cmbUsuario.removeAllItems();
            List<Usuario> usuarios = usuarioService.obtenerTodos();
            for (Usuario u : usuarios) cmbUsuario.addItem(u);
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

        tableModelDisponibles = new DefaultTableModel(
                new String[]{"ID Equipo", "Codigo", "Nombre"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        if (tblCarrito != null) {
            tblCarrito.setModel(tableModelDisponibles);
            tblCarrito.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
    }

    private void cargarEquiposDisponibles() {
        tableModelDisponibles.setRowCount(0);
        List<Equipo> equipos = equipoService.obtenerTodos();
        for (Equipo eq : equipos) {
            if (eq.getEstado() == EstadoEquipo.DISPONIBLE) {
                tableModelDisponibles.addRow(new Object[]{
                        eq.getIdEquipo(), eq.getCodigoInventario(), eq.getNombre()
                });
            }
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

    private void registrarPrestamo() {
        String obs = txtObservaciones != null ? txtObservaciones.getText().trim() : "";
        if (obs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Las observaciones del préstamo son obligatorias.",
                    "Atencion", JOptionPane.WARNING_MESSAGE);
            if (txtObservaciones != null) txtObservaciones.requestFocus();
            return;
        }

        int[] filas = tblCarrito.getSelectedRows();
        if (filas.length == 0) {
            JOptionPane.showMessageDialog(this, "Seleccione al menos un equipo de la tabla de equipos disponibles.",
                    "Atencion", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<DetallePrestamo> detalles = new ArrayList<>();
        for (int fila : filas) {
            int modelo = tblCarrito.convertRowIndexToModel(fila);
            Integer idEquipo = (Integer) tableModelDisponibles.getValueAt(modelo, 0);
            String codigo = String.valueOf(tableModelDisponibles.getValueAt(modelo, 1));
            String nombre = String.valueOf(tableModelDisponibles.getValueAt(modelo, 2));
            detalles.add(new DetallePrestamo(idEquipo, nombre, codigo));
        }

        try {
            Usuario solicitanteSel = (Usuario) cmbUsuario.getSelectedItem();
            String solicitante = solicitanteSel != null
                    ? solicitanteSel.getStrNombre() + " " + solicitanteSel.getStrApellido()
                    : "";
            Integer idUsuarioRegistra = usuarioLogueado != null ? usuarioLogueado.getIdUsuario() : null;

            Calendar cal = Calendar.getInstance();
            cal.setTime((Date) spnFechaDevolucion.getValue());
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 0);
            Timestamp fechaEsperada = new Timestamp(cal.getTimeInMillis());

            prestamoService.realizarPrestamoMultiple(solicitante, detalles, idUsuarioRegistra, fechaEsperada, obs);

            JOptionPane.showMessageDialog(this, "Prestamo registrado exitosamente.", "Exito", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            cargarEquiposDisponibles();
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
            int idUsuarioRecibe = usuarioLogueado != null ? usuarioLogueado.getIdUsuario() : 0;
            String condicion = cmbCondicionDevolucion != null ? (String) cmbCondicionDevolucion.getSelectedItem() : "BUENO";

            prestamoService.procesarDevolucion(idPrestamoSeleccionado, idEquipoSeleccionado, idUsuarioRecibe, condicion, obs);

            JOptionPane.showMessageDialog(this, "Devolucion registrada en condicion '" + condicion + "'.", "Exito", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            cargarEquiposDisponibles();
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

        if (tblCarrito != null) tblCarrito.clearSelection();
        if (txtObservaciones != null) txtObservaciones.setText("");
        if (txtObsDevolucion != null) txtObsDevolucion.setText("");
        if (tblPrestamos != null) tblPrestamos.clearSelection();

        if (spnFechaDevolucion != null) {
            spnFechaDevolucion.setValue(fechaSugerida());
        }
    }

    private void escanearQRDevolucion() {
        JFileChooser chooser = new JFileChooser(qrService.carpetaQr());
        chooser.setDialogTitle("Seleccione la imagen del código QR para Devolución");
        chooser.setFileFilter(new FileNameExtensionFilter("Imágenes (PNG, JPG)", "png", "jpg", "jpeg"));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            Equipo eq = qrService.identificarEquipo(chooser.getSelectedFile());
            String codigoBuscado = eq.getStrCodigo();

            boolean encontrado = false;
            if (tblPrestamos != null && tableModelPrestamos != null) {
                for (int i = 0; i < tableModelPrestamos.getRowCount(); i++) {
                    String codTabla = String.valueOf(tableModelPrestamos.getValueAt(i, 4));
                    if (codigoBuscado.equalsIgnoreCase(codTabla)) {
                        tblPrestamos.setRowSelectionInterval(i, i);
                        tblPrestamos.scrollRectToVisible(tblPrestamos.getCellRect(i, 0, true));
                        encontrado = true;

                        idPrestamoSeleccionado = (Integer) tableModelPrestamos.getValueAt(i, 0);
                        idEquipoSeleccionado = (Integer) tableModelPrestamos.getValueAt(i, 1);
                        break;
                    }
                }
            }

            if (encontrado) {
                JOptionPane.showMessageDialog(this,
                        "Equipo " + codigoBuscado + " localizado en préstamos activos.\nListo para procesar la devolución.",
                        "QR Identificado", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "El equipo " + codigoBuscado + " no tiene un préstamo ACTIVO registrado actualmente.",
                        "Atención", JOptionPane.WARNING_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Lectura de QR", JOptionPane.ERROR_MESSAGE);
        }
    }
}