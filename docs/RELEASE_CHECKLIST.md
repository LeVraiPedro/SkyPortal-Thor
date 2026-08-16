# Checklist de release V5

Version candidate : **0.5.0**, `versionCode 7`. Conserver ce numéro tant qu'aucune release/tag `v0.5.0` n'existe ; passer à 0.5.1 uniquement après vérification de l'état GitHub.

## 1. Dépôt et portée

- [x] Partir de la branche V5 réelle et travailler sur `agent/v5-1-validation-release`.
- [x] Vérifier `git status`, préserver tout travail local et identifier le commit de départ.
- [x] Vérifier branches, tags et releases distants avant de modifier la version.
- [x] Confirmer qu'aucune refonte V6 n'est incluse.
- [x] Relire le diff et exclure données appareil, URI SAF, chemins PC, dumps et clés privées.

## 2. Qualité Android

- [x] `:app:testDebugUnitTest` : 31 tests réussis localement.
- [x] `:app:lintDebug` : réussi localement, aucune erreur bloquante.
- [x] `:app:assembleDebug` : réussi localement.
- [x] Rejouer les trois commandes sur l'état exact qui sera commité.
- [x] Installer l'APK final sur la Thor sans `pm clear`.
- [x] Vérifier que la collection et la permission SAF sont préservées après l'installation.
- [ ] Rejouer explicitement les régressions favoris, récents et équipes rapides sur l'APK final.

## 3. Paire Dolphin/SkyPortal

- [x] Dolphin Debug API 3 compilé et testé localement.
- [x] Certificats identiques pour la paire Debug de validation.
- [x] Recalculer les SHA-256 des artefacts finaux.
- [x] Vérifier les certificats finaux avec `apksigner verify --print-certs`.
- [ ] Ne jamais publier deux APK comme compatibles si les certificats diffèrent.
- [ ] Joindre les sources Dolphin correspondantes et respecter sa licence lors d'une distribution binaire.
- [x] Conserver la révision Dolphin épinglée ; ne pas construire silencieusement depuis une branche mouvante.

## 4. Critères matériels critiques

- [x] SkyPortal s'ouvre sur l'écran inférieur logique `4`.
- [x] Dolphin reste utilisable sur l'écran supérieur logique `0`.
- [x] Binder API 3 se connecte.
- [x] Spyro's Adventure et `SSPP52` sont détectés.
- [x] Le portail peut être activé depuis SkyPortal.
- [x] J1/J2 chargent et retirent sans faux succès ni duplication observée.
- [x] La recréation du compagnon réconcilie les slots.
- [x] La mort/reprise de Dolphin invalide puis retrouve l'état distant.
- [x] Après correction de l'initialisation service-only, aucun crash/ANR n'est observé dans le parcours rejoué.
- [x] Refus matériels de Snap Shot, Magic Log Holder et Unknown avant Binder, sans slot natif.
- [x] Chargement/retrait et backup contrôlé d'Anvil Rain ; `99_Backups` non rescanné.
- [x] Écran éteint/allumé, accueil/retour et recréation du compagnon sans doublon.
- [x] Logcat frais sans crash natif/app, ANR ni spam `DeadObjectException`.
- [ ] Rejouer équipe pendant reconnexion, retrait pendant scan et arrêt de Dolphin pendant chargement.

Ne marquer la candidate prête que si tous les critères critiques applicables sont réussis. Les jeux indisponibles n'empêchent pas la release s'ils restent clairement annoncés comme non testés matériellement et couverts automatiquement.

## 5. GitHub Actions

- [x] `.github/workflows/android-ci.yml` prépare tests, Lint, APK et rapports.
- [x] `.github/workflows/release.yml` prépare APK, SHA-256, archive source et release sur tag `v*`.
- [x] `.github/workflows/full-pair-build.yml` prépare manuellement une paire signée avec révision Dolphin épinglée.
- [x] Syntaxe des trois workflows validée localement.
- [ ] Après push, vérifier le résultat réel d'Android CI.
- [ ] Tester le workflow manuel de paire dans un environnement disposant des secrets requis.
- [ ] Ne pas créer le tag de release tant que la PR et la CI ne sont pas validées.

Secrets attendus pour une signature persistante :

```text
SKYPORTAL_KEYSTORE_BASE64
SKYPORTAL_KEYSTORE_PASSWORD
SKYPORTAL_KEY_ALIAS
SKYPORTAL_KEY_PASSWORD
```

Ils doivent être configurés dans GitHub Actions, jamais committés. Le workflow manuel peut produire une paire de test avec une clé éphémère, mais celle-ci ne doit pas être présentée comme compatible avec une installation utilisateur existante.

## 6. Pull request

- [ ] Créer des commits cohérents et relire chaque périmètre.
- [ ] Pousser `agent/v5-1-validation-release` sans réécrire l'historique partagé.
- [ ] Ouvrir une PR vers `main`, sans fusion automatique.
- [ ] Inclure résultats automatiques, résultats Thor, seul jeu réellement testé, fixtures, limites et artefacts.
- [ ] Utiliser une PR en brouillon si un critère critique reste ouvert.
- [ ] Attendre les checks GitHub et corriger tout échec reproductible.

Titre proposé :

```text
SkyPortal Thor V5.1 — validation, hardening and release automation
```

## 7. Publication

- [ ] Vérifier une dernière fois l'absence de release/tag `v0.5.0` avant de créer le tag.
- [ ] Générer les notes depuis [CHANGELOG.md](../CHANGELOG.md).
- [ ] Vérifier l'APK, l'archive source, les SHA-256 et les signatures téléchargés.
- [ ] Expliquer que le mode Smart complet exige Dolphin API 3 signé avec la même clé.
- [ ] Distinguer explicitement tests matériels, tests automatiques et limites dans les notes.
- [ ] Ne jamais publier de dump utilisateur dans les artefacts.

Documents à relire avant publication : [rapport V5](VALIDATION_V5.md), [matrice](COMPATIBILITY_MATRIX.md), [checklist Thor](../THOR_TEST_CHECKLIST.md) et [intégration Dolphin](../DOLPHIN_INTEGRATION.md).
