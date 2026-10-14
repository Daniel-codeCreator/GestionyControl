package Proyecto.universidad.model;

import java.time.LocalDateTime;

public class Auditoria {

    private Integer idAuditoria;
    private Integer idUsuario;
    private LocalDateTime fechaHora;
    private String accion;
    private String entidad;
    private Integer idRegistro;
    private String descripcion;

    public Auditoria() {
    }

    public Auditoria(Integer idUsuario, LocalDateTime fechaHora, String accion, String entidad, Integer idRegistro, String descripcion) {
        this.idUsuario = idUsuario;
        this.fechaHora = fechaHora;
        this.accion = accion;
        this.entidad = entidad;
        this.idRegistro = idRegistro;
        this.descripcion = descripcion;
    }

    // Getters y Setters
    public Integer getIdAuditoria() {
        return idAuditoria;
    }

    public void setIdAuditoria(Integer idAuditoria) {
        this.idAuditoria = idAuditoria;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public Integer getIdRegistro() {
        return idRegistro;
    }

    public void setIdRegistro(Integer idRegistro) {
        this.idRegistro = idRegistro;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}