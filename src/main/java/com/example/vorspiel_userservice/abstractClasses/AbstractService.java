package com.example.vorspiel_userservice.abstractClasses;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.vorspiel_userservice.repositories.Dao;

import jakarta.validation.constraints.NotNull;


@Service
public abstract class AbstractService<E extends AbstractEntity, Repository extends Dao<E>> {
    
    @Autowired
    private Repository repository;


    /**
     * Save abstract entity as new entity or update existing one.
     * 
     * @param entity to save
     * @return saved abstract entity
     */
    public E save(@NotNull(message = "'entity' cannot be null") E entity) {

        Optional<E> oldAppUser = this.repository.findById(entity.getId());
        
        // case: entity does not exist yet
        if (oldAppUser.isEmpty())
            entity.setCreated(LocalDateTime.now());

        entity.setUpdated(LocalDateTime.now());

        return this.repository.save(entity);
    }


    // TODO: implement getById(), getByCreated(), getByUpdated() (?)
}