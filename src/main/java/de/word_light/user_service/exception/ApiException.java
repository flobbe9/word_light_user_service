package de.word_light.user_service.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;
import lombok.Setter;


/**
 * Custom exception adding the http status code.
 * 
 * @since 0.0.1
 */
@Getter
@Setter
public class ApiException extends RuntimeException {
    
    private HttpStatus status;

    private String reasonPhrase;

    private String path;

    /** Exception that was actually caught in the first place */
    private Exception originalException;


    public ApiException(HttpStatus status, String message, Exception originalException) {

        super(message);
        this.status = status;
        this.originalException = originalException;
    }


    public ApiException(String message, Exception originalException) {

        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
        this.originalException = originalException;
    }


    public ApiException(HttpStatus status, String message) {
        
        super(message);
        this.status = status;
    }


    public ApiException(String message) {

        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }
}