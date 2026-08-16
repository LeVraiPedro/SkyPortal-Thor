# SkyPortal Thor ↔ Dolphin Android

## État V5 / API 3

Le bridge n'est plus théorique : il est défini en AIDL et un service Dolphin minimal est fourni dans `dolphin-patch/`.

### Flux de chargement
1. SkyPortal scanne le dossier racine via Storage Access Framework.
2. Chaque `.sky` garde son `content:// URI` réel.
3. L'utilisateur touche un slot logique puis un personnage.
4. SkyPortal ouvre le fichier en lecture seule, valide sa taille, son en-tête, ses checksums et son identité native.
5. SkyPortal accorde READ + WRITE uniquement au package Dolphin connecté.
6. SkyPortal appelle `ISkylanderPortalService.load()`.
7. Le service Dolphin répète les validations critiques, vérifie la compatibilité avec le jeu actif et appelle l'API existante `SkylanderConfig.loadSkylander()`.
8. Le compagnon confirme l'identité du slot natif avant d'afficher un succès.
9. Dolphin ouvre le même fichier et conserve la propriété exclusive des écritures de progression.

### Pourquoi ne pas modifier directement le dump dans SkyPortal
Pendant qu'une figurine est montée, Dolphin peut écrire XP, or et progression. SkyPortal ne doit jamais écrire simultanément dans le même fichier.

### API AIDL compatible V1/V2/V3
```text
getApiVersion() -> Int
ping() -> Boolean
load(logicalSlot, uri, displayName) -> actualPortalSlot
remove(logicalSlot) -> Boolean
clear()
getStatusJson() -> JSON
setPortalEnabled(enabled) -> code résultat
getFigureCatalogJson() -> JSON
```

Les six méthodes historiques restent inchangées et dans le même ordre. Le compagnon n'appelle les deux nouvelles méthodes qu'après avoir détecté l'API 3. Les contrats API 1 et API 2 conservent un mode dégradé : ils ne fournissent ni jeu actif, ni commande du portail, ni catalogue natif complet.

`getStatusJson()` expose aussi `emulationState`, `gameId`, `gameTitle`, `portalEnabled`, `portalActivated`, `portalProtocolActivated`, `portalUsbPresent`, `portalUsbAttached`, `portalUsbHandshakeSeen`, `conflictingUsbDevices`, `canSetPortalEnabled`, `nativeSlotSchemaVersion` et `nativeSlots`.

Les indicateurs de portail ont des rôles distincts :

| Champ | Signification |
|---|---|
| `portalEnabled` | réglage Dolphin `EmulateSkylanderPortal` actif |
| `portalUsbPresent` | périphérique Skylanders présent dans le scanner USB de Dolphin |
| `portalUsbAttached` | périphérique attaché par l'IOS USB du jeu |
| `portalUsbHandshakeSeen` | au moins une commande USB propre au protocole Skylanders a été reçue |
| `portalProtocolActivated` | ancien état interne du protocole Skylanders, conservé pour le diagnostic |
| `portalActivated` | état effectif compatible avec les anciens compagnons : réglage actif, trois preuves USB cohérentes, protocole actif et aucun conflit |
| `conflictingUsbDevices` | identifiants stables des bases concurrentes, actuellement `DISNEY_INFINITY_BASE` |
| `nativeSlotSchemaVersion` | `2` lorsque `nativeSlots[].occupied` décrit un fichier réellement ouvert, indépendamment de `status` |

`Portail prêt` exige les trois preuves USB à `true` et une liste de conflits vide. Le booléen protocolaire historique était initialisé à `true` par le cœur et ne constitue donc jamais, à lui seul, une preuve que le jeu a trouvé le portail.

Le service fourni annonce l'API 3 et conserve les codes suivants :

```text
-2 : ouverture du fichier impossible
-3 : slot logique invalide
-4 : URI inaccessible dans le processus Dolphin
-5 : données .sky rejetées par SkylanderConfig
-6 : aucun slot natif disponible
-7 : activation/désactivation du portail impossible
-8 : ID/variant absent du catalogue natif
-9 : figurine ou objet incompatible avec le jeu actif
-10 : runtime natif Dolphin pas encore initialisé
-11 : montage natif occupé mais non identifié de façon sûre
-12 : jeu Skylanders actif, mais portail USB absent, non attaché ou sans handshake
-13 : périphérique USB émulé concurrent, actuellement Disney Infinity
255 : ancien signal natif « aucun slot disponible »
```

SkyPortal accepte aussi les services API 1 et API 2 et reconnaît l'ancien signal `255` pour un portail plein. Aucun retour négatif, aucun timeout et aucun slot non confirmé ne doit être transformé en succès.

### Robustesse de la connexion

