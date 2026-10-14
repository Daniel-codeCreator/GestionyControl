package Proyecto.universidad.repository;

import Proyecto.universidad.Config.connection;
import Proyecto.universidad.model.TipoEquipo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TipoEquipoRepository {

    public List<TipoEquipo> listarTodos() {
        List<TipoEquipo> lista = new ArrayList<>();
        String sql = "SELECT IdTipoEquipo, Nombre, Descripcion FROM TipoEquipo";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                TipoEquipo tipo = new TipoEquipo();
                tipo.setIdTipoEquipo(rs.getInt("IdTipoEquipo"));
                tipo.setStrNombre(rs.getString("Nombre"));
                tipo.setStrDescripcion(rs.getString("Descripcion"));
                lista.add(tipo);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar tipos de equipo: " + e.getMessage());
        }
        return lista;
    }

    public boolean guardar(TipoEquipo tipo) {
        String sql = "INSERT INTO TipoEquipo (Nombre, Descripcion) VALUES (?, ?)";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tipo.getStrNombre());
            stmt.setString(2, tipo.getStrDescripcion());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al guardar tipo de equipo: " + e.getMessage());
            return false;
        }
    }
}