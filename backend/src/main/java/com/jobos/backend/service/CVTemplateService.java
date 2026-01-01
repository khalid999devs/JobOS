package com.jobos.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobos.backend.domain.cv.CVTemplate;
import com.jobos.backend.domain.cv.TemplateCategory;
import com.jobos.backend.domain.cv.UserTemplateUnlock;
import com.jobos.backend.domain.user.User;
import com.jobos.backend.repository.CVTemplateRepository;
import com.jobos.backend.repository.UserRepository;
import com.jobos.backend.repository.UserTemplateUnlockRepository;
import com.jobos.shared.dto.cv.CVTemplateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CVTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(CVTemplateService.class);

    private final CVTemplateRepository cvTemplateRepository;
    private final UserTemplateUnlockRepository userTemplateUnlockRepository;
    private final UserRepository userRepository;
    private final CreditService creditService;
    private final ObjectMapper objectMapper;

    public CVTemplateService(CVTemplateRepository cvTemplateRepository,
                             UserTemplateUnlockRepository userTemplateUnlockRepository,
                             UserRepository userRepository,
                             CreditService creditService,
                             ObjectMapper objectMapper) {
        this.cvTemplateRepository = cvTemplateRepository;
        this.userTemplateUnlockRepository = userTemplateUnlockRepository;
        this.userRepository = userRepository;
        this.creditService = creditService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<CVTemplateResponse> getAllTemplates(UUID userId, String category) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<CVTemplate> templates;
        if (category != null && !category.isEmpty()) {
            try {
                TemplateCategory templateCategory = TemplateCategory.valueOf(category.toUpperCase());
                templates = cvTemplateRepository.findByIsActiveTrueAndCategory(templateCategory);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category");
            }
        } else {
            templates = cvTemplateRepository.findByIsActiveTrue();
        }

        List<UserTemplateUnlock> unlocks = userTemplateUnlockRepository.findByUser(user);
        Set<UUID> unlockedTemplateIds = unlocks.stream()
                .map(unlock -> unlock.getTemplate().getId())
                .collect(Collectors.toSet());

        return templates.stream()
                .map(template -> mapToTemplateResponse(template, unlockedTemplateIds.contains(template.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CVTemplateResponse getTemplateById(UUID templateId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        CVTemplate template = cvTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));

        boolean isUnlocked = !template.getIsPremium() ||
                userTemplateUnlockRepository.existsByUserAndTemplate(user, template);

        return mapToTemplateResponse(template, isUnlocked);
    }

    @Transactional
    public CVTemplateResponse unlockTemplate(UUID templateId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        CVTemplate template = cvTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));

        if (!template.getIsPremium()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template is already free");
        }

        if (userTemplateUnlockRepository.existsByUserAndTemplate(user, template)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template already unlocked");
        }

        // Deduct credits
        boolean deducted = creditService.deductCredits(user, template.getCreditCost(), 
                "Unlocked template: " + template.getName());
        
        if (!deducted) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient credits");
        }
        
        UserTemplateUnlock unlock = new UserTemplateUnlock();
        unlock.setUser(user);
        unlock.setTemplate(template);
        userTemplateUnlockRepository.save(unlock);

        return mapToTemplateResponse(template, true);
    }

    private CVTemplateResponse mapToTemplateResponse(CVTemplate template, boolean isUnlocked) {
        CVTemplateResponse response = new CVTemplateResponse();
        response.setId(template.getId().toString());
        response.setName(template.getName());
        response.setDescription(template.getDescription());
        response.setPreviewImageUrl(template.getPreviewImageUrl());
        response.setIsPremium(template.getIsPremium());
        response.setCreditCost(template.getCreditCost());
        response.setCategory(template.getCategory().name());
        response.setIsUnlocked(isUnlocked);
        response.setSectionsConfig(template.getSectionsConfig());
        response.setStyleConfig(template.getStyleConfig());
        response.setSectionCount(countSections(template.getSectionsConfig()));
        response.setCreatedAt(template.getCreatedAt());
        return response;
    }

    private Integer countSections(String sectionsConfig) {
        if (sectionsConfig == null || sectionsConfig.isEmpty()) {
            return 0;
        }
        try {
            List<Map<String, Object>> sections = objectMapper.readValue(
                sectionsConfig,
                new TypeReference<List<Map<String, Object>>>() {}
            );
            return sections.size();
        } catch (Exception e) {
            logger.warn("Failed to parse sectionsConfig: {}", e.getMessage());
            return 0;
        }
    }
}
