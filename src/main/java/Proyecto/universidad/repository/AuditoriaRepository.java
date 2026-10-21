package Proyecto.universidad.repository;

import Proyecto.universidad.Config.connection;
import Proyecto.universidad.model.Auditoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class AuditoriaRepository {

    public void registrar(Auditoria auditoria) {
        // Ajustado a los nombres exactos de columnas de SQL Server en CamelCase
        String sql = "INSERT INTO Auditoria (IdUsuario, FechaHora, Accion, Entidad, IdRegistro, Descripcion) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (auditoria.getIdUsuario() != null) {
                stmt.setInt(1, auditoria.getIdUsuario());
            } else {
                stmt.setNull(1, java.sql.Types.INTEGER);
            }

            stmt.setTimestamp(2, Timestamp.valueOf(auditoria.getFechaHora()));
            stmt.setString(3, auditoria.getAccion());
            stmt.setString(4, auditoria.getEntidad());

            if (auditoria.getIdRegistro() != null) {
                stmt.setInt(5, auditoria.getIdRegistro());
            } else {
                stmt.setNull(5, java.sql.Types.INTEGER);
            }

            stmt.setString(6, auditoria.getDescripcion());

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error al registrar auditoría en BD: " + e.getMessage());
        }
    }
}