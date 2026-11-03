package cl.vidasalud.appointments.model;

/** SOLICITADA → CONFIRMADA → EN_ESPERA → EN_ATENCION → CERRADA / CANCELADA. */
public enum EstadoAtencion {
    SOLICITADA, CONFIRMADA, EN_ESPERA, EN_ATENCION, CERRADA, CANCELADA;

    /** Regla clave: no se puede pasar a EN_ATENCION sin haber confirmado (y pasado por EN_ESPERA). */
    public boolean puedePasarA(EstadoAtencion siguiente) {
        return switch (this) {
            case SOLICITADA -> siguiente == CONFIRMADA || siguiente == CANCELADA;
            case CONFIRMADA -> siguiente == EN_ESPERA || siguiente == CANCELADA;
            case EN_ESPERA -> siguiente == EN_ATENCION || siguiente == CANCELADA;
            case EN_ATENCION -> siguiente == CERRADA;
            case CERRADA, CANCELADA -> false;
        };
    }
}
