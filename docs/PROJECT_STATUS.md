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
- Correctif Dolphin courant poussé sur cette branche :
  `11353ca7cabf28bc4dccbfbefa0593fb321def2f` ; construction de paire réussie,
  Dolphin mis à jour ; revalidation matérielle avec le compagnon `d466536`
  conservé, revalidée en partie le 5 septembre, de 12:16 à 12:33 (Paris).
- [PR #14](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/14) : ouverte,
  **validation matérielle ciblée achevée ; revue et accord de fusion attendus**.
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
SHA-256 et provenance sont joints à l’artefact. Dolphin n’est ni construit ni installé
par ce workflow de validation du compagnon seul.

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
| `23af6d0` | JVM local | 119 tests, 0 échec ; dont quatre gardes structurelles du correctif de menu |
| `23af6d0` | Android Lint local | 0 erreur, 16 avertissements non bloquants |
| `23af6d0` | assembleDebug et contrôle de licence locaux | réussis |
| `23af6d0` | Pile des patchs sur la base Dolphin épinglée | réversion complète puis réapplication réussies sur un arbre contrôlé ; voir la limite de réexécution ci-dessous |
| `23af6d0` | Construction complète de paire | run `33953904485` annulé volontairement avant candidat afin d’inclure le second correctif ; ce n’est pas un échec de compilation |
| `11353ca` | JVM local | 120 tests, 0 échec ; cinq gardes structurelles du correctif de cycle de vie |
| `11353ca` | Android Lint local | 0 erreur, 16 avertissements non bloquants |
| `11353ca` | assembleDebug et contrôle de licence locaux | réussis |
| `11353ca` | Pile complète des patchs JNI + Kotlin | réversion et réapplication recontrôlées avec succès sur la base Dolphin épinglée |
| `11353ca` | Construction complète de paire | run `33954214843` réussi en 31 min 32 s, compilation native Dolphin complète réussie |
| Artefacts `33954214843` | Contrôle après téléchargement | six empreintes du manifeste et CRC des deux ZIP valides ; certificat commun officiel et mode persistant confirmés |
| `6ddd72a` + documentation seulement | JVM, Lint, Debug réexécutés après le parcours matériel avec `--rerun-tasks` | 120 tests, 0 échec/erreur ; Lint 0 erreur/16 avertissements ; build réussi en 56 s, 56 tâches exécutées |
| Même état | Contrôle de licence | réussi, 81 sources ; aucun code de production modifié |

Lors de cette dernière exécution locale, une première commande a échoué avant les
tests car le SDK n’était pas déclaré dans son environnement. `ANDROID_HOME` a été
positionné uniquement pour la commande suivante, puis les trois tâches ont été
réexécutées intégralement avec succès ; aucun chemin privé n’a été ajouté au dépôt.

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
- Les modifications de suivi qui ont immédiatement suivi ce candidat étaient
  documentaires. Les correctifs Dolphin `23af6d0` puis `11353ca` ont ensuite
  produit un nouveau Dolphin, dont les essais sont distingués ci-dessous.

Certificat public officiel commun, vérifié avec `apksigner` sur le compagnon
historique et sur les deux APK installés avant mise à jour :

```text
502ae2f53a97b32a142cb11bda410a62dee5ee80af5b2d8fca2b70e05ed3229e
```

Le Dolphin préexistant utilisé pour ces essais est `org.dolphinemu.dolphinemu`, version `54070da585`, code
`43011`, SHA-256 APK
`b90209d0f466163e193f00d220d4900c416383abc24b0cc4a522120e30dab079`.
Aucune nouvelle paire V5/API 3 n’est mélangée à cette installation API 4.

### Dolphin correctif installé avec le compagnon final conservé

- Run : [33954214843](https://github.com/LeVraiPedro/SkyPortal-Thor/actions/runs/33954214843),
  réussi en 31 min 32 s sur `11353ca7cabf28bc4dccbfbefa0593fb321def2f`.
- Mode `PERSISTENT_RELEASE_KEY`, certificat officiel commun ci-dessus vérifié.
- Les six empreintes du manifeste et les CRC des deux ZIP téléchargés ont été
  vérifiés. L’archive source contient 37 890 entrées, le kit 28 entrées.
- Le source complet contient le lecteur JNI commun aux trois requêtes WiiUtils
  et le `else if` d’EmulationFragment ; le kit contient le nouveau patch.
- Seul `Dolphin_SkyPortal_API4.apk` a été installé par `adb install -r`, à
  10:36:58 ; 21 965 375 octets, package `org.dolphinemu.dolphinemu`, version
  `54070da585`, code `43011` conservés.
- SHA-256 Dolphin :
  `6443c72981e1ab3419abdfbfb655d3b54add91219457f5feac8c75636fb94ee0`.
  L’APK réextrait de la Thor est identique par SHA-256 à l’artefact téléchargé.
- Le compagnon réellement utilisé reste le candidat de composition `d466536`,
  SHA-256 `6091fa3f305488a05dff3601532564ee5bcf064fe94969869d881c91b08aafbc`.
  Il n’a pas été réinstallé lors de cette mise à jour Dolphin.
- Le compagnon généré par le run de paire `33954214843` n’a pas été installé :
  ne pas présenter cet APK comme testé sur matériel, même si son certificat est compatible.

## Matériel de la session

AYN Thor, Android 13 ; écrans logiques `0` (supérieur, 1920×1080) et `4`
(inférieur, 1240×1080), vérifiés sur l’appareil. Autorisation ADB accordée localement.
SkyPortal : `com.skyportalthor.app`, 0.5.0/code 7. Diagnostic : Binder connecté,
API 4 active, permission SAF persistante lecture/écriture, 32 fichiers détectés.
Spyro’s Adventure est lancé sur l’écran supérieur ; l’utilisateur a ouvert sa partie.

Les deux APK préexistants sont conservés localement. Jusqu’aux incidents décrits
ci-dessous, mise à jour par `adb install -r` du compagnon uniquement, sans désinstallation, `pm clear`, nouvelle clé ni
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

À ce premier point d’arrêt après les incidents, aucun nouveau Dolphin n’avait
été installé. L’émulation n’était plus active, les slots étaient vides, le compagnon
était revenu en mode solo sur l’écran inférieur, avec les 32 fichiers et l’accès
SAF conservés. Pas de réinitialisation, suppression de collection ou changement
de clé. Les sauvegardes rapides créées par le système n’ont pas été supprimées.

## Correctif Dolphin autorisé — candidat `11353ca`

L’utilisateur a ensuite autorisé le correctif ciblé du menu Dolphin. Cette
extension de périmètre traite le blocage natif observé ; elle n’autorise ni
Bifrost, ni une nouvelle release, ni une fusion automatique de la PR #14.

Le premier commit `23af6d04693867a8eab8048ef1353935ff2c5b4b` ajoute le patch séparé
`dolphin-patch/android-menu-lifecycle.patch` dans `Source/Android/jni/WiiUtils.cpp` :

- les trois requêtes de menu Wii utilisent un lecteur commun limité au cœur
  entièrement arrêté ; elles retournent un résultat indisponible pendant l’émulation ;
- une vérification avant puis sous `Core::CPUThreadGuard` protège la création
  du noyau IOS temporaire face à un démarrage concurrent ;
- un TMD indisponible est vérifié avant lecture de ses champs ;
- le protocole du portail, le correctif `A 00` et le contrat API 4 sont conservés ;
- le patch est appliqué après les patchs Smart Portal et LED, contrôlé par les
  licences, haché dans la provenance et exigé dans le kit de reconstruction.

L’analyse a également identifié un second défaut dans `EmulationFragment.kt` :
après retour de `NativeLibrary.Run(...)` d’une session restaurée depuis l’état
temporaire, un `if` indépendant pouvait enchaîner un nouveau démarrage. Le commit
`11353ca7cabf28bc4dccbfbefa0593fb321def2f` transforme cette suite en `else if`,
rendant les chemins de restauration et de lancement neuf mutuellement exclusifs.
La sortie d’une session restaurée ne doit donc plus lancer une nouvelle session
dans la même exécution. Le même fichier `android-menu-lifecycle.patch` contient
les changements JNI et Kotlin.

Les cinq nouveaux tests JVM contrôlent la structure du patch et sa distribution.
Ils n’exécutent ni le code C++ de Dolphin, ni le cycle de vie Android. La première
construction [33953904485](https://github.com/LeVraiPedro/SkyPortal-Thor/actions/runs/33953904485)
a été annulée volontairement avant candidat pour intégrer ce second correctif :
elle n’est pas classée comme échec de compilation. La nouvelle construction
[33954214843](https://github.com/LeVraiPedro/SkyPortal-Thor/actions/runs/33954214843)
sur `11353ca` a réussi, y compris la compilation native complète. Après contrôle
des artefacts et du certificat officiel, seul Dolphin a été mis à jour ; les
APK réellement utilisés sont identifiés ci-dessus. Les premiers contrôles
d’installation, de diagnostic et de retour au menu depuis l’écran-titre réussissent.
Les scénarios en partie, retrait, reconnexion et sortie d’une session restaurée
étaient alors en attente ; leur revalidation distincte figure ci-dessous.

### Premiers contrôles après mise à jour Dolphin

- API 4, Spyro’s Adventure `SSPP52`, état `RUNNING` et les trois preuves USB
  présence/attachement/protocole à `true`, confirmés par le diagnostic.
- Les 16 slots natifs sont libres ; les 32 fichiers et les droits SAF sont
  conservés, sans suppression de données.
- Deux avertissements signalent les fichiers Wii SSL `clientca.pem` et
  `clientcakey.pem` manquants. Ils ont été acquittés individuellement avec OK ;
  l’option d’ignorance générale des alertes n’a pas été activée.
- Une `DeadObjectException` à 10:36:59 accompagne le remplacement attendu du
  processus Dolphin pendant l’installation. Elle doit être distinguée des
  erreurs éventuelles du parcours de revalidation suivant.

### Retour au menu vérifié depuis l’écran-titre uniquement

À 10:42, avec SSA sur son écran-titre, état `RUNNING`, portail prêt et aucun
personnage monté, l’action « Dolphin en haut » a ouvert `MainActivity` au-dessus
d’`EmulationActivity`. Le bouton Retour Android a ramené le même écran-titre,
dans le même processus Dolphin, sans nouveau démarrage de l’émulation.

Le Logcat de 10:37 à 10:43 ne contient aucune nouvelle erreur fatale, assertion,
ANR, `SecurityException` ou `DeadObjectException`. Un seul événement `Running`
est relevé à 10:37:28, correspondant au lancement initial. L’historique des
sorties Android conserve la mise à jour de package de 10:36:58 comme sortie la
plus récente ; les crashs natifs historiques de 09:34 et 09:37 sont inchangés.

Ce contrôle valide l’ouverture du menu et le retour depuis l’écran-titre ; il
ne valide pas ces actions avec un personnage monté ou pendant une partie, ni la
sortie d’une session restaurée. Aucun personnage n’a encore été chargé avec ce
nouveau Dolphin. L’action demandée à l’utilisateur est d’ouvrir sa partie SSA
depuis l’écran-titre, puis de confirmer que le chargement de sauvegarde est terminé.

### Reconstruction : checkout Dolphin neuf obligatoire

Exécuter `tools/apply_dolphin_patch.py` sur un **checkout neuf** de la révision
épinglée `54070da5851e12f2d1a4389daa528e4fb81327ce`. Le script complet n’est
pas idempotent sur un arbre déjà passé en API 4 : un nouvel essai dans cette
session a échoué, car le contrôle inverse du patch de base ne correspond plus
à son état après le patch LED. Le script avait déjà recopié l’overlay du service
de base avant cet échec, donc cet arbre partiellement modifié ne doit pas servir
à une construction sans remise en cohérence.

L’arbre de travail contrôlé a été réparé, puis toute la pile a été réversée et
réappliquée avec succès sur la base épinglée. Cela valide la pile de patchs,
**pas** la réexécution du script complet sur une installation de sources API 4.
Le workflow de paire utilise un checkout neuf. Aucune correction générale de
cette limite du script n’entre dans le chantier actuel ; pour reproduire le
build, repartir également d’un checkout neuf, sans toucher aux données de la Thor.

## Revalidation en partie — 5 septembre, 12:16–12:33 (Paris)

Reprise à `6ddd72a1e3456e7dcca6d73a236d532e37f89c26`, arbre propre avant
actualisation documentaire ; `main` distant toujours `ffc1e71`. L’utilisateur a
ouvert sa partie SSA. Aucun APK supplémentaire n’a été installé pendant ce
parcours : compagnon `d466536` (SHA-256 `6091fa3f…`) et Dolphin `11353ca`
(`6443c729…`), identifiés intégralement plus haut. Les changements de branche
postérieurs à leurs sources n’affectent pas leur code de production respectif.

### Observations réelles

- J1 : Lightning Rod, remplacé par Sonic Boom puis Bash ; chaque personnage
  est apparu dans la partie avec son nom, pas seulement dans le compagnon.
  Retrait Bash : retour effectif à la demande de figurine du jeu.
- « Dolphin en haut » avec Lightning Rod monté : bibliothèque, puis Retour
  Android à la même partie, même processus et même personnage, sans nouveau boot.
- Arrêt forcé de SkyPortal avec Bash monté : après relance sur l’écran 4,
  Bash reste en J1 et le diagnostic ne compte qu’un slot natif `#0 (4/0)`.
  Le processus Dolphin est inchangé ; aucun second montage constaté.
- Mode 2J : Warnado chargé depuis J2, présentation du personnage observée dans
  SSA ; J1 vide, unique slot natif `#0 (2/0)`. Son retrait vide J2, puis retour
  au mode solo. Il s’agit du slot logique J2 du compagnon, **pas d’une partie
  coopérative à deux commandes** ; le jeu attribue la seule figurine au joueur actif.
- Équipes, Diagnostic, collection de 32 fichiers et sélecteur Slot 3 accessibles.
  Aucun objet ni montage supplémentaire n’a été ajouté pour cet essai.
- Portail visible en solo/2J, slots vides/occupés et avec messages ; aucune
  superposition texte/Canvas/RGB ni cristal/badge Trap SSA. Couleurs et valeurs
  gauche/droite lisibles et évolutives, sans alternance actif/veille anormale observée.
- Accueil, extinction (`Asleep` vérifié), rallumage et réouverture : Dolphin sur
  l’écran logique 0 et SkyPortal sur 4 ; Binder et portail retrouvés, même processus
  Dolphin. SSA affiche toutefois une interruption de commande Wii après la veille.
  Ce problème de commande est distingué du portail ; la restauration suivante
  retrouve ensuite la demande de figurine. Aucun réglage de commande n’a été changé.

### Restauration Android et sortie — chemin réellement exercé

Après retrait de toutes les figurines, fermeture de SkyPortal pour libérer Binder,
puis accueil Android : `EmulationActivity` est `STOPPED`, avec `mHaveState=true`.
`am kill` arrête le processus Dolphin **en arrière-plan**, sans effacer sa tâche.
Réouverture de la même carte dans Récents : processus neuf et capture de l’OSD
**« Loaded State from temp.sav »**, avec la scène restaurée. Cette preuve distingue
la restauration réussie d’une simple tentative « Decompressing State ».

La sortie « Exit Emulation » revient à la bibliothèque, garde le nouveau processus
et laisse SkyPortal connecté avec « Aucun jeu ». Aucun redémarrage spontané pendant
plus de 30 secondes. Le message debug Kotlin du chemin de restauration n’est pas
émis en Release : il n’est pas revendiqué comme preuve. Une nouvelle session SSA
a ensuite été lancée volontairement depuis la bibliothèque.

### Mort de Dolphin et reconnexion

À 12:30:16, arrêt forcé de Dolphin avec Lightning Rod monté sur l’écran-titre.
Le service redémarre automatiquement 0,31 s plus tard ; le slot passe vide, pas
faussement actif. Après réouverture de Dolphin et lancement volontaire de SSA,
le diagnostic retrouve `SSPP52`, `RUNNING`, USB présent/attaché/protocole à `true`
et les **16 slots libres**, sans action Reconnecter et sans remontage automatique.
La dernière sortie normale depuis cette session neuve revient également au menu.

### Logcat, limites et état laissé sur la console

- Fenêtre 12:16–12:33 : aucun `FATAL EXCEPTION`, signal fatal, ANR, assertion,
  `SecurityException`, `DeadObjectException` ou erreur `SkyPortalBridge` relevé.
- Trois événements natifs `Running` : restauration à 12:27:35, lancement volontaire
  à 12:28:51, puis lancement volontaire après arrêt forcé à 12:30:44. Aucun boot
  supplémentaire après la sortie restaurée. L’historique des sorties Android
  ajoute seulement les arrêts demandés ; les deux crashs de 09:34/09:37 restent
  historiques et n’ont pas récidivé avec le correctif.
- Avertissements SSL Wii manquants toujours présents au démarrage neuf, acquittés
  individuellement sans désactiver les alertes. Configuration réseau non modifiée.
- Réserve d’interface : une fiche d’actions déjà ouverte conserve le nom de l’ancien
  personnage si Dolphin meurt. Le slot de fond est correctement vidé et fermer la
  fiche rétablit l’affichage. Audit du code : Backup vérifie à nouveau URI/montage
  avant copie ; Retirer utilise le slot logique réconcilié, pas l’ancien index natif.
  Un autre client qui remplacerait ce slot pendant l’ouverture pourrait néanmoins
  rendre le libellé trompeur pour Retirer. Rafraîchir/fermer ces fiches est un suivi
  séparé ; ce scénario multi-client n’a pas été testé sur matériel.
- Aucun backup, jeu autre que SSA, objet/Trap, coop à deux commandes ou Bifrost
  n’est revendiqué validé par cette revalidation ciblée. Pas de mesure exhaustive
  de progression : seules les opérations normales de Dolphin ont touché les dumps.
- État final : Dolphin à sa bibliothèque sur 0, SkyPortal en solo sur 4, slots
  vides, collection de 32 fichiers conservée. Aucun effacement, désinstallation,
  changement de clé ou installation nouvelle pendant ce parcours. Captures et
  journaux contenant des données privées restent hors dépôt et PR.

## Prochaine action et conditions de clôture

**Obtenir l’accord explicite de fusion de #14**, après revue des preuves et succès
de la CI sur la tête documentaire finale. Le parcours ciblé de composition et de
cycle de vie Dolphin est terminé ; les réserves ci-dessus ne sont pas dissimulées.
Aucune fusion automatique. Bifrost et toute nouvelle fonctionnalité nécessitent
ensuite une autorisation distincte, même si #14 est fusionnée.
