package Proyecto.universidad.exception;

/** Se lanza cuando el rol del usuario con sesión no permite la operación. */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}