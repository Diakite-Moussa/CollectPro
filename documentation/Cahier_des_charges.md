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

Les objectifs spécifiques du projet sont :

- Développer une application mobile Flutter permettant aux
  Agents terrain de réaliser des collectes d'informations.
- Permettre aux Agents de travailler sans connexion Internet
  grâce au stockage local des données.
- Synchroniser automatiquement les données collectées avec
  le serveur lorsque la connexion Internet est disponible.
- Mettre en place une architecture multi-organisation
  permettant à plusieurs structures d'utiliser la même
  plateforme tout en garantissant l'isolation de leurs
  données.
- Mettre en place une gestion des utilisateurs basée sur
  plusieurs niveaux de responsabilité : Super Admin, Admin
  principal, Admin secondaire, Superviseur et Agent.
- Permettre au Super Admin de gérer les organisations depuis
  son dashboard.
- Permettre à l'Admin principal de gérer les utilisateurs et
  les fonctionnalités de son organisation.
- Permettre aux Administrateurs autorisés de créer des Agents
  et des Superviseurs et de les affecter aux équipes.
- Permettre aux Superviseurs de suivre les Agents qui leur
  sont affectés et de contrôler la qualité des collectes.
- Assurer la traçabilité des données collectées grâce à
  l'historique des synchronisations, des modifications et
  des validations.
- Permettre la gestion et la version des formulaires de
  collecte.
- Fournir un dashboard web Angular adapté aux différents
  rôles administratifs.
- Préparer l'intégration d'un moteur d'analyse par
  intelligence artificielle ainsi que d'un assistant IA
  textuel et vocal.
- Fournir une architecture évolutive permettant l'ajout de
  nouvelles organisations, de nouveaux domaines d'utilisation
  et de nouvelles fonctionnalités.

# 4. Périmètre du projet

## 4.1 Fonctionnalités incluses dans la Version 1

### Application mobile Flutter

- Authentification ;
- Activation du compte par invitation ;
- Réinitialisation du mot de passe ;
- Consultation des missions attribuées ;
- Consultation des formulaires ;
- Remplissage des formulaires ;

> Note de conception : en V1, une « mission » ne constitue
> pas une entité distincte en base de données. Elle
> correspond à l'ensemble des formulaires publiés que
> l'organisation (et, le cas échéant, le superviseur) a rendu
> accessibles à l'agent. « Consulter les missions attribuées »
> et « Consulter les formulaires disponibles » recouvrent donc
> la même donnée côté modèle (voir section 9, entité Form /
> FormVersion). Une entité Mission dédiée (avec zone, échéance,
> agents assignés) pourra être introduite en V1.1 si le besoin
> de suivi de mission dépasse la simple mise à disposition de
> formulaires.
- Création de collectes ;
- Modification des collectes non encore validées ;
- Ajout de photos et documents ;
- Capture de la position GPS ;
- Fonctionnement hors connexion ;
- Stockage local avec SQLite/Drift ;
- Synchronisation automatique ;
- Gestion des erreurs de synchronisation ;
- Indication de l'état de synchronisation des données.

### Backend Spring Boot

- Authentification et autorisation ;
- Gestion des rôles et permissions ;
- Gestion des organisations ;
- Gestion des utilisateurs ;
- Gestion de l'activation des comptes ;
- Gestion de la récupération du mot de passe ;
- Gestion des invitations ;
- Gestion des formulaires ;
- Gestion des versions de formulaires ;
- Gestion des collectes ;
- Gestion des affectations Agents/Superviseurs ;
- Gestion de la validation des collectes ;
- Gestion de la synchronisation ;
- Gestion de l'historique et des journaux ;
- API REST sécurisées.

### Dashboard Web Angular

Le dashboard Angular est une application unique dont les
fonctionnalités et les écrans sont adaptés au rôle de
l'utilisateur.

**Dashboard Super Admin**

- Gestion des organisations ;
- Création du premier Admin principal d'une organisation ;
- Activation et désactivation des organisations ;
- Consultation des statistiques globales ;
- Suivi global de l'activité de la plateforme ;
- Gestion des paramètres globaux.

**Dashboard Admin principal**

- Gestion de son organisation ;
- Création des Administrateurs secondaires ;
- Création des Superviseurs ;
- Création des Agents ;
- Affectation des Agents aux Superviseurs ;
- Gestion des formulaires ;
- Consultation des collectes ;
- Consultation des statistiques ;
- Gestion des invitations ;
- Désactivation des comptes selon ses permissions.

