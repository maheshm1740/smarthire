package com.smarthire.notification_service.template;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplateBuilder {

    // ── Welcome ───────────────────────────────────────────────────────────

    public String welcome(String firstName) {
        return base("Welcome to SmartHire! 🎉", """
            <h2>Hi %s, welcome aboard!</h2>
            <p>Your SmartHire account is ready. Here's what you can do:</p>
            <ul>
                <li>Complete your candidate profile</li>
                <li>Upload your resume</li>
                <li>Browse and apply to jobs</li>
            </ul>
            <p><a href="http://localhost:3000/profile" style="background:#4F46E5;color:white;padding:12px 24px;text-decoration:none;border-radius:6px;">
                Complete Your Profile
            </a></p>
            """.formatted(firstName));
    }

    // ── Application submitted ─────────────────────────────────────────────

    public String applicationSubmitted(String firstName, String jobTitle, String company) {
        return base("Application Submitted ✅", """
            <h2>Hi %s,</h2>
            <p>Your application for <strong>%s</strong> at <strong>%s</strong> has been successfully submitted.</p>
            <p>The recruiter will review your profile and get back to you. You can track your application status in the SmartHire dashboard.</p>
            <p><a href="http://localhost:3000/applications" style="background:#4F46E5;color:white;padding:12px 24px;text-decoration:none;border-radius:6px;">
                View My Applications
            </a></p>
            """.formatted(firstName, jobTitle, company));
    }

    // ── Application status changed ────────────────────────────────────────

    public String applicationStatusChanged(String firstName, String jobTitle,
                                            String company, String status) {
        String statusColor = "SHORTLISTED".equals(status) ? "#16a34a" : "#dc2626";
        String statusEmoji = "SHORTLISTED".equals(status) ? "🎉" : "📋";
        return base("Application Update " + statusEmoji, """
            <h2>Hi %s,</h2>
            <p>Your application for <strong>%s</strong> at <strong>%s</strong> has been updated.</p>
            <p>Status: <strong style="color:%s;">%s</strong></p>
            %s
            <p><a href="http://localhost:3000/applications" style="background:#4F46E5;color:white;padding:12px 24px;text-decoration:none;border-radius:6px;">
                View Application
            </a></p>
            """.formatted(firstName, jobTitle, company, statusColor, status,
                "SHORTLISTED".equals(status)
                    ? "<p>Congratulations! The recruiter will be in touch soon to schedule an interview.</p>"
                    : "<p>Thank you for your interest. We encourage you to keep applying to other positions.</p>"));
    }

    // ── Interview scheduled ───────────────────────────────────────────────

    public String interviewScheduled(String firstName, String jobTitle,
                                      String company, String scheduledAt,
                                      String meetingLink, String interviewerName,
                                      int durationMinutes) {
        return base("Interview Scheduled 📅", """
            <h2>Hi %s,</h2>
            <p>Your interview for <strong>%s</strong> at <strong>%s</strong> has been scheduled.</p>
            <table style="border-collapse:collapse;width:100%%;">
                <tr><td style="padding:8px;font-weight:bold;">Date & Time</td><td style="padding:8px;">%s</td></tr>
                <tr><td style="padding:8px;font-weight:bold;">Duration</td><td style="padding:8px;">%d minutes</td></tr>
                <tr><td style="padding:8px;font-weight:bold;">Interviewer</td><td style="padding:8px;">%s</td></tr>
            </table>
            <br/>
            <p><a href="%s" style="background:#4F46E5;color:white;padding:12px 24px;text-decoration:none;border-radius:6px;">
                Join Interview
            </a></p>
            <p style="color:#6b7280;font-size:12px;">Please be on time and have a stable internet connection.</p>
            """.formatted(firstName, jobTitle, company, scheduledAt,
                durationMinutes, interviewerName, meetingLink));
    }

    // ── Job posted ────────────────────────────────────────────────────────

    public String jobPosted(String firstName, String jobTitle,
                             String company, String location, String jobId) {
        return base("New Job Match Found 💼", """
            <h2>Hi %s,</h2>
            <p>A new job matching your skills has been posted:</p>
            <div style="border:1px solid #e5e7eb;border-radius:8px;padding:16px;margin:16px 0;">
                <h3 style="margin:0 0 8px 0;">%s</h3>
                <p style="margin:4px 0;color:#6b7280;">%s · %s</p>
            </div>
            <p><a href="http://localhost:3000/jobs/%s" style="background:#4F46E5;color:white;padding:12px 24px;text-decoration:none;border-radius:6px;">
                View Job
            </a></p>
            """.formatted(firstName, jobTitle, company, location, jobId));
    }

    // ── Base HTML wrapper ─────────────────────────────────────────────────

    private String base(String title, String content) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><title>%s</title></head>
            <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;color:#111827;">
                <div style="background:#4F46E5;padding:20px;border-radius:8px 8px 0 0;text-align:center;">
                    <h1 style="color:white;margin:0;font-size:24px;">SmartHire</h1>
                </div>
                <div style="border:1px solid #e5e7eb;border-top:none;padding:24px;border-radius:0 0 8px 8px;">
                    %s
                    <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0;"/>
                    <p style="color:#9ca3af;font-size:12px;text-align:center;">
                        SmartHire — AI-Powered Recruitment Platform<br/>
                        <a href="http://localhost:3000/unsubscribe" style="color:#9ca3af;">Unsubscribe</a>
                    </p>
                </div>
            </body>
            </html>
            """.formatted(title, content);
    }
}
