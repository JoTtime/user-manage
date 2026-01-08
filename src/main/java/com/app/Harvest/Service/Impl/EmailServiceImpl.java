package com.app.Harvest.Service.Impl;

import com.app.Harvest.Entity.User;
import com.app.Harvest.Service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendApprovalEmail(User user) {

    }

    @Override
    public void sendRejectionEmail(User user) {

    }

    @Override
    public void sendNewRegistrationNotification(User user) {

    }

    @Override
    public void sendWelcomeEmail(User user) {

    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken, String userName) {
        try {
            String resetUrl = frontendUrl + "/auth/reset-password?token=" + resetToken;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset Request - Harvest Management System");
            message.setText(
                    "Dear " + userName + ",\n\n" +
                            "You requested to reset your password for your Harvest Management System account.\n\n" +
                            "Please click the link below to reset your password:\n" +
                            resetUrl + "\n\n" +
                            "This link will expire in 1 hour.\n\n" +
                            "If you did not request this password reset, please ignore this email or contact support if you have concerns.\n\n" +
                            "Best regards,\n" +
                            "Harvest Management System Team"
            );

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage());
        }
    }

    @Override
    public void sendPasswordChangedConfirmationEmail(String toEmail, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Changed Successfully - Harvest Management System");
            message.setText(
                    "Dear " + userName + ",\n\n" +
                            "Your password has been successfully changed.\n\n" +
                            "If you did not make this change, please contact our support team immediately.\n\n" +
                            "Best regards,\n" +
                            "Harvest Management System Team"
            );

            mailSender.send(message);
            log.info("Password changed confirmation email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send password changed confirmation email to: {}", toEmail, e);
            // Don't throw exception here - password was already changed successfully
        }
    }
}