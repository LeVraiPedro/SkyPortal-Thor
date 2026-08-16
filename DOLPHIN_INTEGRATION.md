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

`getStatusJson()` expose aussi `emulationState`, `gameId`, `gameTitle`, `portalEnabled`, `portalActivated`, `canSetPortalEnabled` et `nativeSlots`.

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
255 : ancien signal natif « aucun slot disponible »
```

SkyPortal accepte aussi les services API 1 et API 2 et reconnaît l'ancien signal `255` pour un portail plein. Aucun retour négatif, aucun timeout et aucun slot non confirmé ne doit être transformé en succès.

### Robustesse de la connexion

- Toutes les opérations Binder sont bornées par un délai maximal et exécutées hors du thread UI.
- Un `DeathRecipient` invalide immédiatement la connexion et les faits distants connus.
- Une génération de connexion empêche un résultat tardif de l'ancien processus Dolphin d'écraser le nouvel état.
- Après reconnexion, le mapping J1/J2 est réconcilié avec l'identité des 16 slots natifs.
- Un même URI ne peut pas être monté silencieusement dans plusieurs slots logiques.
- Avec une API 1/2 qui ne restitue pas l’URI d’un slot restauré, SkyPortal bloque tout nouveau chargement dans un autre slot jusqu’au retrait du montage non identifié. Avec l’API 3, un slot natif occupé directement dans le Manager et non revendiqué est bloquant pour la même raison.
- Les API 1/2 n’exposent pas les slots chargés directement dans le Manager Dolphin : ne jamais utiliser le Manager en parallèle du compagnon dans ce mode. La garantie forte anti-double montage et le backup sécurisé nécessitent l’API 3.
- Les journaux du bridge et du service n'exposent pas les URI SAF complètes.

Le service exporté peut être recréé seul pendant le redémarrage de Dolphin. Il vérifie donc `DirectoryInitialization.areDolphinDirectoriesReady()` avant tout accès à `NativeConfig` ou aux API natives. Tant que l'initialisation n'est pas terminée, `getStatusJson()` renvoie un instantané vide avec `serviceState = INITIALIZING`, le catalogue est vide et une commande de chargement renvoie `-10`. Le compagnon affiche alors un état transitoire et peut réessayer sans faire crasher le processus Dolphin.

Cette garde a été ajoutée après avoir reproduit sur la Thor un `SIGSEGV` du processus service-only avant l'initialisation native. Après rebuild/réinstallation, le scénario d'arrêt brutal a été rejoué : service relancé, slots vidés, nouvelle détection `SSPP52` et aucun crash natif ou spam `DeadObjectException` dans le Logcat frais.

### État et catalogue API 3

`getStatusJson()` fournit au minimum :

```text
emulationState, gameId, gameTitle,
portalEnabled, portalActivated, canSetPortalEnabled,
nativeSlots[0..15]
```

`getFigureCatalogJson()` exporte les identités de la table native `list_skylanders`. Le compagnon garde son modèle central de compatibilité, mais ne duplique pas une grande base de noms indépendante quand le catalogue API 3 est disponible.

### Sécurité
Le service Dolphin est protégé par la permission signature :
`com.skyportalthor.permission.PORTAL_CONTROL`.

Lorsque Debug et Release modifiés sont tous deux présents, l'interface permet de choisir explicitement la cible afin de ne pas charger le personnage dans le mauvais processus Dolphin.

La validation matérielle V5 a utilisé `org.dolphinemu.dolphinemu.debug`, API 3, avec le compagnon `com.skyportalthor.app`. Les deux APK Debug avaient le même certificat. Le build Release installé sur l'appareil n'était pas le service API 3 testé.

## Construction du patch

Le patch est prévu pour la révision Dolphin épinglée et documentée par le workflow manuel `full-pair-build.yml`. L'outil `tools/apply_dolphin_patch.py` applique le patch sans chemins absolus propres à une machine.

Avant de distribuer une paire :

1. construire Dolphin et SkyPortal depuis les sources annoncées ;
2. signer les deux APK avec la même clé ;
3. comparer leurs certificats avec `apksigner verify --print-certs` ;
4. publier les SHA-256 et les sources correspondantes de Dolphin ;
5. ne jamais joindre la clé privée aux artefacts.

Voir [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md) pour la procédure complète.
