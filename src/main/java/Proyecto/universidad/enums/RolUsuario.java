package Proyecto.universidad.enums;

public enum RolUsuario {
    ADMIN(1),
    ENCARGADO(2),
    DOCENTE(3);

    private final int id;

    RolUsuario(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static RolUsuario desdeId(int id) {
        for (RolUsuario rol : values()) {
            if (rol.id == id) {
                return rol;
            }
        }
        return null;
    }
}