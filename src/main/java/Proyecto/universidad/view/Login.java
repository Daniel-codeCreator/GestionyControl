package Proyecto.universidad.view;

import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.service.AuthService;

import javax.swing.*;
import java.awt.*;

<<<<<<< HEAD
public class Login extends JFrame {
=======
public class Login {

>>>>>>> be9d7ef (se agrega la facial-version)
    private JPanel panelPrincipal;
    private JTextField txtUser;
    private JTextField txtPassword;
    private JButton btnIngresar;
    private JButton btnSalir;

<<<<<<< HEAD
    public Login() {
        setTitle("Inicio de Sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        panelPrincipal = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitulo = new JLabel("Sistema de Gestión y Control");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panelPrincipal.add(lblTitulo, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panelPrincipal.add(new JLabel("Usuario:"), gbc);

        txtUser = new JTextField(20);
        gbc.gridx = 1;
        panelPrincipal.add(txtUser, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panelPrincipal.add(new JLabel("Contraseña:"), gbc);

        txtPassword = new JTextField(20);
        gbc.gridx = 1;
        panelPrincipal.add(txtPassword, gbc);

        JPanel panelBotones = new JPanel(new FlowLayout());
        btnIngresar = new JButton("Ingresar");
        btnSalir = new JButton("Salir");
        panelBotones.add(btnIngresar);
        panelBotones.add(btnSalir);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panelPrincipal.add(panelBotones, gbc);

        add(panelPrincipal);
=======
    private final AuthService authService;

    public Login() {

        authService = new AuthService();

        btnIngresar.addActionListener(e -> ingresar());
>>>>>>> be9d7ef (se agrega la facial-version)

        btnSalir.addActionListener(e -> System.exit(0));
    }

<<<<<<< HEAD
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Login login = new Login();
            login.setVisible(true);
        });
    }
}
=======
    private void ingresar() {

        String strUsuario = txtUser.getText().trim();
        String strPassword = txtPassword.getText();

        if (strUsuario.isEmpty() || strPassword.isEmpty()) {

            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "Debe ingresar usuario y contraseña.",
                    "Datos incompletos",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        try {

            Usuario usuario = authService.autenticar(
                    strUsuario,
                    strPassword
            );

            if (usuario == null) {

                JOptionPane.showMessageDialog(
                        panelPrincipal,
                        "Usuario o contraseña incorrectos.",
                        "Error de autenticación",
                        JOptionPane.ERROR_MESSAGE
                );

                return;
            }

//            JOptionPane.showMessageDialog(
//                    panelPrincipal,
//                    "Bienvenido " + usuario.getStrNombre(),
//                    "Inicio de sesión",
//                    JOptionPane.INFORMATION_MESSAGE
//            );

        abrirMenu();

            // Por ahora solamente cerramos el Login.
            // Después aquí abriremos Menu.java.

        } catch (RuntimeException ex) {

            JOptionPane.showMessageDialog(
                    panelPrincipal,
                    "Ocurrió un error al iniciar sesión:\n"
                            + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void abrirMenu() {

        Menu menu = new Menu();

        JFrame frameMenu = new JFrame("Sistema de Gestión y Control");

        frameMenu.setContentPane(menu.getJpanenPrincipal());

        frameMenu.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frameMenu.pack();

        frameMenu.setLocationRelativeTo(null);

        frameMenu.setResizable(false);

        frameMenu.setVisible(true);

        SwingUtilities.getWindowAncestor(panelPrincipal).dispose();
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }
}
>>>>>>> be9d7ef (se agrega la facial-version)
