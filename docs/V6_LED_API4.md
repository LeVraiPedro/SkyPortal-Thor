<!--
  Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
  SPDX-License-Identifier: GPL-2.0-or-later
-->

# SkyPortal Thor V6 — Dolphin LED API 4

## Finalité

Cette étape relie le modèle lumineux pur de la fondation V6 au véritable Portal of Power émulé par Dolphin.

```text
Jeu Skylanders
→ commandes USB C / J / L / A
→ SkylanderPortal dans Dolphin
→ snapshot natif verrouillé
→ JNI SkylanderConfig
→ service Binder API 4
→ PortalLedState dans SkyPortal
```

Elle ne dessine pas encore le portail animé et n’envoie aucune commande à Bifrost.

## État de validation

- contrat et implémentation Android : couverts par tests JVM et Android CI ;
- patch natif : vérifié par application/réversion et construction Dolphin dans le workflow de paire ;
- comportement sur l’AYN Thor : à valider dans une étape matérielle ultérieure ;
- release stable publique : reste `v0.5.0`, Dolphin API 3.

Ne pas présenter l’API 4 comme matériellement validée avant un test de la paire exacte sur la Thor.

## Contrat AIDL

Les huit méthodes API 1 à 3 conservent strictement leur ordre. L’API 4 ajoute uniquement, en dernière position :

```aidl
String getPortalLedStateJson();
```

Le compagnon n’appelle cette transaction qu’après confirmation de `getApiVersion() >= 4`. Une paire API 1, 2 ou 3 reste utilisable selon son mode dégradé historique.

## Payload JSON

Schéma initial :

```json
{
  "schemaVersion": 1,
  "active": true,
  "sequence": 42,
  "left": { "r": 160, "g": 64, "b": 255 },
  "right": { "r": 12, "g": 100, "b": 220 },
  "trap": { "r": 255, "g": 40, "b": 0 }
}
```

Règles :

- `schemaVersion` vaut actuellement `1` ;
- `active` représente l’état d’activation protocolaire du portail ;
- `sequence` augmente uniquement lorsqu’un état lumineux visible change ;
- les commandes identiques ne font pas avancer `sequence` ;
- `left`, `right` et `trap` contiennent des canaux entiers `0..255` ;
- un runtime Dolphin non initialisé renvoie un état éteint minimal ;
- un payload invalide ne bloque jamais les fonctions de chargement des `.sky`.

## Snapshot JNI

`SkylanderConfig.getPortalLedState()` renvoie un `LongArray` de 12 valeurs :

```text
0  schemaVersion
1  active (0 ou 1)
2  sequence
3  left.red
4  left.green
5  left.blue
6  right.red
7  right.green
8  right.blue
9  trap.red
10 trap.green
11 trap.blue
```

Le service vérifie la taille, le schéma, le booléen, la séquence et chaque canal avant de produire le JSON.

## Capture native

Le cœur Dolphin possédait déjà trois couleurs internes :

```text
m_color_left
m_color_right
m_color_trap
```

L’API 4 ajoute :

- `SkylanderLEDStateSnapshot` ;
- `GetLEDStateSnapshot()` protégé par `sky_mutex` ;
- `m_led_sequence` ;
- l’incrément de séquence sur les changements gauche, droite, Trap et activation ;
- la suppression des incréments pour une commande RGB identique ;
- la prise en charge de `0x04` comme alias gauche utilisé par la commande audio `L` ;
- la traduction de la position audio `0x01` vers la zone Trap interne `0x03` ;
- la désactivation réelle lorsque la commande `A` porte la valeur `0`.

Le patch est séparé dans :

```text
dolphin-patch/portal-led-api4.patch
```

Il s’applique après :

```text
dolphin-patch/smart-portal-core.patch
```

## Transport côté compagnon

`DolphinPortalBridge` conserve deux cadences distinctes :

```text
statut Smart Portal complet : toutes les 2 secondes
état LED léger API 4       : toutes les 100 ms
```

Les appels sont sérialisés avec les opérations de chargement/retrait afin qu’une réponse tardive ne remplace pas un état plus récent.

Le transport LED :

- utilise un timeout court de `750 ms` ;
- efface l’état lorsque l’émulation n’est ni en cours ni en pause ;
- conserve le dernier état confirmé en cas d’erreur temporaire ;
- coupe immédiatement les faits distants après une mort Binder ;
- ignore les séquences obsolètes ;
- signale un conflit si deux contenus différents portent la même séquence ;
- n’empêche jamais un chargement de figurine uniquement parce que le canal LED est indisponible.

## État exposé à la future interface

`PortalState` contient désormais :

```kotlin
portalLedState: PortalLedState?
portalLedWarnings: List<String>
portalLedError: String?
```

Ces champs ne sont pas encore rendus dans l’interface V5. Ils serviront de source unique à :

```text
AnimatedPortal
BifrostLedOutputBridge
Diagnostic V6
```

## Tests

Tests JVM ajoutés :

- ordre AIDL et copies identiques ;
- annonce `API_VERSION = 4` ;
- présence de la transaction LED et de la table JNI ;
- parsing et réduction côté compagnon ;
- API 3 sans erreur de compatibilité ;
- runtime indisponible ;
- payload initial, obsolète, conflictuel et malformé ;
- erreur de transport non bloquante ;
- nettoyage de l’état LED après mort Binder ;
- présence et ordre des patches dans l’outil de build ;
- noms des artefacts API 4.

Test natif ajouté dans Dolphin :

```text
LedSnapshotTracksOnlyVisibleChanges
```

Il couvre les trois zones, les doublons, l’alias gauche `0x04` et les transitions d’activation.

## Limites connues

- le snapshot transporte la couleur cible, pas la durée de fondu des commandes `J` ;
- la fluidité visuelle sera produite plus tard par Compose entre deux états ;
- aucune couleur n’est encore affichée à l’écran ;
- Bifrost n’est pas encore déclaré ni contacté ;
- aucune LED physique n’est encore modifiée ;
- la paire API 4 doit encore être installée et observée sur la vraie Thor.

## Étape suivante

```text
agent/v6-animated-portal
```

Cette branche devra consommer `PortalState.portalLedState` pour créer le portail Compose animé, sans intégrer Bifrost dans la même pull request.
