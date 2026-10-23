package Proyecto.universidad.repository;

import Proyecto.universidad.Config.connection;
import Proyecto.universidad.model.Auditoria;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

public class AuditoriaRepository {

    public void registrarLogin(Auditoria auditoria) {
        String sql = "{call dbo.sp_RegistrarAuditoriaLogin(?, ?, ?)}";

        try (Connection conn = connection.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            if (auditoria.getIdUsuario() != null) {
                stmt.setInt(1, auditoria.getIdUsuario());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }

            stmt.setString(2, auditoria.getAccion());
            stmt.setString(3, auditoria.getDescripcion());

            stmt.execute();

        } catch (SQLException e) {
            System.err.println("Error al registrar auditoría de login en BD: " + e.getMessage());
        }
    }
}