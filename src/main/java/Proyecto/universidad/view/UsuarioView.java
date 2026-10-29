package Proyecto.universidad.view;

import Proyecto.universidad.model.Rol;
import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.service.UsuarioService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class UsuarioView extends JFrame {

    private JPanel panelPrincipal;
    private JTextField txtNombre;
    private JTextField txtApellido;
    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JTextField txtCorreo;
    private JComboBox<Rol> cmbRol;
    private JButton btnGuardar;
    private JButton btnLimpiar;
    private JButton btnCerrar;
    private JTable tblUsuarios;
    private JButton btnDesactivar;
    private JButton btnActivar;

    private final UsuarioService usuarioService;
    private DefaultTableModel tableModel;

    public UsuarioView() {
        usuarioService = new UsuarioService();

        setTitle("Gestión de Usuarios del Sistema");
        setSize(850, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        if (panelPrincipal != null) {
            setContentPane(panelPrincipal);
        }

        cargarRoles();
        configurarTabla();
        cargarDatosTabla();
        configurarGeneracionUsuario(); // <-- INVOCACIÓN AGREGADA

        if (btnGuardar != null) btnGuardar.addActionListener(e -> guardarUsuario());
        if (btnLimpiar != null) btnLimpiar.addActionListener(e -> limpiarFormulario());
        if (btnCerrar != null) btnCerrar.addActionListener(e -> dispose());
        if (btnActivar != null) btnActivar.addActionListener(e -> activarUsuario());
        if (btnDesactivar != null) btnDesactivar.addActionListener(e -> desactivarUsuario());
    }

    private void activarUsuario() {
        cambiarEstadoUsuarioSeleccionado("ACTIVO");
    }

    private void desactivarUsuario() {
        cambiarEstadoUsuarioSeleccionado("INACTIVO");
    }

    private void cambiarEstadoUsuarioSeleccionado(String nuevoEstado) {
        if (tblUsuarios == null) return;

        int filaSeleccionada = tblUsuarios.getSelectedRow();
        if (filaSeleccionada < 0) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un usuario de la tabla para cambiar su estado.",
                    "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int filaModelo = tblUsuarios.convertRowIndexToModel(filaSeleccionada);
        Integer idUsuario = (Integer) tableModel.getValueAt(filaModelo, 0);
        String nombreUsuario = (String) tableModel.getValueAt(filaModelo, 1);
        String estadoActual = (String) tableModel.getValueAt(filaModelo, 5);

        if (nuevoEstado.equalsIgnoreCase(estadoActual)) {
            JOptionPane.showMessageDialog(this,
                    "El usuario '" + nombreUsuario + "' ya se encuentra en estado " + estadoActual + ".",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de cambiar el estado de '" + nombreUsuario + "' a " + nuevoEstado + "?",
                "Confirmar cambio de estado", JOptionPane.YES_NO_OPTION);

        if (confirmacion == JOptionPane.YES_OPTION) {
            try {
                usuarioService.cambiarEstado(idUsuario, nuevoEstado);
                JOptionPane.showMessageDialog(this,
                        "Usuario actualizado a estado " + nuevoEstado + " con éxito.",
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);
                cargarDatosTabla();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void configurarGeneracionUsuario() {
        // CORRECCIÓN: Si NO es nulo, deshabilita la edición manual
        if (txtUsuario != null) {
            txtUsuario.setEditable(false);
        }

        javax.swing.event.DocumentListener listener = new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { actualizarVistaPrevia(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { actualizarVistaPrevia(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarVistaPrevia(); }
        };

        if (txtNombre != null) txtNombre.getDocument().addDocumentListener(listener);
        if (txtApellido != null) txtApellido.getDocument().addDocumentListener(listener);
    }

    private void actualizarVistaPrevia() {
        String nombre = txtNombre != null ? txtNombre.getText().trim() : "";
        String apellido = txtApellido != null ? txtApellido.getText().trim() : "";

        if (!nombre.isEmpty() && !apellido.isEmpty()) {
            // Extraer primer nombre y primer apellido
            String primerNombre = nombre.split("\\s+")[0].toLowerCase();
            String primerApellido = apellido.split("\\s+")[0].toLowerCase();

            if (!primerNombre.isEmpty() && !primerApellido.isEmpty()) {
                // Genera la vista previa inicial (ej: d + boc = dboc)
                String usuarioSugerido = primerNombre.substring(0, 1) + primerApellido;
                txtUsuario.setText(usuarioSugerido);
                return;
            }
        }

        txtUsuario.setText("");
    }

    private void cargarRoles() {
        if (cmbRol != null) {
            cmbRol.removeAllItems();
            List<Rol> roles = usuarioService.obtenerRoles();
            for (Rol r : roles) {
                cmbRol.addItem(r);
            }
        }
    }

    private void configurarTabla() {
        tableModel = new DefaultTableModel(
                new String[]{"ID", "Nombre Completo", "Usuario", "Correo", "Rol ID", "Estado"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        if (tblUsuarios != null) {
            tblUsuarios.setModel(tableModel);
        }
    }

    private void cargarDatosTabla() {
        tableModel.setRowCount(0);
        List<Usuario> lista = usuarioService.obtenerTodos();
        for (Usuario u : lista) {
            tableModel.addRow(new Object[]{
                    u.getIdUsuario(),
                    u.getStrNombre() + " " + u.getStrApellido(),
                    u.getStrUsuario(),
                    u.getStrCorreo(),
                    u.getIntRol(),
                    u.getStrEstado()
            });
        }
    }

    private void guardarUsuario() {
        try {
            String nombre = txtNombre.getText().trim();
            String apellido = txtApellido.getText().trim();
            String usuario = txtUsuario.getText().trim(); // Se envía la sugerencia o vacío
            String password = new String(txtPassword.getPassword());
            String correo = txtCorreo.getText().trim();
            Rol rol = (Rol) cmbRol.getSelectedItem();

            Integer idRol = rol != null ? rol.getIdRol() : null;

            usuarioService.registrarUsuario(nombre, apellido, usuario, password, correo, idRol);

            JOptionPane.showMessageDialog(this, "Usuario creado exitosamente mediante Stored Procedure.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            cargarDatosTabla();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarFormulario() {
        if (txtNombre != null) txtNombre.setText("");
        if (txtApellido != null) txtApellido.setText("");
        if (txtUsuario != null) txtUsuario.setText("");
        if (txtPassword != null) txtPassword.setText("");
        if (txtCorreo != null) txtCorreo.setText("");
        if (cmbRol != null && cmbRol.getItemCount() > 0) cmbRol.setSelectedIndex(0);
        if (tblUsuarios != null) tblUsuarios.clearSelection();
    }
}