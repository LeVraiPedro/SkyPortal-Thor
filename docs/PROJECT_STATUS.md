<!-- Copyright 2026 LeVraiPedro and SkyPortal Thor contributors -->
<!-- SPDX-License-Identifier: GPL-2.0-or-later -->

# Suivi de reprise — 5 septembre 2026

Ce document est le point de reprise courant. Les rapports V5 et les anciennes cases
de la checklist restent des preuves historiques, pas des tests de cette session.

## État Git et périmètre

- Publié : `v0.5.0`, paire stable API 3, release du 16 août 2026.
- Fusionné dans `main` : fondation V6 (#10), LED API 4 (#11), portail animé (#12),
  correction activation/keepalive (#13). `main` relevé à
  `ffc1e7158e63abf3dae4a6f08aa372c66d8f35d1`.
- Branche de reprise : `agent/v6-animated-portal-layout-fix` ; tête initiale
  `f0aa3c33a09b1dffa9983001ddb795badea8b97a`, travail local initial propre.
- [PR #14](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/14) : ouverte,
  **maintenue en brouillon : disposition vérifiée, reprise Dolphin bloquante**.
  Aucune fusion autorisée dans cette reprise.
- Aucun travail plus récent trouvé dans les PR lors de l’audit.
- Version Android conservée : `0.5.0`, code `7` ; aucun tag ni release V6 créé.
- Bifrost et les autres fonctions restent hors périmètre jusqu’à clôture de #14
  **et autorisation explicite** de la suite.

## Preuves historiques — non réexécutées implicitement

- PR #13 : correction de l’interprétation de `A 00`, validation corrective sur
  Thor/Spyro’s Adventure, puis fusion le 19 août 2026. Une commande `A` valide
  reste une activation/keepalive ; ne pas réintroduire `A 00 → Deactivate()`.
- Corps de PR #14 : observations historiques des séquences LED, RGB gauche/droite,
  portail animé, J1, emplacements supplémentaires et collection sur Thor/SSA.
- CI de la tête initiale : runs `32306937380` et `32306931515` réussis le 19 août.

## Défauts trouvés et correction de cette session

Le premier APK `final-compose` a été réellement installé en mise à jour et observé
sur la Thor. Le texte et le bandeau RGB étaient séparés, mais le panneau ne recevait
qu’environ 90 dp : les marges réservées au Canvas absorbaient sa hauteur. Le portail
était donc invisible, y compris lorsque SSA annonçait un portail prêt.

Le commit `f91e975` corrige uniquement le compagnon :

- panneau de 144 dp, cartes J1/J2 compactes et slots supplémentaires de 48 dp ;
- défilement vertical de secours si messages ou taille de texte exigent davantage ;
- en-tête et bandeau RGB mesurés séparément, Canvas dans l’espace restant et clipé ;
- reflets animés le long de l’ellipse horizontale, sans faire pivoter son axe large ;
- canal Trap absent aussi de la description accessible dans SSA et jeu inconnu ;
  conservation dans Trap Team, avec deux nouveaux tests JVM.

Le commit `dcdcfe5692067476600effd7c22f4a7b72922cba` ajoute une construction
de validation du compagnon seul. Les quatre secrets persistants et le certificat
officiel sont obligatoires, sans génération de clé de repli. Sources, licences,
SHA-256 et provenance sont joints à l’artefact. Dolphin n’est ni construit ni installé.

Le test réel de ce candidat a confirmé le retour du portail et le chargement de
Lightning Rod, puis son remplacement par Sonic Boom. La capture montrait encore
un bord supérieur rogné. `d4665368903a93435038a6ccd45356760aa6e944` ajuste
uniquement le budget géométrique pour conserver le halo entier dans le Canvas.

## Contrôles automatisés de la session

| État testé | Contrôle | Résultat |
|---|---|---|
| Tête initiale `f0aa3c3` | JVM, Lint, assembleDebug | 113 tests réussis, build réussi |
| Correctifs `dcdcfe5`, puis `d466536` (réexécutés) | JVM | 115 tests, 0 échec |
| Même code | Android Lint | 0 erreur, 16 avertissements non bloquants |
| Même code | assembleDebug | réussi |
| Code + workflow `dcdcfe5` | `python tools/check_licensing.py` | réussi, 80 sources |
| Code + workflow | `git diff --check` | réussi |
| `d466536` | Android CI push / PR | runs `33952416530` / `33952418326` réussis |
| `d466536` | Release signée + sources | run `33952416415` réussi |

Les avertissements Lint existants ne justifient pas une montée des dépendances ou
du SDK dans une correction de mise en page. Les tests JVM couvrent le modèle et
les contrats ; ils ne sont pas des captures ni des tests d’instrumentation Compose.

## Provenance des APK

### APK historique effectivement réinstallé pour diagnostic

- Run : [32306575621](https://github.com/LeVraiPedro/SkyPortal-Thor/actions/runs/32306575621).
- Commit construit : `3b4e6ea08b779597cce427f09f656beb01b633de`, **pas** `f0aa3c3`.
- Le diff intégral jusqu’à `f0aa3c3` ne retire que le workflow temporaire ; arbre
  `app` identique : `8116010cfacdf9dcca12cfef542da6def28bc442`.
- APK : `SkyPortal_Thor_API4_FinalCompose.apk`.
- SHA-256 : `07e3f27583f08152a770d30739a4e8b6a03083d3834a2999389e2444648e264e`.
- Résultat matériel actuel : défaut de hauteur reproduit, donc insuffisant pour clôturer #14.

### Premier candidat correctif

- Run : [33952012175](https://github.com/LeVraiPedro/SkyPortal-Thor/actions/runs/33952012175).
- Commit construit : `dcdcfe5692067476600effd7c22f4a7b72922cba`.
- APK : `SkyPortal_Thor_API4_LayoutValidation.apk`.
- SHA-256 : `7fc60d54cc41abd88b8f1f7de868a2bb94125f1e68ccca43848e5bf1dd9368bc`.
- Construction réussie, cinq sommes vérifiées après téléchargement, package,
  version et certificat vérifiés localement. Installation `-r` réussie.
- Lightning Rod chargé et affiché par le jeu, puis remplacé par Sonic Boom ;
  portail visible mais bord légèrement rogné, corrigé dans le candidat suivant.

### Candidat final de composition

- Run : [33952416415](https://github.com/LeVraiPedro/SkyPortal-Thor/actions/runs/33952416415).
- Commit construit : `d4665368903a93435038a6ccd45356760aa6e944`.
- Arbre `app` : `1e0c1d8042d8be40360dd072a204f18232de96c7`.
- APK : `SkyPortal_Thor_API4_LayoutValidation.apk`, 8 290 662 octets.
- SHA-256 : `6091fa3f305488a05dff3601532564ee5bcf064fe94969869d881c91b08aafbc`.
- Mode `PERSISTENT_RELEASE_KEY`, certificat officiel ci-dessous vérifié localement.
- Les cinq fichiers de sommes ont été vérifiés après téléchargement. Package
  `com.skyportalthor.app`, version `0.5.0`, code `7` vérifiés avec `aapt`.
- Installation `adb install -r` réussie à 09:30 (heure de Paris). L’APK installé
  a ensuite été extrait de l’appareil : SHA-256 identique à l’artefact GitHub.
- Les modifications ultérieures de suivi sont documentaires, pas un nouvel APK.

Certificat public officiel commun, vérifié avec `apksigner` sur le compagnon
historique et sur les deux APK installés avant mise à jour :

```text
502ae2f53a97b32a142cb11bda410a62dee5ee80af5b2d8fca2b70e05ed3229e
```

Le Dolphin conservé est `org.dolphinemu.dolphinemu`, version `54070da585`, code
`43011`, SHA-256 APK
`b90209d0f466163e193f00d220d4900c416383abc24b0cc4a522120e30dab079`.
Aucune nouvelle paire V5/API 3 n’est mélangée à cette installation API 4.

## Matériel de la session

AYN Thor, Android 13 ; écrans logiques `0` (supérieur, 1920×1080) et `4`
(inférieur, 1240×1080), vérifiés sur l’appareil. Autorisation ADB accordée localement.
SkyPortal : `com.skyportalthor.app`, 0.5.0/code 7. Diagnostic : Binder connecté,
API 4 active, permission SAF persistante lecture/écriture, 32 fichiers détectés.
Spyro’s Adventure est lancé sur l’écran supérieur ; l’utilisateur a ouvert sa partie.

Les deux APK préexistants sont conservés localement. Mise à jour par `adb install -r`
du compagnon uniquement, sans désinstallation, `pm clear`, nouvelle clé ni
modification directe des dumps. Captures et journaux bruts restent hors du dépôt,
notamment les images du jeu et les informations privées.

## Résultats matériels du candidat final `d466536`

| Scénario réellement exécuté | Résultat / limite |
|---|---|
| Disposition solo / 2J, slots occupés et messages | Portail entier visible ; texte, halo et RGB séparés ; aucun Trap SSA ; captures à plusieurs phases de l’animation |
| RGB gauche/droite | Repères et hexadécimaux lisibles, couleurs évoluant avec SSA ; aucun passage actif/veille anormal observé avant l’incident natif |
| Installation avec Sonic Boom J1 et Warnado J2 montés | Les deux montages sont retrouvés ; Dolphin n’a pas redémarré pour l’installation |
| J1, remplacements Sonic Boom → Lightning Rod → Bash → Whirlwind | Changements confirmés dans le compagnon et les slots natifs ; la séquence complète de présentation en jeu n’est pas validée de bout en bout (voir limites ci-dessous) |
| J2 Warnado retiré | Retrait confirmé ; Whirlwind ensuite seul en slot natif `#0`, identité `0/0` |
| Arrêt forcé / relance du compagnon avec Whirlwind monté | Réconciliation correcte, même processus Dolphin, un seul slot `#0 (0/0)` ; pas de second montage |
| Bouton Reconnecter puis Actualiser | Binder/API 4, SSA `SSPP52`, `RUNNING`, handshake USB et même slot confirmés |
| Équipes et Diagnostic | Dialogues ouverts et utilisables, aucune équipe créée ou supprimée |
| Emplacement supplémentaire | Ouverture du sélecteur pour Slot 3 vérifiée ; pas de montage supplémentaire dans cette session |
| Accueil / veille / retour | SkyPortal revient sur `4`, mais Cocoon reprend les écrans et le retour à l’accueil Dolphin expose le crash décrit ci-dessous : scénario global **non validé** |
| Protection après crash Dolphin | Le compagnon reste vivant, efface les slots devenus absents et refuse Lightning Rod faute de handshake USB ; aucun faux succès |

Les montages J2 ne valent pas validation d’une partie coopérative avec deux
commandes. Avec deux figurines et une seule commande active, SSA a affiché
« trop de jouets » ; ce message a disparu après retrait J2. Une interruption de
la commande Wii a également suspendu le jeu ; l’utilisateur l’a réactivée.
Whirlwind a ensuite été vu dans le jeu, mais après restauration automatique :
ne pas présenter cette image comme preuve de toute la séquence de remplacements.
J1 retrait propre et nouveau chargement/retrait J2 sur le dernier APK restent à
rejouer après résolution du blocage. Les autres jeux et Trap en jeu ne sont pas
testés dans cette session ; leur couverture JVM est distincte.

## Blocage concret — menu principal Dolphin avec émulation active

Deux crashs natifs réels du **Dolphin préexistant, non remplacé** sont établis par
Logcat et l’historique Android des sorties (`APP CRASH(NATIVE)`, `SIGTRAP`) :

1. 09:34:10, après accueil/veille et action « Dolphin en haut » ;
2. 09:37:06, lors du retour après « Exit Emulation ».

Les deux piles pointent sur :

```text
MainActivity.onPrepareOptionsMenu
→ WiiUtils.isSystemMenuInstalled (JNI)
→ IOS::HLE::Kernel
→ assertion : Core::System::GetInstance().GetIOS() == nullptr
```

La vérification du menu Wii crée un noyau IOS temporaire alors qu’un IOS existe
déjà. Le code concerné correspond au Dolphin épinglé
`54070da5851e12f2d1a4389daa528e4fb81327ce`. `DolphinLauncher.kt` et son callback
sont inchangés dans cette PR ; aucune preuve n’attribue l’assertion au Canvas.
La raison exacte pour laquelle l’IOS existe encore lors du second retour au menu
reste à analyser. Le texte « Decompressing State » apparaît dans cette séquence,
mais **aucune preuve ne désigne un save-state corrompu comme cause** : Android
recrée l’activité d’émulation après la mort du processus.

Deux erreurs `led-status failed (DeadObjectException)` correspondent à ces deux
morts Binder, sans boucle de spam. Aucun crash Java/Kotlin, ANR ou erreur SAF du
compagnon n’a été identifié dans la fenêtre examinée. Des `SecurityException`
`WRITE_SETTINGS` concernent Cocoon, pas SkyPortal ; elles ne sont pas dissimulées
ni comptées comme une validation propre de l’ensemble du parcours.

Après ces incidents, le test matériel a été arrêté ; aucun nouveau Dolphin n’a
été installé. L’émulation n’est plus active, les slots sont vides, le compagnon
est revenu en mode solo sur l’écran inférieur, avec les 32 fichiers et l’accès
SAF conservés. Pas de réinitialisation, suppression de collection ou changement
de clé. Les sauvegardes rapides créées par le système n’ont pas été supprimées.

## Prochaine action et conditions de clôture

**Faire autoriser puis traiter le défaut ciblé d’ouverture/reprise du menu Dolphin**,
sans démarrer Bifrost. Le correctif doit éviter cette requête IOS pendant une
émulation active (ou reprendre la tâche existante sans réinitialiser l’accueil),
après analyse du cycle de vie ; ne pas livrer un contournement non testé.

Ensuite, rejouer sur la Thor le parcours interrompu avec une paire de signatures
compatibles : chargement/remplacement/retrait J1, J2, accueil/veille, reprise et
Logcat sans crash. Tant que ces conditions manquent, garder #14 en brouillon.
Même après réussite, la fusion nécessite l’accord explicite de l’utilisateur.
