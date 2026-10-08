package Proyecto.universidad;

<<<<<<< HEAD
import Proyecto.universidad.view.Login;

public class main {
    public static void main(String[] args) {
        Login login = new Login();
        login.setVisible(true);
=======
import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.service.AuthService;
import Proyecto.universidad.view.Login;

import javax.swing.*;

public class main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            asegurarUsuarioAdministrador();

            Login login = new Login();

            JFrame frame = new JFrame("Sistema de Gestión y Control");

            frame.setContentPane(login.getPanelPrincipal());

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            frame.pack();

            frame.setLocationRelativeTo(null);

            frame.setResizable(false);

            frame.setVisible(true);
        });
    }

    private static void asegurarUsuarioAdministrador() {

        try {

            AuthService authService = new AuthService();

            Usuario usuario = authService.buscarPorUsuario("admin");

            if (usuario == null) {

                authService.crearUsuario("admin", "admin", 1);

                System.out.println("Usuario administrador creado correctamente.");

            } else {

                System.out.println("El usuario administrador ya existe.");
            }

        } catch (RuntimeException e) {

            System.err.println("No fue posible verificar el usuario admin: " + e.getMessage());
        }
>>>>>>> be9d7ef (se agrega la facial-version)
    }
}