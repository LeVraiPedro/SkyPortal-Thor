<!-- Copyright 2026 LeVraiPedro and SkyPortal Thor contributors -->
<!-- SPDX-License-Identifier: GPL-2.0-or-later -->

# Suivi du projet — 5 septembre 2026

Ce document est le point de reprise courant. Le chantier autorisé est désormais
V6.0 Bifrost, avec fiabilisation préalable et audit du contrat officiel terminé.
Bifrost officiel est installé, le contrôle tiers autorisé et son service démarré
pour un test STATIC isolé, dont le bleu gauche / rouge droite a été confirmé
physiquement par l’utilisateur. Les commandes SkyPortal sont maintenant reçues
dans SSA ; l’utilisateur a confirmé leur effet physique et la restitution après
OFF sur le candidat `3be0796`. Les autres interruptions restent distinctes. La campagne PR #14
ci-dessous est conservée comme historique ; elle ne valide pas les changements
du nouveau chantier. Les rapports V5 et leurs cases de checklist restent également
des preuves historiques.

## État Git et périmètre

- Publié : `v0.5.0`, paire stable API 3, release du 16 août 2026.
- Fusionné dans `main` : fondation V6 (#10), LED API 4 (#11), portail animé (#12),
  correction activation/keepalive (#13), composition et correctifs du cycle de vie
  Dolphin (#14). Commit de fusion courant :
  `12d23a1db1b0fb9214d4386072dcfc44c1858f2f`.
- [PR #14](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/14) : **fusionnée le
  5 septembre à 10:58 UTC (12:58 Paris), après autorisation explicite**.
  Tête approuvée : `7684d883e59adaf29386a5eaa06b382aef85415c`, ancêtre du merge,
  avec arbre source identique ; aucun changement introduit par la fusion.
- Android CI de `main` après fusion :
  [run 33962044116](https://github.com/LeVraiPedro/SkyPortal-Thor/actions/runs/33962044116)
  réussi. Ce résultat couvre le commit fusionné, pas le nouveau chantier Bifrost.
- Branche courante : `agent/v6-bifrost-integration`, créée depuis ce `main` ;
  base `12d23a1db1b0fb9214d4386072dcfc44c1858f2f`, arbre propre à son ouverture.
- [PR #15](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/15) : ouverte en
  brouillon ; dernier commit de code `159dbe07facbd7fde414a411a318b6537f372be5`
  (ouverture Bifrost sur l’écran supérieur). Le code LED validé précédemment est
  `3be07965921ee9205a22fdd06c06222fe400d76e` ; provenances distinctes ci-dessous.
- L’utilisateur a ensuite autorisé V6.0 Bifrost avec une fiabilisation préalable,
  puis l’installation officielle de Bifrost, absent de la Thor au contrôle initial.
  L’audit du source Bifrost `1.3.1` / code `16` est terminé et l’APK officiel est
  installé. Les couleurs physiques via SkyPortal et leur restitution après OFF
  ont depuis été confirmées ; la validation complète de V6.0 reste ouverte.
- Version Android conservée : `0.5.0`, code `7` ; aucun tag ni release V6 créé.
  Cette autorisation de développement ne constitue pas une autorisation de fusion
  ou de publication du nouveau chantier.

## Historique de l’ouverture de la reprise PR #14

Les sections qui suivent, jusqu’à « Clôture de la campagne PR #14 », décrivent
exclusivement cette ancienne campagne. Les mentions « cette session », ses APK,
ses tests et son état final ne concernent pas le chantier Bifrost en cours,
consigné séparément en fin de document.

- `main` initial : `ffc1e7158e63abf3dae4a6f08aa372c66d8f35d1` ; la PR #14 était
  alors ouverte en brouillon et attendait sa validation matérielle.
- Branche de reprise : `agent/v6-animated-portal-layout-fix` ; tête initiale
  `f0aa3c33a09b1dffa9983001ddb795badea8b97a`, travail local initial propre.
- Correctif Dolphin ajouté sur cette branche pendant la campagne :
  `11353ca7cabf28bc4dccbfbefa0593fb321def2f` ; construction de paire réussie,
  Dolphin mis à jour ; revalidation matérielle avec le compagnon `d466536`
  conservé, revalidée en partie le 5 septembre, de 12:16 à 12:33 (Paris).
- Aucun travail plus récent n’avait été trouvé dans les PR lors de cet audit.
- Bifrost était hors périmètre de cette campagne. Son autorisation distincte
  est postérieure à la clôture de #14 et n’altère pas les preuves ci-dessous.

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

## Clôture de la campagne PR #14

Le parcours ciblé de composition et de cycle de vie Dolphin a été achevé, les
preuves revues et la CI vérifiée. L’accord explicite de l’utilisateur a ensuite
permis la fusion de #14 dans `12d23a1`, sans modification de l’arbre approuvé.
La CI de `main` a réussi. Les réserves matérielles et les limites de couverture
ci-dessus restent connues ; la fusion ne les transforme pas en scénarios validés.

## Chantier courant — V6.0 Bifrost, contrat audité et fiabilisation préalable

Branche : `agent/v6-bifrost-integration`, base `12d23a1`. L’utilisateur a autorisé
la reprise de V6.0 Bifrost après clôture de #14. La fiabilisation préalable porte
notamment sur les fiches d’actions pouvant conserver une identité périmée.
L’interruption de commande Wii après veille reste une observation à distinguer
de l’état du portail, pas un problème déclaré corrigé.

Audit du 5 septembre : source officiel [Bifrost 1.3.1](https://github.com/Pollux-MoonBench/Bifrost/tree/1.3.1),
commit `1baddf1644ff0d7edd1bd0f4ba02f7eb6c8e3cfa`, version Android `1.3.1` / code
`16`, API externe `1`, Android minimum API `33`. Le [contrat technique](V6_BIFROST.md)
regroupe les références source, les choix et les contrôles à compléter :

- support conservateur de la seule version auditée, jusqu’à un nouvel audit ;
- option désactivée par défaut, sortie `STATIC` gauche/droite issue de l’API 4,
  luminosité réglable, renouvellement à 2 Hz ;
- `EXPLICIT_CLEAR`, bail Bifrost de 1 500 ms et surveillance toutes les 400 ms ;
  pas de durées courtes répétées, dont le fast-path ne renouvelle pas l’échéance ;
- `CLEAR` à la sortie, à la déconnexion et à l’extinction des écrans ;
- commande acceptée par le receiver distincte d’une application physique :
  l’état réel du service n’est pas fourni par cette API ;
- restauration preset/profil par Bifrost si son service reste vivant ; aucune
  garantie après arrêt/crash de Bifrost ni d’isolation entre plusieurs appelants ;
- aucune installation de profil, aucun accès matériel/root supplémentaire,
  aucune modification Dolphin nécessaire à cette intégration.

Bifrost était absent de la Thor au contrôle initial. Après autorisation, l’APK
officiel `1.3.1` a été installé avec `adb install -r` : `Success`, provenance,
certificat et hash conservés conformes à l’audit. Le guide de premier démarrage
a été observé sur l’écran supérieur `0`, malgré une demande de lancement sur `4`.
L’utilisateur a confirmé les notifications et la configuration du contrôle tiers ;
une capture confirme « Allow third-party LED control » activé. À ce premier contrôle,
le service était absent : « Call Heimdall » désactivé et mode initial Ambient.
Le démarrage du test isolé est consigné ci-dessous ; il ne prouve pas une commande
SkyPortal appliquée. Aucun profil n’a été installé par SkyPortal.

L’implémentation a été commitée sur la branche : code initial `93c4e3b`, suivi et
contrat `3d38933`, puis optimisation `3be07965921ee9205a22fdd06c06222fe400d76e` :

- transport Android ordonné borné à 1 000 ms et résultat receiver distinct du matériel ;
- session à 2 Hz, temporisation de 5 s après rejet, contrôle de cycle de vie
  `STARTED` / `isInteractive` et fraîcheur LED de 1 500 ms maximum ;
- réglage OFF par défaut, luminosité initiale 35 %, bouton LED, dialogue et diagnostic ;
- fiabilisation de la fiche d’actions pour refuser une identité de slot périmée.
- découverte PackageManager évitée avant l’échéance d’un tick ; nouvelle
  tentative après 5 s si Bifrost manque, sans recherche répétée à chaque frame.

Tests ciblés réussis sur cet arbre de travail : 8 tests de cible de slot, 16 de
session Bifrost, 9 de politique LED et 8 de résolution LED. La première suite
complète a signalé une erreur Lint `NewApi` : accès à `display` API 30 alors que
le minimum de SkyPortal est 26. L’accès a été corrigé vers `window.decorView.display`.
La première réexécution a réussi avec 154 tests. Après l’optimisation de découverte,
les contrôles complets du code `3be0796` ont de nouveau réussi :

- 157 tests JVM, zéro échec, erreur ou test ignoré ;
- Android Lint : zéro erreur, 17 avertissements, dont un conseil `UseKtx`
  supplémentaire sur les préférences de luminosité ;
- compilation Debug réussie en 33 s ;
- contrôle de licence réussi sur 91 sources ; `git diff --check` réussi.

Ces résultats automatiques sont distincts des contrôles matériels partiels ci-dessous.
La construction signée initiale
[33971097637](https://github.com/LeVraiPedro/SkyPortal-Thor/actions/runs/33971097637)
a réussi sur `3d38933`, mais cet APK n’a pas été installé et ne valide pas le code
ultérieur. Le nouveau [run 33971500140](https://github.com/LeVraiPedro/SkyPortal-Thor/actions/runs/33971500140)
a réussi sur `3be0796` ; **seul ce nouveau candidat a été installé pour les tests**.

### Candidat signé installé — 5 septembre, à partir de 16:25 Paris

- Commit `3be07965921ee9205a22fdd06c06222fe400d76e`, arbre `app`
  `ff8ce7d0649f1fe882959688ab6a28e3212c933c`, run `33971500140`.
- APK `SkyPortal_Thor_API4_LayoutValidation.apk`, 8 323 494 octets. Le nom
  historique du workflow de #14 est réutilisé, mais sa provenance est bien
  celle de la nouvelle branche Bifrost et de ce commit.
- SHA-256 : `7c38b4de2fd78afcdc89f813f2ac2af64737007866402513bd7e4becc8f208c8`.
- Mode `PERSISTENT_RELEASE_KEY`, certificat officiel commun
  `502ae2f53a97b32a142cb11bda410a62dee5ee80af5b2d8fca2b70e05ed3229e`.
- Cinq empreintes du manifeste vérifiées, package `com.skyportalthor.app`,
  version `0.5.0` / code `7`. Installation `adb install -r` réussie ; APK réextrait
  de la Thor identique par SHA-256. Ancien APK conservé hors dépôt.
- Dolphin n’a pas été remplacé : APK installé réextrait, SHA-256
  `6443c72981e1ab3419abdfbfb655d3b54add91219457f5feac8c75636fb94ee0`,
  certificat commun revérifié. Le nouveau diagnostic confirme Binder connecté,
  service initialisé et API 4.
- CI du candidat vertes :
  [push 33971500659](https://github.com/LeVraiPedro/SkyPortal-Thor/actions/runs/33971500659),
  [PR 33971502886](https://github.com/LeVraiPedro/SkyPortal-Thor/actions/runs/33971502886).

Observations réelles **hors jeu**, pas validation des LED physiques :

- écran inférieur `4`, portail visible, zones séparées, RGB et boutons Équipes /
  Diagnostic / LED accessibles ; bascule 1J → 2J → 1J et sélecteur J2 ouverts ;
- 32 fichiers détectés, permission SAF persistante lecture/écriture confirmée,
  favoris visibles dans la collection ; aucun montage ni backup pendant ce
  parcours ; 16 slots natifs libres ;
- dialogue LED : Bifrost 1.3.1 détecté, OFF initial et luminosité 35 % observés ;
  service/LED physiques explicitement non confirmés dans le diagnostic ;
- aucun jeu actif (`NONE`). Chargements/remplacements/retraits, nouvelle protection
  de fiche et reconnexions restent à rejouer en partie ;
- Logcat après installation, de 16:25 au contrôle à 16:31 : aucun
  `FATAL EXCEPTION`, signal fatal, ANR, `SecurityException` ou `DeadObjectException`
  trouvé. Cette fenêtre ne couvre pas un essai en jeu.

État à la fin de ce premier parcours : solo, tous les slots libres, synchronisation SkyPortal OFF à 35 %.
Bifrost a le contrôle tiers autorisé ; son service n’a pas encore été observé
actif. L’utilisateur a été invité à choisir Static puis activer « Call Heimdall ».
Captures et Logcat restent hors Git et hors des pièces jointes publiques.

La synchronisation des LED physiques, la restauration du réglage utilisateur
et le parcours matériel sans Bifrost restent non confirmés. Les anciens résultats
API 4/SSA ne constituent pas des tests Bifrost. Les modes LED J1/J2 et la priorité
J1 n’ont pas été ajoutés ; ce chantier n’est pas une clôture complète de V6.0.
La restauration après arrêt/crash Bifrost reste sans garantie.
Les autres étapes V6.1–V6.4 restent des éléments de roadmap, sans démarrage implicite.

### Test Bifrost isolé — 5 septembre, 16:59–17:02 Paris

À la demande de l’utilisateur, le réglage et le démarrage ont été réalisés via
l’interface Bifrost pilotée par ADB, sans réinstallation ni changement de code :

- preset initial `Default` (Ambient / High) conservé ; création du seul preset
  temporaire `SkyPortal-Test-Temp`, STATIC gauche `#0000FF`, droite `#FF0000`,
  luminosité au curseur d’environ 35 % ;
- `Call Heimdall` activé ; `APP MODE` reste OFF. Aucune autorisation
  d’accessibilité, d’usage, de capture ou root supplémentaire ;
- `LEDService` confirmé `isForeground=true`, `startRequested=true` au démarrage
  puis lors d’un second contrôle ;
- retour à la bibliothèque Dolphin sur `0` : même processus et service Bifrost
  toujours actif. Capture du compagnon sur `4`, connecté, aucun jeu, J1 vide,
  collection de 32 fichiers ; aucun chargement, backup ou écriture de dump ;
- Logcat de 16:59 au contrôle de 17:00 : 357 lignes, aucun `FATAL EXCEPTION`,
  signal fatal, `ANR in`, `SecurityException` ou `DeadObjectException` trouvé ;
- CI de la tête documentaire `3c85064` revérifiées : runs `33972389700` et
  `33972388209` réussis ; PR #15 toujours ouverte en brouillon.

**État historique à 17:02 :** la couleur des anneaux physiques avait été demandée
à l’utilisateur, sans réponse à ce point. Cette attente a depuis été levée :
l’utilisateur a confirmé le bleu gauche / rouge droite du test isolé. Cela valide
ce preset Bifrost, pas la synchronisation SkyPortal ni sa restitution. L’option
SkyPortal était encore OFF pendant ce premier parcours. Le preset temporaire a
été laissé actif pour l’observation, sans altérer Default.
Après les essais, arrêter `Call Heimdall`, supprimer uniquement ce preset par
appui bref sur la corbeille après vérification de son nom, puis sélectionner
explicitement `Default`, service OFF. Ne pas maintenir la corbeille (suppression
globale). Captures et journaux restent privés, hors dépôt.

### Réception SkyPortal dans SSA — 5 septembre, 20:21–20:24 Paris

Reprise au commit documentaire `972e90873bc68e1109928b3a5b554bdbd63c1773`,
arbre propre avant actualisation. La PR #15 reste ouverte en brouillon ; ses CI
[33973678906](https://github.com/LeVraiPedro/SkyPortal-Thor/actions/runs/33973678906)
et [33973677587](https://github.com/LeVraiPedro/SkyPortal-Thor/actions/runs/33973677587)
ont été revérifiées réussies. Elles ne couvrent pas les changements documentaires
postérieurs. Aucun nouvel APK : compagnon signé `3be0796` et Dolphin API 4 conservés.

- Bifrost toujours foreground, même processus derrière Dolphin ; baseline physique
  bleu gauche / rouge droite confirmée par l’utilisateur avant la synchronisation.
- Lancement SSA depuis la bibliothèque sur `0`, SkyPortal sur `4`. Les deux alertes
  SSL connues `clientca.pem` / `clientcakey.pem` ont été acquittées séparément par
  OK, sans changement de configuration ni désactivation des avertissements.
- Diagnostic : API 4, `SSPP52`, `RUNNING`, USB présent / attaché / protocole à
  `true`, **16 slots natifs libres**. Aucun dump chargé, remplacé ou sauvegardé.
- Synchronisation SkyPortal activée à 35 %. Dialogue et diagnostic affichent
  « Commandes acceptées par Bifrost ; éclairage non confirmé. » : accusé receiver,
  pas preuve d’un changement physique des anneaux.
- Bifrost démarre le STATIC externe à 20:22:46.307, puis signale l’override actif
  de façon répétée, sans expiration du bail dans la fenêtre examinée.
- Historique Android entre 20:23:27 et 20:23:51 : **47 `ACTION_DISPLAY`**, appelant
  SkyPortal et `resultCode=0`. Les 46 intervalles vont de **504 à 516 ms**, moyenne
  **506,46 ms** (environ 1,97 Hz). Cette mesure concerne les broadcasts de cet
  échantillon, pas la fréquence des LED physiques.
- Logcat 20:21–20:23:10 : aucun `FATAL EXCEPTION`, signal fatal, ANR,
  `SecurityException`, `DeadObjectException`, `LED transact failed` ou
  `Failed to get PServerBinder` relevé. Cette fenêtre ne valide pas les scénarios
  d’interruption ou de restitution restant à exécuter.

À 20:23:51, SSA attend encore « Appuie sur A ». L’utilisateur a été invité à
entrer dans sa partie et à confirmer que les anneaux quittent le bleu / rouge
fixe pour suivre le bandeau du portail. **Cette observation est en attente**,
ainsi que `CLEAR`, la restitution et les régressions en partie. Synchronisation
laissée ON à 35 % pour cette observation ; preset temporaire conservé, Default
intact. Captures et journaux restent privés. Le test isolé confirmé ne coche
aucune validation physique de la synchronisation SkyPortal.

### Parcours en partie et libération — 5 septembre, 20:46–20:51 Paris

L’utilisateur a ouvert sa partie SSA. Même APK compagnon `3be0796` et même
Dolphin API 4 ; aucune installation ni modification de code pendant ce parcours.

- Lightning Rod chargé en J1 puis remplacé par Sonic Boom : les deux figurines
  sont apparues dans le jeu, pas uniquement dans l’interface du compagnon.
- Sonic Boom seul en slot natif `#0 (1/0)` avant l’arrêt forcé du compagnon à
  20:48:59. Dolphin est resté actif, même processus, Sonic Boom visible en jeu.
- Bifrost a signalé `external override lease expired → reverting` à 20:49:01.339,
  puis redémarré STATIC. C’est une preuve du watchdog logiciel, pas d’un CLEAR
  explicite ni d’une restitution physique observée.
- Relance SkyPortal sur `4` : reconnexion, même slot unique `#0 (1/0)` retrouvé,
  collection de 32 fichiers, option ON/35 % conservée et commandes Bifrost
  de nouveau acceptées. Aucun second chargement demandé.
- OFF à 20:50:15 : historique Android `ACTION_CLEAR`, appelant SkyPortal,
  `resultCode=0`, traitement en 2 ms ; aucun DISPLAY postérieur dans l’historique
  contrôlé à 20:51:36. Bifrost redémarre STATIC à 20:50:15.492, sans nouvelle
  expiration de bail. Le dialogue OFF masque l’accusé, d’où cette preuve Android.
- Retrait de Sonic Boom confirmé par la demande de figurine dans SSA et par
  le diagnostic final : **16 slots libres**, USB prêt et `SSPP52 / RUNNING`.
- Logcat de 20:46 au contrôle de 20:51 : 3 068 lignes, aucun crash Java/natif,
  ANR, `SecurityException`, `DeadObjectException`, erreur de transaction LED ou
  mapping périmé recherché. Avertissements Android de fermeture de canaux
  d’entrée présents lors des fermetures de fenêtres/du processus, sans crash.

**État laissé :** SSA attend une figurine sur `0`, SkyPortal en solo sur `4`,
32 fichiers, slots libres, synchronisation OFF/35 %. Bifrost reste actif sur le
preset temporaire bleu gauche / rouge droite, Default intact. Aucun backup ni
écriture directe dans les dumps par SkyPortal ; les montages ont utilisé Dolphin.
Les couleurs physiques pendant la synchronisation et leur restitution n’ont pas
encore été confirmées par l’utilisateur. La baseline isolée déjà confirmée ne
remplace pas ces observations. Accueil/veille, arrêt Dolphin, arrêt Bifrost, J2 et
la protection des fiches pendant la mort Dolphin restent à rejouer sur ce candidat.

### Confirmation physique et interruptions — 5 septembre, à partir de 21:06 Paris

L’utilisateur a répondu explicitement « OUI » à la question portant sur les anneaux
suivant les couleurs du portail, puis revenant au bleu gauche / rouge droite après
OFF. Cela clôt ces deux observations du parcours précédent, avec l’APK `3be0796`.
Cela ne valide ni les autres chemins de restitution ni la variation/calibration
du curseur de luminosité. Les mentions « en attente » des parcours horodatés
précédents décrivent leur point d’arrêt historique.

Contrôles de reprise : ADB autorisé, deux écrans ON (`0` / `4`), batterie 74 %,
service Bifrost foreground, SSA demandant une figurine et slots vides. Les CI
documentaires de `92d063d` sont réussies : runs `33985608945` et `33985607654`.
Les essais suivants utilisent encore le même candidat `3be0796` et Dolphin API 4 :

- Option ON/35 %, focus donné au jeu sur `0` : DISPLAY continuent sur une
  couleur identique. L’accueil à 21:07:50 masque Dolphin mais laisse SkyPortal
  visible : CLEAR accepté à 21:07:50.691, sans DISPLAY ultérieur dans la fenêtre
  contrôlée. Ce n’est pas un test d’arrêt du cycle de vie de SkyPortal.
- Retour au jeu existant : réception reprise. Le bouton « Dolphin en haut »
  ouvre sa bibliothèque ; Retour permet de retrouver l’émulation existante.
  Un sélecteur de fichier ouvert par le bouton lecture de Dolphin a été annulé,
  sans sélectionner ni modifier de fichier.
- SkyPortal réellement masqué par les paramètres Android sur `4` : activité
  `STOPPED`, CLEAR accepté à 21:09:57.358 (2 ms), dernier DISPLAY antérieur.
  Retour du compagnon : même processus, dialogue ON/35 % et commandes reprises.
- Veille explicite : Android `Asleep`, les deux écrans OFF, CLEAR accepté à
  21:10:20.848 (2 ms), aucun DISPLAY postérieur avant réveil. Réveil : compagnon
  sur `4`, jeu sur `0`, commandes de nouveau acceptées. Résultat physique de ces
  restitutions non demandé ni déduit du test OFF déjà confirmé.
- Arrêt forcé Bifrost à 21:12:32 environ : service absent avant et après la
  demande de chargement de Whirlwind ; SkyPortal reste réactif et précise
  toujours que l’éclairage est non confirmé. Son receiver peut accepter sans
  service vivant, conformément à la limite documentée.
- Whirlwind chargé par le compagnon, identifié seul dans le slot natif
  `#0 (0/0)`. Le jeu affiche toutefois la télécommande Wii déconnectée après
  veille : pas de validation visuelle de Whirlwind dans le jeu pour ce parcours.
  L’utilisateur a été invité à réactiver sa commande habituelle.
- Bifrost relancé explicitement sur `0`, service redémarré par son interface,
  preset temporaire conservé. Contrôle tiers désactivé temporairement : message
  « Autorisez le contrôle LED par les applications tierces dans Bifrost. »,
  essais espacés d’environ 5 s. Autorisation rétablie, réception reprise.
- Diagnostic à 21:16 : Binder/API 4, `SSPP52 / RUNNING`, USB présent / attaché /
  protocole, SAF persistant et 32 fichiers. Whirlwind seul en slot `#0 (0/0)`.
  Synchronisation remise OFF/35 %, contrôle tiers ON, service Bifrost actif,
  preset temporaire conservé, Default intact. Aucun remplacement d’APK à ce point.
- Journaux conservés entre 21:06 et 21:16:30 : 8 107 lignes, aucun crash, ANR,
  `DeadObjectException`, échec de transaction LED ou mapping périmé recherché.
  Une `SecurityException` GoogleCertificates provient de Google Play Services
  (processus vérifié), pas des trois applications testées. Des avertissements
  de canaux d’entrée et de routage multi-écrans Bifrost sont conservés.

**Défaut trouvé :** le bouton du compagnon lançait Bifrost sur l’écran courant,
donc `4`. Bifrost 1.3.1 se relance lui-même sur `0` puis termine son activité ;
sa réutilisation explique probablement la fermeture immédiate observée sans
crash. Le lancement explicite sur `0` fonctionne. Correction minimale en cours
côté compagnon : cible `Display.DEFAULT_DISPLAY`, libellé « Bifrost en haut ».
Aucun changement du transport LED, de Dolphin ou de Bifrost tiers. La préférence
privée Bifrost forçant l’écran inférieur n’est pas couverte par ce parcours.
Un nouvel APK signé devra valider le bouton réel ; les résultats du candidat
`3be0796` ne seront pas attribués automatiquement à ce nouveau binaire.

Le correctif `159dbe07facbd7fde414a411a318b6537f372be5` ne modifie que deux
fichiers : `PortalActivity.kt` et `LightingSettingsDialog.kt`. Contrôles locaux
réexécutés sur cet état : **157 tests JVM, zéro échec/erreur/ignoré**, Lint
**zéro erreur / 17 avertissements**, Debug réussi (suite complète en 38 s),
contrôle de licence réussi sur 91 sources et `git diff --check` réussi.
Construction signée officielle lancée sur ce commit exact :
[run 33986789250](https://github.com/LeVraiPedro/SkyPortal-Thor/actions/runs/33986789250).
L’ancien APK installé a été réextrait et son hash/certificat correspondent toujours
au candidat `3be0796`, conservé en privé pour une éventuelle remise en place.

### Candidat correctif installé et testé — 5 septembre, 21:25–21:27 Paris

Le run `33986789250` a réussi sur `159dbe07facbd7fde414a411a318b6537f372be5`.
Les deux Android CI de ce commit sont vertes (`33986789093`, `33986790891`).
Les contrôles locaux ci-dessus ont été réexécutés, pas seulement relus.

- Arbre app : `28c05b248618f085b236578647ef368b8dc0a9bb`.
- APK : `SkyPortal_Thor_API4_LayoutValidation.apk` (nom hérité du workflow),
  8 323 494 octets, package `com.skyportalthor.app`, `0.5.0` / code `7`.
- SHA-256 APK : `b26ac9276830e68b4e4b0e624c22ea780ebc44482a0f95e75763f4deed04d1fb`.
- SHA-256 source : `20c149becc8b2fab8bcdd507454991f264b15560d9db9add204d596abd1cafc6`.
- Signature `PERSISTENT_RELEASE_KEY`, certificat
  `502ae2f53a97b32a142cb11bda410a62dee5ee80af5b2d8fca2b70e05ed3229e`.
  Les APK SkyPortal précédent et Dolphin installé ont été réextraits avant mise
  à jour : même certificat vérifié. Dolphin reste identique au hash `6443c729…`.
- Les cinq sommes du paquet ont été vérifiées localement, puis mise à jour du
  seul SkyPortal par `adb install -r` réussie. L’APK réextrait après installation
  a exactement le hash du candidat ; aucun fichier de signature manipulé localement.
- Whirlwind retrouvé sans second chargement, même slot natif unique `#0 (0/0)` ;
  scan de 32 fichiers, droits SAF, mode solo et OFF/35 % conservés.
- Bouton réel « Bifrost en haut » testé deux fois, dont réouverture de la même
  activité : Bifrost reste affiché sur `0`, SkyPortal sur `4`, sans disparition.
  Retour permet de retrouver l’émulation existante. Avec ON, le dialogue passe
  de commandes acceptées à restitution demandée pendant que Bifrost masque le
  jeu, puis revient aux commandes acceptées au retour.
- OFF final : CLEAR accepté à 21:27:05.382, dernier DISPLAY à 21:27:04.874,
  aucun DISPLAY ultérieur dans l’historique contrôlé. Synchronisation OFF/35 %
  laissée ; aucune nouvelle observation physique revendiquée pour ce binaire.
- Logcat 21:25:35–21:27:08 : 2 803 lignes, aucune des erreurs recherchées
  (crash/ANR, permission, Binder, transaction LED, mapping périmé ou conflit
  d’écran Bifrost). Le jeu reste bloqué par la commande Wii déjà déconnectée
  avant cette installation ; l’apparition/retrait en jeu ne sont pas revalidés.

**État laissé :** compagnon `159dbe0` sur `4`, Dolphin API 4 inchangé sur `0`,
Whirlwind seul monté, Bifrost actif avec contrôle tiers ON et preset temporaire,
Default intact. Pas de suppression, de nouvelle version, de tag ou de publication.
La PR #15 reste ouverte en brouillon ; les documents actualisés ne modifient pas
l’arbre app de ce candidat. Les captures, journaux et copies d’APK restent privés.

## Prochaine action et conditions de validation du nouveau chantier

**Prochaine action : réactiver la commande Wii dans la partie existante**, puis
reprendre J2, mort/reprise Dolphin, fiches d’actions, backup sécurisé et les
restitutions physiques après interruptions encore non confirmées.
La validation doit associer tests déterministes et observation
réelle des LED, vérifier le mode sans Bifrost et les libérations de contrôle,
puis documenter les limites de restauration du service tiers. Aucun résultat
ne sera coché sans preuve correspondante, avec provenance des APK testés. Toute
fusion ou publication du nouveau chantier reste soumise à une autorisation distincte.