- Toutes les opérations Binder sont bornées par un délai maximal et exécutées hors du thread UI.
- Un `DeathRecipient` invalide immédiatement la connexion et les faits distants connus.
- Une génération de connexion empêche un résultat tardif de l'ancien processus Dolphin d'écraser le nouvel état.
- Après reconnexion, le mapping J1/J2 est réconcilié avec l'identité des 16 slots natifs.
- Avec le schéma natif v2, `occupied` vient exclusivement de `FileIsOpen()` et `status` conserve la transition USB brute. Le remplacement ne peut donc plus effacer un mapping pendant `REMOVING / REMOVED`, ni réutiliser un slot dont le fichier est encore monté.
- Un même URI ne peut pas être monté silencieusement dans plusieurs slots logiques.
- Avec une API 1/2 qui ne restitue pas l’URI d’un slot restauré, SkyPortal bloque tout nouveau chargement dans un autre slot jusqu’au retrait du montage non identifié. Avec l’API 3, un slot natif occupé directement dans le Manager et non revendiqué est bloquant pour la même raison.
- Les API 1/2 n’exposent pas les slots chargés directement dans le Manager Dolphin : ne jamais utiliser le Manager en parallèle du compagnon dans ce mode. La garantie forte anti-double montage et le backup sécurisé nécessitent l’API 3.
- API 1 et API 2 ne disposent pas des preuves USB : elles restent utilisables selon leur chemin dégradé historique, sans affirmation `Portail prêt` vérifiée.
- Un ancien service API 3 qui omet les nouvelles clés reste lisible. Les valeurs sont conservées à `null`, l'état devient `Portail non vérifié` et le chargement Smart est bloqué avec une invitation à mettre à jour la paire, au lieu d'inventer `false` ou `true`.
- La présence de `DISNEY_INFINITY_BASE` est prioritaire sur les autres indicateurs : SkyPortal n'active pas automatiquement le portail et refuse le chargement avant Binder.
- Les journaux du bridge et du service n'exposent pas les URI SAF complètes.

Le service exporté peut être recréé seul pendant le redémarrage de Dolphin. Il vérifie donc `DirectoryInitialization.areDolphinDirectoriesReady()` avant tout accès à `NativeConfig` ou aux API natives. Tant que l'initialisation n'est pas terminée, `getStatusJson()` renvoie un instantané vide avec `serviceState = INITIALIZING`, le catalogue est vide et une commande de chargement renvoie `-10`. Le compagnon affiche alors un état transitoire et peut réessayer sans faire crasher le processus Dolphin.

Cette garde a été ajoutée après avoir reproduit sur la Thor un `SIGSEGV` du processus service-only avant l'initialisation native. Après rebuild/réinstallation, le scénario d'arrêt brutal a été rejoué : service relancé, slots vidés, nouvelle détection `SSPP52` et aucun crash natif ou spam `DeadObjectException` dans le Logcat frais.

### État et catalogue API 3

`getStatusJson()` fournit au minimum :

```text
emulationState, gameId, gameTitle,
portalEnabled, portalActivated, portalProtocolActivated,
portalUsbPresent, portalUsbAttached, portalUsbHandshakeSeen,
conflictingUsbDevices, canSetPortalEnabled,
nativeSlotSchemaVersion,
nativeSlots[0..15]
```

Ces ajouts restent dans le JSON existant : aucune transaction Binder n'est ajoutée et l'ordre des huit méthodes AIDL ne change pas. Un ancien compagnon ignore les clés inconnues. Le nouveau compagnon lit chaque preuve avec une valeur nullable, ce qui distingue explicitement un ancien JSON API 3 d'un portail réellement absent.

`getFigureCatalogJson()` exporte les identités de la table native `list_skylanders`. Le compagnon garde son modèle central de compatibilité, mais ne duplique pas une grande base de noms indépendante quand le catalogue API 3 est disponible.

### Sécurité
Le service Dolphin est protégé par la permission signature :
`com.skyportalthor.permission.PORTAL_CONTROL`.

Lorsque Debug et Release modifiés sont tous deux présents, l'interface permet de choisir explicitement la cible afin de ne pas charger le personnage dans le mauvais processus Dolphin.

La validation matérielle V5 a utilisé `org.dolphinemu.dolphinemu.debug`, API 3, avec le compagnon `com.skyportalthor.app`. Les deux APK Debug avaient le même certificat. Le build Release installé sur l'appareil n'était pas le service API 3 testé.

Après cette campagne, l'utilisateur a confirmé un conflit lorsque `EmulateSkylanderPortal` et `EmulateInfinityBase` étaient actifs simultanément : le compagnon détectait le jeu et le réglage, mais Spyro's Adventure indiquait que le portail était introuvable. Désactiver Disney Infinity puis redémarrer l'émulation a résolu le problème.

