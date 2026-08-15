package com.collectpro.backend.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "fromAddress", "no-reply@collectpro.test");
        lenient().when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getDefaultInstance(new java.util.Properties())));
    }

    @Test
    @DisplayName("sendActivationEmail - Envoie bien le message via le mailSender")
    void sendActivationEmail_Success_CallsSend() {
        emailService.sendActivationEmail("agent@test.com", "Agent", "https://app/activate?token=abc");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendActivationEmail - Une panne SMTP ne remonte jamais d'exception")
    void sendActivationEmail_SmtpFailure_DoesNotThrow() {
        doThrow(new MailSendException("SMTP indisponible")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() ->
                emailService.sendActivationEmail("agent@test.com", "Agent", "https://app/activate?token=abc"));
    }

    @Test
    @DisplayName("sendPasswordResetEmail - Envoie bien le message via le mailSender")
    void sendPasswordResetEmail_Success_CallsSend() {
        emailService.sendPasswordResetEmail("agent@test.com", "Agent", "https://app/reset?token=xyz");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendPasswordResetEmail - Une panne SMTP ne remonte jamais d'exception")
    void sendPasswordResetEmail_SmtpFailure_DoesNotThrow() {
        doThrow(new MailSendException("SMTP indisponible")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() ->
                emailService.sendPasswordResetEmail("agent@test.com", "Agent", "https://app/reset?token=xyz"));
    }
}