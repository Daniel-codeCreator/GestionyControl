package Proyecto.universidad.view;

import javax.swing.*;

public class Menu extends JFrame {

    private JPanel JpanenPrincipal; // Mismo nombre que tu Form
    private JButton btnLaboratorios;
    private JButton btnTiposEquipo;
    private JButton btnEquipos;
    private JButton btnPrestamos;
    private JButton btnMantenimiento;
    private JButton btnCrearUsuario;

    public Menu() {
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
            btnCrearUsuario.addActionListener(e -> JOptionPane.showMessageDialog(this, "Módulo de Usuarios en construcción."));
        }
    }

    private void abrirLaboratorios() {
        // Formulario que crearemos a continuación para el CRUD de Laboratorios
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

    public JPanel getJpanenPrincipal() {
        return JpanenPrincipal;
    }
}