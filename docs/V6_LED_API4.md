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

Le transport décrit ici alimente désormais le [portail animé Compose](V6_ANIMATED_PORTAL.md), intégré séparément dans la PR #12. Il n’envoie aucune commande à Bifrost.

## État de validation

- contrat et implémentation Android : couverts par tests JVM et Android CI ;
- patch natif : vérifié par application/réversion et construction Dolphin dans le workflow de paire ;
- observations matérielles historiques sur l’AYN Thor : séquences LED, valeurs RGB gauche/droite et affichage du portail dans Spyro’s Adventure, consignés dans la [PR #14](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/14) ;
- activation/keepalive : correctif [PR #13](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/13) validé historiquement sur Thor puis fusionné le 19 août 2026 ;
- composition finale du panneau : chantier de validation PR #14, suivi dans [PROJECT_STATUS.md](PROJECT_STATUS.md) ;
- release stable publique : reste `v0.5.0`, Dolphin API 3.

Ces observations historiques ne valident ni les autres jeux, ni le canal Trap en jeu, ni chaque APK ultérieur. La provenance de l’APK et les résultats de chaque nouvelle session doivent être relevés séparément.

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
- la conservation du comportement d’activation/keepalive de Dolphin pour toute commande `A` valide, y compris `A 00`.

Le premier patch LED avait interprété `A 00` comme une désactivation. La PR #13 a supprimé cette régression : Spyro’s Adventure peut alterner `A 00` et `A 01` pendant un polling normal. Le réglage d’émulation USB contrôle la disponibilité du périphérique ; l’octet de polling ne doit pas éteindre artificiellement le portail. Voir [le rapport du correctif](V6_PORTAL_DETECTION_FIX.md).

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

## État exposé à l’interface

`PortalState` contient désormais :

```kotlin
portalLedState: PortalLedState?
portalLedWarnings: List<String>
portalLedError: String?
```

Ces champs alimentent le portail animé V6 via `AnimatedPortalStateMapper`. Le rendu Trap est conditionné à la disponibilité du canal et à `GameFeature.TRAPS` du jeu actif : la présence d’un canal dans le JSON ne suffit pas. Bifrost reste une future sortie facultative du même modèle :

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
- garde contre la réintroduction de `A 00 → Deactivate()` et précontrôle d’activation avant chargement ;
- présence et ordre des patches dans l’outil de build ;
- noms des artefacts API 4.

Test natif ajouté dans Dolphin :

```text
LedSnapshotTracksOnlyVisibleChanges
```

Il couvre les trois zones, les doublons, l’alias gauche `0x04` et les transitions d’activation.

## Limites connues

- le snapshot transporte la couleur cible, pas la durée de fondu des commandes `J` ;
- Compose produit les transitions visuelles entre deux états ; la composition finale sur l’écran inférieur reste à valider dans la PR #14 ;
- Bifrost n’est pas encore déclaré ni contacté ;
- aucune LED physique n’est encore modifiée ;
- seuls les essais historiques dans Spyro’s Adventure sont documentés ; les autres jeux et le canal Trap n’ont pas été validés matériellement.

## Étape suivante

Terminer la validation visuelle et fonctionnelle de la [PR #14](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/14), avec l’APK identifié dans le suivi de reprise. L’intégration Bifrost ne peut commencer qu’après clôture de cette étape et autorisation explicite de l’utilisateur.
