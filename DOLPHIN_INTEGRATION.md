# SkyPortal Thor ↔ Dolphin Android

## État V3
Le bridge n'est plus théorique : il est défini en AIDL et un service Dolphin minimal est fourni dans `dolphin-patch/`.

### Flux de chargement
1. SkyPortal scanne le dossier racine via Storage Access Framework.
2. Chaque `.sky` garde son `content:// URI` réel.
3. L'utilisateur touche un slot logique puis un personnage.
4. SkyPortal vérifie que le fichier est ouvrable en lecture/écriture.
5. SkyPortal accorde READ + WRITE uniquement au package Dolphin connecté.
6. SkyPortal appelle `ISkylanderPortalService.load()`.
7. Le service Dolphin appelle l'API existante `SkylanderConfig.loadSkylander()`.
8. Dolphin ouvre le même fichier et conserve la propriété des écritures de progression.

### Pourquoi ne pas modifier directement le dump dans SkyPortal
Pendant qu'une figurine est montée, Dolphin peut écrire XP, or et progression. SkyPortal ne doit jamais écrire simultanément dans le même fichier.

### API AIDL compatible V1/V2
```text
getApiVersion() -> Int
ping() -> Boolean
load(logicalSlot, uri, displayName) -> actualPortalSlot
remove(logicalSlot) -> Boolean
clear()
getStatusJson() -> JSON
```

La signature AIDL n'a pas changé. Le service fourni annonce l'API 2 et ajoute les codes suivants :

```text
-4 : URI inaccessible dans le processus Dolphin
-5 : données .sky rejetées par SkylanderConfig
-6 : aucun slot natif disponible
```

SkyPortal V3 accepte aussi le service API 1 déjà installé et reconnaît son ancien signal `255` pour un portail plein.

### Sécurité
Le service Dolphin est protégé par la permission signature :
`com.skyportalthor.permission.PORTAL_CONTROL`.

Lorsque Debug et Release modifiés sont tous deux présents, l'interface permet de choisir explicitement la cible afin de ne pas charger le personnage dans le mauvais processus Dolphin.