**Dashboard Admin secondaire**

- Gestion des utilisateurs selon les permissions qui lui
  sont attribuées ;
- Consultation des collectes ;
- Gestion des fonctionnalités autorisées.

**Dashboard Superviseur**

- Consultation de ses Agents ;
- Suivi des activités de son équipe ;
- Consultation des collectes ;
- Validation des collectes ;
- Rejet des collectes avec commentaire ;
- Consultation des informations nécessaires à la
  supervision.

## 4.2 Fonctionnalités de la Version 1.1

- Cartographie GPS avancée ;
- Rapports automatiques ;
- Export avancé des données ;
- Statistiques avancées ;
- Amélioration des outils de suivi des missions.

## 4.3 Fonctionnalités de la Version 2

- Assistant IA textuel ;
- Assistant IA vocal ;
- Réponse vocale de l'assistant ;
- Analyse intelligente des collectes ;
- Génération automatique de rapports par IA ;
- Système de communication intelligent avec les Agents et
  Superviseurs ;
- Escalade automatique vers un administrateur lorsqu'une
  question ne peut pas être résolue par l'IA.

# 5. Acteurs du système

Le système comporte cinq rôles principaux.

## 5.1 Super Administrateur

Le Super Administrateur est responsable de l'administration
globale de la plateforme.

Ses responsabilités sont :

- créer les organisations ;
- modifier les informations des organisations ;
- activer ou désactiver une organisation ;
- créer le premier Administrateur principal d'une
  organisation ;
- consulter les statistiques globales ;
- superviser l'activité globale de la plateforme ;
- gérer les paramètres globaux ;
- récupérer ou transférer la responsabilité administrative
  d'une organisation lorsque cela est nécessaire.

Le Super Administrateur dispose de son propre dashboard web
Angular.

## 5.2 Administrateur principal

L'Administrateur principal est le premier administrateur créé
pour une organisation par le Super Administrateur.

Ses responsabilités sont :

- gérer son organisation ;
- créer des Administrateurs secondaires ;
- créer des Superviseurs ;
- créer des Agents ;
- affecter les Agents aux Superviseurs ;
- gérer les formulaires de son organisation ;
- consulter les collectes ;
- consulter les statistiques ;
- gérer les invitations ;
- désactiver les comptes des utilisateurs de son organisation
  selon ses permissions.

Une organisation possède un seul Administrateur principal
actif.

## 5.3 Administrateur secondaire

L'Administrateur secondaire est un utilisateur disposant de
droits d'administration au sein d'une organisation.

Ses responsabilités dépendent des permissions qui lui sont
attribuées.

Il peut notamment :

- gérer les Agents ;
- gérer les Superviseurs ;
- consulter les collectes ;
- gérer certaines fonctionnalités administratives ;
- consulter les statistiques autorisées.

Un Administrateur secondaire ne peut accéder qu'aux données
de son organisation.

## 5.4 Superviseur

Le Superviseur est responsable du suivi d'une équipe
d'Agents.

Ses responsabilités sont :

- consulter les Agents qui lui sont affectés ;
- suivre l'avancement des missions ;
- consulter les collectes réalisées par son équipe ;
- valider les collectes ;
- rejeter les collectes ;
- fournir un commentaire lors d'un rejet ;
- suivre l'activité de son équipe.

Le Superviseur peut également intervenir sur le terrain et
utiliser l'application mobile lorsque cela est nécessaire à
ses missions.

Cependant, le Superviseur ne dispose pas de la permission
CREATE_COLLECTE.

Il peut accéder au dashboard web et aux fonctionnalités
mobiles autorisées par son rôle.

## 5.5 Agent

L'Agent est chargé de réaliser les collectes sur le terrain.

Ses responsabilités sont :

- recevoir son invitation d'activation ;
- définir son mot de passe ;
- activer son compte ;
- se connecter à l'application mobile ;
- consulter les missions attribuées ;
- consulter les formulaires ;
- créer des collectes ;
- modifier les collectes autorisées ;
- ajouter des informations terrain ;
- prendre des photos ;
- enregistrer la localisation GPS ;
- sauvegarder les données hors connexion ;
- synchroniser les données lorsque la connexion est
  disponible.

## 5.6 Hiérarchie administrative

La plateforme adopte une hiérarchie permettant de séparer
l'administration globale de la plateforme de l'administration
de chaque organisation.

La hiérarchie est la suivante :

```
Super Administrateur
        ↓
    Organisation
        ↓
Administrateur principal
        ↓
Administrateurs secondaires
        ↓
    Superviseurs
        ↓
       Agents
```

