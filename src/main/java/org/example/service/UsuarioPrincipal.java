package org.example.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Usuario autenticado con su id de BD. El JWT guarda ese id para que un token emitido a un
 * usuario borrado no sirva para otro que se registre después con el mismo username.
 */
public class UsuarioPrincipal extends User {

    private final Long id;

    public UsuarioPrincipal(Long id, String username, String password,
                            Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
