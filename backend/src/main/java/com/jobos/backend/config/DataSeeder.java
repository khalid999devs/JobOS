package com.jobos.backend.config;

import com.jobos.backend.domain.credit.PlanType;
import com.jobos.backend.domain.credit.SubscriptionPlan;
import com.jobos.backend.domain.cv.CVTemplate;
import com.jobos.backend.domain.cv.TemplateCategory;
import com.jobos.backend.repository.CVTemplateRepository;
import com.jobos.backend.repository.SubscriptionPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private static final String CLASSIC_PROFESSIONAL_SECTIONS = """
        [
            {"sectionType": "PERSONAL_INFO", "title": "Personal Information", "orderIndex": 0, "isVisible": true, "defaultContent": {"fields": ["fullName", "email", "phone", "address", "linkedIn", "portfolio"]}},
            {"sectionType": "SUMMARY", "title": "Professional Summary", "orderIndex": 1, "isVisible": true, "defaultContent": {"placeholder": "A results-driven professional with X years of experience in..."}},
            {"sectionType": "EXPERIENCE", "title": "Work Experience", "orderIndex": 2, "isVisible": true, "defaultContent": {"fields": ["jobTitle", "company", "location", "startDate", "endDate", "current", "responsibilities", "achievements"]}},
            {"sectionType": "EDUCATION", "title": "Education", "orderIndex": 3, "isVisible": true, "defaultContent": {"fields": ["degree", "institution", "location", "graduationDate", "gpa", "honors", "relevantCoursework"]}},
            {"sectionType": "SKILLS", "title": "Skills", "orderIndex": 4, "isVisible": true, "defaultContent": {"categories": ["Technical Skills", "Soft Skills", "Tools & Software"]}},
            {"sectionType": "CERTIFICATIONS", "title": "Certifications", "orderIndex": 5, "isVisible": true, "defaultContent": {"fields": ["certName", "issuingOrg", "issueDate", "expiryDate", "credentialId"]}}
        ]
        """;

    private static final String MODERN_MINIMALIST_SECTIONS = """
        [
            {"sectionType": "PERSONAL_INFO", "title": "Contact", "orderIndex": 0, "isVisible": true, "defaultContent": {"layout": "sidebar", "fields": ["fullName", "email", "phone", "location", "linkedIn", "github", "portfolio"]}},
            {"sectionType": "SUMMARY", "title": "About Me", "orderIndex": 1, "isVisible": true, "defaultContent": {"placeholder": "Brief professional introduction...", "maxLength": 300}},
            {"sectionType": "EXPERIENCE", "title": "Experience", "orderIndex": 2, "isVisible": true, "defaultContent": {"fields": ["jobTitle", "company", "duration", "highlights"], "bulletStyle": "minimal"}},
            {"sectionType": "PROJECTS", "title": "Projects", "orderIndex": 3, "isVisible": true, "defaultContent": {"fields": ["projectName", "description", "technologies", "link", "highlights"]}},
            {"sectionType": "EDUCATION", "title": "Education", "orderIndex": 4, "isVisible": true, "defaultContent": {"fields": ["degree", "institution", "year"], "compact": true}},
            {"sectionType": "SKILLS", "title": "Skills", "orderIndex": 5, "isVisible": true, "defaultContent": {"displayStyle": "tags", "categories": ["Primary", "Secondary"]}},
            {"sectionType": "LANGUAGES", "title": "Languages", "orderIndex": 6, "isVisible": true, "defaultContent": {"fields": ["language", "proficiency"], "displayStyle": "bars"}}
        ]
        """;

    private static final String EXECUTIVE_ELITE_SECTIONS = """
        [
            {"sectionType": "PERSONAL_INFO", "title": "Executive Profile", "orderIndex": 0, "isVisible": true, "defaultContent": {"layout": "header", "fields": ["fullName", "title", "email", "phone", "linkedIn", "location"], "includePhoto": true}},
            {"sectionType": "SUMMARY", "title": "Executive Summary", "orderIndex": 1, "isVisible": true, "defaultContent": {"placeholder": "Visionary leader with proven track record...", "style": "prominent"}},
            {"sectionType": "CUSTOM", "title": "Key Achievements", "orderIndex": 2, "isVisible": true, "defaultContent": {"type": "achievements", "fields": ["achievement", "impact", "metrics"], "displayStyle": "highlight-boxes"}},
            {"sectionType": "EXPERIENCE", "title": "Career History", "orderIndex": 3, "isVisible": true, "defaultContent": {"fields": ["title", "company", "duration", "scope", "keyAccomplishments", "teamSize", "budget"], "style": "detailed"}},
            {"sectionType": "CUSTOM", "title": "Board Memberships & Advisory Roles", "orderIndex": 4, "isVisible": true, "defaultContent": {"type": "board", "fields": ["organization", "role", "duration", "contributions"]}},
            {"sectionType": "EDUCATION", "title": "Education & Executive Development", "orderIndex": 5, "isVisible": true, "defaultContent": {"fields": ["degree", "institution", "year", "executivePrograms"]}},
            {"sectionType": "CERTIFICATIONS", "title": "Professional Certifications", "orderIndex": 6, "isVisible": true, "defaultContent": {"fields": ["certification", "organization", "year"]}},
            {"sectionType": "CUSTOM", "title": "Publications & Speaking", "orderIndex": 7, "isVisible": true, "defaultContent": {"type": "publications", "fields": ["title", "publication", "date", "topic"]}}
        ]
        """;

    private static final String CREATIVE_PORTFOLIO_SECTIONS = """
        [
            {"sectionType": "PERSONAL_INFO", "title": "Hello, I'm", "orderIndex": 0, "isVisible": true, "defaultContent": {"layout": "creative-header", "fields": ["fullName", "tagline", "email", "phone", "portfolio", "behance", "dribbble", "instagram"], "includePhoto": true}},
            {"sectionType": "SUMMARY", "title": "My Story", "orderIndex": 1, "isVisible": true, "defaultContent": {"placeholder": "Creative professional passionate about...", "style": "narrative"}},
            {"sectionType": "CUSTOM", "title": "Portfolio Highlights", "orderIndex": 2, "isVisible": true, "defaultContent": {"type": "portfolio", "fields": ["projectName", "category", "description", "imageUrl", "projectUrl"], "displayStyle": "grid"}},
            {"sectionType": "SKILLS", "title": "Creative Skills", "orderIndex": 3, "isVisible": true, "defaultContent": {"displayStyle": "visual-bars", "categories": ["Design Tools", "Creative Skills", "Technical Skills"]}},
            {"sectionType": "EXPERIENCE", "title": "Creative Journey", "orderIndex": 4, "isVisible": true, "defaultContent": {"fields": ["role", "company", "duration", "projects", "impact"], "style": "timeline"}},
            {"sectionType": "EDUCATION", "title": "Education", "orderIndex": 5, "isVisible": true, "defaultContent": {"fields": ["degree", "school", "year", "focus"]}},
            {"sectionType": "CUSTOM", "title": "Awards & Recognition", "orderIndex": 6, "isVisible": true, "defaultContent": {"type": "awards", "fields": ["award", "organization", "year", "project"]}},
            {"sectionType": "CUSTOM", "title": "Clients & Collaborations", "orderIndex": 7, "isVisible": true, "defaultContent": {"type": "clients", "displayStyle": "logo-grid"}}
        ]
        """;

    private static final String TECH_DEVELOPER_SECTIONS = """
        [
            {"sectionType": "PERSONAL_INFO", "title": "Contact", "orderIndex": 0, "isVisible": true, "defaultContent": {"layout": "compact", "fields": ["fullName", "email", "phone", "location", "linkedIn", "github", "portfolio", "stackoverflow"]}},
            {"sectionType": "SUMMARY", "title": "About", "orderIndex": 1, "isVisible": true, "defaultContent": {"placeholder": "Full-stack developer with X years of experience...", "maxLength": 400}},
            {"sectionType": "SKILLS", "title": "Technical Skills", "orderIndex": 2, "isVisible": true, "defaultContent": {"displayStyle": "categorized", "categories": ["Languages", "Frameworks & Libraries", "Databases", "Cloud & DevOps", "Tools & IDEs"]}},
            {"sectionType": "EXPERIENCE", "title": "Work Experience", "orderIndex": 3, "isVisible": true, "defaultContent": {"fields": ["title", "company", "duration", "techStack", "responsibilities", "achievements"], "showTechBadges": true}},
            {"sectionType": "PROJECTS", "title": "Notable Projects", "orderIndex": 4, "isVisible": true, "defaultContent": {"fields": ["name", "description", "techStack", "role", "githubUrl", "liveUrl", "highlights"], "displayStyle": "cards"}},
            {"sectionType": "CUSTOM", "title": "Open Source Contributions", "orderIndex": 5, "isVisible": true, "defaultContent": {"type": "opensource", "fields": ["project", "contribution", "stars", "link"]}},
            {"sectionType": "EDUCATION", "title": "Education", "orderIndex": 6, "isVisible": true, "defaultContent": {"fields": ["degree", "institution", "year", "relevantCourses"]}},
            {"sectionType": "CERTIFICATIONS", "title": "Certifications", "orderIndex": 7, "isVisible": true, "defaultContent": {"fields": ["name", "issuer", "date", "credentialUrl"], "displayStyle": "badges"}}
        ]
        """;

    private static final String ACADEMIC_SCHOLAR_SECTIONS = """
        [
            {"sectionType": "PERSONAL_INFO", "title": "Contact Information", "orderIndex": 0, "isVisible": true, "defaultContent": {"layout": "academic", "fields": ["fullName", "title", "department", "institution", "email", "phone", "officeAddress", "orcid", "googleScholar", "researchGate"]}},
            {"sectionType": "SUMMARY", "title": "Research Interests", "orderIndex": 1, "isVisible": true, "defaultContent": {"placeholder": "Research focus areas and interests...", "style": "keywords"}},
            {"sectionType": "EDUCATION", "title": "Education", "orderIndex": 2, "isVisible": true, "defaultContent": {"fields": ["degree", "field", "institution", "year", "thesis", "advisor"], "style": "detailed"}},
            {"sectionType": "EXPERIENCE", "title": "Academic Positions", "orderIndex": 3, "isVisible": true, "defaultContent": {"fields": ["title", "department", "institution", "duration", "responsibilities"]}},
            {"sectionType": "CUSTOM", "title": "Publications", "orderIndex": 4, "isVisible": true, "defaultContent": {"type": "publications", "categories": ["Journal Articles", "Conference Papers", "Book Chapters", "Books"], "fields": ["authors", "title", "venue", "year", "doi", "citations"]}},
            {"sectionType": "CUSTOM", "title": "Research Projects", "orderIndex": 5, "isVisible": true, "defaultContent": {"type": "research", "fields": ["title", "role", "fundingAgency", "amount", "duration", "description"]}},
            {"sectionType": "CUSTOM", "title": "Teaching Experience", "orderIndex": 6, "isVisible": true, "defaultContent": {"type": "teaching", "fields": ["course", "level", "institution", "semesters", "enrollment"]}},
            {"sectionType": "CUSTOM", "title": "Grants & Funding", "orderIndex": 7, "isVisible": true, "defaultContent": {"type": "grants", "fields": ["title", "agency", "amount", "role", "period"]}},
            {"sectionType": "CUSTOM", "title": "Conference Presentations", "orderIndex": 8, "isVisible": true, "defaultContent": {"type": "presentations", "fields": ["title", "conference", "location", "date", "type"]}},
            {"sectionType": "CUSTOM", "title": "Professional Memberships", "orderIndex": 9, "isVisible": true, "defaultContent": {"type": "memberships", "fields": ["organization", "role", "since"]}},
            {"sectionType": "LANGUAGES", "title": "Languages", "orderIndex": 10, "isVisible": true, "defaultContent": {"fields": ["language", "proficiency"]}}
        ]
        """;

    private static final String FRESH_GRADUATE_SECTIONS = """
        [
            {"sectionType": "PERSONAL_INFO", "title": "Contact Information", "orderIndex": 0, "isVisible": true, "defaultContent": {"layout": "clean", "fields": ["fullName", "email", "phone", "location", "linkedIn", "github", "portfolio"]}},
            {"sectionType": "SUMMARY", "title": "Career Objective", "orderIndex": 1, "isVisible": true, "defaultContent": {"placeholder": "Motivated recent graduate seeking opportunity to...", "style": "objective", "maxLength": 300}},
            {"sectionType": "EDUCATION", "title": "Education", "orderIndex": 2, "isVisible": true, "defaultContent": {"fields": ["degree", "major", "institution", "graduationDate", "gpa", "honors", "relevantCoursework", "academicProjects"], "style": "prominent", "showCoursework": true}},
            {"sectionType": "EXPERIENCE", "title": "Internships & Work Experience", "orderIndex": 3, "isVisible": true, "defaultContent": {"fields": ["title", "company", "duration", "responsibilities", "achievements"], "includePartTime": true}},
            {"sectionType": "PROJECTS", "title": "Academic & Personal Projects", "orderIndex": 4, "isVisible": true, "defaultContent": {"fields": ["name", "description", "technologies", "outcome", "link"], "style": "detailed"}},
            {"sectionType": "CUSTOM", "title": "Volunteer Experience", "orderIndex": 5, "isVisible": true, "defaultContent": {"type": "volunteer", "fields": ["organization", "role", "duration", "impact"]}},
            {"sectionType": "SKILLS", "title": "Skills & Competencies", "orderIndex": 6, "isVisible": true, "defaultContent": {"displayStyle": "grouped", "categories": ["Technical Skills", "Soft Skills", "Tools & Software"]}},
            {"sectionType": "CUSTOM", "title": "Extracurricular Activities", "orderIndex": 7, "isVisible": true, "defaultContent": {"type": "activities", "fields": ["activity", "organization", "role", "duration", "achievements"]}},
            {"sectionType": "LANGUAGES", "title": "Languages", "orderIndex": 8, "isVisible": true, "defaultContent": {"fields": ["language", "proficiency"], "displayStyle": "simple"}},
            {"sectionType": "CERTIFICATIONS", "title": "Certifications & Courses", "orderIndex": 9, "isVisible": true, "defaultContent": {"fields": ["name", "platform", "date", "credentialUrl"], "includeOnlineCourses": true}}
        ]
        """;

    private static final String CLASSIC_PROFESSIONAL_STYLE = """
        {"primaryColor": "#2c3e50", "secondaryColor": "#34495e", "accentColor": "#3498db", "fontFamily": "Georgia, serif", "headingFont": "Arial, sans-serif", "fontSize": "11pt", "layout": "single-column", "headerStyle": "centered", "sectionDivider": "line"}
        """;

    private static final String MODERN_MINIMALIST_STYLE = """
        {"primaryColor": "#1a1a2e", "secondaryColor": "#16213e", "accentColor": "#0f3460", "fontFamily": "Inter, sans-serif", "headingFont": "Inter, sans-serif", "fontSize": "10pt", "layout": "sidebar-left", "headerStyle": "left-aligned", "sectionDivider": "space", "whitespace": "generous"}
        """;

    private static final String EXECUTIVE_ELITE_STYLE = """
        {"primaryColor": "#1e3a5f", "secondaryColor": "#2e5077", "accentColor": "#b8860b", "fontFamily": "Garamond, serif", "headingFont": "Didot, serif", "fontSize": "11pt", "layout": "two-column-header", "headerStyle": "executive", "sectionDivider": "elegant-line", "photoPosition": "top-right"}
        """;

    private static final String CREATIVE_PORTFOLIO_STYLE = """
        {"primaryColor": "#ff6b6b", "secondaryColor": "#4ecdc4", "accentColor": "#ffe66d", "fontFamily": "Poppins, sans-serif", "headingFont": "Montserrat, sans-serif", "fontSize": "10pt", "layout": "creative-grid", "headerStyle": "bold-creative", "sectionDivider": "none", "useGradients": true}
        """;

    private static final String TECH_DEVELOPER_STYLE = """
        {"primaryColor": "#0d1117", "secondaryColor": "#161b22", "accentColor": "#58a6ff", "fontFamily": "JetBrains Mono, monospace", "headingFont": "Inter, sans-serif", "fontSize": "10pt", "layout": "clean-modern", "headerStyle": "minimal", "sectionDivider": "subtle", "codeStyle": true}
        """;

    private static final String ACADEMIC_SCHOLAR_STYLE = """
        {"primaryColor": "#1a237e", "secondaryColor": "#283593", "accentColor": "#5c6bc0", "fontFamily": "Times New Roman, serif", "headingFont": "Arial, sans-serif", "fontSize": "11pt", "layout": "academic-standard", "headerStyle": "formal", "sectionDivider": "line", "citationStyle": "APA"}
        """;

    private static final String FRESH_GRADUATE_STYLE = """
        {"primaryColor": "#2196f3", "secondaryColor": "#1976d2", "accentColor": "#64b5f6", "fontFamily": "Roboto, sans-serif", "headingFont": "Roboto, sans-serif", "fontSize": "10pt", "layout": "single-column", "headerStyle": "modern-clean", "sectionDivider": "subtle", "youthful": true}
        """;

    @Bean
    CommandLineRunner seedCVTemplates(CVTemplateRepository templateRepository) {
        return args -> {
            if (templateRepository.count() > 0) {
                logger.info("CV Templates already exist. Skipping seeding.");
                return;
            }

            logger.info("Seeding CV Templates...");

            List<CVTemplate> templates = List.of(
                createTemplate("Classic Professional", "A timeless, traditional CV layout perfect for corporate positions, banking, law, and established industries. Features clean typography, clear section hierarchy, and a formal structure.", "/templates/previews/classic-professional.png", false, 0, TemplateCategory.PROFESSIONAL, CLASSIC_PROFESSIONAL_SECTIONS, CLASSIC_PROFESSIONAL_STYLE),
                createTemplate("Modern Minimalist", "A sleek, contemporary design with generous white space and subtle accent colors. Perfect for tech companies, startups, and modern workplaces.", "/templates/previews/modern-minimalist.png", false, 0, TemplateCategory.MINIMAL, MODERN_MINIMALIST_SECTIONS, MODERN_MINIMALIST_STYLE),
                createTemplate("Executive Elite", "A sophisticated, premium template designed for C-suite executives, directors, and senior managers with elegant layout and comprehensive career sections.", "/templates/previews/executive-elite.png", true, 50, TemplateCategory.PROFESSIONAL, EXECUTIVE_ELITE_SECTIONS, EXECUTIVE_ELITE_STYLE),
                createTemplate("Creative Portfolio", "A visually striking template for designers, artists, marketers, and creative professionals with bold typography and portfolio showcase.", "/templates/previews/creative-portfolio.png", true, 30, TemplateCategory.CREATIVE, CREATIVE_PORTFOLIO_SECTIONS, CREATIVE_PORTFOLIO_STYLE),
                createTemplate("Tech Developer", "Specifically designed for software developers, engineers, and IT professionals with technical skills sections and project showcases.", "/templates/previews/tech-developer.png", false, 0, TemplateCategory.MODERN, TECH_DEVELOPER_SECTIONS, TECH_DEVELOPER_STYLE),
                createTemplate("Academic Scholar", "Tailored for academics, researchers, professors, and PhD candidates with sections for publications, research, teaching, and grants.", "/templates/previews/academic-scholar.png", true, 25, TemplateCategory.PROFESSIONAL, ACADEMIC_SCHOLAR_SECTIONS, ACADEMIC_SCHOLAR_STYLE),
                createTemplate("Fresh Graduate", "An optimized template for recent graduates and entry-level candidates highlighting education, internships, projects, and transferable skills.", "/templates/previews/fresh-graduate.png", false, 0, TemplateCategory.MINIMAL, FRESH_GRADUATE_SECTIONS, FRESH_GRADUATE_STYLE)
            );

            templateRepository.saveAll(templates);
            logger.info("Successfully seeded {} CV Templates", templates.size());
        };
    }

    private CVTemplate createTemplate(String name, String description, String previewImageUrl, Boolean isPremium, Integer creditCost, TemplateCategory category, String sectionsConfig, String styleConfig) {
        CVTemplate template = new CVTemplate();
        template.setName(name);
        template.setDescription(description);
        template.setPreviewImageUrl(previewImageUrl);
        template.setIsPremium(isPremium);
        template.setCreditCost(creditCost);
        template.setCategory(category);
        template.setSectionsConfig(sectionsConfig.trim());
        template.setStyleConfig(styleConfig.trim());
        template.setIsActive(true);
        return template;
    }

    @Bean
    CommandLineRunner seedSubscriptionPlans(SubscriptionPlanRepository planRepository) {
        return args -> {
            if (planRepository.count() > 0) {
                logger.info("Subscription Plans already exist. Skipping seeding.");
                return;
            }

            logger.info("Seeding Subscription Plans...");

            List<SubscriptionPlan> plans = List.of(
                createPlan(PlanType.FREE, "Free Plan", "Get started with essential features for job seekers. Perfect for exploring the platform.", 
                    BigDecimal.ZERO, BigDecimal.ZERO, 100, 3, 25, false, false, false),
                createPlan(PlanType.PRO, "Pro Plan", "Unlock advanced features for serious job seekers. AI assistance and premium templates included.", 
                    new BigDecimal("9.99"), new BigDecimal("99.99"), 500, 10, 100, true, false, true),
                createPlan(PlanType.PREMIUM, "Premium Plan", "Ultimate package for power users. Unlimited access to all features with priority support.", 
                    new BigDecimal("19.99"), new BigDecimal("199.99"), 2000, 999, 999, true, true, true)
            );

            planRepository.saveAll(plans);
            logger.info("Successfully seeded {} Subscription Plans", plans.size());
        };
    }

    private SubscriptionPlan createPlan(PlanType planType, String name, String description,
                                        BigDecimal monthlyPrice, BigDecimal yearlyPrice,
                                        Integer monthlyCredits, Integer maxCVs, Integer maxJobApplications,
                                        Boolean hasAIAssistance, Boolean hasPrioritySupport, Boolean hasPremiumTemplates) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setPlanType(planType);
        plan.setName(name);
        plan.setDescription(description);
        plan.setMonthlyPrice(monthlyPrice);
        plan.setYearlyPrice(yearlyPrice);
        plan.setMonthlyCredits(monthlyCredits);
        plan.setMaxCVs(maxCVs);
        plan.setMaxJobApplications(maxJobApplications);
        plan.setHasAIAssistance(hasAIAssistance);
        plan.setHasPrioritySupport(hasPrioritySupport);
        plan.setHasPremiumTemplates(hasPremiumTemplates);
        plan.setIsActive(true);
        return plan;
    }
}
