package com.collectpro.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class JwtSecretValidator implements ApplicationRunner {

    private static final String KNOWN_WEAK_DEFAULT =
            "change-moi-en-une-longue-chaine-aleatoire-en-production";

    // HS256 exige au minimum 256 bits = 32 octets
    private static final int MIN_SECRET_LENGTH = 32;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    private final Environment environment;

    public JwtSecretValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean isProd = java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod");

        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET n'est pas défini. L'application ne peut pas démarrer sans secret JWT.");
        }

        if (jwtSecret.equals(KNOWN_WEAK_DEFAULT)) {
            if (isProd) {
                throw new IllegalStateException(
                        "JWT_SECRET utilise la valeur par défaut de développement. " +
                                "Définissez un secret fort et unique via la variable d'environnement JWT_SECRET.");
            }
            // En dev, juste un avertissement visible
            System.out.println(
                    "⚠️  ATTENTION : JWT_SECRET utilise la valeur par défaut de dev. " +
                            "Ne jamais utiliser ce secret en production.");
        }

        if (jwtSecret.getBytes().length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET est trop court (" + jwtSecret.getBytes().length +
                            " octets). Il faut au moins " + MIN_SECRET_LENGTH + " octets pour HS256.");
        }
    }
}