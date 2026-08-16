# Checklist de release V5

Version validée pour publication : **0.5.0**, `versionCode 7`.

## 1. Dépôt et portée

- [x] Partir de la branche V5 réelle et travailler sur `agent/v5-1-validation-release`.
- [x] Vérifier `git status`, préserver tout travail local et identifier le commit de départ.
- [x] Vérifier branches, tags et releases distants avant de modifier la version.
- [x] Confirmer qu'aucune refonte V6 n'est incluse.
- [x] Relire le diff et exclure données appareil, URI SAF, chemins PC, dumps et clés privées.

## 2. Qualité Android

- [x] `:app:testDebugUnitTest` : 75 tests réussis localement, dont le parsing et les décisions des nouvelles preuves USB, l'ancien JSON API 3, le conflit Disney Infinity, le schéma natif v2, la bascule de cible, la migration des préférences (y compris ses entrées vides héritées) et le chemin dégradé API 1/2.
- [x] `:app:lintDebug` : réussi localement, aucune erreur bloquante.
- [x] `:app:assembleDebug` : réussi localement.
- [x] Rejouer les trois commandes sur l'état exact qui sera commité.
- [x] Campagne initiale : la candidate antérieure au correctif USB a été installée sur la Thor sans `pm clear`.
- [x] Campagne initiale : la collection et la permission SAF ont été préservées après cette installation.
- [x] Installer et vérifier le nouveau binaire USB/montage sur la Thor sans effacer les données.
- [ ] Rejouer explicitement les régressions favoris, récents et équipes rapides sur l'APK final.

## 3. Paire Dolphin/SkyPortal

- [x] Campagne initiale : Dolphin Debug API 3 compilé et testé localement.
- [x] Campagne initiale : certificats identiques pour la paire Debug de validation.
- [x] Correctifs USB/montage : sources natives compilées, trois tests ARM64 exécutés, patchs vérifiés et parcours ciblé validé sur la Thor.
- [x] Recalculer les SHA-256 des nouveaux artefacts finaux.
- [x] Vérifier les certificats des nouveaux artefacts avec `apksigner verify --print-certs`.
- [x] Les deux APK Release candidats ont exactement le même certificat persistant, vérifié avec `apksigner`.
- [x] Le workflow a produit l'arbre source Dolphin correspondant complet et le kit de reconstruction avec licences et provenance.
- [x] Conserver la révision Dolphin épinglée ; ne pas construire silencieusement depuis une branche mouvante.

## 4. Critères matériels critiques — résultats historiques

- [x] SkyPortal s'ouvre sur l'écran inférieur logique `4`.
- [x] Dolphin reste utilisable sur l'écran supérieur logique `0`.
- [x] Binder API 3 se connecte.
- [x] Spyro's Adventure et `SSPP52` sont détectés.
- [x] Pendant la campagne initiale, le portail a pu être activé depuis SkyPortal.
- [x] Pendant la campagne initiale, J1/J2 ont chargé et retiré sans faux succès ni duplication observée.
- [x] La recréation du compagnon réconcilie les slots.
- [x] La mort/reprise de Dolphin invalide puis retrouve l'état distant.
- [x] Après correction de l'initialisation service-only, aucun crash/ANR n'est observé dans le parcours rejoué.
- [x] Refus matériels de Snap Shot, Magic Log Holder et Unknown avant Binder, sans slot natif.
- [x] Chargement/retrait et backup contrôlé d'Anvil Rain ; `99_Backups` non rescanné.
- [x] Écran éteint/allumé, accueil/retour et recréation du compagnon sans doublon.
- [x] Campagne initiale : Logcat frais sans crash natif/app, ANR ni spam `DeadObjectException`.
- [ ] Rejouer équipe pendant reconnexion, retrait pendant scan et arrêt de Dolphin pendant chargement.

### Correctif USB — validation matérielle finale

La cause du premier bug a été confirmée puis le scénario volontaire a été rejoué avec la paire Release exacte :

- [x] Les deux bases actives produisent `PORTAL_CONFLICT`, jamais `READY`.
- [x] Le conflit bloque l'activation automatique et le chargement avant Binder, avec un message français demandant un arrêt complet de l'émulation.
- [x] Disney Infinity désactivé puis émulation relancée : présence, attachement et handshake passent à `true`, puis seulement l'en-tête affiche `Portail prêt`.
- [ ] Un portail configuré mais non attaché demande un redémarrage au lieu d'annoncer un faux succès.
- [x] Le parcours ciblé J1, double remplacement, retrait et reconnexion reste fonctionnel avec les nouveaux indicateurs.
- [x] Un Logcat frais du parcours ne contient ni crash, ni ANR, ni erreur Binder/USB inattendue.

