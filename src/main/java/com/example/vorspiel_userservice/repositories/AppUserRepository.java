package com.example.vorspiel_userservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.example.vorspiel_userservice.entites.AppUser;


@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<UserDetails> findByEmail(String email);

    boolean existsByEmail(String email);
}