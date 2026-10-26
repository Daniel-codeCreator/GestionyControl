package Proyecto.universidad.model;

public record Dashboard(
        int totalEquipos,
        int disponibles,
        int prestados,
        int danados,
        int enMantenimiento,
        int deBaja,
        int prestamosActivos,
        int prestamosVencidos
) {
}