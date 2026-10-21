package Proyecto.universidad.integration.facial;

/** Error controlado al comunicarse con el servicio facial local. */
public class FacialClientException extends RuntimeException {

    private final int statusCode;

    public FacialClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public FacialClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
