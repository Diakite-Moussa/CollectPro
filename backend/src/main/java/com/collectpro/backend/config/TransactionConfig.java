package com.collectpro.backend.config;

// Config supprimee : l'approche par transaction NESTED (points de sauvegarde
// JPA/Hibernate) s'est averee non fonctionnelle avec Hibernate (le driver
// JPA ne remonte pas d'acces direct a la connexion JDBC permettant a Spring
// de creer un savepoint). Voir AuditLogService.logAfterCommit() pour la
// solution retenue : differer l'ecriture du log d'audit via un callback
// afterCommit plutot que via une sous-transaction imbriquee ou separee.