Le Super Administrateur crée une organisation et son premier
Administrateur principal.

L'Administrateur principal assure ensuite la gestion
quotidienne de son organisation.

Il peut créer plusieurs Administrateurs secondaires,
Superviseurs et Agents.

Les utilisateurs d'une organisation ne peuvent accéder qu'aux
données et fonctionnalités autorisées de cette organisation.

Une organisation possède un seul Administrateur principal
actif.

Plusieurs Administrateurs secondaires peuvent être créés dans
une même organisation.

Un Agent peut être affecté à un seul Superviseur et un
Superviseur peut gérer plusieurs Agents.

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

Le système doit permettre à chaque utilisateur de se
connecter à la plateforme après activation de son compte.

Acteurs concernés :

- Super Administrateur ;
- Administrateur principal ;
- Administrateur secondaire ;
- Superviseur ;
- Agent.

Règles :

- Un compte INVITED ne peut pas se connecter.
- Un compte DISABLED ne peut pas se connecter.
- L'accès aux fonctionnalités dépend du rôle et des
  permissions de l'utilisateur.
- L'utilisateur est automatiquement limité à son organisation
  lorsque le rôle l'exige.

Priorité :

Très haute.

---

## RF-002 : Gestion des rôles et permissions

Description :

Le système doit gérer plusieurs rôles avec des niveaux
d'accès différents.

Rôles :

- SUPER_ADMIN ;
- ADMIN_PRINCIPAL ;
- ADMIN_SECONDAIRE ;
- SUPERVISOR ;
- AGENT.

Le système doit également permettre d'associer des
permissions aux rôles.

Exemples de permissions :

- CREATE_ORGANIZATION ;
- CREATE_USER ;
- UPDATE_USER ;
- DISABLE_USER ;
- CREATE_FORM ;
- UPDATE_FORM ;
- PUBLISH_FORM ;
- CREATE_COLLECTE ;
- VIEW_COLLECTE ;
- VALIDATE_COLLECTE ;
- REJECT_COLLECTE ;
- GENERATE_REPORT ;
- VIEW_STATISTICS.

Priorité :

Très haute.

## RF-003 : Gestion des organisations

Description :

Le Super Administrateur doit pouvoir gérer les organisations
utilisant la plateforme.

Fonctionnalités :

- Créer une organisation ;
- Modifier une organisation ;
- Consulter une organisation ;
- Activer une organisation ;
- Désactiver une organisation ;
- Consulter les statistiques d'une organisation ;
- Créer le premier Administrateur principal de
  l'organisation.

Règles :

- Une organisation doit posséder un Administrateur principal
  actif.
- Le Super Administrateur est le seul acteur pouvant créer
  une nouvelle organisation.
- Les utilisateurs d'une organisation ne peuvent pas accéder
  aux données d'une autre organisation.

Informations :

- Nom ;
- Description ;
- Informations générales ;
- Statut ;
- Date de création.

Acteur :

SUPER_ADMIN.

Priorité :

Très haute.

---

## RF-004 : Création des comptes utilisateurs

Description :

Le système doit permettre aux administrateurs autorisés de
créer les comptes des utilisateurs de leur organisation.

Création par rôle :

Le Super Administrateur peut créer :

- un Administrateur principal lors de la création d'une
  organisation.

L'Administrateur principal peut créer :

- des Administrateurs secondaires ;
- des Superviseurs ;
- des Agents.

Un Administrateur secondaire peut créer ou gérer les
utilisateurs uniquement lorsque les permissions nécessaires
lui sont attribuées.

Informations utilisateur :

- Nom ;
- Prénom ;
- Email ;
- Téléphone ;
- Rôle ;
- Organisation.

Le mot de passe n'est pas défini par l'administrateur.

Priorité :

Très haute.

---

## RF-005 : Affectation des Agents aux Superviseurs

Description :

Le système doit permettre à un administrateur autorisé
d'affecter les Agents aux Superviseurs.

Règles métier :

- Un Agent appartient à une seule organisation.
- Un Superviseur appartient à une seule organisation.
- Un Agent ne peut être affecté qu'à un Superviseur
  appartenant à la même organisation.
- Un Superviseur peut gérer plusieurs Agents.
- Un Agent ne peut avoir qu'un seul Superviseur principal.

Acteur :

ADMIN_PRINCIPAL ou ADMIN_SECONDAIRE disposant de la
permission appropriée.

Priorité :

