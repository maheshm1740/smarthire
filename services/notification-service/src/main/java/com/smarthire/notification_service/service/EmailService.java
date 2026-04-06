package com.smarthire.notification_service.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    @Value("${sendgrid.api-key}")
    private String apiKey;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    @Value("${sendgrid.from-name}")
    private String fromName;

    public boolean sendEmail(String toEmail, String toName, String subject, String htmlContent) {
        try {
            Email from = new Email(fromEmail, fromName);
            Email to   = new Email(toEmail, toName);
            Content content = new Content("text/html", htmlContent);
            Mail mail = new Mail(from, subject, to, content);

            SendGrid sg = new SendGrid(apiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);
            boolean success = response.getStatusCode() >= 200 && response.getStatusCode() < 300;

            if (success) {
                log.info("Email sent to {} — subject: {}", toEmail, subject);
            } else {
                log.error("Failed to send email to {} — status: {} body: {}",
                    toEmail, response.getStatusCode(), response.getBody());
            }

            return success;

        } catch (Exception e) {
            log.error("Email send error to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }
}
