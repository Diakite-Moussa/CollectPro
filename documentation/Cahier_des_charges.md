# Cahier des charges fonctionnel

## 1. Présentation générale du projet

### Nom du projet

CollectPro

### Description

CollectPro est une plateforme de collecte d'informations
fonctionnant en mode connecté et hors connexion.

Elle permet aux organisations telles que les ONG,
les structures de santé ou les entreprises de réaliser
des collectes de données sur le terrain.

La plateforme permet aux agents terrain de saisir des
informations depuis une application mobile, même sans
connexion Internet.

Lorsque la connexion est disponible, les données sont
automatiquement synchronisées avec le serveur central.

Les superviseurs peuvent contrôler et valider les
collectes réalisées par leurs équipes, tandis que les
administrateurs gèrent l'ensemble de la plateforme.

## 2. Contexte et problématique

Dans plusieurs domaines comme la santé, les ONG,
l'agriculture ou les enquêtes terrain, la collecte
d'informations nécessite souvent des déplacements
dans des zones où la connexion Internet est instable.

Les principales difficultés rencontrées sont :

- perte des données collectées ;
- dépendance à la connexion Internet ;
- utilisation de formulaires papier ;
- difficulté de suivi des agents ;
- retard dans la transmission des informations.

Ce projet vise donc à proposer une solution
numérique capable de fonctionner en mode offline
et online afin d'améliorer la fiabilité et la rapidité
des collectes.

# 3. Objectifs du projet

## 3.1 Objectif général

L'objectif de ce projet est de concevoir et développer une
plateforme numérique de collecte d'informations capable de
fonctionner en mode connecté (online) et hors connexion
(offline).

La solution permettra aux organisations de collecter,
centraliser, contrôler et exploiter des données provenant
du terrain tout en garantissant la fiabilité, la sécurité
et la disponibilité des informations.

---

## 3.2 Objectifs spécifiques

Les objectifs spécifiques sont :

- Développer une application mobile permettant aux agents
  terrain de réaliser des collectes d'informations.

- Permettre aux agents de travailler sans connexion Internet
  grâce à un stockage local des données.

- Synchroniser automatiquement les données collectées avec
  le serveur lorsque la connexion est disponible.

- Mettre en place un système de gestion des utilisateurs
  avec différents niveaux d'accès.

- Permettre aux administrateurs de gérer les organisations,
  les utilisateurs et les paramètres du système.

- Permettre aux superviseurs de suivre les agents de leur
  équipe et de contrôler la qualité des collectes.

- Assurer la traçabilité des données collectées grâce à
  l'historique des synchronisations et des validations.

- Fournir une architecture évolutive permettant l'ajout
  futur d'un système d'analyse par intelligence artificielle.

  # 4. Périmètre du projet

## 4.1 Fonctionnalités incluses dans la Version 1 (MVP)

La première version du système comprend :

### Application mobile Flutter

- Authentification des utilisateurs ;
- Consultation des missions attribuées ;
- Remplissage des formulaires de collecte ;
- Ajout de photos et documents ;
- Capture de la position GPS ;
- Fonctionnement hors connexion ;
- Synchronisation automatique des données.


### Backend Spring Boot

- Gestion des utilisateurs ;
- Gestion des rôles et permissions ;
- Gestion des organisations ;
- API REST sécurisées ;
- Gestion des collectes ;
- Gestion de la synchronisation.


### Dashboard Web Angular

- Authentification administrateur ;
- Gestion des comptes utilisateurs ;
- Affectation des agents aux superviseurs ;
- Consultation des collectes ;
- Suivi global de l'activité.


## 4.2 Fonctionnalités futures

Les fonctionnalités suivantes seront ajoutées dans les
versions ultérieures :

- Générateur dynamique de formulaires ;
- Cartographie avancée GPS ;
- Rapports automatiques ;
- Assistant IA textuel ;
- Assistant IA vocal ;
- Analyse intelligente des données ;
- Système de communication intelligent.

# 5. Acteurs du système

## 5.1 Administrateur

L'administrateur est responsable de la gestion globale
de la plateforme.

Ses responsabilités :

- Créer et gérer les comptes utilisateurs ;
- Créer les superviseurs et les agents ;
- Affecter les agents aux superviseurs ;
- Gérer les organisations ;
- Consulter les données collectées ;
- Superviser le fonctionnement général du système.


## 5.2 Superviseur

Le superviseur est responsable du contrôle d'une équipe
d'agents terrain.

Ses responsabilités :

- Consulter les agents de son équipe ;
- Suivre l'avancement des missions ;
- Consulter les collectes réalisées ;
- Valider ou rejeter les collectes ;
- Ajouter des commentaires lors d'un rejet.


## 5.3 Agent terrain

L'agent est chargé de réaliser les collectes.