Très haute.

## RF-USER : Gestion et activation des comptes utilisateurs

Description :

Les administrateurs autorisés (SUPER_ADMIN pour le premier
Admin principal d'une organisation, ADMIN_PRINCIPAL et
ADMIN_SECONDAIRE selon leurs permissions) peuvent créer les
comptes des utilisateurs de leur périmètre depuis
l'application. Lors de la création d'un compte,
l'administrateur renseigne au minimum les informations
nécessaires à l'identification de l'utilisateur ainsi que son
adresse e-mail et/ou son numéro de téléphone.

Le système ne demande pas à l'administrateur de définir le
mot de passe de l'utilisateur. Après la création du compte,
le système génère automatiquement un lien ou un code
d'activation temporaire et transmet une invitation à
l'utilisateur par e-mail et/ou SMS.

L'utilisateur doit utiliser cette invitation afin de définir
son propre mot de passe et d'activer son compte. Le lien ou
le code d'activation a une durée de validité limitée et ne
peut être utilisé qu'une seule fois. L'utilisateur ne peut
pas se connecter tant que son compte n'a pas été activé.

L'administrateur autorisé peut consulter l'état de
l'invitation et, si nécessaire, demander le renvoi d'une
nouvelle invitation.

Le système doit gérer les situations suivantes :

- invitation expirée ;
- invitation déjà utilisée ;
- invitation invalide ;
- utilisateur n'ayant pas reçu l'invitation ;
- compte désactivé par un administrateur autorisé.

États d'un compte :

INVITED → ACTIVE → DISABLED

En cas d'expiration de l'invitation, le compte reste INVITED :
une nouvelle invitation peut ensuite être générée afin de
permettre l'activation du compte. INVITATION_EXPIRED n'est
pas un statut permanent du compte, mais un état transitoire
de l'invitation elle-même.

Exigences :

| ID | Exigence |
|----|----------|
| RF-USER-01 | Un administrateur autorisé peut créer un Admin secondaire, un Superviseur ou un Agent ; le Super Admin peut créer un Admin principal |
| RF-USER-02 | Le mot de passe n'est pas défini par l'administrateur |
| RF-USER-03 | Le système génère un mécanisme d'activation temporaire |
| RF-USER-04 | Le système envoie une invitation par e-mail et/ou SMS |
| RF-USER-05 | L'utilisateur peut définir son mot de passe |
| RF-USER-06 | L'utilisateur peut activer son compte |
| RF-USER-07 | Le lien d'activation expire après une durée définie |
| RF-USER-08 | Un lien d'activation ne peut être utilisé qu'une seule fois |
| RF-USER-09 | Un administrateur autorisé peut renvoyer une invitation |
| RF-USER-10 | Un compte non activé ne peut pas se connecter |
| RF-USER-11 | Un administrateur autorisé peut désactiver un compte |
| RF-USER-12 | Le système informe l'utilisateur en cas d'échec d'activation |

Acteurs :

Super Administrateur, Administrateur principal, Administrateur
secondaire, Superviseur, Agent.

Priorité :

Très haute.

---

## RF-006 : Réinitialisation du mot de passe

Description :

Le système doit permettre à un utilisateur actif de
réinitialiser son mot de passe lorsqu'il l'a oublié.

Fonctionnement :

1. L'utilisateur sélectionne « Mot de passe oublié ».
2. Il fournit son adresse e-mail ou son numéro de téléphone.
3. Le système vérifie les informations fournies.
4. Le système génère un mécanisme temporaire de
   réinitialisation.
5. Le système transmet un lien ou un code par un canal
   vérifié.
6. L'utilisateur définit un nouveau mot de passe.
7. Le mécanisme de réinitialisation est invalidé.

Règles :

- Le mécanisme possède une durée de validité limitée.
- Il ne peut être utilisé qu'une seule fois.
- L'ancien mot de passe n'est jamais communiqué.
- L'administrateur ne connaît jamais le nouveau mot de passe.
- Un administrateur autorisé peut déclencher une nouvelle
  procédure de récupération d'accès pour un utilisateur sans
  connaître son mot de passe.

Acteurs :

Tous les utilisateurs authentifiables.

Priorité :

Haute.

---

## RF-007 : Création d'une collecte

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

## RF-008 : Sauvegarde locale des collectes

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

## RF-009 : Synchronisation automatique

Description :

Le système doit envoyer automatiquement les données
localisées sur le téléphone vers le serveur lorsque
la connexion Internet est disponible.

Fonctionnement :

