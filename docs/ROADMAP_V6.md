<!--
  Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
  SPDX-License-Identifier: GPL-2.0-or-later
-->

# SkyPortal Thor V6 — Roadmap

## Vision

SkyPortal Thor V6 doit transformer l’écran inférieur et les anneaux RGB des joysticks de l’AYN Thor en un **Portal of Power numérique cohérent, animé et synchronisé avec le jeu**.

```text
Écran supérieur
└─ Dolphin SkyPortal Edition + jeu Skylanders

Écran inférieur
└─ SkyPortal Thor : portail animé, joueurs, collection et commandes

Joysticks RGB
└─ prolongement lumineux facultatif via Bifrost
```

Le socle V5 reste la référence de sûreté :

```text
SkyPortal Android
→ Binder/AIDL local
→ Dolphin SkyPortal API
→ émulation native du Portal of Power
→ fichiers .sky
```

Dolphin demeure le seul processus autorisé à écrire dans une figurine montée.

## Avancement actuel

```text
V6 Foundation       ✓ fusionnée
Dolphin LED API 4   ✓ fusionnée, validation Thor à faire
Portail animé       ✓ implémenté dans la source, validation visuelle à faire
Bifrost             non commencé
```

Le contrat technique API 4 est documenté dans [`V6_LED_API4.md`](V6_LED_API4.md).

## Principes non négociables

- aucune régression des fonctions V5 ;
- aucune écriture directe par SkyPortal dans un fichier `.sky` monté ;
- fonctionnement sans Internet et sans serveur externe ;
- communication locale uniquement ;
- fonctionnement complet de base sans Bifrost ;
- restauration du profil LED de l’utilisateur après libération du contrôle ;
- aucune ROM, aucun jeu et aucun dump tiers distribué ;
- aucune ressource officielle non autorisée intégrée ;
- compatibilité prioritaire avec l’AYN Thor Max sous Android 13 ;
- validation réelle sur les écrans logiques `0` et `4` ;
- fonctions matérielles désactivables individuellement ;
- toute nouvelle méthode AIDL est ajoutée à la fin du contrat existant.

---

# V6 Foundation — chantier préparatoire

## Objectif

Préparer l’architecture V6 sans modifier le comportement de la version stable 0.5.0.

Cette phase doit fournir :

- la présente roadmap versionnée ;
- un modèle pur `PortalLedState` ;
- un parseur strict et testable du futur payload Dolphin LED ;
- une abstraction `LedOutputBridge` indépendante de Bifrost ;
- une sortie `NoOp` lorsque les LED ne sont pas disponibles ;
- une politique déterministe de limitation de fréquence ;
- des tests JVM sans dépendance au matériel ;
- une frontière claire entre l’état lumineux Dolphin, le rendu Compose et la sortie physique.

Cette phase ne doit pas encore :

- modifier l’AIDL publié ;
- passer Dolphin en API 4 ;
- envoyer de commande à Bifrost ;
- modifier les LED de la Thor ;
- changer l’interface visible ;
- augmenter le numéro de version Android ;
- publier une release.

## Architecture cible

```text
Dolphin API 4
└─ payload LED versionné
   ↓
PortalLedStateParser
   ↓
PortalLedState
   ├─ AnimatedPortal (Compose)
   └─ LedOutputCoordinator
      ├─ LedCommandRateLimiter
      ├─ BifrostLedOutputBridge
      └─ NoOpLedOutputBridge
```

Le même état doit alimenter l’écran et les joysticks afin d’éviter toute divergence visuelle.

---

# V6.0 — Portal Experience

## Objectif

Créer la nouvelle identité visuelle de SkyPortal et synchroniser en temps réel :

- l’état lumineux émulé dans Dolphin ;
- le portail animé sur l’écran inférieur ;
- les LED gauche et droite de l’AYN Thor via Bifrost.

## Interface

- portail central animé ;
- cartes J1 et J2 clairement séparées ;
- états de connexion compacts ;
- couleurs par élément ;
- animations de pose, remplacement et retrait ;
- favoris, récents, équipes rapides et mode 1J/2J conservés ;
- navigation tactile adaptée à l’écran inférieur ;
- mode d’animations réduites pour l’accessibilité et la batterie.

## Dolphin SkyPortal API 4

**État : implémenté dans la source, non encore validé sur la Thor.**

Le patch Dolphin expose un état lumineux versionné comprenant au minimum :

- couleur gauche ;
- couleur droite ;
- couleur de Trap facultative ;
- portail actif ou éteint ;
- compteur de séquence monotone ;
- version du schéma.

Première forme envisagée :

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

Le contrat API 1–3 existant doit rester inchangé. Toute nouvelle méthode AIDL sera ajoutée à la fin et ne sera appelée qu’après confirmation d’une API 4 compatible.

## Portail animé

**État : implémenté dans la source, non encore validé sur l’écran inférieur réel.**

Le rendu Compose doit prendre en charge :

- activation et extinction progressives ;
- changements de couleur fluides ;
- couleurs gauche et droite distinctes ;
- couleur de Trap lorsqu’elle est disponible ;
- pulsation de pose ;
- fondu de retrait ;
- conflit USB ;
- attente de handshake ;
- perte et reprise de Dolphin.

