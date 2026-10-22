package Proyecto.universidad.view;

import Proyecto.universidad.model.Usuario;

import javax.swing.*;

public class Menu extends JFrame {

    private JPanel JpanenPrincipal;
    private JButton btnLaboratorios;
    private JButton btnTiposEquipo;
    private JButton btnEquipos;
    private JButton btnPrestamos;
    private JButton btnMantenimiento;
    private JButton btnCrearUsuario;

    // Atributo para guardar la sesión del usuario autenticado
    private Usuario usuarioLogueado;

    // Constructor sin parámetros (por compatibilidad)
    public Menu() {
        this(null);
    }

    // Constructor principal que recibe al usuario que inició sesión
    public Menu(Usuario usuario) {
        this.usuarioLogueado = usuario;

        setTitle("Sistema de Gestión y Control - Menú Principal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setResizable(false);

        if (JpanenPrincipal != null) {
            setContentPane(JpanenPrincipal);
        }

        // Asignación de Listeners para cada módulo
        if (btnLaboratorios != null) {
            btnLaboratorios.addActionListener(e -> abrirLaboratorios());
        }

        if (btnTiposEquipo != null) {
            btnTiposEquipo.addActionListener(e -> abrirTiposEquipo());
        }

        if (btnEquipos != null) {
            btnEquipos.addActionListener(e -> abrirEquipos());
        }

        if (btnPrestamos != null) {
            btnPrestamos.addActionListener(e -> abrirPrestamos());
        }

        if (btnMantenimiento != null) {
            btnMantenimiento.addActionListener(e -> JOptionPane.showMessageDialog(this, "Módulo de Mantenimiento en construcción."));
        }

        if (btnCrearUsuario != null) {
            btnCrearUsuario.addActionListener(e -> abrirPantallaCrearUsuario());
        }
    }

    private void abrirLaboratorios() {
        LaboratorioView labView = new LaboratorioView();
        labView.setVisible(true);
    }

    private void abrirTiposEquipo() {
        TipoEquipoView tipoView = new TipoEquipoView();
        tipoView.setVisible(true);
    }

    private void abrirEquipos() {
        EquiposView equiView = new EquiposView();
        equiView.setVisible(true);
    }

    private void abrirPrestamos() {
        PrestamoView prestamoView = new PrestamoView();
        prestamoView.setVisible(true);
    }

    private void abrirPantallaCrearUsuario() {
        // Valida que el usuario exista y que su IdRol sea 1 (ADMIN)
        if (usuarioLogueado != null && usuarioLogueado.getIntRol() == 1) {
            UsuarioView usuarioView = new UsuarioView();
            usuarioView.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Acceso Denegado para tu rol.",
                    "Solo Admin",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    public JPanel getJpanenPrincipal() {
        return JpanenPrincipal;
    }
}