Mode offline :

```
Application mobile
        |
      SQLite
```

Mode online :

```
SQLite
   |
API REST
   |
PostgreSQL
```

Acteur :

Agent

Priorité :

Haute

---

## RF-010 : Gestion des erreurs de synchronisation

Description :

Le système doit gérer les problèmes pendant la
synchronisation.

Exemples :

- Perte de connexion ;
- Données invalides ;
- Doublons.

Priorité :

Moyenne

## RF-011 : Consultation des collectes

Description :

Le superviseur doit pouvoir consulter les collectes
réalisées par les agents de son équipe.

Acteur :

Superviseur

Priorité :

Haute

---

## RF-012 : Validation des collectes

Description :

Le superviseur doit pouvoir accepter ou refuser
une collecte.

Décision du superviseur (ValidationDecision) :

- VALIDEE
- REJETEE

Cette décision est enregistrée dans l'entité Validation ; elle
fait passer la collecte concernée de PENDING_VALIDATION à
VALIDATED ou REJECTED (voir CollecteStatus, section 9).

En cas de rejet :

Le superviseur doit fournir un commentaire.

Acteur :

Superviseur

Priorité :

Haute

## RF-013 : Dashboards Web par rôle

Description :

Le système doit fournir un dashboard web Angular dont les
fonctionnalités sont adaptées au rôle de l'utilisateur.

**Dashboard Super Admin**

- Gestion des organisations ;
- Gestion des Administrateurs principaux ;
- Statistiques globales ;
- Suivi global de la plateforme ;
- Paramètres globaux.

**Dashboard Admin principal**

- Gestion de son organisation ;
- Gestion des utilisateurs ;
- Gestion des formulaires ;
- Gestion des collectes ;
- Affectation des équipes ;
- Statistiques ;
- Rapports.

**Dashboard Admin secondaire**

- Gestion des fonctionnalités autorisées ;
- Gestion des utilisateurs selon ses permissions ;
- Consultation des données autorisées.

**Dashboard Superviseur**

- Consultation des Agents ;
- Suivi des équipes ;
- Consultation des collectes ;
- Validation et rejet des collectes.

Les données affichées doivent être limitées à l'organisation
et aux permissions de l'utilisateur.

Acteur :

SUPER_ADMIN, ADMIN_PRINCIPAL, ADMIN_SECONDAIRE, SUPERVISOR.

Priorité :

Très haute.

## RF-014 : Gestion des formulaires

Description :

Le système doit permettre aux administrateurs autorisés de
créer et gérer les formulaires de collecte de leur
organisation.

Fonctionnalités :

- Créer un formulaire ;
- Modifier un formulaire ;
- Ajouter ou supprimer des champs ;
- Créer une nouvelle version ;
- Publier une version ;
- Désactiver un formulaire ;
- Consulter l'historique des versions.

Règles :

- Chaque formulaire appartient à une organisation.
- Chaque modification importante crée une nouvelle version.
- Une collecte conserve la version du formulaire utilisée
  lors de sa création.
- La publication d'une nouvelle version ne modifie pas les
  anciennes collectes.
- Les Agents et Superviseurs reçoivent uniquement les
  formulaires auxquels ils sont autorisés.

Acteur :

ADMIN_PRINCIPAL ou ADMIN_SECONDAIRE disposant de la
permission appropriée.

Priorité :

Très haute.

## RF-015 : Communication avec les utilisateurs

Description :

Le système pourra permettre aux administrateurs de
communiquer avec les Agents et les Superviseurs depuis le
dashboard.

Fonctionnalités prévues :

- envoyer un message ;
- recevoir un message ;
- répondre à un message ;
- consulter l'historique des conversations ;
- transmettre une question à un administrateur lorsqu'elle
  ne peut pas être traitée automatiquement.

Cette fonctionnalité pourra être enrichie par l'assistant IA
dans une version ultérieure.

Priorité :

Basse (fonctionnalité future).

## RF-016 : Assistant IA textuel et vocal

Description :

Le système pourra intégrer un assistant IA dans le dashboard
des administrateurs.

L'assistant pourra :

- répondre aux questions des administrateurs ;
- analyser les données de collecte ;
- rechercher des informations dans les données autorisées ;
- fournir des synthèses ;
- générer des rapports ;
- répondre sous forme textuelle ;
- répondre sous forme vocale ;
- permettre à l'administrateur d'utiliser une interaction
  vocale.

L'assistant pourra également communiquer avec les Agents et
les Superviseurs selon les permissions définies.

