package de.word_light.user_service.abstracts;

import java.io.File;


/**
 * Functional interface defining a boolean function to determine if the file in the param should be deleted or not.
 * 
 * @since 0.0.1
 * @see de.word_light.user_service.utils.Utils
 */
@FunctionalInterface
public interface FileDeletionCondition {
    
    boolean shouldFileBeDeleted(File file);
}
