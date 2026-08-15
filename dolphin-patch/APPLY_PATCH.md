# Dolphin Android — patch SkyPortal Thor

Base vérifiée : `dolphin-emu/dolphin` commit `54070da5851e12f2d1a4389daa528e4fb81327ce`.

## But
Exposer uniquement les opérations Skylanders déjà présentes dans Dolphin Android :
- load
- remove
- clear
- status

Le cœur USB / Portal of Power de Dolphin n'est pas modifié.

Cette révision du service annonce `API_VERSION = 2`, mais conserve exactement la même interface AIDL que la V2.

## Fichiers à copier
Copier depuis ce dossier :

- `Source/Android/app/src/main/aidl/com/skyportalthor/ipc/ISkylanderPortalService.aidl`
- `Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/skyportal/SkyPortalService.kt`

vers les mêmes chemins dans le dépôt Dolphin.

## AndroidManifest.xml
Dans `Source/Android/app/src/main/AndroidManifest.xml`, ajouter :

```xml
<permission
    android:name="com.skyportalthor.permission.PORTAL_CONTROL"
    android:protectionLevel="signature" />
```

sous la balise `<manifest>`.

Puis dans `<application>` :

```xml
<service
    android:name=".skyportal.SkyPortalService"
    android:exported="true"
    android:permission="com.skyportalthor.permission.PORTAL_CONTROL" />
```

## build.gradle.kts
Dans `Source/Android/app/build.gradle.kts` :

```kotlin
buildFeatures {
    compose = true
    viewBinding = true
    buildConfig = true
    resValues = true
    aidl = true
}
```

## Signature
La permission est `signature`. SkyPortal Thor et le build Dolphin modifié doivent donc être signés avec la même clé.
Pour les builds debug créés sur le même PC Android Studio, ils utilisent normalement le même debug keystore.
Pour une distribution propre, signer les deux APK avec une clé de release commune.

## Pourquoi les URI content:// fonctionnent
Le Skylanders Manager Android officiel transmet déjà `uri.toString()` à `SkylanderConfig.loadSkylander(...)`.
`File::IOFile` détecte les chemins Android `content://` et les ouvre via le ContentResolver Android.
SkyPortal Thor appelle `grantUriPermission()` avant `load`, afin que Dolphin ait accès au fichier sélectionné.

## Slots
L'application expose 8 slots logiques. Dolphin peut gérer jusqu'à 16 entrées portail côté cœur, mais l'interface Android officielle présente 8 slots. Le service mémorise la correspondance slot logique → slot réel retourné par Dolphin.

Le mapping est conservé dans le processus Dolphin lors d'une simple recréation du service Binder. Un redémarrage complet du processus réinitialise naturellement le mapping et le portail natif ensemble.

Cette conservation est la principale raison de préférer l'API 2 : l'ancien service API 1 peut perdre son mapping si l'application compagnon est fermée alors que des figurines sont encore montées.

## Codes de chargement

```text
-2 : URI vide / ouverture impossible générique
-3 : slot logique invalide
-4 : URI content:// inaccessible en lecture/écriture
-5 : données .sky rejetées par SkylanderConfig
-6 : portail natif plein
```

Le retour natif `255` est converti en `-6` au lieu d'être annoncé comme un slot valide.

## Test minimal
1. Compiler/installer le Dolphin modifié.
2. Compiler/installer SkyPortal Thor avec la même signature.
3. Lancer Skylanders dans Dolphin et activer le portail émulé.
4. Lancer SkyPortal Thor sur l'écran inférieur.
5. Choisir le dossier `Skylanders`.
6. Appuyer sur `Reconnecter` si nécessaire.
7. Toucher J1 puis une carte `.sky` pour la charger.
8. Vérifier que le Skylander apparaît dans le jeu sans ouvrir le manager Dolphin.