Lorsque l'assistant ne dispose pas d'une réponse fiable ou
suffisante, il doit informer l'utilisateur et, lorsque cela
est prévu, transmettre la question à un administrateur
humain.

Cette fonctionnalité est prévue pour une version ultérieure
du système.

Priorité :

Basse (fonctionnalité future).

# 6bis. Règles métier

### Règles liées aux organisations

RB-ORG-01
Une organisation est créée par le Super Administrateur.

RB-ORG-02
Lors de la création d'une organisation, le Super
Administrateur crée son premier Administrateur principal.

RB-ORG-03
Une organisation possède un seul Administrateur principal
actif.

RB-ORG-04
L'Administrateur principal peut créer plusieurs
Administrateurs secondaires.

RB-ORG-05
Un Administrateur secondaire appartient obligatoirement à
une organisation.

RB-ORG-06
Un utilisateur ne peut accéder qu'aux données de son
organisation, sauf le Super Administrateur qui possède une
vision globale autorisée.

RB-ORG-07
Un Administrateur ne peut pas créer d'utilisateur dans une
autre organisation.

RB-ORG-08
Un Agent ne peut être affecté qu'à un Superviseur appartenant
à la même organisation.

RB-ORG-09
Un Agent possède un seul Superviseur principal.

RB-ORG-10
Un Superviseur peut gérer plusieurs Agents.

RB-ORG-11
Le Superviseur ne possède pas la permission CREATE_COLLECTE.

RB-ORG-12
L'Agent possède la permission CREATE_COLLECTE.

RB-ORG-13
L'Administrateur principal ne peut pas désactiver son propre
compte.

RB-ORG-14
En cas d'indisponibilité de l'Administrateur principal, la
responsabilité peut être transférée à un Administrateur
secondaire autorisé.

### Règles liées aux comptes utilisateurs

RB-USER-01
Un utilisateur nouvellement créé possède initialement le
statut INVITED.

RB-USER-02
Un utilisateur INVITED ne peut pas se connecter.

RB-USER-03
Une invitation possède une durée de validité limitée.

RB-USER-04
Une invitation ne peut être utilisée qu'une seule fois.

RB-USER-05
Après définition du mot de passe, le compte passe à ACTIVE.

RB-USER-06
Un compte DISABLED ne peut pas se connecter.

RB-USER-07
L'Administrateur autorisé peut renvoyer une invitation.

RB-USER-08
Un utilisateur peut demander une réinitialisation de son mot
de passe.

RB-USER-09
Un mécanisme de réinitialisation expiré ou déjà utilisé est
invalide.

RB-USER-10
Le mot de passe de l'utilisateur n'est jamais communiqué à un
administrateur.

# 7. Besoins non fonctionnels

Les besoins non fonctionnels définissent les contraintes
techniques et les critères de qualité que le système doit
respecter.

## RNF-001 : Sécurité des accès

Description :

Le système doit garantir que chaque utilisateur accède
uniquement aux fonctionnalités et données correspondant à
son rôle, ses permissions et son organisation.

Mécanismes :

- Authentification sécurisée ;
- Autorisation basée sur les rôles et permissions ;
- Isolation des organisations ;
- Protection des API ;
- Gestion sécurisée des sessions ;
- JWT et Refresh Token ;
- BCrypt pour les mots de passe ;
- HTTPS pour les communications ;
- Protection contre les accès inter-organisations ;
- Journalisation des opérations sensibles.

Technologies :

- Spring Security ;
- JWT ;
- BCrypt ;
- HTTPS/TLS.

Priorité :

Très haute.

## RNF-001bis : Isolation des organisations

Description :

Le système doit garantir l'isolation des données entre les
différentes organisations utilisant la plateforme.

Les utilisateurs d'une organisation ne doivent pas pouvoir
consulter, modifier ou supprimer les données d'une autre
organisation.

Cette règle doit être appliquée au niveau du backend et des
API.

Le Super Administrateur dispose d'un accès global contrôlé
pour administrer la plateforme.

Priorité :

Très haute.

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

## RNF-010 : Sécurité des tokens

Description :

Les tokens d'activation et de réinitialisation de mot de
passe doivent être protégés.

Le système doit :

- générer des tokens aléatoires et suffisamment complexes ;
- limiter leur durée de validité ;
- empêcher leur réutilisation ;
- invalider les tokens après utilisation ;
- éviter de stocker les tokens en clair lorsque cela est
  possible ;
- journaliser les opérations importantes liées aux
  invitations et réinitialisations.

