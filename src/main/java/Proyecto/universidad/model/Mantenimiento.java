package Proyecto.universidad.model;

import java.sql.Timestamp;

public class Mantenimiento {

    private Integer idMantenimiento;
    private Integer idEquipo;
    private Timestamp fechaIngreso;
    private String tipoMantenimiento; // 'PREVENTIVO' o 'CORRECTIVO'
    private String descripcion;
    private String responsable;
    private Timestamp fechaSalida;
    private String resultado; // 'REPARADO' o 'NO_REPARABLE'
    private String observaciones;

    // Campos auxiliares para presentar en vistas
    private String codigoEquipo;
    private String nombreEquipo;

    public Mantenimiento() {
    }

    public Integer getIdMantenimiento() { return idMantenimiento; }
    public void setIdMantenimiento(Integer idMantenimiento) { this.idMantenimiento = idMantenimiento; }

    public Integer getIdEquipo() { return idEquipo; }
    public void setIdEquipo(Integer idEquipo) { this.idEquipo = idEquipo; }

    public Timestamp getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(Timestamp fechaIngreso) { this.fechaIngreso = fechaIngreso; }

    public String getTipoMantenimiento() { return tipoMantenimiento; }
    public void setTipoMantenimiento(String tipoMantenimiento) { this.tipoMantenimiento = tipoMantenimiento; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }

    public Timestamp getFechaSalida() { return fechaSalida; }
    public void setFechaSalida(Timestamp fechaSalida) { this.fechaSalida = fechaSalida; }

    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getCodigoEquipo() { return codigoEquipo; }
    public void setCodigoEquipo(String codigoEquipo) { this.codigoEquipo = codigoEquipo; }

    public String getNombreEquipo() { return nombreEquipo; }
    public void setNombreEquipo(String nombreEquipo) { this.nombreEquipo = nombreEquipo; }
}