package Proyecto.universidad.view;

import Proyecto.universidad.model.Laboratorio;
import Proyecto.universidad.service.LaboratorioService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class LaboratorioView extends JFrame {

    private JPanel panelPrincipal;
    private JTextField txtCodigo;
    private JTextField txtNombre;
    private JTextField txtUbicacion;
    private JTextField txtCapacidad;
    private JComboBox<String> cmbEstado;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnLimpiar;
    private JButton btnCerrar;
    private JTable tblLaboratorios;

    private final LaboratorioService laboratorioService;
    private DefaultTableModel tableModel;
    private Integer idLaboratorioSeleccionado;

    public LaboratorioView() {
        laboratorioService = new LaboratorioService();

        setTitle("Gestión de Laboratorios");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        if (panelPrincipal != null) {
            setContentPane(panelPrincipal);
        }

        inicializarCombo();
        configurarTabla();
        cargarDatosTabla();

        // Listeners de los botones
        if (btnGuardar != null) btnGuardar.addActionListener(e -> guardar());
        if (btnActualizar != null) btnActualizar.addActionListener(e -> actualizar());
        if (btnLimpiar != null) btnLimpiar.addActionListener(e -> limpiarFormulario());
        if (btnCerrar != null) btnCerrar.addActionListener(e -> dispose());

        // Evento al seleccionar fila en la tabla
        if (tblLaboratorios != null) {
            tblLaboratorios.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    seleccionarFila();
                }
            });
        }
    }

    private void inicializarCombo() {
        if (cmbEstado != null) {
            cmbEstado.removeAllItems();
            cmbEstado.addItem("ACTIVO");
            cmbEstado.addItem("INACTIVO");
        }
        if (txtCodigo != null) {
            txtCodigo.setEditable(false);//ya que el codigo es automatico no lo van a poder cambiar
            txtCodigo.setText("(automatico)");
        }
    }

    private void configurarTabla() {
        tableModel = new DefaultTableModel(
                new String[]{"ID", "Código", "Nombre", "Ubicación", "Capacidad", "Estado"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Tabla no editable directamente
            }
        };

        if (tblLaboratorios != null) {
            tblLaboratorios.setModel(tableModel);

            // Si en el .form la tabla no fue envuelta dentro de un JScrollPane,
            // garantizamos mostrar el encabezado de las columnas asociándolo a su contenedor:
            if (tblLaboratorios.getParent() == null || !(tblLaboratorios.getParent().getParent() instanceof JScrollPane)) {
                if (panelPrincipal != null && tblLaboratorios.getTableHeader() != null) {
                    tblLaboratorios.getTableHeader().setReorderingAllowed(false);
                }
            }
        }
    }

    private void cargarDatosTabla() {
        tableModel.setRowCount(0);
        List<Laboratorio> lista = laboratorioService.obtenerTodos();
        for (Laboratorio lab : lista) {
            tableModel.addRow(new Object[]{
                    lab.getIdLaboratorio(),
                    lab.getStrCodigo(),
                    lab.getStrNombre(),
                    lab.getStrUbicacion(),
                    lab.getIntCapacidad(),
                    lab.getStrEstado()
            });
        }
    }

    private void guardar() {
        try {
            String nombre = txtNombre.getText();
            String ubicacion = txtUbicacion.getText();
            int capacidad = Integer.parseInt(txtCapacidad.getText().trim());

            String codigo = laboratorioService.registrarLaboratorio(nombre, ubicacion, capacidad);

            JOptionPane.showMessageDialog(this,
                    "Laboratorio registrado exitosamente.\nCódigo asignado: " + codigo,
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            cargarDatosTabla();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "La capacidad debe ser un número entero válido.", "Error de Validación", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizar() {
        if (idLaboratorioSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un laboratorio de la tabla para actualizar.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Laboratorio lab = new Laboratorio();
            lab.setIdLaboratorio(idLaboratorioSeleccionado);
            lab.setStrCodigo(txtCodigo.getText().trim());
            lab.setStrNombre(txtNombre.getText().trim());
            lab.setStrUbicacion(txtUbicacion.getText().trim());
            lab.setIntCapacidad(Integer.parseInt(txtCapacidad.getText().trim()));
            lab.setStrEstado((String) cmbEstado.getSelectedItem());

            laboratorioService.actualizarLaboratorio(lab);

            JOptionPane.showMessageDialog(this, "Laboratorio actualizado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            cargarDatosTabla();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "La capacidad debe ser un número entero válido.", "Error de Validación", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void seleccionarFila() {
        int fila = tblLaboratorios.getSelectedRow();
        if (fila >= 0) {
            idLaboratorioSeleccionado = (Integer) tableModel.getValueAt(fila, 0);
            txtCodigo.setText((String) tableModel.getValueAt(fila, 1));
            txtNombre.setText((String) tableModel.getValueAt(fila, 2));
            txtUbicacion.setText((String) tableModel.getValueAt(fila, 3));
            txtCapacidad.setText(String.valueOf(tableModel.getValueAt(fila, 4)));
            cmbEstado.setSelectedItem(tableModel.getValueAt(fila, 5));
        }
    }

    private void limpiarFormulario() {
        idLaboratorioSeleccionado = null;
        if (txtCodigo != null) txtCodigo.setText("(automático)");
        if (txtNombre != null) txtNombre.setText("");
        if (txtUbicacion != null) txtUbicacion.setText("");
        if (txtCapacidad != null) txtCapacidad.setText("");
        if (cmbEstado != null && cmbEstado.getItemCount() > 0) cmbEstado.setSelectedIndex(0);
        if (tblLaboratorios != null) tblLaboratorios.clearSelection();
    }
}