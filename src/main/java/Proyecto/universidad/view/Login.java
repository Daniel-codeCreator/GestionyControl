package Proyecto.universidad.view;

import Proyecto.universidad.integration.facial.FacialAuthClient;
import Proyecto.universidad.integration.facial.FacialClientException;
import Proyecto.universidad.integration.facial.FacialEnrollmentResult;
import Proyecto.universidad.integration.facial.FacialVerificationResult;
import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.service.AuthService;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;

public class Login extends JFrame {

    private JPanel panelPrincipal;
    private JTextField txtUser;
    private JPasswordField passwordField1;
    private JButton btnIngresar;
    private JButton btnSalir;
    private JButton btnRegistrarRostro;

    private final AuthService authService;
    private final FacialAuthClient facialAuthClient;
    private boolean autenticando;

    public Login() {
        authService = new AuthService();
        facialAuthClient = new FacialAuthClient();

        setTitle("Inicio de Sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setResizable(false);

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
            btnRegistrarRostro = new JButton("Registrar Rostro");
            btnSalir = new JButton("Salir");
            panelBotones.add(btnIngresar);
            panelBotones.add(btnRegistrarRostro);
            panelBotones.add(btnSalir);

            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.gridwidth = 2;
            panelPrincipal.add(panelBotones, gbc);
        }

        setContentPane(panelPrincipal);

        if (btnIngresar != null) {
            btnIngresar.addActionListener(e -> ingresar());
        }
        if (btnRegistrarRostro != null) {
            btnRegistrarRostro.addActionListener(e -> registrarRostro());
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
                Usuario usuario = authService.autenticar(strUsuario, strPassword);
                if (usuario == null) return LoginOutcome.sinCredenciales();

                FacialVerificationResult facial = facialAuthClient.verify(usuario.getIdUsuario());
                return new LoginOutcome(usuario, facial);
            }

            @Override
            protected void done() {
                try {
                    LoginOutcome outcome = get();

                    if (outcome.credencialesInvalidas()) {
                        authService.registrarAuditoriaLoginFallido(null, strUsuario, "Usuario/contraseña incorrectos o usuario inactivo");
                        mostrarError("Usuario o contraseña incorrectos, o usuario inactivo.");
                        limpiarSeleccionPassword();

                    } else if (outcome.facial() == null || !outcome.facial().success() || !outcome.facial().matched()) {
                        String msg = (outcome.facial() != null && outcome.facial().message() != null)
                                ? outcome.facial().message()
                                : "Verificación facial no coincidente.";

                        authService.registrarAuditoriaLoginFallido(outcome.usuario().getIdUsuario(), strUsuario, "Fallo biométrico: " + msg);
                        mostrarError(msg);
                        limpiarSeleccionPassword();

                    } else {
                        authService.registrarAuditoriaLoginExitoso(outcome.usuario());
                        // Pasamos la sesión del usuario al menú
                        abrirMenu(outcome.usuario());
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    authService.registrarAuditoriaLoginFallido(null, strUsuario, "Proceso de verificación interrumpido");
                    mostrarError("La verificación facial fue interrumpida.");

                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    String errorMsg = cause instanceof FacialClientException ? cause.getMessage() : "Error en el cliente facial";

                    authService.registrarAuditoriaLoginFallido(null, strUsuario, "Excepción de login: " + errorMsg);

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

    private void registrarRostro() {
        String usuario = txtUser != null ? txtUser.getText().trim() : "";
        String password = passwordField1 != null ? new String(passwordField1.getPassword()).trim() : "";

        if (usuario.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Por favor, ingrese su usuario y contraseña para vincular su rostro.",
                    "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // 1. Validar credenciales usando AuthService
            Usuario u = authService.autenticar(usuario, password);
            if (u == null) {
                JOptionPane.showMessageDialog(this,
                        "Credenciales incorrectas. No se puede registrar el rostro.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 2. Notificar e iniciar captura facial
            JOptionPane.showMessageDialog(this,
                    "Se abrirá la cámara. Por favor mire hacia el frente para capturar su rostro.",
                    "Reconocimiento Facial", JOptionPane.INFORMATION_MESSAGE);

            // 3. Evaluar el resultado de tipo FacialEnrollmentResult
            FacialEnrollmentResult resultado = facialAuthClient.enroll(u.getIdUsuario(), true);

            if (resultado != null && resultado.success()) {
                JOptionPane.showMessageDialog(this,
                        "¡Rostro registrado exitosamente! Ya puede iniciar sesión usando reconocimiento facial.",
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                String msg = (resultado != null && resultado.message() != null) ? resultado.message() : "Error en la captura.";
                JOptionPane.showMessageDialog(this,
                        "No se pudo completar el registro facial: " + msg,
                        "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error al conectar con el servicio facial: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarSeleccionPassword() {
        if (passwordField1 != null) {
            passwordField1.selectAll();
            passwordField1.requestFocus();
        }
    }

    private void abrirMenu(Usuario usuarioAutenticado) {
        // Inicializa el Menú pasando la información del usuario autenticado
        Menu menu = new Menu(usuarioAutenticado);
        JFrame frameMenu = new JFrame("Sistema de Gestión y Control - Menú Principal");

        frameMenu.setContentPane(menu.getJpanenPrincipal());
        frameMenu.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frameMenu.setSize(800, 600);
        frameMenu.setLocationRelativeTo(null);
        frameMenu.setResizable(false);
        frameMenu.setVisible(true);

        this.dispose();
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