package com.example.club_sporting_final.utils;

import jakarta.mail.*;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailSender {

    private static final String DEFAULT_SENDER_EMAIL = "3bdhmeed@gmail.com";
    private static final String SENDER_EMAIL = System.getenv("SENDER_EMAIL") != null && !System.getenv("SENDER_EMAIL").isBlank()
            ? System.getenv("SENDER_EMAIL")
            : DEFAULT_SENDER_EMAIL;
    private static final String SENDER_PASSWORD = "tqkr rmek cmzl spql";

    /**
     * Sends an email. Returns true if sent successfully, false otherwise.
     * Uses a boolean return so callers don't need to handle checked exceptions.
     */
    public static boolean sendEmail(String recipient, String subject, String messageBody) {
        if (SENDER_EMAIL == null || SENDER_EMAIL.isBlank()) {
            System.err.println("Warning: SENDER_EMAIL is not set. Skipping email to: " + recipient);
            return false;
        }
        if (recipient == null || recipient.isBlank()) {
            System.err.println("Skipping email: recipient is empty.");
            return false;
        }

        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.host", "smtp.gmail.com");
            properties.put("mail.smtp.port", "587");

            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            message.setSubject(subject);
            message.setText(messageBody);

            Transport.send(message);
            System.out.println("Email sent successfully to: " + recipient);
            return true;

        } catch (AddressException e) {
            System.err.println("Invalid email address: " + recipient + " — " + e.getMessage());
            return false;
        } catch (MessagingException e) {
            System.err.println("Failed to send email to " + recipient + ": " + e.getMessage());
            return false;
        }
    }
}