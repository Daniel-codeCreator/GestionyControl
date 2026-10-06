package Proyecto.universidad.view;

import javax.swing.*;
import java.awt.*;

public class Login extends JFrame {
    private JPanel panelPrincipal;
    private JTextField txtUser;
    private JTextField txtPassword;
    private JButton btnIngresar;
    private JButton btnSalir;

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

        btnSalir.addActionListener(e -> System.exit(0));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Login login = new Login();
            login.setVisible(true);
        });
    }
}
