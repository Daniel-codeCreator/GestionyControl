package Proyecto.universidad.enums;

/**
 * Rol del usuario tal como se registran en la base de datos.
 * Los IdRol corresponden al orden de los INSERT de la tabla Rol
 * en Config/tablas.sql (ADMIN=1, ENCARGADO=2, DOCENTE=3).
 */
public enum RolUsuario {

    ADMIN(1),
    ENCARGADO(2),
    DOCENTE(3);

    private final int intIdRol;

    RolUsuario(int intIdRol) {
        this.intIdRol = intIdRol;
    }

    public int getIdRol() {
        return intIdRol;
    }

    /**
     * Devuelve el rol asociado al nombre exacto usado en la base de datos.
     */
    public static RolUsuario desdeNombre(String strNombreRol) {
        if (strNombreRol == null) {
            return null;
        }
        return RolUsuario.valueOf(strNombreRol.trim().toUpperCase());
    }
}
