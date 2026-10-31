package Proyecto.universidad.view;

import Proyecto.universidad.model.Dashboard;
import Proyecto.universidad.model.Usuario;
import Proyecto.universidad.service.DashboardService;
import Proyecto.universidad.enums.RolUsuario;
import Proyecto.universidad.service.Sesion;

import javax.swing.*;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Menu extends JFrame {

    private JPanel JpanenPrincipal;
    private JButton btnLaboratorios;
    private JButton btnTiposEquipo;
    private JButton btnEquipos;
    private JButton btnPrestamos;
    private JButton btnMantenimiento;
    private JButton btnCrearUsuario;
    private JButton btnReportes;

    // Contadores del dashboard
    private JLabel lblTotalEquipos;
    private JLabel lblDisponibles;
    private JLabel lblPrestados;
    private JLabel lblDanados;
    private JLabel lblEnMantenimiento;
    private JLabel lblDeBaja;
    private JLabel lblPrestamosActivos;
    private JLabel lblPrestamosVencidos;

    private static final Color COLOR_ALERTA = new Color(192, 0, 0);

    private final DashboardService dashboardService = new DashboardService();

    // Atributo para guardar la sesión del usuario autenticado
    private Usuario usuarioLogueado;

    // Constructor sin parámetros (por compatibilidad)
    public Menu() {
        this(null);
    }

    // Constructor principal que recibe al usuario que inició sesión
    public Menu(Usuario usuario) {
        this.usuarioLogueado = usuario;
        Sesion.iniciar(usuario);

        setTitle("Sistema de Gestión y Control - Menú Principal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setResizable(false);

        if (JpanenPrincipal != null) {
            setContentPane(JpanenPrincipal);
        }

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
            btnMantenimiento.addActionListener(e -> abrirMantenimiento());
        }

        if (btnCrearUsuario != null) {
            btnCrearUsuario.addActionListener(e -> abrirPantallaCrearUsuario());
        }

        if (btnReportes != null) {
            btnReportes.addActionListener(e -> abrirReportes());
        }

        actualizarDashboard();
    }



    // Consulta la BD y muestra las 8 cantidades del dashboard
    private void actualizarDashboard() {

        aplicarPermisosPorRol();
        try {
            Dashboard r = dashboardService.obtenerResumen();

            mostrar(lblTotalEquipos, String.valueOf(r.totalEquipos()), null);
            mostrar(lblDisponibles, String.valueOf(r.disponibles()), null);
            mostrar(lblPrestados, String.valueOf(r.prestados()), null);
            mostrar(lblDanados, String.valueOf(r.danados()), r.danados() > 0 ? COLOR_ALERTA : null);
            mostrar(lblEnMantenimiento, String.valueOf(r.enMantenimiento()), null);
            mostrar(lblDeBaja, String.valueOf(r.deBaja()), null);
            mostrar(lblPrestamosActivos, String.valueOf(r.prestamosActivos()), null);
            mostrar(lblPrestamosVencidos, String.valueOf(r.prestamosVencidos()),
                    r.prestamosVencidos() > 0 ? COLOR_ALERTA : null);

        } catch (RuntimeException e) {
            // Si la BD no responde se muestra "-" para no mostrar ceros falsos
            for (JLabel lbl : new JLabel[]{lblTotalEquipos, lblDisponibles, lblPrestados, lblDanados,
                    lblEnMantenimiento, lblDeBaja, lblPrestamosActivos, lblPrestamosVencidos}) {
                mostrar(lbl, "-", null);
            }


        }

    }

    private void mostrar(JLabel etiqueta, String texto, Color color) {
        if (etiqueta == null) return;
        etiqueta.setText(texto);
        etiqueta.setForeground(color != null ? color : UIManager.getColor("Label.foreground"));
    }

    // Oculta el menú (NO lo cierra), abre el módulo y, al cerrarse, lo vuelve a mostrar con datos frescos
    private void abrirVentana(JFrame ventana) {
        setVisible(false);
        ventana.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                actualizarDashboard();
                setVisible(true);
            }
        });
        ventana.setVisible(true);
    }

    private void abrirLaboratorios() {
        abrirVentana(new LaboratorioView());
    }

    private void abrirTiposEquipo() {
        abrirVentana(new TipoEquipoView());
    }

    private void abrirEquipos() {
        abrirVentana(new EquiposView());
    }

    private void abrirPrestamos() {
        abrirVentana(new PrestamoView(usuarioLogueado));
    }

    private void abrirMantenimiento() {
        abrirVentana(new MantenimientoView(usuarioLogueado));
    }

    // Ayuda visual: la validación real de permisos está en los Service
    private void aplicarPermisosPorRol() {
        boolean admin = Sesion.tieneRol(RolUsuario.ADMIN);
        boolean operativo = Sesion.tieneRol(RolUsuario.ADMIN, RolUsuario.ENCARGADO);

        mostrar(btnLaboratorios, admin);
        mostrar(btnTiposEquipo, admin);
        mostrar(btnCrearUsuario, admin);

        mostrar(btnEquipos, operativo);
        mostrar(btnPrestamos, operativo);
        mostrar(btnMantenimiento, operativo);

        // Reportes lo ven los tres roles
    }

    private void mostrar(JButton boton, boolean visible) {
        if (boton != null) boton.setVisible(visible);
    }

    private void abrirPantallaCrearUsuario() {
        if (Sesion.tieneRol(RolUsuario.ADMIN)) {
            abrirVentana(new UsuarioView());
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Acceso denegado para tu rol.",
                    "Solo Admin",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }


    private void abrirReportes() {
        abrirVentana(new ReportesView());
    }

    public JPanel getJpanenPrincipal() {
        return JpanenPrincipal;
    }
}