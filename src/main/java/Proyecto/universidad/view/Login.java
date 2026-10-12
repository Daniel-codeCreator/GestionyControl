package Proyecto.universidad.view;

import Proyecto.universidad.integration.facial.FacialAuthClient;
import Proyecto.universidad.integration.facial.FacialClientException;
import Proyecto.universidad.integration.facial.FacialVerificationResult;
import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.service.AuthService;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;

public class Login extends JFrame {

    private JPanel panelPrincipal;
    private JTextField txtUser;
    private JPasswordField passwordField1; // Mismo nombre asignado en Login.form
    private JButton btnIngresar;
    private JButton btnSalir;

    private final AuthService authService;
    private final FacialAuthClient facialAuthClient;
    private boolean autenticando;

    public Login() {
        authService = new AuthService();
        facialAuthClient = new FacialAuthClient();

        // Configuración de la ventana principal
        setTitle("Inicio de Sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600); // Tamaño estándar de 800x600
        setLocationRelativeTo(null);
        setResizable(false);

        // Si usas el GUI Designer de IntelliJ, no reinstancies panelPrincipal;
        // pero asignamos fallback/panel en caso de instanciación directa.
        if (panelPrincipal == null) {
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

            passwordField1 = new JPasswordField(20);
            gbc.gridx = 1;
            panelPrincipal.add(passwordField1, gbc);

            JPanel panelBotones = new JPanel(new FlowLayout());
            btnIngresar = new JButton("Ingresar");
            btnSalir = new JButton("Salir");
            panelBotones.add(btnIngresar);
            panelBotones.add(btnSalir);

            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.gridwidth = 2;
            panelPrincipal.add(panelBotones, gbc);
        }

        setContentPane(panelPrincipal);

        // Asignación de Listeners de los componentes
        if (btnIngresar != null) {
            btnIngresar.addActionListener(e -> ingresar());
        }
        if (btnSalir != null) {
            btnSalir.addActionListener(e -> System.exit(0));
        }
        if (passwordField1 != null) {
            passwordField1.addActionListener(e -> ingresar());
        }
    }

    private void ingresar() {
        if (autenticando) return;

        String strUsuario = txtUser != null ? txtUser.getText().trim() : "";
        String strPassword = passwordField1 != null ? new String(passwordField1.getPassword()) : "";

        if (strUsuario.isBlank()) {
            mostrarError("Ingrese el usuario.");
            if (txtUser != null) txtUser.requestFocus();
            return;
        }
        if (strPassword.isBlank()) {
            mostrarError("Ingrese la contraseña.");
            if (passwordField1 != null) passwordField1.requestFocus();
            return;
        }

        autenticando = true;
        if (btnIngresar != null) {
            btnIngresar.setEnabled(false);
            btnIngresar.setText("Verificando identidad...");
        }

        new SwingWorker<LoginOutcome, Void>() {

            @Override
            protected LoginOutcome doInBackground() {
                // Paso 1: Usuario ACTIVO + contraseña (RN-01)
                Usuario usuario = authService.autenticar(strUsuario, strPassword);
                if (usuario == null) return LoginOutcome.sinCredenciales();

                // Paso 2: Rostro (RN-02). Python recibe solo el ID del usuario
                FacialVerificationResult facial = facialAuthClient.verify(usuario.getIdUsuario());
                return new LoginOutcome(usuario, facial);
            }

            @Override
            protected void done() {
                try {
                    LoginOutcome outcome = get();

                    if (outcome.credencialesInvalidas()) {
                        mostrarError("Usuario o contraseña incorrectos, o usuario inactivo.");
                        limpiarSeleccionPassword();

                    } else if (outcome.facial() == null || !outcome.facial().success() || !outcome.facial().matched()) {
                        String msg = (outcome.facial() != null && outcome.facial().message() != null)
                                ? outcome.facial().message()
                                : "Verificación facial no coincidente.";
                        mostrarError(msg);
                        limpiarSeleccionPassword();

                    } else {
                        abrirMenu();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarError("La verificación facial fue interrumpida.");

                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();

                    if (cause instanceof FacialClientException facialError) {
                        mostrarError(facialError.getMessage());
                    } else {
                        System.err.println("Error en login: " + cause);
                        mostrarError("No fue posible completar la autenticación.");
                    }

                } finally {
                    autenticando = false;
                    if (btnIngresar != null) {
                        btnIngresar.setEnabled(true);
                        btnIngresar.setText("Ingresar");
                    }
                }
            }
        }.execute();
    }

    private void limpiarSeleccionPassword() {
        if (passwordField1 != null) {
            passwordField1.selectAll();
            passwordField1.requestFocus();
        }
    }

    private void abrirMenu() {
        Menu menu = new Menu();
        JFrame frameMenu = new JFrame("Sistema de Gestión y Control - Menú Principal");

        frameMenu.setContentPane(menu.getJpanenPrincipal());
        frameMenu.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frameMenu.setSize(800, 600);
        frameMenu.setLocationRelativeTo(null);
        frameMenu.setResizable(false);
        frameMenu.setVisible(true);

        this.dispose(); // Cierra esta ventana de Login
    }

    private void mostrarError(String strMensaje) {
        if (strMensaje == null || strMensaje.isBlank()) {
            strMensaje = "Ocurrió un error inesperado.";
        }
        JOptionPane.showMessageDialog(
                panelPrincipal,
                strMensaje,
                "Inicio de Sesión",
                JOptionPane.ERROR_MESSAGE
        );
    }

    public JPanel getPanelPrincipal() {
        return panelPrincipal;
    }

    private record LoginOutcome(Usuario usuario, FacialVerificationResult facial,
                                boolean credencialesInvalidas) {

        private LoginOutcome(Usuario usuario, FacialVerificationResult facial) {
            this(usuario, facial, false);
        }

        private static LoginOutcome sinCredenciales() {
            return new LoginOutcome(null, null, true);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Login login = new Login();
            login.setVisible(true);
        });
    }
}