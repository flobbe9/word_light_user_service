package com.example.vorspiel_userservice.enums;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Enum defining a role an {@code AppUser} can have.
 * 
 * @since 0.0.1
 */
public enum AppUserRole {
    USER, 
    ADMIN;


    /**
     * Return {@code this.name()} with {@code "ROLE_"} appended for spring security.
     * 
     * @return
     */
    public SimpleGrantedAuthority getGrantedAuthority() {

        return new SimpleGrantedAuthority("ROLE_" + this.name());
    }
}