La paire Release officielle a ensuite été rejouée de bout en bout sur la Thor avec Disney Infinity désactivé : présence, attachement et trafic protocolaire à `true`, `SSPP52`, `Portail prêt`, remplacements J1 en jeu, J2, retrait et reconnexions confirmés. Le scénario volontaire avec **les deux bases actives** a également été validé : conflit explicite, aucun faux état prêt et aucun chargement Binder ; après restauration de Disney Infinity désactivé et redémarrage complet, le portail redevient prêt.

### Conflit Disney Infinity et redémarrage

Certains jeux ne lisent que la première liste de périphériques USB présentée au démarrage. Activer ou désactiver une base après ce moment ne garantit donc pas sa prise en compte. Si SkyPortal affiche `Conflit : base Disney Infinity — redémarrage requis` ou `Portail absent — redémarrage requis` :

1. arrêter complètement l'émulation, sans réinitialiser les données de Dolphin ;
2. désactiver la base Disney Infinity dans les périphériques USB émulés ;
3. conserver le portail Skylanders activé ;
4. relancer le jeu et attendre la confirmation du handshake USB.

SkyPortal ne désactive pas silencieusement une autre base configurée par l'utilisateur.

## Construction du patch

Le patch est vérifié sur la révision Dolphin amont
`54070da5851e12f2d1a4389daa528e4fb81327ce`. L'outil
`tools/apply_dolphin_patch.py` vérifie ce commit **avant toute modification** et applique les
overlays ainsi que le patch natif sans chemin absolu propre à une machine :

```powershell
python tools/apply_dolphin_patch.py C:\chemin\vers\dolphin
```

Une autre révision est refusée par défaut. L'option `--allow-unsupported` existe uniquement pour
un portage volontaire dont le diff, la compilation et les tests ont été revus de nouveau ; elle ne
transforme pas cette révision en base officiellement prise en charge.

Avant de distribuer une paire :

1. construire Dolphin et SkyPortal depuis les sources annoncées ;
2. signer les deux APK avec la même clé ;
3. comparer leurs certificats avec `apksigner verify --print-certs` ;
4. publier les SHA-256 et les sources correspondantes de Dolphin ;
5. ne jamais joindre la clé privée aux artefacts.

Voir [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md) pour la procédure complète.

## Redistribution publique du Dolphin modifié

Dolphin et les ajouts SkyPortal intégrés à son arborescence sont distribués sous les conditions de
licence applicables au projet Dolphin. Tous les avis de copyright et identifiants SPDX existants
doivent être conservés. Le fichier `COPYING` de Dolphin et les textes concernés de son dossier
`LICENSES/` doivent accompagner le code source correspondant.

Une publication binaire doit fournir ensemble, sur un emplacement durable :

```text
Dolphin_SkyPortal_API3.apk
Dolphin_SkyPortal_API3_Source.zip
Dolphin_SkyPortal_API3_SHA256.txt
Dolphin_SkyPortal_API3_Rebuild_Kit.zip (traçabilité et reconstruction)
```

`Dolphin_SkyPortal_API3_Source.zip` ne doit pas être un simple patch dépendant d'une copie amont
éphémère. Il contient l'arborescence source Dolphin modifiée complète du binaire publié, basée sur
le commit amont `54070da5851e12f2d1a4389daa528e4fb81327ce`, sous-modules compris. Il conserve
également `COPYING`, les fichiers `LICENSES/` applicables, les sources ajoutées et un manifeste de
provenance, ainsi que `SKYPORTAL_LICENSE.txt` et `SKYPORTAL_NOTICE.md`.

Le kit de reconstruction publié au même endroit conserve les éléments de traçabilité :

- `smart-portal-core.patch` et `skyportal-dolphin.patch` ;
- les overlays AIDL et Kotlin ajoutés, ainsi que les modifications Manifest et Gradle ;
- `tools/apply_dolphin_patch.py` et tout autre script réellement utilisé pour produire l'APK ;
- les options de construction, dont un éventuel `-PskyPortalVersionCode=...` ;
- des instructions reproductibles indiquant les outils, commandes et variantes de build.

Le code source complet et le kit doivent rester associés. Publier uniquement le kit de patches ou
uniquement un lien vers le dépôt amont ne constitue pas ici le paquet de code source correspondant
attendu.

`Dolphin_SkyPortal_API3_SHA256.txt` doit contenir au minimum les SHA-256 de l'APK, de l'archive
source et du kit. Les artefacts doivent rester accessibles ensemble pendant toute la durée de mise à
disposition du binaire. La clé de signature et ses secrets ne font jamais partie de l'archive
source ni des artefacts publics.
