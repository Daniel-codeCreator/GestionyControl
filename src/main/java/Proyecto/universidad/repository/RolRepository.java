package Proyecto.universidad.repository;

import Proyecto.universidad.Config.connection;
import Proyecto.universidad.model.Rol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RolRepository {

    /**
     * Obtiene los roles ACTIVOS para poblar el ComboBox de creación de usuarios.
     */
    public List<Rol> listarActivos() {
        List<Rol> lista = new ArrayList<>();
        String strSql = """
                SELECT IdRol, Nombre, Descripcion, Estado
                FROM Rol
                WHERE Estado = 'ACTIVO'
                ORDER BY Nombre
                """;

        try (
                Connection conn = connection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(strSql);
                ResultSet rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                Rol rol = new Rol();
                rol.setIntIdRol(rs.getInt("IdRol"));
                rol.setStrNombre(rs.getString("Nombre"));
                rol.setStrDescripcion(rs.getString("Descripcion"));
                rol.setStrEstado(rs.getString("Estado"));
                lista.add(rol);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar los roles: " + e.getMessage(), e);
        }

        return lista;
    }
}
