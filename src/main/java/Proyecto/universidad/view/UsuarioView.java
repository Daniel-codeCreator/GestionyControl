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

        if (btnGuardar != null) btnGuardar.addActionListener(e -> guardarUsuario());
        if (btnLimpiar != null) btnLimpiar.addActionListener(e -> limpiarFormulario());
        if (btnCerrar != null) btnCerrar.addActionListener(e -> dispose());
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
            String nombre = txtNombre.getText();
            String apellido = txtApellido.getText();
            String usuario = txtUsuario.getText();
            String password = new String(txtPassword.getPassword());
            String correo = txtCorreo.getText();
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
    }
}