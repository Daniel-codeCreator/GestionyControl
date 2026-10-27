package Proyecto.universidad.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class connection {

    private static final String URL = configuracion(
            "universidad.db.url", "DB_URL",
            "jdbc:sqlserver://progra2.parqueobelen.site:1434;databaseName=PROYCPROGRA2;encrypt=true;trustServerCertificate=true;sendTimeAsDatetime=false;");
    private static final String USER = configuracion("universidad.db.user", "DB_USER", "sa");
    private static final String PASSWORD = configuracion("universidad.db.password", "DB_PASSWORD", "Programacion2*2024#");

    private connection() {
    }

    public static Connection getConnection()
            throws SQLException {

        return DriverManager.getConnection(
                URL,
                USER,
                PASSWORD
        );
    }

    private static String configuracion(String propiedad, String variable, String valorPorDefecto) {
        String configurado = System.getProperty(propiedad);
        if (configurado == null || configurado.isBlank()) configurado = System.getenv(variable);
        return configurado == null || configurado.isBlank() ? valorPorDefecto : configurado;
    }
}
