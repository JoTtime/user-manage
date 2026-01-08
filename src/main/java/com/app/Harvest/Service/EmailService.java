package com.app.Harvest.Service;

import com.app.Harvest.Entity.User;

public interface EmailService {

    /**
     * Send account approval notification to cooperative
     */
    void sendApprovalEmail(User user);

    /**
     * Send account rejection notification to cooperative
     */
    void sendRejectionEmail(User user);

    /**
     * Send new registration notification to super admin
     */
    void sendNewRegistrationNotification(User user);

    /**
     * Send welcome email after successful signup
     */
    void sendWelcomeEmail(User user);

    // NEW METHODS FOR PASSWORD RESET
    /**
     * Send password reset email with token
     */
    void sendPasswordResetEmail(String toEmail, String resetToken, String userName);

    /**
     * Send password changed confirmation email
     */
    void sendPasswordChangedConfirmationEmail(String toEmail, String userName);
}