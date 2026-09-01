# Recette V1.1 — Checklist de test manuel de bout en bout

Ce document couvre le test combiné des 4 sprints de la V1.1 (Cartographie GPS,
Statistiques avancées, Rapports PDF, Export CSV/Excel) sur une plateforme
CollectPro fonctionnant déjà en V1 (authentification, formulaires, collectes,
validation).

**Pré-requis** : 2 comptes ADMIN_PRINCIPAL sur 2 organisations différentes
(pour les tests d'isolation multi-tenant).

**Légende** : ☐ à faire · Renseigner OK/KO + commentaire une fois testé.

---

## Phase 0 — Préparation

- [ ] Se connecter en SUPER_ADMIN, créer une organisation de test avec un logo (upload) → vérifier qu'il s'affiche bien
- [ ] Créer/vérifier qu'il existe : 1 ADMIN_PRINCIPAL, 1 SUPERVISEUR, 2-3 AGENTS, 1 formulaire publié dans cette organisation
- [ ] Affecter les agents au superviseur

## Phase 1 — Mission avec zone GPS (Sprint 3)

- [ ] Créer une mission avec : nom, formulaire(s) rattaché(s), zone GPS (lat/lng/rayon réaliste, ex. 500 m), objectif de collectes (`expectedCollectesCount`)
- [ ] Vérifier que la mission apparaît en statut `DRAFT`, puis la passer en `ACTIVE`
- [ ] Affecter 2-3 agents à la mission
- [ ] Vérifier qu'une collecte **ne peut pas** être créée sur une mission `DRAFT`/`CANCELLED`/`COMPLETED` (rejet avec message explicite)

## Phase 2 — Collectes terrain (dans et hors zone)

- [ ] Créer une collecte **à l'intérieur** de la zone GPS → `outsideMissionZone` = false/non affiché
- [ ] Créer une collecte **en dehors** du rayon → badge ⚠️ "hors zone" affiché dans la liste des collectes
- [ ] Créer une collecte **sans coordonnées GPS** → pas d'erreur, `outsideMissionZone` reste `null`, pas de badge
- [ ] Créer une collecte sur une mission d'une **autre organisation** → refusée (403 / erreur métier)

## Phase 3 — Validation et suivi (transverse)

- [ ] Superviseur valide une collecte, en rejette une autre avec commentaire
- [ ] Onglet "Carte" des collectes : marqueurs aux bonnes coordonnées, colorés selon le statut
- [ ] Onglet "Carte" des missions : cercle de zone affiché autour du bon centre GPS
- [ ] Onglet "Suivi" : barre de progression = collectes reçues / `expectedCollectesCount`, plafonnée à 100 % en cas de dépassement
- [ ] Mission **sans** objectif défini → progression affiche un état neutre (pas de division par zéro, pas de NaN%)

## Phase 4 — Statistiques avancées (Sprint 4)

- [ ] Graphique de tendance des collectes : basculer 7j / 30j / 90j → chiffres cohérents
- [ ] Widget taux de rejet par agent → les agents ayant rejeté en Phase 3 apparaissent
- [ ] Temps moyen de validation → cohérent avec les validations de la Phase 3
- [ ] SUPER_ADMIN : vue statistiques **globale** (toutes organisations)
- [ ] ADMIN_PRINCIPAL : vue limitée à **son** organisation
- [ ] SUPERVISEUR : stats limitées à **son équipe** (pas toute l'organisation)

## Phase 5 — Rapports PDF (Sprint 5)

- [ ] Générer un rapport **organisation** sans filtre de période → PDF téléchargé, en-tête avec le logo de la Phase 0
- [ ] Générer un rapport **organisation** avec période (dates début/fin) → chiffres correspondant uniquement à la période choisie
- [ ] Générer un rapport **mission** → objectif vs reçues, liste des agents assignés présents
- [ ] Historique des rapports → les 3 rapports ci-dessus apparaissent, avec date et auteur corrects
- [ ] Re-télécharger un rapport depuis l'historique (pas juste après génération) → fonctionne
- [ ] Organisation/mission **sans aucune collecte** → PDF généré proprement (pas d'erreur 500), sections vides/à zéro
- [ ] Organisation **sans logo** → en-tête neutre, pas de cassure de mise en page
- [ ] SUPERVISEUR : peut consulter/télécharger l'historique mais **ne voit pas** le bouton "Générer" (permission `GENERATE_REPORT` absente)
- [ ] AGENT : aucun accès aux endpoints `/reports/*` (403)

## Phase 6 — Export CSV/Excel (Sprint 6)

- [ ] Export CSV sans filtre → encodage correct (accents, BOM UTF-8), en-têtes de colonnes corrects
- [ ] Export Excel (.xlsx) → ouverture sans erreur dans Excel/LibreOffice, colonnes correctement typées
- [ ] Export avec filtres combinés (mission + statut + période) → seules les lignes correspondantes apparaissent
- [ ] **Sécurité** : collecte avec un champ commençant par `=`, `+`, `-` ou `@` (ex. `=CMD|'/c calc'`) → export CSV → cellule préfixée d'une apostrophe, ne s'exécute pas comme formule à l'ouverture
- [ ] Export depuis un compte d'une **autre organisation** sur l'organisation de test → refusé (403)

## Phase 7 — Cohérence croisée

- [ ] Comparer, pour la même période : totaux du widget statistiques (Phase 4), contenu du rapport PDF (Phase 5), export CSV (Phase 6) → les totaux de collectes/rejets concordent
- [ ] La collecte "hors zone" (Phase 2) apparaît de façon cohérente dans le rapport de mission / l'export (colonne GPS)

## Phase 8 — Régression V1 (non-cassure)

- [ ] Authentification, activation de compte, mot de passe oublié fonctionnent toujours
- [ ] Création/validation/rejet de collecte **sans mission rattachée** (`missionId = null`) fonctionne comme avant
- [ ] Gestion des formulaires, versions, invitations : aucune régression visible

---

## Bilan de la recette

| Phase | Résultat | Anomalies relevées |
|---|---|---|
| 0. Préparation | | |
| 1. Mission GPS | | |
| 2. Collectes terrain | | |
| 3. Validation & suivi | | |
| 4. Statistiques avancées | | |
| 5. Rapports PDF | | |
| 6. Export CSV/Excel | | |
| 7. Cohérence croisée | | |
| 8. Régression V1 | | |

**Décision finale** : ☐ V1.1 validée pour mise en production ☐ Corrections nécessaires avant validation
