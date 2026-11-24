package com.jbkloh.ficha_buffet.service;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Quem está usando o sistema, pelo token: o autor dos registros nunca vem do corpo da requisição. */
public final class Usuario {

    private Usuario() {}

    /** Login do usuário autenticado; "" fora de uma requisição autenticada. */
    public static String atual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a instanceof AnonymousAuthenticationToken || a.getName() == null) {
            return "";
        }
        String nome = a.getName();
        return nome.length() > 100 ? nome.substring(0, 100) : nome;
    }
}