Ses responsabilités :

- Se connecter à l'application mobile ;
- Consulter les missions attribuées ;
- Remplir les formulaires ;
- Ajouter des informations terrain ;
- Prendre des photos ;
- Enregistrer la localisation GPS ;
- Sauvegarder les données hors connexion ;
- Synchroniser les données.

# 6. Besoins fonctionnels

Les besoins fonctionnels décrivent les fonctionnalités que
le système doit fournir aux différents utilisateurs.

Chaque besoin possède :
- un identifiant ;
- une description ;
- un acteur concerné ;
- une priorité.

## RF-001 : Authentification utilisateur

Description :

Le système doit permettre aux utilisateurs de se connecter
à la plateforme avec leurs identifiants.

Acteurs concernés :

- Administrateur
- Superviseur
- Agent

Priorité :

Haute


---

## RF-002 : Gestion des rôles

Description :

Le système doit gérer différents types d'utilisateurs
avec des droits d'accès spécifiques.

Rôles :

- ADMIN
- SUPERVISEUR
- AGENT

Priorité :

Haute

## RF-003 : Gestion des organisations

Description :

L'administrateur doit pouvoir créer et gérer une
organisation utilisant la plateforme.

Informations :

- Nom de l'organisation ;
- Description ;
- Informations générales.

Acteur :

Administrateur

Priorité :

Haute


---

## RF-004 : Création des comptes utilisateurs

Description :

L'administrateur doit pouvoir créer les comptes des
superviseurs et des agents.

Informations utilisateur :

- Nom ;
- Prénom ;
- Email ;
- Téléphone ;
- Rôle.

Acteur :

Administrateur

Priorité :

Haute


---

## RF-005 : Affectation des agents aux superviseurs

Description :

L'administrateur doit pouvoir associer un ou plusieurs
agents à un superviseur.

Règles métier :

- Un agent appartient à un seul superviseur.
- Un superviseur peut gérer plusieurs agents.

Acteur :

Administrateur

Priorité :

Haute

## RF-006 : Création d'une collecte

Description :

L'agent doit pouvoir remplir un formulaire afin
d'enregistrer une nouvelle collecte.

Informations possibles :

- Données saisies ;
- Date et heure ;
- Position GPS ;
- Photos ;
- Documents.

Acteur :

Agent

Priorité :

Haute


---

## RF-007 : Sauvegarde locale des collectes

Description :

L'application mobile doit permettre à l'agent de
sauvegarder une collecte même sans connexion Internet.

Fonctionnement :

Les données sont stockées localement dans une base
SQLite avant synchronisation.

Acteur :

Agent

Priorité :

Haute

## RF-008 : Synchronisation automatique

Description :

Le système doit envoyer automatiquement les données
localisées sur le téléphone vers le serveur lorsque
la connexion Internet est disponible.

Fonctionnement :

Mode offline :

Application mobile
        |
        |
      SQLite


Mode online :

SQLite
   |
   |
API REST
   |
   |
PostgreSQL


Acteur :

Agent

Priorité :

Haute


---

## RF-009 : Gestion des erreurs de synchronisation

Description :

Le système doit gérer les problèmes pendant la
synchronisation.

Exemples :

- Perte de connexion ;
- Données invalides ;
- Doublons.

Priorité :

Moyenne

## RF-010 : Consultation des collectes

Description :

Le superviseur doit pouvoir consulter les collectes
réalisées par les agents de son équipe.

Acteur :

Superviseur

Priorité :

Haute


---

## RF-011 : Validation des collectes

Description :

Le superviseur doit pouvoir accepter ou refuser
une collecte.

Statuts possibles :

- EN_ATTENTE
- VALIDEE
- REJETEE


En cas de rejet :

Le superviseur doit fournir un commentaire.

Acteur :

Superviseur

Priorité :

Haute

## RF-012 : Administration de la plateforme

Description :

L'administrateur doit disposer d'une interface web
pour gérer la plateforme.

Fonctionnalités :

- Gestion utilisateurs ;
- Gestion organisations ;
- Consultation des collectes ;
- Suivi des activités.

Acteur :

Administrateur

Priorité :

Haute

## RF-013 : Gestion des formulaires

Description :

Le système doit permettre à terme la création de
formulaires dynamiques.

L'administrateur pourra :

- créer un formulaire ;
- ajouter des champs ;
- modifier une version ;
- publier un formulaire.


Version 1 :

Les formulaires peuvent être prédéfinis.

Evolution future :

Form Builder dynamique.


Acteur :

Administrateur

Priorité :

Moyenne

# 7. Besoins non fonctionnels

Les besoins non fonctionnels définissent les contraintes
techniques et les critères de qualité que le système doit
respecter.

## RNF-001 : Sécurité des accès

Description :

