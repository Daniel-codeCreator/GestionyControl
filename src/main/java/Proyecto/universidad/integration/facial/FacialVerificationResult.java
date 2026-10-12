package Proyecto.universidad.integration.facial;

public record FacialVerificationResult(
        boolean success,
        boolean matched,
        int userId,
        Double score,
        double threshold,
        boolean livenessPassed,
        int framesEvaluated,
        String message
) {
}
