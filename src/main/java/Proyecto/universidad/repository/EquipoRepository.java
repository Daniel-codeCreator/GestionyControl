package Proyecto.universidad.repository;

import Proyecto.universidad.Config.connection;
import Proyecto.universidad.enums.EstadoEquipo;
import Proyecto.universidad.model.Equipo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipoRepository {

    public List<Equipo> listarTodos() {
        List<Equipo> lista = new ArrayList<>();
        String sql = "SELECT e.IdEquipo, e.CodigoInventario, e.Nombre, e.IdTipoEquipo, t.Nombre AS TipoNombre, " +
                "e.IdLaboratorio, l.Nombre AS LabNombre, e.Estado, e.Marca, e.Modelo, e.NumeroSerie " +
                "FROM Equipo e " +
                "INNER JOIN TipoEquipo t ON e.IdTipoEquipo = t.IdTipoEquipo " +
                "INNER JOIN Laboratorio l ON e.IdLaboratorio = l.IdLaboratorio";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Equipo eq = new Equipo();
                eq.setIdEquipo(rs.getInt("IdEquipo"));
                eq.setStrCodigo(rs.getString("CodigoInventario"));
                eq.setStrNombre(rs.getString("Nombre"));
                eq.setIdTipoEquipo(rs.getInt("IdTipoEquipo"));
                eq.setStrNombreTipo(rs.getString("TipoNombre"));
                eq.setIdLaboratorio(rs.getInt("IdLaboratorio"));
                eq.setStrNombreLaboratorio(rs.getString("LabNombre"));

                String estadoStr = rs.getString("Estado");
                if (estadoStr != null) {
                    try {
                        eq.setEstado(EstadoEquipo.valueOf(estadoStr.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        eq.setEstado(EstadoEquipo.DISPONIBLE);
                    }
                }

                eq.setStrMarca(rs.getString("Marca"));
                eq.setStrModelo(rs.getString("Modelo"));
                eq.setStrSerie(rs.getString("NumeroSerie"));
                lista.add(eq);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar equipos: " + e.getMessage());
        }
        return lista;
    }

    public Equipo buscarPorCodigo(String strCodigo) {
        String sql = "SELECT IdEquipo, CodigoInventario FROM Equipo WHERE CodigoInventario = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, strCodigo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Equipo eq = new Equipo();
                    eq.setIdEquipo(rs.getInt("IdEquipo"));
                    eq.setStrCodigo(rs.getString("CodigoInventario"));
                    return eq;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar equipo por código: " + e.getMessage());
        }
        return null;
    }

    public Equipo obtenerPorId(int idEquipo) {
        String sql = "SELECT IdEquipo, CodigoInventario, Nombre, Estado FROM Equipo WHERE IdEquipo = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idEquipo);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Equipo eq = new Equipo();
                    eq.setIdEquipo(rs.getInt("IdEquipo"));
                    eq.setStrCodigo(rs.getString("CodigoInventario"));
                    eq.setStrNombre(rs.getString("Nombre"));

                    String estadoStr = rs.getString("Estado");
                    if (estadoStr != null) {
                        try {
                            eq.setEstado(EstadoEquipo.valueOf(estadoStr.toUpperCase().trim()));
                        } catch (IllegalArgumentException e) {
                            eq.setEstado(EstadoEquipo.DISPONIBLE);
                        }
                    }

                    return eq;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener equipo por ID: " + e.getMessage());
        }

        return null;
    }

    public boolean guardar(Equipo eq) {
        String sql = "INSERT INTO Equipo (CodigoInventario, Nombre, IdTipoEquipo, IdLaboratorio, Estado, Marca, Modelo, NumeroSerie) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, eq.getStrCodigo());
            stmt.setString(2, eq.getStrNombre());
            stmt.setInt(3, eq.getIdTipoEquipo());
            stmt.setInt(4, eq.getIdLaboratorio());
            stmt.setString(5, eq.getEstado().name());
            stmt.setString(6, eq.getStrMarca());
            stmt.setString(7, eq.getStrModelo());
            stmt.setString(8, eq.getStrSerie());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al guardar equipo: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(Equipo eq) {
        String sql = "UPDATE Equipo SET CodigoInventario = ?, Nombre = ?, IdTipoEquipo = ?, IdLaboratorio = ?, Estado = ?, Marca = ?, Modelo = ?, NumeroSerie = ? WHERE IdEquipo = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, eq.getStrCodigo());
            stmt.setString(2, eq.getStrNombre());
            stmt.setInt(3, eq.getIdTipoEquipo());
            stmt.setInt(4, eq.getIdLaboratorio());
            stmt.setString(5, eq.getEstado().name());
            stmt.setString(6, eq.getStrMarca());
            stmt.setString(7, eq.getStrModelo());
            stmt.setString(8, eq.getStrSerie());
            stmt.setInt(9, eq.getIdEquipo());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar equipo: " + e.getMessage());
            return false;
        }
    }

    public Equipo buscarFichaPorCodigo(String strCodigo) {
        String sql = "SELECT e.IdEquipo, e.CodigoInventario, e.Nombre, t.Nombre AS TipoNombre, " +
                "l.Nombre AS LabNombre, e.Estado, e.Marca, e.Modelo, e.NumeroSerie " +
                "FROM Equipo e " +
                "INNER JOIN TipoEquipo t ON e.IdTipoEquipo = t.IdTipoEquipo " +
                "INNER JOIN Laboratorio l ON e.IdLaboratorio = l.IdLaboratorio " +
                "WHERE e.CodigoInventario = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, strCodigo);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Equipo eq = new Equipo();
                    eq.setIdEquipo(rs.getInt("IdEquipo"));
                    eq.setStrCodigo(rs.getString("CodigoInventario"));
                    eq.setStrNombre(rs.getString("Nombre"));
                    eq.setStrNombreTipo(rs.getString("TipoNombre"));
                    eq.setStrNombreLaboratorio(rs.getString("LabNombre"));
                    eq.setStrMarca(rs.getString("Marca"));
                    eq.setStrModelo(rs.getString("Modelo"));
                    eq.setStrSerie(rs.getString("NumeroSerie"));

                    try {
                        eq.setEstado(EstadoEquipo.valueOf(rs.getString("Estado").trim().toUpperCase()));
                    } catch (Exception e) {
                        eq.setEstado(null);
                    }
                    return eq;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar la ficha del equipo: " + e.getMessage());
        }
        return null;
    }
}