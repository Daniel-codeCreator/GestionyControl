package Proyecto.universidad.model;

public class Rol {

    private int intIdRol;
    private String strNombre;
    private String strDescripcion;
    private String strEstado;

    public Rol() {
    }

    public int getIntIdRol() {
        return intIdRol;
    }

    public void setIntIdRol(int intIdRol) {
        this.intIdRol = intIdRol;
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

    public String getStrEstado() {
        return strEstado;
    }

    public void setStrEstado(String strEstado) {
        this.strEstado = strEstado;
    }

    @Override
    public String toString() {
        return strNombre;
    }
}
