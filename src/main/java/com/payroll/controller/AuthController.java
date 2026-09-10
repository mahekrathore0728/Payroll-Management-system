package com.payroll.controller;

import com.payroll.dto.ApiResponse;
import com.payroll.dto.LoginRequest;
import com.payroll.dto.UserProfileDto;
import com.payroll.security.CustomUserDetails;
import com.payroll.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest request,
                                                                   HttpServletRequest httpRequest) {
        String loginIdentifier = request.getUsername() != null ? request.getUsername().trim() : "";
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginIdentifier, request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UserProfileDto profile = userService.getProfileByUsername(userDetails.getUsername());

        String redirectUrl = switch (userDetails.getUser().getRole()) {
            case "ROLE_ADMIN", "ROLE_HR" -> "/app.html#dashboard";
            case "ROLE_MANAGER" -> "/app.html#manager-dashboard";
            case "ROLE_EMPLOYEE" -> "/app.html#employee-dashboard";
            default -> "/app.html";
        };

        Map<String, Object> data = new HashMap<>();
        data.put("username", userDetails.getUsername());
        data.put("role", userDetails.getUser().getRole());
        data.put("redirectUrl", redirectUrl);
        data.put("profile", profile);

        return ResponseEntity.ok(ApiResponse.ok("Login successful", data));
    }

    @GetMapping("/current-user")
    public ResponseEntity<ApiResponse<UserProfileDto>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }

        UserProfileDto profile = userService.getProfileByUsername(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @GetMapping("/setup-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSetupStatus() {
        boolean setupRequired = userService.isSetupRequired();
        Map<String, Object> data = new HashMap<>();
        data.put("setupRequired", setupRequired);
        data.put("userCount", userService.countUsers());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/setup-admin")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setupAdmin(@Valid @RequestBody com.payroll.dto.AdminSetupRequest request,
                                                                       HttpServletRequest httpRequest) {
        UserProfileDto admin = userService.setupInitialAdmin(request);

        // Authenticate the newly created administrator
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());

        Map<String, Object> data = new HashMap<>();
        data.put("username", admin.getUsername());
        data.put("role", "ROLE_ADMIN");
        data.put("redirectUrl", "/app.html#dashboard");
        data.put("profile", admin);

        return ResponseEntity.ok(ApiResponse.ok("Administrator account created successfully.", data));
    }
}