Le système doit garantir que chaque utilisateur accède
uniquement aux fonctionnalités correspondant à son rôle.

Mécanisme :

- Authentification sécurisée ;
- Gestion des rôles et permissions ;
- Protection des API.

Technologies :

- Spring Security ;
- JWT ;
- BCrypt pour le chiffrement des mots de passe.

Priorité :

Haute

## RNF-002 : Protection des données collectées

Description :

Les données collectées sur le terrain doivent être
protégées contre les accès non autorisés.

Mesures :

- Chiffrement des communications API avec HTTPS ;
- Protection des données locales ;
- Contrôle des permissions utilisateurs.

Priorité :

Haute 

## RNF-003 : Fonctionnement hors connexion

Description :

L'application mobile doit rester utilisable même en
absence de connexion Internet.

Contraintes :

- Les agents doivent pouvoir créer des collectes offline ;
- Les données doivent être stockées localement ;
- Les données doivent être synchronisées automatiquement
  au retour de la connexion.

Technologie :

- SQLite ;
- Drift.

Priorité :

Très haute

## RNF-004 : Performance du système

Description :

L'application doit fournir des temps de réponse rapides
pour garantir une bonne expérience utilisateur.

Objectifs :

- Chargement rapide des écrans mobiles ;
- Réponse API inférieure à quelques secondes ;
- Synchronisation optimisée.

Mesures :

- Pagination des données ;
- Compression des images ;
- Optimisation des requêtes SQL.

Priorité :

Haute

## RNF-005 : Évolutivité du système

Description :

L'architecture doit permettre l'ajout de nouvelles
fonctionnalités et l'augmentation du nombre d'utilisateurs.

Exemples d'évolution :

- Ajout de nouveaux domaines :
  - Santé ;
  - ONG ;
  - Agriculture ;
  - Enquêtes.

- Ajout futur :
  - Intelligence artificielle ;
  - Cartographie avancée ;
  - Rapports automatiques.

Technologies :

- Architecture modulaire Spring Boot ;
- API REST ;
- Services séparés.

Priorité :

Haute

## RNF-006 : Maintenabilité du code

Description :

Le code doit respecter les bonnes pratiques de
développement afin de faciliter la maintenance.

Contraintes :

Backend :

- Architecture en couches ;
- Séparation Controller / Service / Repository ;
- Documentation API.

Mobile :

- Architecture Clean ;
- Séparation des responsabilités.

Frontend :

- Composants réutilisables.

Priorité :

Haute

## RNF-007 : Compatibilité multiplateforme

Description :

L'application mobile doit fonctionner sur les appareils
Android utilisés par les agents.

Le dashboard doit être accessible depuis un navigateur web.

Plateformes :

Mobile :

- Android

Web :

- Chrome ;
- Edge ;
- Firefox.

Priorité :

Moyenne

## RNF-008 : Historisation des données

Description :

Le système doit conserver l'historique des opérations
importantes.

Informations conservées :

- Date de création ;
- Utilisateur responsable ;
- Date de modification ;
- Historique de synchronisation ;
- Historique de validation.

Priorité :

Haute

## RNF-009 : Déploiement conteneurisé

Description :

Le système doit pouvoir être déployé facilement grâce
à la conteneurisation.

Technologies :

- Docker ;
- Docker Compose.

Services :

- Spring Boot Backend ;
- PostgreSQL ;
- Angular Dashboard.

Priorité :

Moyenne

# 8. Architecture fonctionnelle et technique

L'architecture du système est basée sur une approche
client-serveur avec une application mobile Flutter,
un backend Spring Boot et un dashboard web Angular.

Le système est conçu selon une architecture permettant
le fonctionnement en mode connecté et hors connexion.

## 8.1 Architecture générale

Le système est composé de trois applications principales :

1. Application mobile Flutter
2. Backend Spring Boot
3. Dashboard Web Angular


Architecture globale :

                    Administrateur
                         |
                         |
                Dashboard Angular
                         |
                         |
                    API REST
                         |
                         |
                Spring Boot Backend
                         |
              ---------------------
              |
              |
          PostgreSQL


       Agent / Superviseur

              |
              |
        Application Flutter

              |
              |
          SQLite Local

              |
              |
       Synchronisation API

## 8.2 Application mobile Flutter

L'application mobile est destinée aux agents terrain
et aux superviseurs.

Elle permet :

- Authentification ;
- Consultation des missions ;
- Remplissage des formulaires ;
- Stockage local des données ;
- Capture GPS ;
- Ajout de photos ;
- Synchronisation des données.

Technologies :

- Flutter ;
- Dart ;
- Riverpod ;
- Drift SQLite ;
- Dio HTTP Client.

## 8.3 Backend Spring Boot

Le backend constitue le cœur métier du système.

Responsabilités :

