package com.collectpro.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public void sendActivationEmail(String toEmail, String firstName, String activationLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Activez votre compte CollectPro");
            helper.setText(buildActivationHtml(firstName, activationLink), true);
            mailSender.send(message);
            log.info("[EMAIL] Invitation envoyée à {}", toEmail);
        } catch (Exception e) {
            // On ne bloque JAMAIS la création du compte si l'email échoue
            // (réseau, serveur SMTP indisponible, etc.) : l'admin peut
            // toujours renvoyer l'invitation plus tard.
            log.error("[EMAIL] Échec d'envoi à {} : {}", toEmail, e.getMessage());
        }
    }

    public void sendPasswordResetEmail(String toEmail, String firstName, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Réinitialisation de votre mot de passe CollectPro");
            helper.setText(buildResetHtml(firstName, resetLink), true);
            mailSender.send(message);
            log.info("[EMAIL] Réinitialisation envoyée à {}", toEmail);
        } catch (Exception e) {
            log.error("[EMAIL] Échec d'envoi (reset) à {} : {}", toEmail, e.getMessage());
        }
    }

    private String buildResetHtml(String firstName, String resetLink) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto;">
                <h2 style="color: #2563eb;">Réinitialisation de mot de passe</h2>
                <p>Bonjour %s,</p>
                <p>Vous avez demandé la réinitialisation de votre mot de passe CollectPro. Cliquez sur le bouton ci-dessous pour en définir un nouveau :</p>
                <p style="text-align: center; margin: 24px 0;">
                    <a href="%s" style="background: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 8px; display: inline-block;">
                        Réinitialiser mon mot de passe
                    </a>
                </p>
                <p style="color: #64748b; font-size: 0.85rem;">Ce lien expire dans 2 heures. Si vous n'êtes pas à l'origine de cette demande, ignorez cet email — votre mot de passe actuel reste inchangé.</p>
            </div>
            """.formatted(firstName, resetLink);
    }

    private String buildActivationHtml(String firstName, String activationLink) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto;">
                    <h2 style="color: #2563eb;">Bienvenue sur CollectPro</h2>
                    <p>Bonjour %s,</p>
                    <p>Un compte vient d'être créé pour vous sur CollectPro. Cliquez sur le bouton ci-dessous pour définir votre mot de passe et activer votre compte :</p>
                    <p style="text-align: center; margin: 24px 0;">
                        <a href="%s" style="background: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 8px; display: inline-block;">
                            Activer mon compte
                        </a>
                    </p>
                    <p style="color: #64748b; font-size: 0.85rem;">Ce lien expire dans 48 heures. Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.</p>
                </div>
                """.formatted(firstName, activationLink);
    }
}