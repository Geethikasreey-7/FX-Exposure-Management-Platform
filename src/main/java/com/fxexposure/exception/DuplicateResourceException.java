package com.fxexposure.exception;

/**
 * Exception thrown when attempting to create or update an entity with duplicate unique attributes (e.g., username, email).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
