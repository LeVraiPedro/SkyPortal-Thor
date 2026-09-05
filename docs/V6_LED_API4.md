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

Le transport décrit ici alimente le [portail animé Compose](V6_ANIMATED_PORTAL.md),
intégré séparément dans la PR #12. Il n’envoie pas lui-même de commande à Bifrost :
la sortie facultative du compagnon, désormais développée dans la
[PR #15 en brouillon](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/15), consomme
ce modèle sans modifier Dolphin. Voir le [contrat Bifrost](V6_BIFROST.md).

## État de validation

- contrat et implémentation Android : couverts par tests JVM et Android CI ;
- patch natif : vérifié par application/réversion et construction Dolphin dans le workflow de paire ;
- observations matérielles historiques sur l’AYN Thor : séquences LED, valeurs RGB gauche/droite et affichage du portail dans Spyro’s Adventure, consignés dans la [PR #14](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/14) ;
- activation/keepalive : correctif [PR #13](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/13) validé historiquement sur Thor puis fusionné le 19 août 2026 ;
- composition et fonctions du compagnon : revalidation ciblée en partie SSA le 5 septembre 2026, de 12:16 à 12:33, avec le compagnon `d466536` et Dolphin `11353ca` ; affichage sans chevauchement ni Trap, chargements/remplacements/retraits J1 et cycle J2 logique confirmés ;
- cycle de vie Dolphin : menu avec figurine, sortie d’une session restaurée et reconnexion automatique après arrêt forcé contrôlés ; preuves et limites dans [PROJECT_STATUS.md](PROJECT_STATUS.md) ;
- release stable publique : reste `v0.5.0`, Dolphin API 3.

Les preuves historiques restent distinctes de la revalidation du 5 septembre.
Elles ne valident ni les autres jeux, ni le canal Trap en jeu, ni chaque APK ultérieur.
Les deux APK réellement utilisés et leur provenance sont identifiés dans le suivi :
le compagnon produit par le workflow de paire Dolphin de cette campagne n’a pas
été installé. La PR #14 a été fusionnée après autorisation explicite dans
`12d23a1db1b0fb9214d4386072dcfc44c1858f2f`, avec CI de `main` réussie. La PR #15
reste un chantier distinct, sans validation matérielle Bifrost revendiquée.

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

Ces champs alimentent le portail animé V6 via `AnimatedPortalStateMapper`. Le rendu
Trap est conditionné à la disponibilité du canal et à `GameFeature.TRAPS` du jeu
actif : la présence d’un canal dans le JSON ne suffit pas. La PR #15 ajoute une
sortie Bifrost facultative, désactivée par défaut, utilisant le même modèle avec
un contrôle de fraîcheur de 1 500 ms maximum. Les séquences et couleurs observées
dans Compose ne prouvent pas l’application aux LED physiques :

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
- Compose produit les transitions visuelles entre deux états ; la disposition corrigée a été contrôlée sur l’écran inférieur dans SSA, sans chevauchement ni canal Trap injustifié ;
- Bifrost est désormais déclaré et son transport est implémenté en branche dans
  la PR #15, pas encore fusionné ; la provenance du candidat et les contrôles
  locaux sont consignés dans le suivi ;
- aucun résultat de commande sur les LED physiques ni de restauration Bifrost
  n’est encore validé ; une réponse du receiver ne confirme pas le matériel ;
- les essais matériels, historiques et du 5 septembre, restent limités à Spyro’s Adventure ; les autres jeux et le canal Trap n’ont pas été validés matériellement.

## Étape suivante

La PR #14 est clôturée et l’intégration Bifrost a été autorisée séparément.
La [PR #15](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/15) reste en brouillon :
terminer la préparation du service Bifrost et vérifier le candidat signé du dernier
commit avant les tests matériels décrits dans [V6_BIFROST.md](V6_BIFROST.md).
Les modes LED J1/J2 et priorité J1 restent à faire ; cette étape ne clôture pas
V6.0. Le [suivi courant](PROJECT_STATUS.md) fait autorité sur les résultats récents.
