package com.example.vorspiel_userservice.abstractClasses;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.example.vorspiel_userservice.exception.ApiException;
import com.example.vorspiel_userservice.repositories.Dao;

import jakarta.validation.constraints.NotNull;


/**
 * Abstract class defining minimum fields all entites must have.
 * 
 * @since 0.0.1
 */
@Service
@Validated
public abstract class AbstractService<E extends AbstractEntity, Repository extends Dao<E>> {
    
    @Autowired
    private Repository repository;


    /**
     * Save abstract entity as new entity or update existing one.
     * 
     * @param entity to save
     * @return saved abstract entity
     */
    public E save(@NotNull(message = "'entity' cannot be null") @Validated E entity) {

        Optional<E> oldAppUser = this.repository.findById(entity.getId());
        
        // case: entity does not exist yet
        if (oldAppUser.isEmpty())
            entity.setCreated(LocalDateTime.now());

        entity.setUpdated(LocalDateTime.now());

        return this.repository.save(entity);
    }


    public E getById(@NotNull(message = "Failed to find entity. 'id' cannot be null.") Long id) {

        return this.repository.findById(id)
                              .orElseThrow(() -> new ApiException("Failed to find entity with id: " + id + "."));
    }
}