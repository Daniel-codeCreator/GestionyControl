package Proyecto.universidad.view;

import javax.swing.*;

public class Menu {

    private JPanel JpanenPrincipal;
    private JButton button1;
    private JButton button2;
    private JButton button3;
    private JButton button4;
    private JButton button5;
    private JButton button6;

    public Menu() {

        button1.addActionListener(e -> {
            JOptionPane.showMessageDialog(JpanenPrincipal, "Botón 1 presionado");
        });

        button2.addActionListener(e -> {
            JOptionPane.showMessageDialog(JpanenPrincipal, "Botón 2 presionado");
        });

        button3.addActionListener(e -> {
            JOptionPane.showMessageDialog(JpanenPrincipal, "Botón 3 presionado");
        });

        button4.addActionListener(e -> {
            JOptionPane.showMessageDialog(JpanenPrincipal, "Botón 4 presionado");
        });

        button5.addActionListener(e -> {
            JOptionPane.showMessageDialog(JpanenPrincipal, "Botón 5 presionado");
        });

        button6.addActionListener(e -> {
            JOptionPane.showMessageDialog(JpanenPrincipal, "Botón 6 presionado");
        });
    }

    public JPanel getJpanenPrincipal() {
        return JpanenPrincipal;
    }
}