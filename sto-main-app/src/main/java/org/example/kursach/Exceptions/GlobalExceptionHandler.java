package org.example.kursach.Exceptions;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

   @ExceptionHandler(exception = {
           BadCredentialsException.class,
           EntityNotFoundException.class,
           AddressNotFoundException.class
   })
    public ResponseEntity<String> handleBadRequest(Exception e){
       return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
   }

   @ExceptionHandler(MethodArgumentNotValidException.class)
   public ResponseEntity<String> handleNotValid(MethodArgumentNotValidException e){
       return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
   }

   @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException e){
       return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
   }

   @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e){
       return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
   }

}
