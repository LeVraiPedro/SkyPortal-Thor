# SkyPortal Thor V5 — Smart Portal

Application compagnon Android pour utiliser les fichiers `.sky` avec un Dolphin Android modifié sur l'AYN Thor. La version stable actuelle est **0.5.0** (`versionCode 7`).

> **Développement V6 :** la [PR #14](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/14) est fusionnée dans `main` (`12d23a1`), après validation ciblée Thor/SSA et accord utilisateur. L’intégration facultative **V6.0 Bifrost** se poursuit dans la [PR #15 en brouillon](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/15), branche `agent/v6-bifrost-integration`. Bifrost officiel **1.3.1 / code 16** est installé ; la baseline STATIC bleu gauche / rouge droite est confirmée physiquement. Le candidat `3be0796` a passé **157 tests**, Lint et CI ; son APK signé officiel est installé sur la Thor. Les contrôles hors jeu puis la réception des commandes dans SSA sont réussis, avec une cadence observée d’environ 1,97 Hz. **L’effet physique de la synchronisation SkyPortal, sa restitution et les régressions en partie restent à vérifier.** Option désactivée par défaut, sans changement Dolphin ni permission root supplémentaire ; une commande acceptée ne prouve pas un changement physique. La stable `v0.5.0` reste une paire **API 3**, la source V6 utilise **API 4** : ne pas mélanger leurs APK. Voir [le suivi](docs/PROJECT_STATUS.md), [la roadmap](docs/ROADMAP_V6.md), [le contrat LED API 4](docs/V6_LED_API4.md) et [le contrat Bifrost](docs/V6_BIFROST.md).

Depuis la version 0.3.1, le lanceur cible toujours l'écran Android secondaire (`Screen-2`) de la Thor, même si l'icône SkyPortal est touchée depuis l'écran inférieur. L'écran supérieur reste ainsi libre pour Dolphin.

Le compagnon démarre en mode solo : seule la carte Joueur 1 est affichée. Le bouton `1J` de l'en-tête ouvre le réglage permettant d'activer Joueur 2 ; ce choix est conservé entre les lancements.

## Nouveautés V5

- Détection locale de l'état d'émulation, du Game ID et du titre actuellement exécuté par Dolphin.
- Reconnaissance centralisée de Spyro's Adventure, Giants, Swap Force, Trap Team, SuperChargers et Imaginators.
- Lecture séparée du réglage `EmulateSkylanderPortal` et des preuves USB réelles : présence dans le scanner Dolphin, attachement au jeu et première commande Skylanders.
- Détection des bases USB concurrentes, notamment Disney Infinity, avec blocage du chargement et demande de redémarrage complet de l'émulation.
- État compact `Dolphin | jeu | portail` dans l'en-tête, avec délais maximaux et reconnexion automatique. `Portail prêt` n'est affiché qu'après un échange USB confirmé avec le jeu.
- Catalogue de figurines fourni par la table native de Dolphin : ID, variant, génération, élément et type.
- Identification read-only des dumps aux offsets ID/variant ; le nom des dossiers n'est plus la source principale quand l'API 3 est disponible.
- Sélecteur `Personnages | Objets`, filtre automatique adapté au jeu, et option temporaire `Toute la collection`.
- Moteur de compatibilité central : un dump incompatible est expliqué et n'est jamais envoyé à Dolphin.
- Instantané des 16 slots natifs et diagnostic Smart Portal enrichi.

## Fonctionnalités V4 conservées

- Favoris persistants directement sur les cartes des Skylanders.
- Vue Récents ordonnée selon les derniers chargements réussis.
- Équipes rapides solo ou duo enregistrées depuis les slots actifs.
- Chargement d'une équipe en une action avec activation automatique du mode 2J si nécessaire.
- Assistant de diagnostic pour l'écran inférieur, SAF, la collection, Dolphin, les signatures APK, Binder et la version de l'API.
- Diagnostic lisible de la connexion Binder, du jeu actif, de l'API et du Portal of Power, avec activation directe en API 3.

La V3 remplace la collection permanente de la V2 par un portail tactile :

1. toucher **Joueur 1** ou **Joueur 2** ;
2. choisir un personnage dans la grille ;
3. le chargement démarre immédiatement ;
4. la sélection se ferme uniquement après confirmation de Dolphin.

Un slot occupé ouvre les actions **Changer**, **Retirer**, **Backup** et **Informations**.

## Nouveautés principales

- Sélecteur plein écran adapté à l'écran inférieur de la Thor.
- Grille adaptative des Skylanders jouables détectés.
- Filtres par élément et par jeu.
- Recherche secondaire, dépliable uniquement quand elle est utile.
- États visuels `Placement…`, `Chargé` et `Échec`.
- Diagnostic détaillé avec code, cause technique, conseil et nouvelle tentative.
- Blocage des doubles touchers pendant une opération.
- Choix explicite entre Dolphin Debug et Release lorsque les deux services modifiés sont installés.
- Préflight lecture/écriture du fichier SAF avant l'appel Binder.
- Détection du retour natif `255`, qui signifie « portail plein » et ne doit jamais être traité comme un succès.
- Les fichiers de `99_Backups` ne sont plus réimportés dans la collection.
- Backup sûr d'un personnage monté : confirmation, retrait du portail, puis copie.

Consulter [CHANGELOG.md](CHANGELOG.md) pour la liste détaillée.

## Architecture

```text
AYN Thor
├─ écran supérieur : Dolphin SkyPortal Edition
│  └─ SkylanderConfig → Portal of Power émulé
└─ écran inférieur : SkyPortal Thor V5
   ├─ sélection tactile J1 / J2 / slots 3 à 8
   ├─ dossier persistant via Storage Access Framework
   └─ Binder/AIDL → SkyPortalService dans Dolphin
```

SkyPortal ne modifie jamais directement un personnage monté. Dolphin reste le seul processus qui écrit la progression. Pour cette raison, l'action Backup retire d'abord le personnage avant de copier son fichier.

## Compatibilité Dolphin

Les six premières méthodes AIDL restent dans le même ordre : la V5 garde son mode dégradé avec les services API 1 et API 2.

Le dossier `dolphin-patch/` contient l'API 3 Smart Portal qui ajoute :

- état précis de l'émulation, Game ID et titre ;
- lecture/activation/désactivation du réglage officiel Portal of Power ;
- présence, attachement et handshake USB réels du portail, distincts du simple réglage Dolphin ;
- détection d'une base Disney Infinity concurrente ;
- instantané réel des slots natifs ;
- catalogue des figurines directement issu de `list_skylanders` ;
- les diagnostics et protections API 2 (URI, dump invalide, portail plein et mapping persistant).

Reconstruire Dolphin n'est pas obligatoire pour les fonctions V4, mais l'API 3 est nécessaire pour le mode Smart Portal complet.

> **Recommandation :** utiliser ensemble le compagnon et le Dolphin API 3 fournis par cette révision. API 1/2 restent acceptées en mode dégradé. Un ancien Dolphin API 3 sans les nouveaux indicateurs USB reste Binder-compatible, mais SkyPortal affiche alors `Portail non vérifié` et bloque le chargement Smart plutôt que d'annoncer un faux état prêt.

Dans la source V6 non publiée, l’API 4 ajoute un état lumineux versionné à la fin du contrat AIDL. Les fonctions Smart Portal API 3 et leurs fallbacks restent inchangés. Le canal LED est non bloquant et alimente le portail animé Compose. Les observations historiques ont été complétées par la revalidation ciblée de la PR #14 le 5 septembre 2026, avant sa fusion ; les APK exacts, les scénarios réellement exécutés et les limites sont identifiés dans le suivi. L’intégration Bifrost en branche utilise les couleurs gauche/droite en mode `STATIC`, une luminosité réglable (35 % par défaut) et un renouvellement à 2 Hz ; ces preuves historiques API 4 ne valident pas les commandes des LED physiques. La libération de l’éclairage reste sous le contrôle de Bifrost lorsque son service fonctionne ; sa restauration après arrêt ou crash de Bifrost n’est pas garantie. Les modes LED J1/J2 et priorité J1 ne sont pas ajoutés à ce stade. Voir le [contrat technique](docs/V6_BIFROST.md).

Les deux APK doivent être signés avec la même clé, car la permission `com.skyportalthor.permission.PORTAL_CONTROL` est de niveau `signature`.

La paire Debug utilisée pendant la validation matérielle possède bien une signature identique. Cela ne garantit pas qu'un APK SkyPortal arbitraire fonctionnera avec un autre build Dolphin : les deux artefacts distribués ensemble doivent être construits et signés comme une paire.

## Compilation

Prérequis :

- Android Studio ou JDK 17/21 ;
- Android SDK 36 ;
- connexion Internet au premier téléchargement des dépendances.

Sous Windows :

```powershell
.\gradlew.bat --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

APK généré :

```text
app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` n'est volontairement pas inclus dans l'archive. Android Studio le crée automatiquement ; sinon, ajouter localement `sdk.dir=...` avec le chemin du SDK Android.

## Premier démarrage

1. Installer le Dolphin API 3 modifié et SkyPortal Thor V5.
2. Dans les périphériques USB émulés de Dolphin, laisser le portail Skylanders actif et désactiver la base Disney Infinity. Ces deux bases actives simultanément peuvent empêcher certains jeux Skylanders de voir le Portal of Power.
3. Lancer le jeu Skylanders sur l'écran supérieur. Si un réglage de base a été modifié pendant que le jeu tournait, arrêter complètement l'émulation puis relancer le jeu.
4. Ouvrir SkyPortal sur l'écran inférieur.
5. Attendre `Portail prêt`, qui signifie désormais que le jeu a réellement envoyé une commande USB Skylanders.
6. Toucher **Dossier** et autoriser le dossier racine contenant les `.sky`.
7. Toucher **Joueur 1**, puis **Spyro**.

Si une erreur survient, la fenêtre reste ouverte. Utiliser **Voir détails** pour relever le code exact. La checklist complète se trouve dans [THOR_TEST_CHECKLIST.md](THOR_TEST_CHECKLIST.md).

## Validation effectuée

La campagne V5.1 de fiabilisation a validé sur une vraie AYN Thor Android 13 : le routage sur les écrans logiques `0`/`4`, la connexion Binder API 3, la détection de Spyro's Adventure (`SSPP52`), l'activation du portail, le chargement/retrait J1-J2, les principales reconnexions, le cycle écran éteint/allumé et un backup contrôlé. **Spyro's Adventure est le seul jeu lancé sur le matériel pendant cette campagne.** Giants, Swap Force, Trap Team, SuperChargers et Imaginators sont couverts par les tests automatisés du modèle central, pas par un lancement matériel.

Après cette campagne, un cas de faux `Portail prêt` a été reproduit : Dolphin avait à la fois le portail Skylanders et la base Disney Infinity activés. L'utilisateur a confirmé que la désactivation de Disney Infinity, suivie d'un redémarrage de l'émulation, supprimait le problème. Le correctif distingue maintenant le réglage du portail de son utilisation USB réelle et signale le conflit.

La paire Release officielle, signée avec le certificat persistant commun, a ensuite été installée sans `pm clear` et sans suppression de la collection sur la Thor. Avec Disney Infinity désactivé, le chemin complet jeu → USB → JNI → service → compagnon a confirmé `SSPP52`, les trois preuves USB et `Portail prêt`. En jeu, Whirlwind → Sonic Boom → Lightning Rod sur J1 a réussi sans mapping obsolète, slot fantôme ni faux échec ; le retrait, J2, la reconnexion de SkyPortal avec un personnage monté, la mort/reprise de Dolphin et le cycle écran/accueil ont également été validés.

Le conflit volontaire a lui aussi été rejoué sur cette paire exacte : portail Skylanders et base Disney Infinity simultanément actifs produisent un état de conflit explicite, jamais `Portail prêt`, et le chargement est bloqué avant Binder. Après restauration de Disney Infinity à `false` et redémarrage complet de l'émulation, les trois preuves USB repassent à `true` et le portail redevient prêt.

Les contrôles automatisés comprennent 75 tests unitaires, Android Lint et la compilation Debug. Ils couvrent notamment le parsing des nouveaux indicateurs USB, les décisions de disponibilité, les anciens payloads API 3, le conflit Disney Infinity, le schéma natif v2, la bascule sûre Debug/Release et le format de migration des préférences lors du passage à la clé officielle. Trois tests natifs ciblés ont aussi été exécutés sur la Thor ARM64. Dix fixtures contrôlées ont été créées localement avec le Skylanders Manager officiel de Dolphin, dans un dossier séparé de la collection utilisateur ; aucun dump n'a été téléchargé. Sur SSA, elles ont permis de valider le filtre Personnages/Objets, les refus avant Binder, le chargement réel d'un Magic Item et la protection des backups.

Documentation de validation :

- [checklist matérielle Thor](THOR_TEST_CHECKLIST.md) ;
- [matrice de compatibilité](docs/COMPATIBILITY_MATRIX.md) ;
- [rapport de validation V5](docs/VALIDATION_V5.md) ;
- [checklist de release](docs/RELEASE_CHECKLIST.md) ;
- [intégration Dolphin](DOLPHIN_INTEGRATION.md).

Les workflows GitHub Actions présents dans `.github/workflows/` couvrent la CI Android, la préparation d'une release sur tag et, manuellement, la construction d'une paire SkyPortal/Dolphin avec signature commune. Aucune clé privée n'est stockée dans le dépôt.

## Licence

SkyPortal Thor est distribué sous `GPL-2.0-or-later`. Le texte complet de la GNU General Public License version 2 se trouve dans [`LICENSE`](LICENSE) ; la mention « or later » permet au destinataire de choisir une version ultérieure de la GPL.

Certains fichiers du dossier [`dolphin-patch/`](dolphin-patch/) proviennent de Dolphin Emulator ou en constituent des modifications. Leurs copyrights et identifiants SPDX originaux doivent être conservés. Toute distribution publique d’un Dolphin modifié doit respecter les licences amont et fournir durablement le code source correspondant au binaire distribué ; voir [`DOLPHIN_INTEGRATION.md`](DOLPHIN_INTEGRATION.md).

Les composants tiers conservent leur propre licence. En particulier, le wrapper Gradle reste sous Apache-2.0. La GPL du code SkyPortal n’accorde aucun droit sur les noms, marques, personnages, illustrations ou autres œuvres Skylanders et de leurs ayants droit. Les attributions détaillées figurent dans [`NOTICE.md`](NOTICE.md) et l’inventaire graphique dans [`docs/ASSET_AUDIT.md`](docs/ASSET_AUDIT.md).

## Projet non officiel

SkyPortal Thor est un projet communautaire non officiel. Il n’est affilié à, approuvé par ou sponsorisé par Activision, Toys for Bob, Microsoft, Nintendo, Sony, AYN Technologies ou l’équipe Dolphin. Le dépôt ne distribue aucun jeu, ROM, clé de chiffrement ni collection de dumps `.sky` provenant de tiers.
