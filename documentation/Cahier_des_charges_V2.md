# Cahier des charges — Version 2
## Assistant IA vocal exécuteur de tâches

Ce document complète le *Cahier des charges fonctionnel* (V1/V1.1) et détaille
la fonctionnalité annoncée en section 6 (RF-016) : un assistant IA que
l'utilisateur pilote **à la voix**, qui **comprend la demande, exécute
l'action correspondante dans CollectPro, puis répond oralement** du résultat.

---

## 1. Principe général

Parcours utilisateur cible :

```
1. L'utilisateur appuie sur le bouton micro (dashboard ou mobile).
2. Il énonce sa demande en langage naturel.
3. Le terminal (navigateur ou app mobile) transcrit la voix en
   texte localement — aucun audio n'est envoyé au backend.
4. Le texte est envoyé au backend, où le moteur IA interprète
   l'intention et choisit une action (function calling).
5. L'action est exécutée via les services CollectPro existants,
   avec les permissions réelles de l'utilisateur connecté.
6. Le backend renvoie une réponse texte ; le terminal la
   restitue à l'oral (synthèse vocale locale) et à l'écrit.
```

Point clé n°1 : **le STT et le TTS sont réalisés entièrement côté
client** (navigateur pour le dashboard, app pour le mobile). Le
backend ne reçoit et ne renvoie **que du texte** — jamais d'audio.
Cela simplifie fortement l'architecture (pas de service STT/TTS à
héberger, pas de fichiers audio à stocker ou à transmettre) et
réduit la surface de données sensibles côté serveur.

Point clé n°2 : **l'IA n'a pas de pouvoir propre**. Elle agit *au nom
de* l'utilisateur connecté, via les mêmes contrôles d'accès (rôles,
permissions, organisation) que le reste de la plateforme. Elle ne
contourne jamais RNF-001 / RNF-001bis.

---

## 2. Périmètre fonctionnel par rôle

L'assistant ne propose que les tâches déjà permises par le rôle de
l'utilisateur (mêmes permissions que RF-002). Exemples concrets :

| Rôle | Exemples de commandes vocales | Action backend déclenchée |
|---|---|---|
| Agent | « Crée une nouvelle collecte pour le formulaire Recensement » | Ouvre `CreateCollecteRequest` pré-rempli, guide le remplissage |
| Agent | « Quel est l'état de synchronisation de mes collectes ? » | `GET /sync-logs` (résumé oral) |
| Superviseur | « Valide la dernière collecte de Mamadou » | `POST /collectes/{id}/validate` |
| Superviseur | « Combien de collectes sont en attente pour mon équipe ? » | `GET /collectes?status=PENDING_VALIDATION` |
| Admin principal | « Génère le rapport PDF de l'organisation pour le mois dernier » | `POST /reports` (type ORGANIZATION) |
| Admin principal | « Crée un agent Fatou Traoré avec l'email f.traore@... » | `POST /users` |
| Super Admin | « Quelles organisations ont le plus de rejets ce mois-ci ? » | `GET /statistics` (agrégation globale) |

Toute commande hors du périmètre autorisé du rôle doit être refusée par
l'assistant avec une explication orale claire (pas d'erreur technique brute).

---

## 3. Exigences fonctionnelles détaillées

### RF-016 : Assistant IA vocal exécuteur de tâches *(remplace/complète l'ancien RF-016)*

