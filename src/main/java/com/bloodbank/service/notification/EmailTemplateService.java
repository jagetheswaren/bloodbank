package com.bloodbank.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    /**
     * Renders a Thymeleaf HTML email template from templates/email/{templateName}.html
     */
    public String renderTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        if (variables != null) {
            variables.forEach(context::setVariable);
        }
        try {
            return templateEngine.process("email/" + templateName, context);
        } catch (Exception ex) {
            log.error("Failed to render email template [email/{}]: {}", templateName, ex.getMessage());
            // Fallback plain html
            StringBuilder fallback = new StringBuilder("<html><body><div style='font-family:sans-serif;padding:20px;'>");
            fallback.append("<h2>BloodBank Notification</h2>");
            if (variables != null) {
                variables.forEach((k, v) -> fallback.append("<p><strong>").append(k).append(":</strong> ").append(v).append("</p>"));
            }
            fallback.append("</div></body></html>");
            return fallback.toString();
        }
    }
}
