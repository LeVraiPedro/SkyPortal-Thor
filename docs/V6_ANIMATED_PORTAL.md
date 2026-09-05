<!--
  Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
  SPDX-License-Identifier: GPL-2.0-or-later
-->

# SkyPortal Thor V6 — Portail animé

## Finalité

Cette étape transforme `PortalState.portalLedState` en un rendu Compose visible sur l’écran inférieur de l’AYN Thor.

```text
Jeu Skylanders
→ Dolphin SkyPortal API 4
→ PortalLedState
→ AnimatedPortalStateMapper
→ AnimatedPortalPanel
→ Canvas Compose
```

Elle ne pilote encore aucune LED physique et ne contacte pas Bifrost.

## État de reprise

Le portail animé est fusionné dans `main` depuis la [PR #12](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/12). Son affichage, les séquences LED et les valeurs gauche/droite ont été observés historiquement sur la Thor avec Spyro’s Adventure, comme le consigne la [PR #14](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/14). La correction de composition portée par cette dernière PR reste en validation ; les résultats du 5 septembre 2026 et la provenance de l’APK sont suivis dans [PROJECT_STATUS.md](PROJECT_STATUS.md).

## Position dans l’interface

Le portail animé remplace le panneau d’actions rapide qui occupait l’espace flexible de l’écran principal.

```text
En-tête
→ Joueur 1 / Joueur 2
→ Portail animé
→ Slots supplémentaires
→ Barre de collection
```

La PR #14 sépare le panneau en trois zones : état et actions en haut, portail central, puis bande RGB gauche/droite en bas. Les actions `Équipes` et `Diagnostic` restent accessibles dans la zone supérieure. La hauteur réellement disponible pour le Canvas et l’absence de chevauchement doivent être contrôlées sur l’écran inférieur ; les identifiants d’écran sont revérifiés par ADB à chaque session.

## États visuels

Le mapper pur `AnimatedPortalStateMapper` convertit l’état complet du Smart Portal en l’un des modes suivants :

- Dolphin déconnecté ;
- connexion ou attente du premier état API 4 ;
- portail désactivé ;
- initialisation ;
- conflit de périphériques USB ;
- erreur ou redémarrage requis ;
- Dolphin API 1–3 sans couleurs réelles ;
- API 4 en veille ;
- API 4 active.

Les erreurs du canal LED restent non bloquantes. Lorsqu’un dernier snapshot fiable existe, il reste affiché avec un avertissement au lieu d’éteindre brutalement le portail.

## Rendu

Le composant dessine sans ressource graphique tierce :

- le socle du portail ;
- une plateforme elliptique ;
- plusieurs couches de halo ;
- un anneau coloré gauche/droite ;
- des repères lumineux ;
- deux points lumineux latéraux ;
- une zone Trap en forme de cristal si le canal est fourni et si le jeu actif déclare `GameFeature.TRAPS` ;
- des arcs en rotation lente ;
- une respiration lumineuse ;
- une pulsation courte lorsque la composition des slots change.

Les transitions de couleur utilisent les valeurs RGB exactes reçues de Dolphin API 4.

## Compatibilité

Avec Dolphin API 4 :

- les couleurs gauche et droite viennent du jeu ;
- la zone et le badge Trap sont affichés uniquement si le canal est disponible et si le jeu actif déclare `GameFeature.TRAPS` ; Spyro’s Adventure, Giants et Swap Force n’affichent pas ce canal ;
- l’état actif et la séquence sont affichés.

Le libellé « Éclairage du portail en veille » distingue l’état lumineux du fonctionnement du Portal of Power. Le correctif [PR #13](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/13) préserve l’activation/keepalive pendant le polling `A 00` de Spyro’s Adventure ; un changement visuel ne doit pas modifier ce comportement Dolphin.

Avec Dolphin API 1, 2 ou 3 :

- le Smart Portal continue de fonctionner selon son mode historique ;
- le portail animé utilise une palette de secours ;
- l’interface indique que les couleurs du jeu sont indisponibles.

Sans Dolphin :

- le portail reste visible mais fortement atténué ;
- l’utilisateur voit immédiatement que la reconnexion est nécessaire.

## Accessibilité

Le Canvas expose une description sémantique contenant :

- l’état du portail ;
- le détail API ;
- les couleurs gauche et droite ;
- la couleur Trap éventuelle ;
- le dernier avertissement LED.

Les informations importantes ne reposent donc pas uniquement sur la couleur.

Un réglage explicite d’animations réduites reste prévu dans la suite de V6.0.

## Tests

Les tests JVM couvrent :

- le mode déconnecté ;
- la priorité du conflit USB sur un snapshot LED existant ;
- le mode dégradé API 3 ;
- l’attente du premier snapshot API 4 ;
- la conservation exacte des couleurs et de la séquence ;
- la conservation des couleurs lors d’une erreur temporaire du transport ;
- le mode veille API 4 ;
- la visibilité du canal Trap selon les fonctionnalités du jeu actif.

Ces tests ne remplacent pas le contrôle de disposition réel. La validation finale de la PR #14 doit vérifier le portail central visible, les zones sans chevauchement, les valeurs RGB lisibles, les actions accessibles et les fonctions de chargement/remplacement/retrait préservées. Les observations historiques ne suffisent pas à valider un nouvel APK ; voir le suivi de reprise pour les résultats associés à chaque artefact.

## Hors périmètre

Cette étape ne comprend pas :

- l’intégration Bifrost ;
- les broadcasts Android vers les anneaux RGB ;
- les réglages de luminosité ;
- la restauration `ACTION_CLEAR` ;
- la validation physique gauche/droite ;
- une nouvelle release publique.

## Étape suivante

Après clôture de la PR #14, validation matérielle et autorisation explicite de l’utilisateur :

```text
agent/v6-bifrost
```

Cette branche ajoutera la sortie LED physique en réutilisant le même `PortalLedState` et le limiteur de fréquence déjà présents dans la fondation V6.
