package Proyecto.universidad.model;

import java.time.LocalDateTime;

public class Usuario {

    private int idUsuario;
    private String strNombre;
    private String strApellido;
    private String strUsuario;
    private String strPassword;
    private String strCorreo;
    private int intRol;
    private String strEstado;
    private LocalDateTime fechaCreacion;

    public Usuario() {

    }

    public Usuario(int idUsuario, String strNombre, String strApellido,
                   String strUsuario, String strPassword, String strCorreo,
                   int intRol, String strEstado,
                   LocalDateTime fechaCreacion) {

        this.idUsuario = idUsuario;
        this.strNombre = strNombre;
        this.strApellido = strApellido;
        this.strUsuario = strUsuario;
        this.strPassword = strPassword;
        this.strCorreo = strCorreo;
        this.intRol = intRol;
        this.strEstado = strEstado;
        this.fechaCreacion = fechaCreacion;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getStrNombre() {
        return strNombre;
    }

    public void setStrNombre(String strNombre) {
        this.strNombre = strNombre;
    }

    public String getStrApellido() {
        return strApellido;
    }

    public void setStrApellido(String strApellido) {
        this.strApellido = strApellido;
    }

    public String getStrUsuario() {
        return strUsuario;
    }

    public void setStrUsuario(String strUsuario) {
        this.strUsuario = strUsuario;
    }

    public String getStrPassword() {
        return strPassword;
    }

    public void setStrPassword(String strPassword) {
        this.strPassword = strPassword;
    }

    public String getStrCorreo() {
        return strCorreo;
    }

    public void setStrCorreo(String strCorreo) {
        this.strCorreo = strCorreo;
    }

    public int getIntRol() {
        return intRol;
    }

    public void setIntRol(int intRol) {
        this.intRol = intRol;
    }

    public String getStrEstado() {
        return strEstado;
    }

    public void setStrEstado(String strEstado) {
        this.strEstado = strEstado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}