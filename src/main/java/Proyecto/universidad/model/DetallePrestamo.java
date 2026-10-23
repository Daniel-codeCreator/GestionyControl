package Proyecto.universidad.model;

public class DetallePrestamo {
    private Integer idDetallePrestamo;
    private Integer idPrestamo;
    private Integer idEquipo;
    private String estado; // 'PENDIENTE', 'DEVUELTO'

    // Atributos auxiliares para visualización
    private String nombreEquipo;
    private String codigoEquipo;

    public DetallePrestamo() {
        this.estado = "PENDIENTE";
    }

    public DetallePrestamo(Integer idEquipo, String nombreEquipo, String codigoEquipo) {
        this();
        this.idEquipo = idEquipo;
        this.nombreEquipo = nombreEquipo;
        this.codigoEquipo = codigoEquipo;
    }

    public Integer getIdDetallePrestamo() { return idDetallePrestamo; }
    public void setIdDetallePrestamo(Integer idDetallePrestamo) { this.idDetallePrestamo = idDetallePrestamo; }

    public Integer getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Integer idPrestamo) { this.idPrestamo = idPrestamo; }

    public Integer getIdEquipo() { return idEquipo; }
    public void setIdEquipo(Integer idEquipo) { this.idEquipo = idEquipo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getNombreEquipo() { return nombreEquipo; }
    public void setNombreEquipo(String nombreEquipo) { this.nombreEquipo = nombreEquipo; }

    public String getCodigoEquipo() { return codigoEquipo; }
    public void setCodigoEquipo(String codigoEquipo) { this.codigoEquipo = codigoEquipo; }
}