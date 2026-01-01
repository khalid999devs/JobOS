package com.jobos.backend.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    public EmailTemplateService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String processTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateName, context);
    }

    public String buildEmail(String emailTitle, String contentTemplate, 
                            String recipientEmail, Map<String, Object> contentVariables) {
        String processedContent = processTemplate(contentTemplate, contentVariables);
        Map<String, Object> layoutVariables = Map.of(
            "emailTitle", emailTitle,
            "contentTemplate", "~{:: content}",
            "recipientEmail", recipientEmail
        );
        
        Context context = new Context();
        context.setVariables(layoutVariables);
        context.setVariable("content", processedContent);
        
        return templateEngine.process("email/base/email-layout", context);
    }
}
