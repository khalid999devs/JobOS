package com.jobos.backend.controller;

import com.jobos.backend.security.AuthenticatedUser;
import com.jobos.backend.service.ProfileService;
import com.jobos.shared.dto.profile.ProfileResponse;
import com.jobos.shared.dto.profile.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        ProfileResponse profile = profileService.getProfile(user.getUserId());
        return ResponseEntity.ok(profile);
    }

    @PatchMapping
    public ResponseEntity<ProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        ProfileResponse profile = profileService.updateProfile(user.getUserId(), request);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/preferences")
    public ResponseEntity<ProfileResponse> updatePreferences(
            Authentication authentication,
            @RequestBody Map<String, Object> preferences
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        ProfileResponse profile = profileService.updatePreferences(user.getUserId(), preferences);
        return ResponseEntity.ok(profile);
    }
}