Les tests automatisés et les builds peuvent valider la logique et l'intégration statique, mais ils ne permettent pas de cocher ces six points matériels.

Ne marquer la candidate prête que si tous les critères critiques applicables sont réussis. Les jeux indisponibles n'empêchent pas la release s'ils restent clairement annoncés comme non testés matériellement et couverts automatiquement.

## 5. GitHub Actions

- [x] `.github/workflows/android-ci.yml` prépare tests, Lint, APK et rapports.
- [x] `.github/workflows/release.yml` prépare APK, SHA-256, archive source et release sur tag `v*`.
- [x] `.github/workflows/full-pair-build.yml` prépare manuellement une paire signée avec révision Dolphin épinglée.
- [x] Syntaxe des trois workflows validée localement.
- [x] Android CI de `main` réussie sur le commit candidat.
- [x] Workflow manuel de paire réussi en mode `PERSISTENT_RELEASE_KEY`.
- [ ] Ne pas créer le tag de release tant que la PR et la CI ne sont pas validées.

Secrets attendus pour une signature persistante :

```text
SKYPORTAL_KEYSTORE_BASE64
SKYPORTAL_KEYSTORE_PASSWORD
SKYPORTAL_KEY_ALIAS
SKYPORTAL_KEY_PASSWORD
```

Ils doivent être configurés dans GitHub Actions, jamais committés. Le workflow manuel peut produire une paire de test avec une clé éphémère, mais celle-ci ne doit pas être présentée comme compatible avec une installation utilisateur existante.

## 6. Licence et code source correspondant

- [x] `LICENSE`, `LICENSES/GPL-2.0-or-later.txt` et `NOTICE.md` sont présents et cohérents.
- [x] Les sources originales SkyPortal portent `SPDX-License-Identifier: GPL-2.0-or-later`.
- [x] Les avis Dolphin et Apache-2.0 existants sont conservés sans réattribution.
- [x] La CI vérifie les documents de licence, les en-têtes SPDX et l’absence de données interdites suivies par Git.
- [x] L’archive source du compagnon contient `LICENSE` et `NOTICE.md`.
- [x] Le workflow de paire produit un code source Dolphin complet ainsi qu’un kit de reconstruction avec commit amont, patchs, ajouts, script et instructions.
- [x] Les quatre secrets de signature sont configurés dans GitHub sans exposer leurs valeurs et ne sont pas suivis par Git.
- [x] Le lot candidat contient le source Dolphin correspondant complet et son SHA-256 ; il sera joint durablement à la release avec l'APK.
- [ ] Faire relire les attributions et la portée de la licence par une personne compétente avant la première publication sous cette licence.
- [ ] Ne pas republier silencieusement les anciennes archives v0.3/v0.4 dépourvues de licence ; publier un correctif explicitement identifié si elles doivent rester distribuées.

## 7. Pull request

- [ ] Créer des commits cohérents pour le correctif USB et relire chaque périmètre.
- [ ] Pousser le correctif sur `agent/v5-1-validation-release` sans réécrire l'historique partagé.
- [x] PR #2 ouverte vers `main`, sans fusion automatique.
- [ ] Inclure résultats automatiques, résultats Thor, seul jeu réellement testé, fixtures, limites et artefacts.
- [ ] Utiliser une PR en brouillon si un critère critique reste ouvert.
- [ ] Attendre les nouveaux checks GitHub du correctif et corriger tout échec reproductible.

Titre proposé :

```text
SkyPortal Thor V5.1 — validation, hardening and release automation
```

## 8. Publication

- [ ] Vérifier une dernière fois l'absence de release/tag `v0.5.0` avant de créer le tag.
- [ ] Générer les notes depuis [CHANGELOG.md](../CHANGELOG.md).
- [ ] Vérifier l'APK, l'archive source, les SHA-256 et les signatures téléchargés.
- [ ] Expliquer que le mode Smart complet exige Dolphin API 3 signé avec la même clé.
- [ ] Distinguer explicitement tests matériels, tests automatiques et limites dans les notes.
- [ ] Ne jamais publier de dump utilisateur dans les artefacts.

Documents à relire avant publication : [rapport V5](VALIDATION_V5.md), [matrice](COMPATIBILITY_MATRIX.md), [checklist Thor](../THOR_TEST_CHECKLIST.md) et [intégration Dolphin](../DOLPHIN_INTEGRATION.md).
