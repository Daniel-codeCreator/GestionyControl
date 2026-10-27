package Proyecto.universidad.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class connection {

    // Posiciones dentro del arreglo DATOS
    private static final int HOST = 0;
    private static final int PORT = 1;
    private static final int BASE_DATOS = 2;
    private static final int USUARIO = 3;
    private static final int CONTRASENA = 4;

    /**
     * Datos de conexión: { HOST, PORT, BASE DE DATOS, USUARIO, CONTRASEÑA }.
     *
     * Se pueden sobrescribir sin recompilar con:
     *   -Duniversidad.db.host / -Duniversidad.db.port / -Duniversidad.db.name
     *   -Duniversidad.db.user / -Duniversidad.db.password
     * o con las variables de entorno DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD.
     */
    private static final String[] DATOS = {
            "progra2.parqueobelen.site",
            "1434",
            "PROYCPROGRA2",
            "sa",
            "Programacion2*2024#"
    };

    private static final String URL = "jdbc:sqlserver://" + host() + ":" + port()
            + ";databaseName=" + baseDatos()
            + ";encrypt=true;trustServerCertificate=true;sendTimeAsDatetime=false;";

    private connection() {
    }

    public static Connection getConnection()
            throws SQLException {

        return DriverManager.getConnection(
                URL,
                usuario(),
                contrasena()
        );
    }

    public static String url() {
        return URL;
    }

    public static String host() {
        return valor("universidad.db.host", "DB_HOST", HOST);
    }

    public static String port() {
        return valor("universidad.db.port", "DB_PORT", PORT);
    }

    public static String baseDatos() {
        return valor("universidad.db.name", "DB_NAME", BASE_DATOS);
    }

    public static String usuario() {
        return valor("universidad.db.user", "DB_USER", USUARIO);
    }

    public static String contrasena() {
        return valor("universidad.db.password", "DB_PASSWORD", CONTRASENA);
    }

    /**
     * Muestra la configuración efectiva sin exponer la contraseña.
     */
    public static String descripcion() {
        return "host=" + host() + " | port=" + port() + " | base=" + baseDatos()
                + " | user=" + usuario()
                + " | password=" + (contrasena().isEmpty() ? "(vacia)" : "********");
    }

    private static String valor(String strPropiedad, String strVariable, int intPosicion) {
        String configurado = System.getProperty(strPropiedad);
        if (configurado == null || configurado.isBlank()) {
            configurado = System.getenv(strVariable);
        }
        if (configurado == null || configurado.isBlank()) {
            configurado = DATOS[intPosicion];
        }
        return configurado;
    }
}
