package Proyecto.universidad.Config;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class connection {

    private static String url;
    private static String user;
    private static String password;

    static {
        cargarConfiguracionXml();
    }

    private static void cargarConfiguracionXml() {
        InputStream is = null;
        try {
            // 1. Buscar en la raiz del classpath (/conexion.xml en src/main/resources)
            is = connection.class.getResourceAsStream("/conexion.xml");

            // 2. Buscar dentro del paquete Config
            if (is == null) {
                is = connection.class.getResourceAsStream("/Proyecto/universidad/Config/conexion.xml");
            }

            // 3. Buscar como archivo fisico en el sistema
            if (is == null) {
                File file = new File("src/main/resources/conexion.xml");
                if (!file.exists()) {
                    file = new File("src/main/java/Proyecto/universidad/Config/conexion.xml");
                }
                if (file.exists()) {
                    is = new FileInputStream(file);
                }
            }

            if (is == null) {
                throw new RuntimeException("No se encontro el archivo conexion.xml en ninguna ubicacion conocida.");
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(is);
            doc.getDocumentElement().normalize();

            Element dbElement = (Element) doc.getElementsByTagName("db").item(0);

            String host = dbElement.getElementsByTagName("host").item(0).getTextContent().trim();
            String port = dbElement.getElementsByTagName("port").item(0).getTextContent().trim();
            String database = dbElement.getElementsByTagName("database").item(0).getTextContent().trim();
            user = dbElement.getElementsByTagName("user").item(0).getTextContent().trim();
            password = dbElement.getElementsByTagName("password").item(0).getTextContent().trim();

            url = String.format("jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=false;trustServerCertificate=true;", host, port, database);

            System.out.println("Configuracion de BD cargada exitosamente desde XML -> " + host + ":" + port);

        } catch (Exception e) {
            System.err.println("Error al leer conexion.xml: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (is != null) {
                try { is.close(); } catch (Exception ignored) {}
            }
        }

        sincronizarEnvPython();
    }

    private static void sincronizarEnvPython() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "facial-service/scripts/configure_from_java.py");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            // Si el entorno no tiene python global, intenta con py
            try {
                ProcessBuilder pbPy = new ProcessBuilder("py", "facial-service/scripts/configure_from_java.py");
                pbPy.start();
            } catch (Exception ignored) {}
        }
    }

    public static Connection getConnection() throws SQLException {
        if (url == null || url.isBlank()) {
            throw new SQLException("La URL de conexion no se pudo inicializar. Verifique el archivo conexion.xml.");
        }
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            return DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver JDBC de SQL Server no encontrado: " + e.getMessage());
        }
    }
}