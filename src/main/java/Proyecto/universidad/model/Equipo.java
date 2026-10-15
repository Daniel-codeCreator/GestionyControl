package Proyecto.universidad.model;

import Proyecto.universidad.enums.EstadoEquipo;

public class Equipo {

    private Integer idEquipo;
    private String strCodigo;
    private String strNombre;
    private Integer idTipoEquipo;
    private Integer idLaboratorio;
    private EstadoEquipo estado;
    private String strMarca;
    private String strModelo;
    private String strSerie;

    // Campos auxiliares para mostrar en tablas/combos
    private String strNombreTipo;
    private String strNombreLaboratorio;

    public Equipo() {
        this.estado = EstadoEquipo.DISPONIBLE;
    }

    public Integer getIdEquipo() {
        return idEquipo;
    }

    public void setIdEquipo(Integer idEquipo) {
        this.idEquipo = idEquipo;
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

    public Integer getIdTipoEquipo() {
        return idTipoEquipo;
    }

    public void setIdTipoEquipo(Integer idTipoEquipo) {
        this.idTipoEquipo = idTipoEquipo;
    }

    public Integer getIdLaboratorio() {
        return idLaboratorio;
    }

    public void setIdLaboratorio(Integer idLaboratorio) {
        this.idLaboratorio = idLaboratorio;
    }

    public EstadoEquipo getEstado() {
        return estado;
    }

    public void setEstado(EstadoEquipo estado) {
        this.estado = estado;
    }

    public String getStrMarca() {
        return strMarca;
    }

    public void setStrMarca(String strMarca) {
        this.strMarca = strMarca;
    }

    public String getStrModelo() {
        return strModelo;
    }

    public void setStrModelo(String strModelo) {
        this.strModelo = strModelo;
    }

    public String getStrSerie() {
        return strSerie;
    }

    public void setStrSerie(String strSerie) {
        this.strSerie = strSerie;
    }

    public String getStrNombreTipo() {
        return strNombreTipo;
    }

    public void setStrNombreTipo(String strNombreTipo) {
        this.strNombreTipo = strNombreTipo;
    }

    public String getStrNombreLaboratorio() {
        return strNombreLaboratorio;
    }

    public void setStrNombreLaboratorio(String strNombreLaboratorio) {
        this.strNombreLaboratorio = strNombreLaboratorio;
    }
}