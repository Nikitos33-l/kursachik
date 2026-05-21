package org.example.notification.service.exception;

public class EmailSenderException extends RuntimeException {
    public EmailSenderException(String message) {
        super(message);
    }
}
