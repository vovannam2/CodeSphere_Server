package com.hcmute.codesphere_server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ResourceConflictException extends RuntimeException {
    public ResourceConflictException() { super(); }
    public ResourceConflictException(String message) { super(message); }
    public ResourceConflictException(String message, Throwable cause) { super(message, cause); }
}