| ID | Exigence |
|----|----------|
| RF-016-01 | L'utilisateur peut démarrer une écoute vocale via un bouton dédié (dashboard et/ou mobile). |
| RF-016-02 | Le terminal (navigateur ou app mobile) transcrit la voix en texte localement (Speech-to-Text côté client), sans transmettre d'audio au serveur. |
| RF-016-03 | Le système identifie l'intention de l'utilisateur et la ou les actions correspondantes (function calling / tool use). |
| RF-016-04 | Le système n'exécute une action que si l'utilisateur dispose de la permission requise ; sinon il refuse et l'explique oralement. |
| RF-016-05 | Avant toute action **sensible ou irréversible** (désactiver un compte, rejeter une collecte, créer un utilisateur...), le système demande une confirmation orale ou visuelle explicite. |
| RF-016-06 | Le système exécute l'action via les services métier existants (mêmes règles de validation que l'UI classique — aucun raccourci qui contournerait les contrôles). |
| RF-016-07 | Le terminal restitue la réponse du backend à l'oral via une synthèse vocale locale (Text-to-Speech côté client), confirmant le résultat (succès, échec, données demandées). |
| RF-016-08 | Le système affiche également un résumé textuel de l'échange et de l'action réalisée (accessibilité, traçabilité, environnements bruyants). |
| RF-016-09 | Chaque action déclenchée par l'assistant est journalisée dans `audit_logs` avec une origine `SOURCE = AI_ASSISTANT`. |
| RF-016-10 | En cas d'incompréhension ou d'ambiguïté, l'assistant pose une question de clarification plutôt que de deviner. |
| RF-016-11 | En cas d'échec technique (API indisponible, erreur métier), l'assistant l'annonce clairement sans exposer de détails techniques bruts. |
| RF-016-12 | L'assistant fonctionne dans au moins la langue principale de l'organisation (FR prioritaire ; EN en option V2.1) ; la langue de reconnaissance/synthèse vocale du navigateur ou de l'app doit être configurée en conséquence. |
| RF-016-13 | L'historique de conversation d'une session est conservé le temps de la session (contexte multi-tour), sans être mélangé entre utilisateurs. |
| RF-016-14 | L'assistant est indisponible en mode offline mobile (nécessite une connexion pour joindre le backend, même si le STT/TTS local ne le nécessite pas) ; il l'indique explicitement à l'agent. |
| RF-016-15 | Si le navigateur ou l'appareil ne supporte pas la reconnaissance/synthèse vocale (compatibilité Web Speech API notamment), l'assistant bascule automatiquement en mode texte (saisie + affichage), sans bloquer l'utilisateur. |

