package Proyecto.universidad.model;

public class Laboratorio {

    private Integer idLaboratorio;
    private String strCodigo;
    private String strNombre;
    private String strUbicacion;
    private Integer intCapacidad;
    private String strEstado; // "ACTIVO", "INACTIVO"

    public Laboratorio() {
    }

    public Laboratorio(Integer idLaboratorio, String strCodigo, String strNombre, String strUbicacion, Integer intCapacidad, String strEstado) {
        this.idLaboratorio = idLaboratorio;
        this.strCodigo = strCodigo;
        this.strNombre = strNombre;
        this.strUbicacion = strUbicacion;
        this.intCapacidad = intCapacidad;
        this.strEstado = strEstado;
    }

    public Integer getIdLaboratorio() {
        return idLaboratorio;
    }

    public void setIdLaboratorio(Integer idLaboratorio) {
        this.idLaboratorio = idLaboratorio;
    }

    public String getStrCodigo() {
        return strCodigo;
    }

    public void setStrCodigo(String strCodigo) {
        this.strCodigo = strCodigo;
    }

    public String getStrNombre() {
        return strNombre;
    }

    public void setStrNombre(String strNombre) {
        this.strNombre = strNombre;
    }

    public String getStrUbicacion() {
        return strUbicacion;
    }

    public void setStrUbicacion(String strUbicacion) {
        this.strUbicacion = strUbicacion;
    }

    public Integer getIntCapacidad() {
        return intCapacidad;
    }

    public void setIntCapacidad(Integer intCapacidad) {
        this.intCapacidad = intCapacidad;
    }

    public String getStrEstado() {
        return strEstado;
    }

    public void setStrEstado(String strEstado) {
        this.strEstado = strEstado;
    }

    @Override
    public String toString() {
        return strNombre + " (" + strCodigo + ")";
    }
}