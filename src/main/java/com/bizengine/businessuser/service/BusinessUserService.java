package com.bizengine.businessuser.service;

import com.bizengine.businessuser.dto.CreateBusinessUserRequest;
import com.bizengine.businessuser.model.BusinessUser;
import com.bizengine.businessuser.repository.BusinessUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BusinessUserService {

    private final BusinessUserRepository repository;

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public BusinessUserService(BusinessUserRepository repository,
                               PasswordEncoder passwordEncoder,
                               JwtTokenProvider tokenProvider) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    // Update provision logic to encode incoming text passwords
    @Transactional
    public BusinessUser processOrCreateLocalUser(CreateBusinessUserRequest request, String plainPassword) {
        return repository.findByExternalUserId(request.getExternalUserId())
                .orElseGet(() -> {
                    BusinessUser localUser = new BusinessUser();
                    localUser.setExternalUserId(request.getExternalUserId());
                    localUser.setEmail(request.getEmail());
                    localUser.setCompanyName(request.getCompanyName());
                    localUser.setPassword(passwordEncoder.encode(plainPassword)); // Secure hashing
                    return repository.save(localUser);
                });
    }

    public String authenticateAndGenerateToken(String email, String plainPassword) {
        BusinessUser user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(plainPassword, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return tokenProvider.generateToken(user.getEmail(), user.getExternalUserId());
    }

    public List<BusinessUser> findAllUsers() {
        return repository.findAll();
    }

    public BusinessUser findUserById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }
}
