package Proyecto.universidad.integration.facial;

public record FacialStatus(
        int userId,
        boolean enrolled,
        String registeredAt,
        String updatedAt,
        String model,
        int dimension,
        int samples
) {
}