Priorité :

Haute

# 8. Architecture fonctionnelle et technique

L'architecture du système est basée sur une approche
client-serveur avec une application mobile Flutter,
un backend Spring Boot et un dashboard web Angular.

Le système est conçu selon une architecture permettant
le fonctionnement en mode connecté et hors connexion.

## 8.1 Architecture générale

Le système est composé de trois applications principales :

1. Application mobile Flutter ;
2. Backend Spring Boot ;
3. Dashboard Web Angular.

Architecture globale :

```
Super Admin
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
     |----------------- PostgreSQL
     |
     |----------------- Service Email
     |
     |----------------- Service SMS
     |
     |----------------- Services IA futurs


Admin principal / Admin secondaire
     |
     |
Dashboard Angular
     |
     |
  API REST


Superviseur
     |
     |---- Dashboard Angular
     |
     |---- Application Flutter


Agent
     |
     |
Application Flutter
     |
     |
SQLite / Drift
     |
     | Synchronisation
     |
  API REST
     |
     |
Spring Boot
     |
     |
PostgreSQL
```

## 8.2 Application mobile Flutter

L'application mobile Flutter est principalement destinée aux
Agents terrain et peut également être utilisée par les
Superviseurs lorsqu'ils effectuent des missions sur le
terrain.

Les fonctionnalités sont contrôlées selon le rôle.

**Agent**

- Authentification ;
- Consultation des missions ;
- Consultation des formulaires ;
- Création de collectes ;
- Modification des collectes autorisées ;
- Capture GPS ;
- Ajout de photos ;
- Ajout de documents ;
- Stockage local ;
- Synchronisation automatique.

**Superviseur**

- Authentification ;
- Consultation des missions autorisées ;
- Consultation des informations nécessaires à la
  supervision ;
- Consultation des collectes ;
- Fonctionnalités terrain autorisées ;
- Synchronisation.

Le Superviseur ne possède pas la permission CREATE_COLLECTE.

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

Le dashboard web Angular est une application unique dont les
fonctionnalités sont contrôlées selon le rôle et les
permissions de l'utilisateur.

**Super Admin**

Le Super Admin possède un dashboard permettant :

- de gérer les organisations ;
- de créer les Admins principaux ;
- de consulter les statistiques globales ;
- de suivre l'activité de la plateforme ;
- de gérer les paramètres globaux.

**Administrateur principal**

L'Admin principal possède un dashboard permettant :

- de gérer son organisation ;
- de gérer les utilisateurs ;
- de gérer les formulaires ;
- de consulter les collectes ;
- de gérer les équipes ;
- de consulter les statistiques ;
- de générer des rapports.

**Administrateur secondaire**

L'Admin secondaire possède un dashboard limité aux
fonctionnalités correspondant à ses permissions.

**Superviseur**

Le Superviseur possède un dashboard permettant :

- de consulter ses Agents ;
- de suivre leur activité ;
- de consulter les collectes ;
- de valider ou rejeter les collectes.

Technologies :

- Angular ;
- TypeScript ;
- Angular Material ;
- Guards d'autorisation ;
- Services HTTP REST.

## 8.5 Fonctionnement Offline / Online

Le système utilise une architecture Offline First.

Principe :

Les données sont d'abord enregistrées localement
sur l'appareil mobile avant d'être envoyées au serveur.

```
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
```

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

```
DRAFT
  ↓
PENDING_SYNC
  ↓
SYNCING
  ↓
SYNCED
ou
ERROR
```

## 8.7 Extension future Intelligence Artificielle

L'architecture prévoit l'ajout d'un service IA séparé.

Ce service permettra :

- Analyse automatique des collectes ;
- Assistant conversationnel ;
- Reconnaissance vocale ;
- Génération de rapports.

Architecture future :

```
Angular
   |
AI Service Python
   |
Spring Boot
   |
PostgreSQL + pgvector
```

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

```
organizations

id
name
description
status
created_at
```

## Utilisateur

Un utilisateur représente une personne utilisant
la plateforme.

Types :

- Super Administrateur
- Administrateur principal
- Administrateur secondaire
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

```
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
updated_at
```

Chaque utilisateur appartenant à une organisation possède un
`organization_id`. Le Super Admin constitue une exception
fonctionnelle puisqu'il dispose d'un accès global à la
plateforme et n'est donc pas rattaché à une organisation
spécifique.

Statuts possibles (UserStatus) :

```
INVITED
ACTIVE
DISABLED
```

## Rôle