Le rendu peut rester fluide à la fréquence de l’écran, indépendamment de la cadence des LED physiques.

## Intégration Bifrost

SkyPortal utilisera l’API publique de broadcasts Android de Bifrost, sans copier son contrôleur matériel ni appeler directement `PServerBinder`.

États à gérer :

- Bifrost absent ;
- Bifrost installé mais service arrêté ;
- contrôle tiers désactivé ;
- commande acceptée ;
- commande rejetée ou limitée ;
- API Bifrost incompatible.

Modes prévus :

1. **Portal of Power** : gauche du portail → stick gauche, droite → stick droit ;
2. **Joueurs** : élément de J1 → gauche, élément de J2 → droite ;
3. **Élément actif** : dernière figurine active sur les deux sticks ;
4. **Désactivé** : portail animé uniquement.

Réglages prévus :

- activation de la synchronisation ;
- mode lumineux ;
- même couleur sur les deux sticks ;
- luminosité maximale ;
- transitions douces ;
- pulsation lors d’une pose ;
- signal lumineux des erreurs ;
- restauration du profil précédent.

## Fréquence et sécurité LED

- maximum nominal : 4 commandes par seconde ;
- suppression des commandes identiques ;
- regroupement des changements rapides ;
- dernière couleur utile conservée ;
- événements critiques envoyés sans boucle ;
- `ACTION_CLEAR` lors de l’arrêt du jeu, de la désactivation ou de la fermeture contrôlée ;
- aucune erreur bloquante si Bifrost est absent.

## Diagnostic V6.0

Le diagnostic doit afficher :

- version API Dolphin ;
- version du schéma LED ;
- séquence reçue ;
- couleurs gauche, droite et Trap ;
- état actif ;
- présence et version de Bifrost ;
- état du service ;
- contrôle tiers autorisé ou non ;
- dernière commande et dernier code résultat ;
- cadence effective ;
- mode et luminosité choisis.

## Validation V6.0

- portail animé conforme au payload Dolphin ;
- couleurs gauche et droite distinctes validées ;
- absence de clignotement parasite ;
- limite de fréquence respectée ;
- restauration Bifrost après `ACTION_CLEAR` ;
- absence de crash lorsque Bifrost manque ;
- aucune perte de performance perceptible dans Dolphin ;
- écran inférieur fluide ;
- modes 1J/2J et fonctions V5 inchangés ;
- tests réels sur AYN Thor et contrôle Logcat.

---

# V6.1 — Créateur de figurines

## Objectif

Créer une figurine depuis SkyPortal sans ouvrir le gestionnaire Dolphin.

```text
Jeu d’origine
→ élément
→ personnage
→ variante
→ nom du fichier
→ dossier de destination
→ création par Dolphin
```

Dolphin doit :

- valider ID et variant ;
- générer un dump vierge valide ;
- écrire via SAF ;
- refuser tout écrasement involontaire ;
- retourner le résultat réel.

SkyPortal choisit et affiche ; Dolphin génère et écrit ; SkyPortal rescanne.

---

# V6.2 — Objets temporaires

## Objectif

Permettre d’utiliser sans fichier permanent les contenus qui ne nécessitent pas de progression durable :

- Magic Items ;
- Adventure Packs et Location Pieces ;
- Sidekicks ;
- autres objets explicitement reconnus comme temporaires.

Les personnages, Traps, Creation Crystals et véhicules avec progression conservent un fichier permanent.

API envisagée :

```text
loadTemporaryFigure(slot, figureId, variantId, displayName)
removeTemporaryFigure(slot)
```

La génération et la mémoire temporaire restent sous la responsabilité de Dolphin.

---

# V6.3 — Lecture de progression

## Objectif

Afficher en lecture seule les informations contenues dans les dumps :

- niveau ;
- expérience ;
- or ;
- chemin d’amélioration ;
- améliorations débloquées ;
- chapeau ;
- surnom ;
- défis héroïques ;
- génération et variante ;
- état du fichier et checksums ;
- dernière modification ;
- backup le plus récent.

Une figurine active ne doit jamais être lue au milieu d’une écriture. SkyPortal utilisera soit un snapshot cohérent fourni par Dolphin, soit une lecture hors montage.

---

# V6.4 — Collection avancée et finition

Fonctions envisagées :

- fiches détaillées ;
- tris par niveau, élément, jeu et dernière utilisation ;
- détection des doublons ;
- comparaison de variantes ;
- progression globale de collection ;
- statistiques par élément ;
- personnages jamais utilisés ;
- filtres enregistrés ;
- recherche tolérante ;
- backups automatiques configurables ;
- restauration guidée ;
- export JSON ;
- mode galerie ;
- optimisation batterie et accessibilité.

---

# Ordre officiel

```text
v0.5.0 stable et figée
    ↓
V6 Foundation
    ↓
V6.0 — Portal Experience
    ↓
V6.1 — Créateur de figurines
    ↓
V6.2 — Objets temporaires
    ↓
V6.3 — Lecture de progression
    ↓
V6.4 — Collection avancée et finition
```

Chaque étape doit passer par une branche `agent/*`, une pull request vers `main` et le contrôle obligatoire `validate`. Les tags `v*` publiés ne doivent jamais être déplacés ni réutilisés.
