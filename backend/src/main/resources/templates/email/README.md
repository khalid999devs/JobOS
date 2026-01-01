# Email Templates Guide

## Overview

This directory contains production-grade email templates using Thymeleaf templating engine. The structure is designed for maintainability, reusability, and easy addition of new email types.

## Directory Structure

```
templates/email/
├── base/
│   └── email-layout.html          # Base layout with header, footer, and styling
├── otp/
│   └── password-reset-otp.html    # Password reset OTP email
└── welcome/
    └── welcome-email.html          # Welcome email for new users
```

## How It Works

### 1. Base Layout (`email-layout.html`)

- Contains common structure: header, footer, and styling
- Uses Thymeleaf variables for dynamic content
- Provides consistent branding across all emails

### 2. Content Templates

Each email type has its own template that focuses only on content:

- Variables are passed from the service layer
- Uses Thymeleaf syntax: `th:text="${variable}"`
- Automatically inherits base styling

## Creating New Email Templates

### Step 1: Create Template Directory

```bash
mkdir -p backend/src/main/resources/templates/email/[category]
```

### Step 2: Create Template File

Create `[template-name].html` with this structure:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
  <body>
    <p>Hello <strong th:text="${userName}">User</strong>,</p>

    <p>Your email content here...</p>

    <div class="content-box">
      <!-- Highlighted content -->
      <h2 th:text="${dynamicHeading}">Default Heading</h2>
    </div>

    <div class="info-list">
      <ul>
        <li>Point 1</li>
        <li>Point 2</li>
      </ul>
    </div>

    <p style="text-align: center;">
      <a th:href="${actionUrl}" class="button">Action Button</a>
    </p>

    <p>Best regards,<br /><strong>The JobOS Team</strong></p>
  </body>
</html>
```

### Step 3: Add Service Method

In `EmailService.java`:

```java
public void sendYourNewEmail(String to, String param1, String param2) {
    String htmlContent = templateService.processTemplate(
        "email/[category]/[template-name]",
        Map.of(
            "param1", param1,
            "param2", param2,
            "recipientEmail", to,
            "emailTitle", "Your Email Title"
        )
    );

    sendHtmlEmail(to, "Email Subject", htmlContent);
}
```

## Available CSS Classes

Use these pre-defined classes in your templates:

- `.content-box` - Highlighted content box with dashed border
- `.button` - Green call-to-action button
- `.info-list` - Information list with left border
- Standard HTML tags are automatically styled

## Template Variables

### Required in All Templates:

- `recipientEmail` - Recipient's email (shown in footer)
- `emailTitle` - Title displayed in header

### Custom Variables:

Pass any additional variables via `Map.of()` in the service method.

## Examples

### OTP Email

```java
emailService.sendOtpEmail("user@example.com", "123456");
```

### Welcome Email

```java
emailService.sendWelcomeEmail("user@example.com", "John Doe");
```

### Custom Email

```java
Map<String, Object> variables = Map.of(
    "userName", "Jane",
    "jobTitle", "Senior Developer",
    "companyName", "TechCorp",
    "recipientEmail", "jane@example.com",
    "emailTitle", "New Job Match"
);

String html = templateService.processTemplate("email/jobs/job-match", variables);
emailService.sendHtmlEmail("jane@example.com", "New Job Match!", html);
```

## Best Practices

1. **Separate Concerns**: Keep content in templates, logic in services
2. **Use Variables**: Never hardcode content that might change
3. **Test Templates**: Preview templates before deployment
4. **Mobile-Friendly**: All templates use responsive max-width design
5. **Fallback Content**: Provide default values in `th:text="${var}:default"`
6. **Security**: Always validate and sanitize variables before passing to templates

## Testing

To test email rendering without sending:

```java
String html = templateService.processTemplate(
    "email/otp/password-reset-otp",
    Map.of("otp", "123456", "recipientEmail", "test@example.com", "emailTitle", "Test")
);
System.out.println(html);
```

## Production Considerations

- Templates are cached by Thymeleaf for performance
- Use environment-specific URLs (dev, staging, prod)
- Consider i18n for multi-language support
- Monitor email delivery rates
- A/B test subject lines and content

## Future Additions

Planned email templates:

- Application status updates
- Interview invitations
- Job recommendations
- Account notifications
- Payment receipts
