package com.example.vorspiel_userservice.entites;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.vorspiel_userservice.abstractClasses.AbstractEntity;
import com.example.vorspiel_userservice.enums.AppUserRole;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppUser extends AbstractEntity implements UserDetails {

    @Pattern(regexp ="^[\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$", message = "'email' pattern invalid")
    @NotNull(message = "'email' cannot be null")
    private String email;

    // min length
    // max length
    // pattern
    // not null
    private String password;

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


    public Collection<? extends GrantedAuthority> getAuthorities() {

        return List.of(role.getGrantedAuthority());
    }

    
    public String getPassword() {

        return this.password;
    }

    
    public String getUsername() {

        return this.email;
    }

    
    public boolean isAccountNonExpired() {

        return this.isAccountNonExpired;
    }

    
    public boolean isAccountNonLocked() {

        return this.isAccountNonLocked;
    }

    
    public boolean isCredentialsNonExpired() {

        return this.isCredentialsNonExpired;
    }

    
    public boolean isEnabled() {

        return this.isEnabled;
    }
}