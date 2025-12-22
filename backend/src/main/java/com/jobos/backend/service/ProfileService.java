package com.jobos.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobos.backend.domain.user.*;
import com.jobos.backend.exception.ResourceNotFoundException;
import com.jobos.backend.repository.PosterProfileRepository;
import com.jobos.backend.repository.SeekerPreferencesRepository;
import com.jobos.backend.repository.UserRepository;
import com.jobos.shared.dto.profile.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final SeekerPreferencesRepository seekerPreferencesRepository;
    private final PosterProfileRepository posterProfileRepository;
    private final ObjectMapper objectMapper;

    public ProfileService(
            UserRepository userRepository,
            SeekerPreferencesRepository seekerPreferencesRepository,
            PosterProfileRepository posterProfileRepository,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.seekerPreferencesRepository = seekerPreferencesRepository;
        this.posterProfileRepository = posterProfileRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ProfileResponse response = new ProfileResponse();
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setBio(user.getBio());
        response.setLocation(user.getLocation());
        response.setTimezone(user.getTimezone());
        response.setProfileCompleted(user.getProfileCompleted());
        response.setCreatedAt(user.getCreatedAt());

        if (user.getRole() == UserRole.SEEKER) {
            seekerPreferencesRepository.findByUser(user).ifPresent(prefs -> {
                ProfileResponse.SeekerPreferencesData data = new ProfileResponse.SeekerPreferencesData();
                data.setDesiredRoles(parseJsonArray(prefs.getDesiredRoles()));
                data.setSkills(parseJsonArray(prefs.getSkills()));
                data.setJobTypes(parseJsonArray(prefs.getJobTypes()));
                data.setWorkingHours(prefs.getWorkingHours());
                data.setSalaryMin(prefs.getSalaryMin());
                data.setSalaryMax(prefs.getSalaryMax());
                data.setWillingToRelocate(prefs.getWillingToRelocate());
                data.setAvailableFrom(prefs.getAvailableFrom());
                response.setSeekerPreferences(data);
            });
        } else if (user.getRole() == UserRole.POSTER) {
            posterProfileRepository.findByUser(user).ifPresent(profile -> {
                ProfileResponse.PosterProfileData data = new ProfileResponse.PosterProfileData();
                data.setCompanyName(profile.getCompanyName());
                data.setCompanySize(profile.getCompanySize());
                data.setIndustry(profile.getIndustry());
                data.setWebsite(profile.getWebsite());
                data.setVerificationStatus(profile.getVerificationStatus());
                data.setVerificationDocuments(parseJsonArray(profile.getVerificationDocuments()));
                response.setPosterProfile(data);
            });
        }

        return response;
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getLocation() != null) {
            user.setLocation(request.getLocation());
        }
        if (request.getTimezone() != null) {
            user.setTimezone(request.getTimezone());
        }

        userRepository.save(user);
        return getProfile(userId);
    }

    @Transactional
    public ProfileResponse updatePreferences(UUID userId, Object preferencesRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == UserRole.SEEKER) {
            SeekerPreferencesRequest request = objectMapper.convertValue(preferencesRequest, SeekerPreferencesRequest.class);
            updateSeekerPreferences(user, request);
        } else if (user.getRole() == UserRole.POSTER) {
            PosterProfileRequest request = objectMapper.convertValue(preferencesRequest, PosterProfileRequest.class);
            updatePosterProfile(user, request);
        }

        if (!user.getProfileCompleted()) {
            user.setProfileCompleted(true);
            userRepository.save(user);
        }

        return getProfile(userId);
    }

    private void updateSeekerPreferences(User user, SeekerPreferencesRequest request) {
        SeekerPreferences prefs = seekerPreferencesRepository.findByUser(user)
                .orElseGet(() -> {
                    SeekerPreferences newPrefs = new SeekerPreferences();
                    newPrefs.setUser(user);
                    return newPrefs;
                });

        if (request.getDesiredRoles() != null) {
            prefs.setDesiredRoles(toJsonArray(request.getDesiredRoles()));
        }
        if (request.getSkills() != null) {
            prefs.setSkills(toJsonArray(request.getSkills()));
        }
        if (request.getJobTypes() != null) {
            prefs.setJobTypes(toJsonArray(request.getJobTypes()));
        }
        if (request.getWorkingHours() != null) {
            prefs.setWorkingHours(request.getWorkingHours());
        }
        if (request.getSalaryMin() != null) {
            prefs.setSalaryMin(request.getSalaryMin());
        }
        if (request.getSalaryMax() != null) {
            prefs.setSalaryMax(request.getSalaryMax());
        }
        if (request.getWillingToRelocate() != null) {
            prefs.setWillingToRelocate(request.getWillingToRelocate());
        }
        if (request.getAvailableFrom() != null) {
            prefs.setAvailableFrom(request.getAvailableFrom());
        }

        seekerPreferencesRepository.save(prefs);
    }

    private void updatePosterProfile(User user, PosterProfileRequest request) {
        PosterProfile profile = posterProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    PosterProfile newProfile = new PosterProfile();
                    newProfile.setUser(user);
                    return newProfile;
                });

        if (request.getCompanyName() != null) {
            profile.setCompanyName(request.getCompanyName());
        }
        if (request.getCompanySize() != null) {
            profile.setCompanySize(request.getCompanySize());
        }
        if (request.getIndustry() != null) {
            profile.setIndustry(request.getIndustry());
        }
        if (request.getWebsite() != null) {
            profile.setWebsite(request.getWebsite());
        }
        if (request.getVerificationDocuments() != null) {
            profile.setVerificationDocuments(toJsonArray(request.getVerificationDocuments()));
        }

        posterProfileRepository.save(profile);
    }

    private String toJsonArray(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