Définit les permissions d'un utilisateur.

Valeurs :

```
SUPER_ADMIN
ADMIN_PRINCIPAL
ADMIN_SECONDAIRE
SUPERVISOR
AGENT
```

```
roles

id
name
```

## Permission

Représente une autorisation précise pouvant être associée à
un rôle ou à un utilisateur (notamment pour les
Administrateurs secondaires, dont les droits sont
personnalisables).

```
permissions

id
code
description
```

```
role_permissions

id
role_id
permission_id
```

Pour permettre la personnalisation des droits d'un
Administrateur secondaire évoquée en section 5.3, une
permission peut également être accordée directement à un
utilisateur, en complément de celles héritées de son rôle.

```
user_permissions

id
user_id
permission_id
granted_by
created_at
```

Les permissions effectives d'un utilisateur correspondent à
l'union des permissions de son rôle (`role_permissions`) et
de ses permissions individuelles (`user_permissions`). Ce
mécanisme concerne principalement les Administrateurs
secondaires ; il reste disponible pour les autres rôles si un
besoin similaire apparaît.

## Token d'activation

Permet à un utilisateur invité de définir son mot de passe
et d'activer son compte.

Un utilisateur peut recevoir plusieurs invitations (une par
renvoi), d'où la relation 1 → 0..*.

```
activation_tokens

id
user_id
token_hash
expires_at
used_at
created_at
```

## Token de réinitialisation du mot de passe

Le système utilise un mécanisme temporaire permettant à un
utilisateur de définir un nouveau mot de passe lorsqu'il a
oublié l'ancien.

```
password_reset_tokens

id
user_id
token_hash
expires_at
used_at
created_at
```

Relation :

```
User 1 ───────── 0..* PasswordResetToken
```

Un utilisateur peut demander plusieurs réinitialisations au
cours du temps, mais chaque token ne peut être utilisé qu'une
seule fois.

## Affectation des équipes

Un superviseur peut gérer plusieurs agents.

Un agent appartient à un seul superviseur.

```
supervisor_agents

id
supervisor_id
agent_id
created_at
```

```
Superviseur
     1
     |
     |
     *
Agents
```

## Formulaire

Un formulaire représente un modèle de collecte.

Exemples :

- Enquête santé ;
- Recensement ;
- Inspection terrain.

```
forms

id
name
description
organization_id
status
created_at
```

## Version Formulaire

Chaque modification crée une nouvelle version.

Exemple :

Santé V1

Santé V2

Santé V3

```
form_versions

id
form_id
version_number
schema_json
created_by
created_at
```

## Collecte

Une collecte représente une information saisie
par un agent terrain.

```
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
```

Statuts possibles (CollecteStatus) — cycle complet couvrant
à la fois la synchronisation (côté mobile) et la validation
(côté superviseur), conformément au diagramme de classe et à
la section 8.6 :

```
DRAFT
PENDING_SYNC
SYNCING
SYNCED
PENDING_VALIDATION
VALIDATED
REJECTED
ERROR
```

## Validation

Permet de garder l'historique des décisions
du superviseur.

```
validations

id
collecte_id
supervisor_id
decision
comment
created_at
```

## Historique Synchronisation

Permet de suivre les données envoyées
depuis les appareils mobiles.

```
sync_logs

id
collecte_id
device_id
status
sync_date
error_message
```

## Historique d'administration (Audit)

Permet de tracer les opérations sensibles réalisées sur la
plateforme, en complément du RNF-008 (historisation).

```
audit_logs

id
user_id
organization_id
action
entity_type
entity_id
created_at
details
```

Exemples de valeurs pour `action` :

- ADMIN_CREATED
- USER_DISABLED
- FORM_PUBLISHED
- COLLECTE_VALIDATED
- ORGANIZATION_CREATED
- ROLE_CHANGED
- INVITATION_SENT
- ACCOUNT_ACTIVATED
- PASSWORD_RESET_COMPLETED

## Schéma relationnel synthétique

```
ORGANIZATION
      |
      |
      *
    USERS
      |
      |
      *
    ROLES
      |
      |
      *
 PERMISSIONS


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
```

```
organizations
        |
        |
users ---- roles ---- permissions

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
```

## Relation Organisation → Administrateur principal

```
Organization
    |
    | 1
    ▼
Admin Principal
    |
    | 0..*
    ▼
Admin secondaires
```

Une organisation ne peut avoir qu'un seul Admin principal
actif.

L'Admin principal ne peut pas désactiver son propre compte.
