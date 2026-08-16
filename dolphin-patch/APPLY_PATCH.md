# Dolphin Android — patch SkyPortal Thor

Base vérifiée : `dolphin-emu/dolphin` commit `54070da5851e12f2d1a4389daa528e4fb81327ce`.

## But
Exposer les opérations Skylanders et l'état Smart Portal nécessaires au compagnon :
- load
- remove
- clear
- status, état d'émulation et jeu actif
- activation/désactivation du réglage officiel Portal of Power
- preuves USB réelles : présence, attachement et première commande Skylanders
- détection des bases USB concurrentes, notamment Disney Infinity
- instantané natif des slots et catalogue d'identification

Le protocole USB et les écritures de progression ne sont pas modifiés. Le cœur reçoit un snapshot verrouillé des slots et des indicateurs atomiques read-only sur le cycle USB du portail.

Cette révision annonce `API_VERSION = 3`. Les six méthodes API 1/2 restent inchangées et les deux méthodes V3 sont ajoutées à la fin de l'AIDL.

## Fichiers à copier
Copier depuis ce dossier :

- `Source/Android/app/src/main/aidl/com/skyportalthor/ipc/ISkylanderPortalService.aidl`
- `Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/skyportal/SkyPortalService.kt`

vers les mêmes chemins dans le dépôt Dolphin.

Le script applique ensuite `smart-portal-core.patch`, limité au bridge `SkylanderConfig`, à
`USB::Device`, au verrouillage de lecture du scanner et à l'implémentation du portail Skylanders.

## État USB réel (API 3)

`getStatusJson()` conserve l'AIDL et la version API 3 existants, et ajoute des champs JSON :

- `portalUsbPresent` : l'instance émulée Skylanders figure dans le scanner USB actif ;
- `portalUsbAttached` : IOS a ouvert cette instance ;
- `portalUsbHandshakeSeen` : nom compatible désignant un trafic protocolaire réel, c'est-à-dire une
  commande de classe Skylanders valide ou une requête interrupt validée ;
- `portalProtocolActivated` : valeur brute de l'état protocolaire natif, réservée au diagnostic ;
- `conflictingUsbDevices` : liste de périphériques concurrents, avec actuellement le jeton stable
  `DISNEY_INFINITY_BASE`.

Le champ historique `portalActivated` devient volontairement plus strict : il n'est vrai que si
le portail est configuré, présent, attaché, a reçu du trafic protocolaire, est actif au niveau protocolaire
et qu'aucun périphérique concurrent n'est configuré. Cela évite qu'un ancien compagnon API 3
annonce « Portail prêt » à partir de la valeur native initialisée à `true` avant toute communication
USB.

Les indicateurs d'attachement et de trafic protocolaire utilisent des atomiques appartenant à
l'instance USB active.
Ils sont donc remis à zéro naturellement quand Dolphin recrée ou retire cette instance. Une requête
virtuelle read-only sur `USB::Device` évite tout downcast RTTI ou déduction dangereuse par VID/PID.
Lorsque la liste USB change, le scanner conserve le même `shared_ptr` pour chaque identifiant encore
présent. IOS et le diagnostic Binder observent ainsi la même instance et les mêmes atomiques.

La base Disney Infinity est détectée depuis le réglage Dolphin existant `EmulateInfinityBase`.
Le service la signale au compagnon, mais ne modifie jamais silencieusement ce choix utilisateur.
SkyPortal demande de désactiver la base concurrente puis d'arrêter complètement l'émulation : certains
jeux ne tiennent compte que de la première liste de périphériques USB reçue au démarrage.

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

L'API 3 réconcilie en plus ce mapping avec l'occupation réelle des 16 slots natifs.

## Codes de chargement

```text
-2 : URI vide / ouverture impossible générique
-3 : slot logique invalide
-4 : URI content:// inaccessible en lecture/écriture
-5 : données .sky rejetées par SkylanderConfig
-6 : portail natif plein
-12 : jeu Skylanders actif, mais instance USB non prête ou sans handshake
-13 : périphérique USB émulé concurrent (Disney Infinity Base)
```

Le retour natif `255` est converti en `-6` au lieu d'être annoncé comme un slot valide.
Les gardes `-12` et `-13` s'appliquent juste avant le chargement natif lorsque le jeu Skylanders
est en cours ou en pause. Le préchargement sans jeu reste compatible avec le comportement existant.

## Test minimal
1. Compiler/installer le Dolphin modifié.
2. Compiler/installer SkyPortal Thor avec la même signature.
3. Pour le cas nominal, activer le portail Skylanders, désactiver Disney Infinity, puis lancer le jeu.
4. Lancer SkyPortal Thor sur l'écran inférieur.
5. Choisir le dossier `Skylanders`.
6. Appuyer sur `Reconnecter` si nécessaire.

Pour une copie Git peu profonde dont le `versionCode` calculé serait inférieur à celui déjà installé, le script ajoute un override facultatif :

```powershell
.\gradlew.bat :app:assembleDebug -PskyPortalVersionCode=43010
```

Omettre cette propriété conserve intégralement le calcul de version officiel de Dolphin.
7. Vérifier que présence, attachement et handshake USB sont confirmés avant l'affichage `Portail prêt`.
8. Toucher J1 puis une carte `.sky` pour la charger.
9. Vérifier que le Skylander apparaît dans le jeu sans ouvrir le manager Dolphin.
10. Arrêter l'émulation, activer également Disney Infinity et relancer : le conflit doit être affiché et le chargement bloqué avant Binder.
11. Désactiver Disney Infinity, arrêter complètement l'émulation, relancer et vérifier le retour au handshake normal.

La cause du conflit a été confirmée par l'utilisateur sur sa configuration : la désactivation de
Disney Infinity suivie du redémarrage de l'émulation permettait au jeu de retrouver le portail.
L'implémentation décrite ici dispose de contrôles automatisés et de build, mais **les étapes 7 à 11
n'ont pas encore été revalidées sur la Thor, indisponible au moment du correctif**.
