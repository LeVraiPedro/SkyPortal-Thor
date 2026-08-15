# SkyPortal Thor V3

Application compagnon Android pour utiliser les fichiers `.sky` avec un Dolphin Android modifié sur l'AYN Thor.

Depuis la version 0.3.1, le lanceur cible toujours l'écran Android secondaire (`Screen-2`) de la Thor, même si l'icône SkyPortal est touchée depuis l'écran inférieur. L'écran supérieur reste ainsi libre pour Dolphin.

Le compagnon démarre en mode solo : seule la carte Joueur 1 est affichée. Le bouton `1J` de l'en-tête ouvre le réglage permettant d'activer Joueur 2 ; ce choix est conservé entre les lancements.

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
└─ écran inférieur : SkyPortal Thor V3
   ├─ sélection tactile J1 / J2 / slots 3 à 8
   ├─ dossier persistant via Storage Access Framework
   └─ Binder/AIDL → SkyPortalService dans Dolphin
```

SkyPortal ne modifie jamais directement un personnage monté. Dolphin reste le seul processus qui écrit la progression. Pour cette raison, l'action Backup retire d'abord le personnage avant de copier son fichier.

## Compatibilité Dolphin

L'interface AIDL reste identique à celle de la V2. La V3 fonctionne donc avec le Dolphin déjà patché en API 1.

Le dossier `dolphin-patch/` contient une révision API 2 facultative qui ajoute :

- des codes distincts pour accès URI, dump invalide et portail plein ;
- des journaux Logcat sans URI complète ;
- un mapping des slots conservé lors de la recréation du service dans le même processus Dolphin.

Reconstruire Dolphin n'est pas obligatoire pour utiliser la nouvelle interface. Cela améliore seulement le diagnostic et la robustesse du service.

> **Recommandation :** l'API 1 reste utilisable pendant une session, mais son ancien service peut oublier la correspondance des slots si SkyPortal est fermé ou recréé alors qu'un personnage est encore monté. Pour les sessions longues et les backups après relance, utiliser le service API 2 fourni. Avec l'API 1, retirer les personnages avant de fermer SkyPortal ; si un slot disparaît après une relance, redémarrer Dolphin avant de recharger un dump.

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

1. Installer le Dolphin modifié et SkyPortal Thor V3.
2. Activer `Emulated USB Devices > Skylanders Portal` dans Dolphin.
3. Lancer le jeu sur l'écran supérieur.
4. Ouvrir SkyPortal sur l'écran inférieur.
5. Toucher **Dossier** et autoriser le dossier racine contenant les `.sky`.
6. Toucher **Joueur 1**, puis **Spyro**.

Si une erreur survient, la fenêtre reste ouverte. Utiliser **Voir détails** pour relever le code exact. La checklist complète se trouve dans [THOR_TEST_CHECKLIST.md](THOR_TEST_CHECKLIST.md).

## Validation effectuée

La V3 a été compilée, passée dans les tests unitaires et Android Lint, puis installée sur une AYN Thor Android 13. Le test réel a détecté les 32 dumps existants et validé le flux **Joueur 1 → Magic → Spyro → succès Binder/Dolphin**, ainsi que l'affichage du slot occupé et de ses quatre actions.
