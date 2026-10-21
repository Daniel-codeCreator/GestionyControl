package Proyecto.universidad.view;

import Proyecto.universidad.model.TipoEquipo;
import Proyecto.universidad.service.TipoEquipoService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class TipoEquipoView extends JFrame {

    private JPanel panelPrincipal;
    private JTextField txtNombre;
    private JTextField txtDescripcion;
    private JButton btnGuardar;
    private JButton btnLimpiar;
    private JButton btnCerrar;
    private JTable tblTiposEquipo;

    private final TipoEquipoService tipoEquipoService;
    private DefaultTableModel tableModel;

    public TipoEquipoView() {
        tipoEquipoService = new TipoEquipoService();

        setTitle("Gestión de Tipos de Equipo");
        setSize(700, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        if (panelPrincipal != null) {
            setContentPane(panelPrincipal);
        }

        configurarTabla();
        cargarDatosTabla();

        // Listeners de los botones
        if (btnGuardar != null) btnGuardar.addActionListener(e -> guardar());
        if (btnLimpiar != null) btnLimpiar.addActionListener(e -> limpiarFormulario());
        if (btnCerrar != null) btnCerrar.addActionListener(e -> dispose());
    }

    private void configurarTabla() {
        tableModel = new DefaultTableModel(
                new String[]{"ID", "Nombre", "Descripción"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Tabla de solo lectura
            }
        };
        if (tblTiposEquipo != null) {
            tblTiposEquipo.setModel(tableModel);
        }
    }

    private void cargarDatosTabla() {
        tableModel.setRowCount(0);
        List<TipoEquipo> lista = tipoEquipoService.obtenerTodos();
        for (TipoEquipo tipo : lista) {
            tableModel.addRow(new Object[]{
                    tipo.getIdTipoEquipo(),
                    tipo.getStrNombre(),
                    tipo.getStrDescripcion()
            });
        }
    }

    private void guardar() {
        try {
            String nombre = txtNombre.getText();
            String descripcion = txtDescripcion.getText();

            tipoEquipoService.registrarTipoEquipo(nombre, descripcion);

            JOptionPane.showMessageDialog(this, "Tipo de equipo guardado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            cargarDatosTabla();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarFormulario() {
        if (txtNombre != null) txtNombre.setText("");
        if (txtDescripcion != null) txtDescripcion.setText("");
        if (tblTiposEquipo != null) tblTiposEquipo.clearSelection();
    }
}