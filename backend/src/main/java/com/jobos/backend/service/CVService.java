package com.jobos.backend.service;

import com.jobos.backend.domain.cv.*;
import com.jobos.backend.domain.user.User;
import com.jobos.backend.repository.*;
import com.jobos.shared.dto.cv.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CVService {

    private final CVRepository cvRepository;
    private final CVSectionRepository cvSectionRepository;
    private final CVTemplateRepository cvTemplateRepository;
    private final UserRepository userRepository;
    private static final int MAX_CVS_PER_USER = 5;
    private static final int MAX_SECTIONS_PER_CV = 15;

    public CVService(CVRepository cvRepository, CVSectionRepository cvSectionRepository,
                     CVTemplateRepository cvTemplateRepository, UserRepository userRepository) {
        this.cvRepository = cvRepository;
        this.cvSectionRepository = cvSectionRepository;
        this.cvTemplateRepository = cvTemplateRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CVResponse createCV(UUID userId, CVCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        long cvCount = cvRepository.countByUser(user);
        if (cvCount >= MAX_CVS_PER_USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Maximum CV limit reached (" + MAX_CVS_PER_USER + " CVs per user)");
        }

        CV cv = new CV();
        cv.setUser(user);
        cv.setTitle(request.getTitle());

        if (request.getTemplateId() != null && !request.getTemplateId().isEmpty()) {
            UUID templateId = UUID.fromString(request.getTemplateId());
            CVTemplate template = cvTemplateRepository.findById(templateId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
            cv.setTemplate(template);
        }

        if (request.getVisibility() != null && !request.getVisibility().isEmpty()) {
            try {
                cv.setVisibility(CVVisibility.valueOf(request.getVisibility().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid visibility value");
            }
        }

        if (cvCount == 0) {
            cv.setIsDefault(true);
        }

        cv = cvRepository.save(cv);
        return mapToCVResponse(cv);
    }

    @Transactional(readOnly = true)
    public Page<CVListResponse> getMyCVs(UUID userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Page<CV> cvPage = cvRepository.findByUser(user, pageable);
        List<CVListResponse> responses = cvPage.getContent().stream()
                .map(this::mapToCVListResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, cvPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CVResponse getCVById(UUID cvId, UUID userId) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));

        if (!cv.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return mapToCVResponse(cv);
    }

    @Transactional
    public CVResponse updateCV(UUID cvId, UUID userId, CVUpdateRequest request) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));

        if (!cv.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (request.getTitle() != null && !request.getTitle().isEmpty()) {
            cv.setTitle(request.getTitle());
        }

        if (request.getTemplateId() != null && !request.getTemplateId().isEmpty()) {
            UUID templateId = UUID.fromString(request.getTemplateId());
            CVTemplate template = cvTemplateRepository.findById(templateId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
            cv.setTemplate(template);
        }

        if (request.getVisibility() != null && !request.getVisibility().isEmpty()) {
            try {
                cv.setVisibility(CVVisibility.valueOf(request.getVisibility().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid visibility value");
            }
        }

        cv = cvRepository.save(cv);
        return mapToCVResponse(cv);
    }

    @Transactional
    public void deleteCV(UUID cvId, UUID userId) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));

        if (!cv.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        cvRepository.delete(cv);
    }

    @Transactional
    public CVResponse setDefaultCV(UUID cvId, UUID userId) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));

        if (!cv.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        User user = cv.getUser();
        cvRepository.clearDefaultForUser(user);
        cv.setIsDefault(true);
        cv = cvRepository.save(cv);

        return mapToCVResponse(cv);
    }

    @Transactional
    public CVSectionResponse addSection(UUID cvId, UUID userId, CVSectionRequest request) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));

        if (!cv.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (cv.getSections().size() >= MAX_SECTIONS_PER_CV) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Maximum section limit reached (" + MAX_SECTIONS_PER_CV + " sections per CV)");
        }

        CVSection section = new CVSection();
        section.setCv(cv);

        try {
            section.setSectionType(CVSectionType.valueOf(request.getSectionType().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid section type");
        }

        section.setTitle(request.getTitle());
        section.setContent(request.getContent());
        section.setIsVisible(request.getIsVisible());
        section.setOrderIndex(cv.getSections().size());

        section = cvSectionRepository.save(section);
        return mapToCVSectionResponse(section);
    }

    @Transactional
    public CVSectionResponse updateSection(UUID cvId, UUID sectionId, UUID userId, CVSectionRequest request) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));

        if (!cv.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        CVSection section = cvSectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));

        if (!section.getCv().getId().equals(cvId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section does not belong to this CV");
        }

        if (request.getSectionType() != null && !request.getSectionType().isEmpty()) {
            try {
                section.setSectionType(CVSectionType.valueOf(request.getSectionType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid section type");
            }
        }

        if (request.getTitle() != null) {
            section.setTitle(request.getTitle());
        }

        if (request.getContent() != null && !request.getContent().isEmpty()) {
            section.setContent(request.getContent());
        }

        if (request.getIsVisible() != null) {
            section.setIsVisible(request.getIsVisible());
        }

        section = cvSectionRepository.save(section);
        return mapToCVSectionResponse(section);
    }

    @Transactional
    public void deleteSection(UUID cvId, UUID sectionId, UUID userId) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));

        if (!cv.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        CVSection section = cvSectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));

        if (!section.getCv().getId().equals(cvId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section does not belong to this CV");
        }

        cvSectionRepository.delete(section);
    }

    @Transactional
    public CVResponse reorderSections(UUID cvId, UUID userId, SectionReorderRequest request) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));

        if (!cv.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        List<String> sectionIds = request.getSectionIds();
        List<CVSection> sections = cv.getSections();

        if (sectionIds.size() != sections.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section count mismatch");
        }

        for (int i = 0; i < sectionIds.size(); i++) {
            UUID sectionId = UUID.fromString(sectionIds.get(i));
            CVSection section = sections.stream()
                    .filter(s -> s.getId().equals(sectionId))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Section not found: " + sectionId));
            section.setOrderIndex(i);
        }

        cv = cvRepository.save(cv);
        return mapToCVResponse(cv);
    }

    private CVResponse mapToCVResponse(CV cv) {
        CVResponse response = new CVResponse();
        response.setId(cv.getId().toString());
        response.setTitle(cv.getTitle());
        response.setIsDefault(cv.getIsDefault());
        response.setVisibility(cv.getVisibility().name());
        response.setCreatedAt(cv.getCreatedAt());
        response.setUpdatedAt(cv.getUpdatedAt());

        if (cv.getTemplate() != null) {
            response.setTemplateId(cv.getTemplate().getId().toString());
            response.setTemplateName(cv.getTemplate().getName());
        }

        List<CVSectionResponse> sectionResponses = cv.getSections().stream()
                .map(this::mapToCVSectionResponse)
                .collect(Collectors.toList());
        response.setSections(sectionResponses);

        return response;
    }

    private CVListResponse mapToCVListResponse(CV cv) {
        CVListResponse response = new CVListResponse();
        response.setId(cv.getId().toString());
        response.setTitle(cv.getTitle());
        response.setIsDefault(cv.getIsDefault());
        response.setVisibility(cv.getVisibility().name());
        response.setSectionCount(cv.getSections().size());
        response.setCreatedAt(cv.getCreatedAt());
        response.setUpdatedAt(cv.getUpdatedAt());

        if (cv.getTemplate() != null) {
            response.setTemplateName(cv.getTemplate().getName());
        }

        return response;
    }

    private CVSectionResponse mapToCVSectionResponse(CVSection section) {
        CVSectionResponse response = new CVSectionResponse();
        response.setId(section.getId().toString());
        response.setSectionType(section.getSectionType().name());
        response.setTitle(section.getTitle());
        response.setContent(section.getContent());
        response.setOrderIndex(section.getOrderIndex());
        response.setIsVisible(section.getIsVisible());
        response.setCreatedAt(section.getCreatedAt());
        return response;
    }
}
