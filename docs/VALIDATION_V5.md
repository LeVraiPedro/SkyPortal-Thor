# Rapport de validation SkyPortal Thor V5

## Portée

Ce rapport décrit les résultats établis pour la candidate Android **0.5.0** (`versionCode 7`). Il ne constitue pas une validation de la future refonte visuelle V6.

La validation combine :

- des tests unitaires indépendants du matériel ;
- Android Lint et les builds Debug ;
- un parcours ADB sur une AYN Thor Max Android 13 ;
- des fixtures générées avec les mécanismes officiels de Dolphin.

## Addendum post-validation : conflit Disney Infinity

Après la campagne ADB décrite dans ce document, un faux état `Portail prêt` a été signalé : SkyPortal détectait Dolphin et Spyro's Adventure, tandis que le jeu indiquait que le Portal of Power était introuvable. Les réglages Dolphin avaient simultanément activé le portail Skylanders et la base Disney Infinity. L'utilisateur a confirmé que désactiver Disney Infinity puis redémarrer complètement l'émulation rétablissait la détection du portail.

L'analyse a également établi que l'ancien état `portalActivated` reflétait un booléen protocolaire initialisé à `true`, et non une preuve que le jeu avait réellement attaché puis interrogé le périphérique USB. Le correctif ajoute trois preuves distinctes — présence, attachement et handshake Skylanders — ainsi qu'une liste de bases concurrentes. `Portail prêt` exige désormais ces preuves cohérentes et aucun conflit.

La Thor a été reconnectée après l'implémentation. La paire corrigée a été installée en mise à jour, sans `pm clear`, désinstallation ni suppression de données. Le chemin normal `SkylanderUSB` → JNI → service → compagnon a été validé avec Disney Infinity désactivé. Le conflit produit par les deux bases simultanément actives n'a pas été rejoué sur le nouveau binaire et reste explicitement en attente.

## Environnement matériel

| Élément | Valeur |
|---|---|
| Console | AYN Thor Max |
| Android | 13, API 33 |
| Affichage Dolphin | logique `0`, 1920 × 1080 |
| Affichage SkyPortal | logique `4`, 1240 × 1080 |
| Compagnon | `com.skyportalthor.app`, 0.5.0, code 7 |
| Dolphin | `org.dolphinemu.dolphinemu.debug`, API SkyPortal 3 |
| Jeu matériel | Skylanders: Spyro's Adventure |
| Game ID matériel | `SSPP52` |

Les identifiants uniques de l'appareil, URI SAF et chemins locaux ont été retirés de ce rapport.

## Résultats automatiques

Les commandes de référence sont :

```powershell
.\gradlew.bat --no-daemon :app:testDebugUnitTest
.\gradlew.bat --no-daemon :app:lintDebug
.\gradlew.bat --no-daemon :app:assembleDebug
```

Résultats établis :

- 70 tests unitaires réussis ;
- Android Lint réussi, aucune erreur bloquante et 16 avertissements non bloquants ;
- APK Debug SkyPortal compilé ;
- campagne initiale : Dolphin Debug patché compilé et installé ;
- correctifs USB et montage : patchs vérifiés, paire installée et parcours ciblé rejoué sur la Thor ;
- trois tests natifs ciblés réussis sur la Thor ARM64 ;
- contrat AIDL identique des deux côtés et ordre historique préservé ;
- patch Dolphin applicable et réversible sur la révision ciblée ;
- syntaxe des trois workflows validée localement.

Les tests couvrent les six jeux et leurs IDs régionaux connus, les API 1/2/3, le parsing des 16 slots, les générations/types, les dumps invalides, l'identité inconnue, le portail plein, la déconnexion logique, les équipes manquantes, les favoris/récents et les exclusions de scan. La suite post-validation couvre aussi le parsing des preuves USB nullable, l'ancien JSON API 3, les décisions qui interdisent `READY` sans handshake, le conflit Disney Infinity et le maintien du chemin dégradé API 1/2. Elle n'exécute pas le chemin natif USB de bout en bout.

## Résultats matériels

### Écrans et lancement

SkyPortal reste sur l'écran inférieur logique `4`. Dolphin, son interface et l'émulation restent sur l'écran supérieur logique `0`.

### Smart Portal API 3

