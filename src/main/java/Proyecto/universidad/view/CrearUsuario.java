package Proyecto.universidad.view;

import Proyecto.universidad.controller.CrearUsuarioController;
import Proyecto.universidad.model.Rol;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Vista para crear una cuenta nueva, abierta desde el formulario de Login.
 *
 * El diseño principal viene de CrearUsuario.form (bind-to-class = CrearUsuario);
 * si el archivo se compiló sin el instrumentador de IntelliJ, se usa un
 * formulario de respaldo construido en código.
 */
public class CrearUsuario extends JFrame {

    private JPanel panelPrincipal;              // vinculada por CrearUsuario.form
    private JTextField txtNombre;
    private JTextField txtApellido;
    private JTextField txtUsuario;
    private JPasswordField passwordFieldPassword;
    private JPasswordField passwordFieldConfirmacion;
    private JTextField txtCorreo;
    private JComboBox<Rol> cmbRol;
    private JButton btnCrear;
    private JButton btnCancelar;

    private final CrearUsuarioController controller;

    public CrearUsuario() {
        controller = new CrearUsuarioController();

        setTitle("SIGELAB - Crear nueva cuenta");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);

        if (panelPrincipal != null) {
            setContentPane(panelPrincipal);
        } else {
            construirFormularioRespaldo();
        }

        if (btnCrear != null) {
            btnCrear.addActionListener(e -> crearUsuario());
        }
        if (btnCancelar != null) {
            btnCancelar.addActionListener(e -> dispose());
        }

        cargarRoles();
    }

    private void construirFormularioRespaldo() {
        panelPrincipal = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitulo = new JLabel("SIGELAB - Crear nueva cuenta");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panelPrincipal.add(lblTitulo, gbc);

        gbc.gridwidth = 1;

        txtNombre = agregarCampoRespaldo(gbc, 1, "Nombre:");
        txtApellido = agregarCampoRespaldo(gbc, 2, "Apellido:");
        txtUsuario = agregarCampoRespaldo(gbc, 3, "Usuario:");

        passwordFieldPassword = agregarCampoRespaldoPassword(gbc, 4, "Contraseña:");
        passwordFieldConfirmacion = agregarCampoRespaldoPassword(gbc, 5, "Confirmar contraseña:");
        txtCorreo = agregarCampoRespaldo(gbc, 6, "Correo:");

        gbc.gridx = 0;
        gbc.gridy = 7;
        panelPrincipal.add(new JLabel("Rol:"), gbc);
        cmbRol = new JComboBox<>();
        gbc.gridx = 1;
        panelPrincipal.add(cmbRol, gbc);

        JLabel lblAyuda = new JLabel("Para iniciar sesión deberá registrar su rostro con un administrador.");
        lblAyuda.setFont(new Font("Arial", Font.ITALIC, 12));
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        panelPrincipal.add(lblAyuda, gbc);

        btnCrear = new JButton("Crear cuenta");
        btnCancelar = new JButton("Cancelar");
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 1;
        panelPrincipal.add(btnCrear, gbc);
        gbc.gridx = 1;
        panelPrincipal.add(btnCancelar, gbc);

        setContentPane(panelPrincipal);
    }

    private JTextField agregarCampoRespaldo(GridBagConstraints gbc, int fila, String etiqueta) {
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        panelPrincipal.add(new JLabel(etiqueta), gbc);

        JTextField campo = new JTextField(20);
        gbc.gridx = 1;
        panelPrincipal.add(campo, gbc);
        return campo;
    }

    private JPasswordField agregarCampoRespaldoPassword(GridBagConstraints gbc, int fila, String etiqueta) {
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        panelPrincipal.add(new JLabel(etiqueta), gbc);

        JPasswordField campo = new JPasswordField(20);
        gbc.gridx = 1;
        panelPrincipal.add(campo, gbc);
        return campo;
    }

    private void cargarRoles() {
        if (cmbRol == null) {
            return;
        }

        try {
            List<Rol> roles = controller.obtenerRoles();
            for (Rol rol : roles) {
                cmbRol.addItem(rol);
            }

            if (roles.isEmpty()) {
                btnCrear.setEnabled(false);
                mostrarError("No existen roles ACTIVOS en la base de datos. Ejecute el script tablas.sql.");
            }
        } catch (RuntimeException e) {
            btnCrear.setEnabled(false);
            mostrarError("No se pudieron cargar los roles: " + e.getMessage());
        }
    }

    private void crearUsuario() {
        Rol rolSeleccionado = cmbRol != null ? (Rol) cmbRol.getSelectedItem() : null;

        if (rolSeleccionado == null) {
            mostrarError("Debe seleccionar un rol.");
            return;
        }

        if (txtNombre == null || txtApellido == null || txtUsuario == null
                || passwordFieldPassword == null || passwordFieldConfirmacion == null || txtCorreo == null) {
            mostrarError("El formulario se compiló sin el diseño (.form). Compile con IntelliJ.");
            return;
        }

        try {
            controller.crearUsuario(
                    txtNombre.getText(),
                    txtApellido.getText(),
                    txtUsuario.getText(),
                    new String(passwordFieldPassword.getPassword()),
                    new String(passwordFieldConfirmacion.getPassword()),
                    txtCorreo.getText(),
                    rolSeleccionado.getIntIdRol()
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Usuario creado correctamente.\n"
                            + "Para poder iniciar sesión deberá registrar su rostro facial.",
                    "Crear nueva cuenta",
                    JOptionPane.INFORMATION_MESSAGE
            );
            dispose();

        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    private void mostrarError(String strMensaje) {
        JOptionPane.showMessageDialog(
                this,
                strMensaje == null || strMensaje.isBlank() ? "No se pudo completar la operación." : strMensaje,
                "Crear nueva cuenta",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
