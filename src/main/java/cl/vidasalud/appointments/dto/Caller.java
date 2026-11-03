package cl.vidasalud.appointments.dto;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Identidad propagada por el BFF (ya validada contra el JWT de Entra ID). */
public record Caller(String email, Set<String> roles) {
    public static Caller of(String email, String rolesHeader) {
        Set<String> roles = rolesHeader == null ? Set.of()
                : Arrays.stream(rolesHeader.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        return new Caller(email, roles);
    }

    public boolean isStaff() {
        return roles.contains("Recepcionista") || roles.contains("Admin");
    }
}
