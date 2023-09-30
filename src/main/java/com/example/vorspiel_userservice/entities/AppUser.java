package com.example.vorspiel_userservice.entities;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.vorspiel_userservice.abstractClasses.AbstractEntity;
import com.example.vorspiel_userservice.enums.AppUserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * Class defining the User entity for this api. Implements {@link UserDetails} for Spring Security.
 * 
 * @since 0.0.1
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class AppUser extends AbstractEntity implements UserDetails {

    @Pattern(regexp = "^[\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$", message = "'email' pattern invalid")
    @NotNull(message = "'email' cannot be null")
    @Schema(example = "max.mustermann@domain.com")
    @EqualsAndHashCode.Include
    private String email;

    @NotNull(message = "'password' cannot be null")
    @Schema(example = "Abc123,.")
    private String password;

    @NotNull(message = "'role' cannot be null")
    @Enumerated(EnumType.STRING)
    private AppUserRole role;

        private boolean isAccountNonExpired;

        private boolean isAccountNonLocked;

        private boolean isCredentialsNonExpired;

        private boolean isEnabled;


    /**
     * Used for initial creation. Security boolean fields will all be set to {@code true} except for {@code enabled}.
     * 
     * @param email
     * @param password
     * @param role
     */
    public AppUser(String email, String password, AppUserRole role) {
        
        this.email = email;
        this.password = password;
        this.role = role;
        this.isAccountNonExpired = true;
        this.isAccountNonLocked = true;
        this.isCredentialsNonExpired = true;
        this.isEnabled = false;
    }


    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return List.of(role.getGrantedAuthority());
    }

    
    @Override
    public String getPassword() {

        return this.password;
    }

    
    @Override
    @JsonIgnore
    public String getUsername() {

        return this.email;
    }

    
    @Override
    public boolean isAccountNonExpired() {

        return this.isAccountNonExpired;
    }

    
    @Override
    public boolean isAccountNonLocked() {

        return this.isAccountNonLocked;
    }

    
    @Override
    public boolean isCredentialsNonExpired() {

        return this.isCredentialsNonExpired;
    }

    
    @Override
    public boolean isEnabled() {

        return this.isEnabled;
    }


    @Override
    public String toString() {

        return this.email;
    }
}