Avec Spyro's Adventure actif, le service historique rapporte `RUNNING`, `SSPP52`, le titre du jeu et les 16 slots natifs. En partant du réglage Portal of Power désactivé, le compagnon a demandé son activation et l'en-tête est passé à `Connecté | Spyro’s Adventure | Portail prêt`. Les chargements matériels suivants ont confirmé que le portail fonctionnait dans cette session, mais cet ancien libellé ne reposait pas encore sur le nouveau handshake USB.

### Chargement et retrait

- Lightning Rod a été chargé en J1 et associé au slot natif `0`, identité `3 / 0`.
- Sonic Boom a été chargé en J2 et associé au slot natif `1`, identité `1 / 0`.
- Un double toucher rapide n'a produit qu'un seul montage J2.
- Le retrait a été reflété côté compagnon et côté service Dolphin.
- Le filtre SSA a classé Terrabite dans Personnages et Anvil Rain/Dragon's Peak dans Objets.
- `Toute la collection` a montré les générations futures sans supprimer la protection de compatibilité.
- Snap Shot, Magic Log Holder et une identité inconnue ont été refusés avant Binder avec une explication française.
- Anvil Rain a été chargé puis retiré réellement dans SSA.
- Sur la paire finale, Lightning Rod a été remplacé dans J1 par Sonic Boom puis Whirlwind ; chaque changement a été confirmé par le même slot natif sans mapping obsolète ni faux échec.
- Après retrait final, le diagnostic a confirmé le schéma natif v2 et les 16 slots libres.

La première vérification sur l'écran inférieur réel a révélé une grille Objets trop basse. Les filtres ont été rendus repliables ; la grille et ses catégories ont ensuite été revalidées sur la Thor.

### Reconnexions

- Après arrêt forcé/recréation du compagnon, Dolphin et le jeu sont restés actifs ; J1/J2 ont été réconciliés sans second chargement.
- Après arrêt forcé de Dolphin, le compagnon n'a pas crashé et les slots distants ont été invalidés.
- Après relance de Dolphin et de Spyro's Adventure, l'API, le Game ID et le portail prêt ont été détectés de nouveau.
- Après arrêt normal de l'émulation, l'ancien jeu et les anciens slots ne sont pas restés affichés comme actifs.
- Avec Lightning Rod monté, l'extinction/rallumage de l'écran et un passage par l'accueil ont conservé un unique slot actif.
- Après force-stop/relaunch du compagnon, le PID Dolphin est resté inchangé et Lightning Rod a été réconcilié sans doublon.
- Ce dernier scénario a été rejoué avec la paire finale après le correctif de remplacement, puis Lightning Rod a été retiré avec confirmation.

### Logcat

Le premier arrêt forcé de Dolphin a découvert un défaut réel : le processus service-only rebondissait avant l'initialisation native et un accès prématuré à `NativeConfig` provoquait un `SIGSEGV`. La correction vérifie `DirectoryInitialization`, publie l'état transitoire `INITIALIZING` et renvoie `-10` aux commandes tant que le runtime n'est pas prêt.

Après rebuild et réinstallation, le scénario a été rejoué. Le compagnon est resté vivant, les slots ont été effacés sans fantôme, le service a rebondi, puis SSA `SSPP52` a été détecté à nouveau après relance. Le Logcat frais ne contient aucun crash natif ou applicatif, aucun ANR et aucun spam `DeadObjectException`.

Sur le parcours final Lightning Rod → Sonic Boom → Whirlwind → retrait, Logcat ne contient ni `Dropping stale logical portal mapping`, ni échec de confirmation, crash, ANR ou `SecurityException`.

### Anomalie d'affichage observée

Après l'arrêt forcé du processus Dolphin, l'accueil AYN Cocoon a temporairement recouvert l'affichage inférieur. Un lancement explicite de SkyPortal sur l'affichage logique `4` a remis le compagnon au premier plan, avec les slots distants correctement vidés. Aucun crash n'a accompagné ce comportement. Les parcours ultérieurs écran éteint/allumé et accueil/retour ont conservé l'activité et le slot attendus.

## Fixtures officielles

