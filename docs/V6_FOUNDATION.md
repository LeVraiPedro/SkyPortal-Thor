<!--
  Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
  SPDX-License-Identifier: GPL-2.0-or-later
-->

# SkyPortal Thor V6 Foundation

## Finalité

Cette fondation prépare la V6 sans modifier le comportement de SkyPortal Thor 0.5.0. Elle établit des contrats purs et testables avant toute modification de Dolphin, de l’interface ou des LED de l’AYN Thor.

## État implémenté

```text
app/src/main/java/com/skyportalthor/app/portal/led/
├─ PortalLedState.kt
├─ PortalLedStateParser.kt
├─ PortalLedStateReducer.kt
├─ LedOutputBridge.kt
└─ LedCommandRateLimiter.kt
```

### `PortalLedState`

État lumineux indépendant de toute interface :

- version de schéma ;
- portail actif ou éteint ;
- séquence monotone ;
- couleur gauche ;
- couleur droite ;
- couleur de Trap facultative.

Les canaux RGB sont strictement limités à `0..255`.

### `PortalLedStateParser`

Parse le contrat JSON envisagé pour Dolphin API 4 :

```json
{
  "schemaVersion": 1,
  "active": true,
  "sequence": 42,
  "left": { "r": 160, "g": 64, "b": 255 },
  "right": { "r": 160, "g": 64, "b": 255 },
  "trap": null
}
```

Règles actuelles :

- `schemaVersion`, `active` et `sequence` sont obligatoires ;
- seule la version de schéma `1` est acceptée ;
- `sequence` doit être un entier positif ou nul ;
- `left` est obligatoire lorsque le portail est actif ;
- l’absence de `right` recopie `left` et produit un avertissement ;
- un portail éteint peut omettre les couleurs ;
- un payload invalide produit un résultat typé, jamais une exception vers l’appelant.

### `PortalLedStateReducer`

Protège l’état contre les réponses reçues hors ordre :

- première valeur : `INITIAL` ;
- séquence supérieure : `ADVANCED` ;
- même valeur : `DUPLICATE` ;
- séquence inférieure : `STALE` ;
- même séquence avec contenu différent : `CONFLICT` et conservation de l’état courant.

### `LedOutputBridge`

Frontière entre le domaine SkyPortal et un contrôleur physique :

```text
PortalLedState
→ LedOutputFrame
→ LedOutputBridge
```

La fondation fournit uniquement `NoOpLedOutputBridge`. Elle garantit que l’absence de Bifrost ou de matériel ne devient jamais une erreur bloquante.

### `LedCommandRateLimiter`

Politique déterministe conçue pour la limite publique Bifrost :

- intervalle par défaut : `250 ms` ;
- première trame immédiatement admissible ;
- trames identiques supprimées ;
- changement trop rapide différé avec délai restant ;
- répétition volontaire possible, sans contourner la cadence ;
- récupération après remise à zéro ou recul de l’horloge injectée.

Le limiteur ne réalise aucun envoi. Le futur coordinateur devra appeler `markEmitted` uniquement après un envoi réellement accepté.

## Couverture de tests

```text
app/src/test/java/com/skyportalthor/app/portal/led/
├─ PortalLedStateParserTest.kt
├─ PortalLedStateReducerTest.kt
├─ LedCommandRateLimiterTest.kt
└─ LedOutputBridgeTest.kt
```

Les tests couvrent :

- couleurs gauche/droite indépendantes ;
- couleur de Trap ;
- miroir gauche → droite ;
- extinction sans couleur ;
- schéma inconnu ;
- canal hors plage ;
- séquence fractionnaire ;
- JSON malformé ;
- états initial, avancé, dupliqué, obsolète et conflictuel ;
- suppression des trames identiques ;
- délai de `250 ms` ;
- répétition d’événement ;
- récupération après reset ;
- sortie `NoOp` ;
- conversion RGB/ARGB.

## Étape suivante réalisée : Dolphin LED API 4

La fondation est maintenant reliée au cœur Dolphin par une couche API 4 séparée :

- méthode AIDL ajoutée à la fin du contrat ;
- snapshot natif gauche/droite/Trap ;
- séquence monotone ;
- JSON de schéma 1 ;
- résolution non bloquante côté compagnon ;
- polling léger dédié ;
- tests JVM et natif.

La spécification complète se trouve dans [`V6_LED_API4.md`](V6_LED_API4.md). Cette implémentation reste non validée sur le matériel tant que la paire API 4 n’a pas été testée sur l’AYN Thor.

## Hors périmètre volontaire de la fondation initiale

Cette branche ne modifie pas :

- `ISkylanderPortalService.aidl` ;
- la valeur `API_VERSION` de Dolphin ;
- `DolphinPortalBridge` ;
- `PortalActivity` ;
- `PortalScreen` ;
- `AndroidManifest.xml` ;
- la version Android `0.5.0` / code `7` ;
- les workflows de release ;
- les APK publiés sous `v0.5.0`.

Elle ne contacte pas Bifrost et ne touche pas `PServerBinder`.

## Prochaines PR recommandées

### 1. `agent/v6-led-api4` — réalisé dans la source

- capture native et payload versionné ajoutés ;
- AIDL étendu en dernière position ;
- API Dolphin portée à 4 ;
- API 1–3 conservées ;
- transport et tests ajoutés ;
- validation matérielle encore requise.

### 2. `agent/v6-animated-portal` — prochaine étape

- extraire les composants de `PortalScreen.kt` ;
- ajouter `AnimatedPortal` ;
- utiliser `PortalLedState` comme source unique ;
- gérer animations réduites et perte de connexion ;
- ajouter des tests Compose ciblés.

### 3. `agent/v6-bifrost-bridge`

- déclarer la permission et la visibilité du package Bifrost ;
- implémenter les broadcasts explicites documentés par Bifrost ;
- ajouter le coordinateur avec limitation et coalescence ;
- envoyer `ACTION_CLEAR` lors de la libération ;
- gérer absence, service arrêté, contrôle tiers désactivé et rate limit ;
- conserver `NoOpLedOutputBridge` comme fallback.

### 4. `agent/v6-thor-led-validation`

- compiler la paire exacte SkyPortal/Dolphin ;
- installer sur l’AYN Thor sans effacer les données ;
- valider écrans `0` et `4` ;
- tester couleurs identiques et indépendantes ;
- tester extinction, reconnexion et restauration Bifrost ;
- mesurer la cadence et l’impact sur Dolphin ;
- examiner Logcat ;
- documenter les résultats matériels.

## Règles de reprise

- partir du dernier `main` après fusion de cette fondation ;
- ne jamais pousser directement sur `main` ;
- passer par une branche `agent/*` et une pull request ;
- attendre le contrôle obligatoire `validate` ;
- ne jamais déplacer un tag `v*` publié ;
- ne pas présenter une fonction LED comme validée avant le test réel sur la Thor.
