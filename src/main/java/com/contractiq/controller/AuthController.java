package com.contractiq.controller;

import com.contractiq.domain.party.User;
import com.contractiq.domain.party.UserRepository;
import com.contractiq.dto.request.LoginRequest;
import com.contractiq.dto.request.RegisterRequest;
import com.contractiq.dto.response.AuthResponse;
import com.contractiq.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request)
    {
         if(userRepository.existsByEmail(request.getEmail())){
             return ResponseEntity.badRequest().
                     body(AuthResponse.builder().message("Email already exist").build());
         }

         User user=User.builder()
                 .fullName(request.getFullName())
                 .email(request.getEmail())
                 .passwordHash(passwordEncoder.encode(request.getPassword()))
                 .role(request.getRole())
                 .isActive(true)
                 .build();

         userRepository.save(user);

         String token=jwtUtil.generateToken(user.getEmail(), user.getRole().name());

         return ResponseEntity.ok(AuthResponse.builder().token(token).email(user.getEmail()).role(user.getRole().name()).message("user registered successfully").build());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request)
    {
        try{
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            User user=userRepository.findByEmail(request.getEmail()).orElseThrow();
            String token=jwtUtil.generateToken(user.getEmail(), user.getRole().name());

            return ResponseEntity.ok(AuthResponse.builder().token(token).email(user.getEmail()).role(user.getRole().name()).message("user successfully logged in").build());
        }
        catch (AuthenticationException e){
            return ResponseEntity.status(401).body(AuthResponse.builder().message("Invalid credentials").build());
        }
    }

}
