package Proyecto.universidad.view;

import Proyecto.universidad.enums.EstadoEquipo;
import Proyecto.universidad.model.Equipo;
import Proyecto.universidad.model.Laboratorio;
import Proyecto.universidad.model.TipoEquipo;
import Proyecto.universidad.service.EquipoService;
import Proyecto.universidad.service.LaboratorioService;
import Proyecto.universidad.service.TipoEquipoService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class EquiposView extends JFrame {

    private JPanel panelPrincipal;
    private JTextField txtCodigo;
    private JTextField txtNombre;
    private JComboBox<TipoEquipo> cmbTipoEquipo;
    private JComboBox<Laboratorio> cmbLaboratorio;
    private JTextField txtMarca;
    private JTextField txtModelo;
    private JTextField txtSerie;
    private JComboBox<EstadoEquipo> cmbEstado;
    private JButton btnGuardar;
    private JButton btnActualizar;
    private JButton btnLimpiar;
    private JButton btnCerrar;
    private JTable tblEquipos;

    private final EquipoService equipoService;
    private final TipoEquipoService tipoEquipoService;
    private final LaboratorioService laboratorioService;

    private DefaultTableModel tableModel;
    private Integer idEquipoSeleccionado;

    public EquiposView() {
        equipoService = new EquipoService();
        tipoEquipoService = new TipoEquipoService();
        laboratorioService = new LaboratorioService();

        setTitle("Gestión de Equipos");
        setSize(900, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        if (panelPrincipal != null) {
            setContentPane(panelPrincipal);
        }

        cargarCombos();
        configurarTabla();
        cargarDatosTabla();

        if (btnGuardar != null) btnGuardar.addActionListener(e -> guardar());
        if (btnActualizar != null) btnActualizar.addActionListener(e -> actualizar());
        if (btnLimpiar != null) btnLimpiar.addActionListener(e -> limpiarFormulario());
        if (btnCerrar != null) btnCerrar.addActionListener(e -> dispose());

        if (tblEquipos != null) {
            tblEquipos.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    seleccionarFila();
                }
            });
        }
    }

    private void cargarCombos() {
        if (cmbTipoEquipo != null) {
            cmbTipoEquipo.removeAllItems();
            List<TipoEquipo> tipos = tipoEquipoService.obtenerTodos();
            for (TipoEquipo t : tipos) cmbTipoEquipo.addItem(t);
        }

        if (cmbLaboratorio != null) {
            cmbLaboratorio.removeAllItems();
            List<Laboratorio> labs = laboratorioService.obtenerTodos();
            for (Laboratorio l : labs) cmbLaboratorio.addItem(l);
        }

        if (cmbEstado != null) {
            cmbEstado.removeAllItems();
            for (EstadoEquipo e : EstadoEquipo.values()) cmbEstado.addItem(e);
            cmbEstado.setSelectedItem(EstadoEquipo.DISPONIBLE);
        }
    }

    private void configurarTabla() {
        // Índices de columnas:
        // 0: ID | 1: Serie | 2: Código | 3: Nombre | 4: Modelo | 5: Estado | 6: Marca | 7: Laboratorio | 8: Tipo
        tableModel = new DefaultTableModel(
                new String[]{"ID", "Serie", "Código", "Nombre", "Modelo", "Estado", "Marca", "Laboratorio", "Tipo"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        if (tblEquipos != null) {
            tblEquipos.setModel(tableModel);
        }
    }

    private void cargarDatosTabla() {
        tableModel.setRowCount(0);
        List<Equipo> lista = equipoService.obtenerTodos();
        for (Equipo eq : lista) {
            tableModel.addRow(new Object[]{
                    eq.getIdEquipo(),               // 0. ID
                    eq.getStrSerie(),               // 1. Serie
                    eq.getStrCodigo(),              // 2. Código
                    eq.getStrNombre(),              // 3. Nombre
                    eq.getStrModelo(),              // 4. Modelo
                    eq.getEstado(),                 // 5. Estado
                    eq.getStrMarca(),               // 6. Marca
                    eq.getStrNombreLaboratorio(),   // 7. Laboratorio
                    eq.getStrNombreTipo()           // 8. Tipo
            });
        }
    }

    private void guardar() {
        try {
            String codigo = txtCodigo.getText();
            String nombre = txtNombre.getText();
            TipoEquipo tipo = (TipoEquipo) cmbTipoEquipo.getSelectedItem();
            Laboratorio lab = (Laboratorio) cmbLaboratorio.getSelectedItem();
            String marca = txtMarca.getText();
            String modelo = txtModelo.getText();
            String serie = txtSerie.getText();

            Integer idTipo = tipo != null ? tipo.getIdTipoEquipo() : null;
            Integer idLab = lab != null ? lab.getIdLaboratorio() : null;

            equipoService.registrarEquipo(codigo, nombre, idTipo, idLab, marca, modelo, serie);

            JOptionPane.showMessageDialog(this, "Equipo registrado con éxito en estado DISPONIBLE.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            cargarDatosTabla();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizar() {
        if (idEquipoSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un equipo de la tabla para actualizar.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            TipoEquipo tipo = (TipoEquipo) cmbTipoEquipo.getSelectedItem();
            Laboratorio lab = (Laboratorio) cmbLaboratorio.getSelectedItem();

            Equipo eq = new Equipo();
            eq.setIdEquipo(idEquipoSeleccionado);
            eq.setStrCodigo(txtCodigo.getText().trim());
            eq.setStrNombre(txtNombre.getText().trim());
            eq.setIdTipoEquipo(tipo != null ? tipo.getIdTipoEquipo() : null);
            eq.setIdLaboratorio(lab != null ? lab.getIdLaboratorio() : null);
            eq.setEstado((EstadoEquipo) cmbEstado.getSelectedItem());
            eq.setStrMarca(txtMarca.getText().trim());
            eq.setStrModelo(txtModelo.getText().trim());
            eq.setStrSerie(txtSerie.getText().trim());

            equipoService.actualizarEquipo(eq);

            JOptionPane.showMessageDialog(this, "Equipo actualizado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            cargarDatosTabla();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void seleccionarFila() {
        int fila = tblEquipos.getSelectedRow();
        if (fila >= 0) {
            idEquipoSeleccionado = (Integer) tableModel.getValueAt(fila, 0);
            txtSerie.setText((String) tableModel.getValueAt(fila, 1));
            txtCodigo.setText((String) tableModel.getValueAt(fila, 2));
            txtNombre.setText((String) tableModel.getValueAt(fila, 3));
            txtModelo.setText((String) tableModel.getValueAt(fila, 4));

            cmbEstado.setSelectedItem(tableModel.getValueAt(fila, 5));
            txtMarca.setText((String) tableModel.getValueAt(fila, 6));

            // Posición 7: Laboratorio
            String nombreLab = (String) tableModel.getValueAt(fila, 7);
            if (cmbLaboratorio != null) {
                for (int i = 0; i < cmbLaboratorio.getItemCount(); i++) {
                    if (cmbLaboratorio.getItemAt(i).getStrNombre().equals(nombreLab)) {
                        cmbLaboratorio.setSelectedIndex(i);
                        break;
                    }
                }
            }

            // Posición 8: Tipo de Equipo
            String nombreTipo = (String) tableModel.getValueAt(fila, 8);
            if (cmbTipoEquipo != null) {
                for (int i = 0; i < cmbTipoEquipo.getItemCount(); i++) {
                    if (cmbTipoEquipo.getItemAt(i).getStrNombre().equals(nombreTipo)) {
                        cmbTipoEquipo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
    }

    private void limpiarFormulario() {
        idEquipoSeleccionado = null;
        if (txtCodigo != null) txtCodigo.setText("");
        if (txtNombre != null) txtNombre.setText("");
        if (txtMarca != null) txtMarca.setText("");
        if (txtModelo != null) txtModelo.setText("");
        if (txtSerie != null) txtSerie.setText("");
        if (cmbEstado != null) cmbEstado.setSelectedItem(EstadoEquipo.DISPONIBLE);
        if (tblEquipos != null) tblEquipos.clearSelection();
    }
}