- Gestion des utilisateurs ;
- Gestion des rôles ;
- Gestion des collectes ;
- Validation métier ;
- Synchronisation des données ;
- Sécurité des API.


Technologies :

- Java 21 ;
- Spring Boot ;
- Spring Security ;
- Spring Data JPA ;
- PostgreSQL.

## 8.4 Dashboard Web Angular

Le dashboard web est destiné principalement aux
administrateurs.

Fonctionnalités :

- Gestion des utilisateurs ;
- Gestion des organisations ;
- Affectation des équipes ;
- Consultation des collectes ;
- Visualisation des statistiques.


Technologies :

- Angular ;
- TypeScript ;
- Angular Material.

## 8.5 Fonctionnement Offline / Online

Le système utilise une architecture Offline First.

Principe :

Les données sont d'abord enregistrées localement
sur l'appareil mobile avant d'être envoyées au serveur.

Agent

   |
   |
Formulaire collecte

   |
   |
SQLite Drift

   |
   |
Données en attente
(PENDING_SYNC)

Agent

   |
   |
Application Flutter

   |
   |
API REST Spring Boot

   |
   |
PostgreSQL

## 8.6 Processus de synchronisation

La synchronisation permet de transférer
automatiquement les données locales vers le serveur.


Processus :

1. L'application détecte une connexion Internet.

2. Elle recherche les données locales non synchronisées.

3. Elle envoie les données au backend.

4. Le backend vérifie les données.

5. Les données sont enregistrées dans PostgreSQL.

6. Le serveur retourne une confirmation.

7. Les données locales passent à l'état synchronisé.

DRAFT

  ↓

PENDING_SYNC

  ↓

SYNCING

  ↓

SYNCED

ou

ERROR

## 8.7 Extension future Intelligence Artificielle

L'architecture prévoit l'ajout d'un service IA séparé.

Ce service permettra :

- Analyse automatique des collectes ;
- Assistant conversationnel ;
- Reconnaissance vocale ;
- Génération de rapports.


Architecture future :

Angular

   |

AI Service Python

   |

Spring Boot

   |

PostgreSQL + pgvector

# 9. Modèle de données initial

La base de données utilise PostgreSQL.

Le modèle est conçu pour supporter plusieurs
organisations utilisant la plateforme.

Chaque organisation possède ses propres utilisateurs,
formulaires et collectes.

## Organisation

Une organisation représente une structure utilisant
la plateforme.

Exemples :

- ONG ;
- Hôpital ;
- Entreprise ;
- Projet d'enquête.


Attributs :

- id
- nom
- description
- date_creation
- statut

organizations

id
name
description
status
created_at

## Utilisateur

Un utilisateur représente une personne utilisant
la plateforme.

Types :

- Administrateur
- Superviseur
- Agent


Attributs :

- id
- nom
- prénom
- email
- téléphone
- mot de passe
- rôle
- organisation

users

id
first_name
last_name
email
phone
password
role_id
organization_id
status
created_at

## Rôle

Définit les permissions d'un utilisateur.

Valeurs :

ADMIN

SUPERVISOR

AGENT

roles

id
name

## Affectation des équipes

Un superviseur peut gérer plusieurs agents.

Un agent appartient à un seul superviseur.

supervisor_agents

id
supervisor_id
agent_id
created_at

Superviseur

     1
     |
     |
     *

Agents

## Formulaire

Un formulaire représente un modèle de collecte.

Exemples :

- Enquête santé ;
- Recensement ;
- Inspection terrain.

forms

id
name
description
organization_id
status
created_at

## Version Formulaire

Chaque modification crée une nouvelle version.

Exemple :

Santé V1

Santé V2

Santé V3

form_versions

id
form_id
version_number
schema_json
created_by
created_at

## Collecte

Une collecte représente une information saisie
par un agent terrain.

collectes

id
agent_id
form_version_id
data_json
latitude
longitude
status
created_at
updated_at

DRAFT

PENDING_VALIDATION

VALIDATED

REJECTED

## Validation

Permet de garder l'historique des décisions
du superviseur.

validations

id
collecte_id
supervisor_id
decision
comment
created_at

## Historique Synchronisation

Permet de suivre les données envoyées
depuis les appareils mobiles.

sync_logs

id
collecte_id
device_id
status
sync_date
error_message

ORGANIZATION
      |
      |
      *
    USERS
      |
      |
      *
    ROLES


SUPERVISOR
      |
      |
      *
    AGENTS


FORM
      |
      |
      *
FORM_VERSION
      |
      |
      *
COLLECTE
      |
      |
      *
 VALIDATION


organizations
        |
        |
users ---- roles

users(supervisor)
        |
        |
supervisor_agents
        |
        |
users(agent)


forms
 |
form_versions
 |
collectes
 |
validations
