package com.bizengine.businessuser.controller;

import com.bizengine.businessuser.dto.CreateBusinessUserRequest;
import com.bizengine.businessuser.dto.BusinessUserLoginRequest;
import com.bizengine.businessuser.model.BusinessUser;
import com.bizengine.businessuser.service.BusinessUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/business-users")
public class BusinessUserController {

    private final BusinessUserService service;

    public BusinessUserController(BusinessUserService service) {
        this.service = service;
    }

    @PostMapping("/provision")
    public ResponseEntity<BusinessUser> provisionUser(@RequestBody CreateBusinessUserRequest request) {
        BusinessUser savedUser = service.processOrCreateLocalUser(request, request.getPassword());
        return ResponseEntity.ok(savedUser);
    }

    @PostMapping("/login")
    public ResponseEntity<java.util.Map<String, String>> login(@RequestBody BusinessUserLoginRequest request) {
        String token = service.authenticateAndGenerateToken(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(java.util.Map.of("access_token", token, "token_type", "Bearer"));
    }

    @GetMapping
    public ResponseEntity<List<BusinessUser>> getAllUsers() {
        return ResponseEntity.ok(service.findAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessUser> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findUserById(id));
    }
}