Priorité : **Haute** (fonctionnalité phare de la V2, mais dépendante de la
disponibilité réseau et d'un service externe).

---

## 4. Architecture technique proposée

```
Utilisateur (voix)
      |
      | audio
      v
Client (Angular / Flutter)
      |
      | 1. audio ---------------> Service STT (ex: Whisper API)
      |                                  |
      |                                  v
      |                            texte transcrit
      |
      | 2. texte ---------------> AI Orchestrator (backend)
      |                                  |
      |                    [LLM + function calling]
      |                                  |
      |                     choisit un "outil" =
      |                     endpoint métier existant
      |                     (CollecteService, MissionService,
      |                      ReportService, UserService, ...)
      |                                  |
      |                     vérifie permissions RBAC
      |                     (SecurityAuthorizationService)
      |                                  |
      |                     exécute l'action via les
      |                     services Spring existants
      |                                  |
      |                                  v
      |                          résultat structuré
      |
      | 3. texte réponse <---------------|
      v
Service TTS (ex: ElevenLabs / API TTS)
      |
      v
Audio de réponse -> Utilisateur
```

### Nouveau composant backend : `AiAssistantController` / `AiAssistantService`

- Reçoit le texte transcrit + le contexte utilisateur (JWT existant).
- Appelle un LLM avec **function calling**, où chaque "fonction" exposée au
  modèle correspond à **une action déjà existante et sécurisée** du backend
  (ex : `validateCollecte`, `createUser`, `generateReport`, `getStatistics`).
- Le LLM ne reçoit **jamais** un accès direct à la base de données : il ne
  fait que proposer quel outil appeler avec quels paramètres ; c'est le
  backend Spring qui exécute réellement l'action, avec les mêmes contrôles
  `@PreAuthorize` que les endpoints REST classiques.
- Retourne une réponse texte formatée pour la synthèse vocale.

### Composants externes suggérés (à confirmer selon budget/latence)
- **STT** : Whisper (API OpenAI) ou solution embarquée si contrainte de coût.
- **LLM function calling** : API Claude ou GPT (le choix n'affecte pas
  l'architecture ci-dessus, qui reste "outils = endpoints existants").
- **TTS** : API TTS du même fournisseur ou service dédié (ElevenLabs, Azure
  Speech...).

---

## 5. Sécurité et permissions

- L'assistant s'exécute **avec le token JWT de l'utilisateur connecté** —
  jamais avec un compte "super-pouvoir".
- Chaque action proposée par le LLM est revalidée côté backend par les
  mêmes règles `@PreAuthorize` / `SecurityAuthorizationService` que le reste
  de l'application (aucune confiance aveugle accordée au LLM).
- Les actions sensibles (RF-016-05) exigent une confirmation explicite.
- Aucune donnée d'une autre organisation ne peut être exposée au LLM
  (le contexte envoyé au modèle est filtré par organisation avant l'appel).
- Journalisation systématique (RF-016-09) pour permettre un audit a
  posteriori de toute action initiée par l'IA.

---

## 6. Modèle de données à ajouter

```
ai_conversations
  id
  user_id
  organization_id
  started_at
  ended_at

ai_messages
  id
  conversation_id
  role            -- USER | ASSISTANT | SYSTEM
  content_text
  audio_ref       -- optionnel, référence fichier audio si conservé
  created_at

ai_actions
  id
  conversation_id
  action_type      -- ex: VALIDATE_COLLECTE, CREATE_USER, GENERATE_REPORT
  target_entity_type
  target_entity_id
  status           -- SUCCESS | REFUSED_PERMISSION | FAILED | NEEDS_CONFIRMATION
  created_at
```

Extension simple de `audit_logs` existant : ajouter une colonne
`source` (`WEB` | `MOBILE` | `AI_ASSISTANT`) pour distinguer l'origine
d'une action sans dupliquer la table.

---

## 7. Besoins non fonctionnels spécifiques

| ID | Exigence | Priorité |
|---|---|---|
| RNF-011 | Latence perçue de bout en bout (voix → action → réponse vocale) raisonnable pour un usage terrain (quelques secondes). | Haute |
| RNF-012 | Dégradation propre si le service IA externe est indisponible (message clair, retour au mode manuel). | Haute |
| RNF-013 | Aucune donnée vocale ou transcrite d'une organisation ne doit être utilisée pour entraîner un modèle tiers sans consentement explicite. | Haute |
| RNF-014 | Les coûts d'appel aux API externes (STT/LLM/TTS) doivent être maîtrisables (limitation du nombre d'échanges, timeout de session). | Moyenne |
| RNF-015 | L'assistant doit rester utilisable au clavier/à l'écrit si le micro n'est pas disponible (fallback texte). | Moyenne |

---

## 8. Proposition de périmètre MVP réaliste (contrainte de temps)

Étant donné les délais serrés, une version réduite mais démontrable est
recommandée plutôt que de couvrir tout le périmètre du RF-016 d'un coup :

**MVP recommandé (V2.0) :**
1. Un seul point d'entrée vocal, côté **dashboard** uniquement (pas mobile,
   plus complexe à cause de l'offline).
2. **3 à 5 actions** couvertes en priorité, choisies pour leur fort impact
   démo :
   - consulter des statistiques ("Combien de collectes en attente ?") ;
   - valider/rejeter une collecte (Superviseur) ;
   - générer un rapport PDF (Admin principal).
3. STT + LLM function calling + TTS via une seule API fournisseur pour
   limiter l'intégration (ex. tout via l'API OpenAI ou tout via l'API
   Claude, qui gèrent function calling nativement).
4. Confirmation obligatoire avant toute action qui modifie une donnée.
5. Journalisation minimale (`ai_actions` uniquement, `ai_conversations`
   simplifié).

**Reporté à une itération suivante (V2.1) :**
- Support mobile Flutter (contrainte offline).
- Multi-langue.
- Historique de conversation persistant et navigable.
- Auto-suggestions proactives de l'assistant.

Cette réduction de périmètre permet de livrer une démonstration
fonctionnelle et sécurisée du concept sans bloquer sur l'intégralité du
RF-016.

---

## 9. Risques et points de vigilance

- **Fiabilité du function calling** : le LLM peut mal interpréter une
  demande ambiguë → toujours prévoir une confirmation avant action
  d'écriture (RF-016-05) et un chemin de repli manuel.
- **Coût et latence des API externes** : à chiffrer avant intégration,
  surtout si utilisation en direct sur le terrain.
- **Dépendance à un fournisseur tiers** : prévoir une abstraction du
  service STT/LLM/TTS pour pouvoir en changer sans réécrire l'orchestrateur.
- **Confidentialité** : les transcriptions vocales peuvent contenir des
  données sensibles (noms, localisation) → appliquer la même isolation
  multi-organisation que le reste de la plateforme (RNF-001bis).
