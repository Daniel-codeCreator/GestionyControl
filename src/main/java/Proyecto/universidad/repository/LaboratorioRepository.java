package Proyecto.universidad.repository;

import Proyecto.universidad.Config.connection;
import Proyecto.universidad.model.Laboratorio;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LaboratorioRepository {

    public List<Laboratorio> listarTodos() {
        List<Laboratorio> lista = new ArrayList<>();
        String sql = "SELECT IdLaboratorio, Codigo, Nombre, Ubicacion, Capacidad, Estado FROM Laboratorio";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Laboratorio lab = new Laboratorio();
                lab.setIdLaboratorio(rs.getInt("IdLaboratorio"));
                lab.setStrCodigo(rs.getString("Codigo"));
                lab.setStrNombre(rs.getString("Nombre"));
                lab.setStrUbicacion(rs.getString("Ubicacion"));
                lab.setIntCapacidad(rs.getInt("Capacidad"));
                lab.setStrEstado(rs.getString("Estado"));
                lista.add(lab);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar laboratorios: " + e.getMessage());
        }
        return lista;
    }

    public Laboratorio buscarPorCodigo(String strCodigo) {
        String sql = "SELECT IdLaboratorio, Codigo, Nombre, Ubicacion, Capacidad, Estado FROM Laboratorio WHERE Codigo = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, strCodigo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Laboratorio(
                            rs.getInt("IdLaboratorio"),
                            rs.getString("Codigo"),
                            rs.getString("Nombre"),
                            rs.getString("Ubicacion"),
                            rs.getInt("Capacidad"),
                            rs.getString("Estado")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar laboratorio por código: " + e.getMessage());
        }
        return null;
    }

    public boolean guardar(Laboratorio lab) {
        String sql = "INSERT INTO Laboratorio (Codigo, Nombre, Ubicacion, Capacidad, Estado) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, lab.getStrCodigo());
            stmt.setString(2, lab.getStrNombre());
            stmt.setString(3, lab.getStrUbicacion());
            stmt.setInt(4, lab.getIntCapacidad());
            stmt.setString(5, lab.getStrEstado() != null ? lab.getStrEstado() : "ACTIVO");

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al guardar laboratorio: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(Laboratorio lab) {
        String sql = "UPDATE Laboratorio SET Codigo = ?, Nombre = ?, Ubicacion = ?, Capacidad = ?, Estado = ? WHERE IdLaboratorio = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, lab.getStrCodigo());
            stmt.setString(2, lab.getStrNombre());
            stmt.setString(3, lab.getStrUbicacion());
            stmt.setInt(4, lab.getIntCapacidad());
            stmt.setString(5, lab.getStrEstado());
            stmt.setInt(6, lab.getIdLaboratorio());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar laboratorio: " + e.getMessage());
            return false;
        }
    }
}