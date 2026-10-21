package Proyecto.universidad.model;

public class TipoEquipo {

    private Integer idTipoEquipo;
    private String strNombre;
    private String strDescripcion;

    public TipoEquipo() {
    }

    public TipoEquipo(Integer idTipoEquipo, String strNombre, String strDescripcion) {
        this.idTipoEquipo = idTipoEquipo;
        this.strNombre = strNombre;
        this.strDescripcion = strDescripcion;
    }

    public Integer getIdTipoEquipo() {
        return idTipoEquipo;
    }

    public void setIdTipoEquipo(Integer idTipoEquipo) {
        this.idTipoEquipo = idTipoEquipo;
    }

    public String getStrNombre() {
        return strNombre;
    }

    public void setStrNombre(String strNombre) {
        this.strNombre = strNombre;
    }

    public String getStrDescripcion() {
        return strDescripcion;
    }

    public void setStrDescripcion(String strDescripcion) {
        this.strDescripcion = strDescripcion;
    }

    @Override
    public String toString() {
        return strNombre;
    }
}