Le Skylanders Manager de Dolphin a créé dix fichiers de 1 024 octets dans un dossier exclu de la collection utilisateur : Giant, SWAP Force, Trap Master, Trap, Magic Item, Adventure/Location, véhicule Land, Trophy, Sidekick et identité inconnue. Ils permettent des tests reproductibles sans télécharger de dumps.

Les fichiers sont Tree Rex, Pop Thorn, Snap Shot, Magic Log Holder, Anvil Rain, Dragon's Peak, Hot Streak, Sky Trophy, Terrabite et Unknown. Seul Anvil Rain a été chargé en jeu ; l'affichage ou le refus des autres dans SSA ne vaut pas validation de leur jeu d'origine. Aucun Creation Crystal n'a pu être généré avec la révision Dolphin testée.

## Backup et SAF

Le backup contrôlé d'Anvil Rain a demandé confirmation, confirmé son retrait, puis produit une copie exacte de 1 024 octets. Le dossier `99_Backups` n'a pas été rescanné : la collection de fixtures est restée à 10 fichiers. La racine utilisateur a ensuite été restaurée et ses 32 fichiers ont de nouveau été détectés.

## Signature et artefacts de la campagne initiale

La paire Debug installée pendant la campagne initiale avait un certificat SHA-256 identique :

```text
fbefc11952c49c60bfd937eb77b0a5882f898a097184391eda3b616f1cfe7f4e
```

Artefacts locaux attendus dans le dépôt ou le build Dolphin :

```text
app/build/outputs/apk/debug/app-debug.apk
Source/Android/app/build/outputs/apk/debug/app-debug.apk
```

La candidate Debug historique installée sur la Thor correspondait exactement aux artefacts locaux vérifiés avant le correctif USB :

```text
SkyPortal Debug : f60da0491a476be49498bd881e03b6fe2619cac91b1fb8f16678510ba725b344
Dolphin Debug   : 12c67afe04f1186593e7dde8ae5f51a55270316c80d18b572236be408c8822a2
```

Sur cette paire historique exacte, Lightning Rod a de nouveau été chargé réellement puis retiré avant backup. La copie produite fait exactement 1 024 octets, la collection utilisateur est restée à 32 fichiers et un Logcat frais n'a révélé aucune erreur critique.

La paire Debug finale installée correspond exactement aux artefacts locaux :

```text
SkyPortal Debug : a5919d036317d8baa8267d10c13cb3eedd330589e088800d9916952c5d715491
Dolphin Debug   : 27f45e12172b750bbe9f5919d800829573e24a3ccd34c714cc1fa4939d7ae9f1
```

Les deux APK utilisent le même certificat Debug SHA-256 `fbefc11952c49c60bfd937eb77b0a5882f898a097184391eda3b616f1cfe7f4e`.

Une construction Release locale a aussi confirmé que les deux APK peuvent être signés avec un certificat commun. Elle utilisait toutefois une clé de test locale : elle ne doit pas être publiée comme une paire utilisateur. Les hashes d'une future construction de release signée doivent être recalculés au moment de la publication ; ils ne remplacent pas la vérification du certificat commun.

## Limites et actions restantes

- Seul Spyro's Adventure (`SSPP52`) a été lancé sur la Thor.
- Giants, Swap Force, Trap Team, SuperChargers et Imaginators restent couverts automatiquement, mais non testés en jeu sur matériel.
- Les opérations équipe pendant reconnexion, retrait pendant scan et arrêt de Dolphin pendant chargement restent à rejouer.
- Imaginators n'est pas pris en charge par le Manager de la révision Dolphin utilisée ; aucun Creation Crystal matériel n'a été testé.
- L'exécution réelle des workflows sur GitHub doit être confirmée après push ; une validation de syntaxe locale n'est pas un résultat GitHub Actions.
- Le nouveau suivi `portalUsbPresent` / `portalUsbAttached` / `portalUsbHandshakeSeen` est validé dans le chemin normal, mais le conflit et le blocage avant Binder avec les deux bases actives restent à rejouer.
- Une prochaine campagne matérielle doit activer simultanément les deux bases, vérifier `PORTAL_CONFLICT`, puis restaurer Disney Infinity désactivé et relancer complètement l'émulation.

La [matrice de compatibilité](COMPATIBILITY_MATRIX.md) et la [checklist Thor](../THOR_TEST_CHECKLIST.md) indiquent le niveau de preuve de chaque scénario.
