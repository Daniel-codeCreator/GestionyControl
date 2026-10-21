package Proyecto.universidad.integration.facial;

public record FacialEnrollmentResult(
        boolean success,
        int userId,
        String model,
        int dimension,
        int samples,
        Double qualityScore,
        boolean livenessPassed,
        String message
) {
}
