package com.smarthire.notification_service.service;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplateService {

    // ── Welcome email ─────────────────────────────────────────────────────

    public String welcome(String firstName) {
        return base(
            "Welcome to SmartHire, " + firstName + "! 🎉",
            "<p>Hi <strong>" + firstName + "</strong>,</p>" +
            "<p>Welcome to <strong>SmartHire</strong> — your AI-powered hiring platform.</p>" +
            "<p>Here's what you can do:</p>" +
            "<ul>" +
            "<li>📝 Complete your candidate profile</li>" +
            "<li>🔍 Browse and apply to jobs</li>" +
            "<li>📄 Upload your resume for AI-powered matching</li>" +
            "<li>📅 Schedule interviews directly from the platform</li>" +
            "</ul>" +
            "<p>Get started by completing your profile today!</p>"
        );
    }

    // ── Application submitted ─────────────────────────────────────────────

    public String applicationSubmitted(String firstName, String jobTitle, String company) {
        return base(
            "Application Submitted — " + jobTitle,
            "<p>Hi <strong>" + firstName + "</strong>,</p>" +
            "<p>Your application for <strong>" + jobTitle + "</strong> at <strong>" + company +
            "</strong> has been successfully submitted.</p>" +
            "<p>What happens next?</p>" +
            "<ol>" +
            "<li>Our AI will analyse your profile and resume</li>" +
            "<li>A recruiter will review your application</li>" +
            "<li>You'll be notified of any status updates</li>" +
            "</ol>" +
            "<p>You can track your application status anytime in your dashboard.</p>"
        );
    }

    // ── Application status changed ────────────────────────────────────────

    public String statusChanged(String firstName, String jobTitle, String company, String status) {
        String statusMessage = switch (status) {
            case "SHORTLISTED" -> "🎉 Congratulations! You've been <strong>shortlisted</strong> for " + jobTitle + " at " + company + ". A recruiter will be in touch soon.";
            case "REJECTED"    -> "Thank you for applying for " + jobTitle + " at " + company + ". After careful review, we've decided to move forward with other candidates at this time.";
            case "UNDER_REVIEW" -> "Your application for " + jobTitle + " at " + company + " is now <strong>under review</strong> by the hiring team.";
            default            -> "Your application status for " + jobTitle + " at " + company + " has been updated to <strong>" + status + "</strong>.";
        };

        return base(
            "Application Update — " + jobTitle,
            "<p>Hi <strong>" + firstName + "</strong>,</p>" +
            "<p>" + statusMessage + "</p>" +
            "<p>Log in to your SmartHire dashboard to view more details.</p>"
        );
    }

    // ── Interview scheduled ───────────────────────────────────────────────

    public String interviewScheduled(String firstName, String jobTitle, String company,
                                      String scheduledAt, Integer durationMinutes,
                                      String meetingLink, String location) {
        String locationInfo = meetingLink != null
            ? "<p>📹 <strong>Meeting Link:</strong> <a href='" + meetingLink + "'>" + meetingLink + "</a></p>"
            : "<p>📍 <strong>Location:</strong> " + (location != null ? location : "TBD") + "</p>";

        return base(
            "Interview Scheduled — " + jobTitle,
            "<p>Hi <strong>" + firstName + "</strong>,</p>" +
            "<p>Your interview for <strong>" + jobTitle + "</strong> at <strong>" + company +
            "</strong> has been scheduled.</p>" +
            "<div style='background:#f4f4f4;padding:16px;border-radius:8px;margin:16px 0'>" +
            "<p>📅 <strong>Date & Time:</strong> " + scheduledAt + "</p>" +
            "<p>⏱ <strong>Duration:</strong> " + durationMinutes + " minutes</p>" +
            locationInfo +
            "</div>" +
            "<p>Please make sure to:</p>" +
            "<ul>" +
            "<li>Be available 5 minutes before the scheduled time</li>" +
            "<li>Test your audio/video if it's a virtual interview</li>" +
            "<li>Review the job description beforehand</li>" +
            "</ul>"
        );
    }

    // ── Job posted ────────────────────────────────────────────────────────

    public String jobPosted(String firstName, String jobTitle, String company, String location, String jobId) {
        return base(
            "New Job Match — " + jobTitle,
            "<p>Hi <strong>" + firstName + "</strong>,</p>" +
            "<p>A new job matching your skills has been posted!</p>" +
            "<div style='background:#f4f4f4;padding:16px;border-radius:8px;margin:16px 0'>" +
            "<p>💼 <strong>Role:</strong> " + jobTitle + "</p>" +
            "<p>🏢 <strong>Company:</strong> " + company + "</p>" +
            "<p>📍 <strong>Location:</strong> " + location + "</p>" +
            "</div>" +
            "<p>Don't miss this opportunity — apply before it closes!</p>"
        );
    }

    // ── Base HTML wrapper ─────────────────────────────────────────────────

    private String base(String heading, String body) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <style>
                body { font-family: Arial, sans-serif; background: #f9f9f9; margin: 0; padding: 0; }
                .container { max-width: 600px; margin: 40px auto; background: #fff; border-radius: 8px; padding: 32px; }
                .header { background: #4F46E5; color: #fff; padding: 24px 32px; border-radius: 8px 8px 0 0; margin: -32px -32px 24px; }
                .header h1 { margin: 0; font-size: 22px; }
                .footer { margin-top: 32px; padding-top: 16px; border-top: 1px solid #eee; font-size: 12px; color: #999; }
                a { color: #4F46E5; }
                p { line-height: 1.6; color: #333; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header"><h1>SmartHire</h1></div>
                <h2>""" + heading + """
                </h2>
                """ + body + """
                <div class="footer">
                  <p>You're receiving this email because you have an account on SmartHire.</p>
                  <p>&copy; 2026 SmartHire. All rights reserved.</p>
                </div>
              </div>
            </body>
            </html>
            """;
    }
}
