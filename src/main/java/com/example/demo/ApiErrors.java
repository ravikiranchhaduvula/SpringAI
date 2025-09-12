package com.example.demo;

import org.apache.coyote.BadRequestException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@ControllerAdvice
class ApiErrors {
    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<Map<String,Object>> badReq(BadRequestException ex){
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String,Object>> boom(Exception ex){
        return ResponseEntity.status(500).body(Map.of("error","internal_error","detail", ex.getClass().getSimpleName()));
    }
}
