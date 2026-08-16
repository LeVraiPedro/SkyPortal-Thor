# SkyPortal Thor V5 — Smart Portal

Application compagnon Android pour utiliser les fichiers `.sky` avec un Dolphin Android modifié sur l'AYN Thor.

Depuis la version 0.3.1, le lanceur cible toujours l'écran Android secondaire (`Screen-2`) de la Thor, même si l'icône SkyPortal est touchée depuis l'écran inférieur. L'écran supérieur reste ainsi libre pour Dolphin.

Le compagnon démarre en mode solo : seule la carte Joueur 1 est affichée. Le bouton `1J` de l'en-tête ouvre le réglage permettant d'activer Joueur 2 ; ce choix est conservé entre les lancements.

## Nouveautés V5

- Détection locale de l'état d'émulation, du Game ID et du titre actuellement exécuté par Dolphin.
- Reconnaissance centralisée de Spyro's Adventure, Giants, Swap Force, Trap Team, SuperChargers et Imaginators.
- Lecture de l'état officiel `EmulateSkylanderPortal` et activation à chaud depuis le compagnon via l'API 3.
- État compact `Dolphin | jeu | portail` dans l'en-tête, avec délais maximaux et reconnexion automatique.
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
- instantané réel des slots natifs ;
- catalogue des figurines directement issu de `list_skylanders` ;
- les diagnostics et protections API 2 (URI, dump invalide, portail plein et mapping persistant).

Reconstruire Dolphin n'est pas obligatoire pour les fonctions V4, mais l'API 3 est nécessaire pour le mode Smart Portal complet.

> **Recommandation :** utiliser l'API 3 fournie. API 1/2 restent acceptées, mais n'exposent pas le jeu ni l'état du portail et désactivent donc automatiquement les fonctions Smart dépendantes.

Les deux APK doivent être signés avec la même clé, car la permission `com.skyportalthor.permission.PORTAL_CONTROL` est de niveau `signature`.

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
2. Lancer un jeu Skylanders dans Dolphin. SkyPortal peut activer automatiquement le portail si nécessaire.
3. Lancer le jeu sur l'écran supérieur.
4. Ouvrir SkyPortal sur l'écran inférieur.
5. Toucher **Dossier** et autoriser le dossier racine contenant les `.sky`.
6. Toucher **Joueur 1**, puis **Spyro**.

Si une erreur survient, la fenêtre reste ouverte. Utiliser **Voir détails** pour relever le code exact. La checklist complète se trouve dans [THOR_TEST_CHECKLIST.md](THOR_TEST_CHECKLIST.md).

## Validation effectuée

La V5 est validée par tests unitaires, Android Lint, compilation des deux APK et essais ADB sur AYN Thor Android 13. Voir [THOR_TEST_CHECKLIST.md](THOR_TEST_CHECKLIST.md) pour le relevé détaillé.
