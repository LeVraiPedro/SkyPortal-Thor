<!--
  Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
  SPDX-License-Identifier: GPL-2.0-or-later
-->

# SkyPortal Thor V6 — correctif de détection des figurines

## Symptôme matériel

Lors de la première validation de Dolphin SkyPortal API 4 sur l’AYN Thor Max :

- SkyPortal ouvrait correctement le fichier `.sky` ;
- le slot natif Dolphin était bien monté ;
- la carte du personnage passait à l’état actif dans le compagnon ;
- le jeu ne détectait pourtant pas toujours la figurine ;
- le panneau animé alternait rapidement entre portail actif et portail en veille.

Le problème a été reproduit avec *Skylanders: Spyro’s Adventure* et plusieurs figurines, notamment Sonic Boom et Dino-Rang.

## Cause

Le patch LED API 4 avait commencé à interpréter littéralement la valeur de la commande USB `A` :

```text
A 01 → Activate()
A 00 → Deactivate()
```

Sur le matériel réel, Spyro’s Adventure peut alterner `A 00` et `A 01` pendant le polling normal d’un portail déjà sain. Le passage à `Deactivate()` faisait donc osciller l’état protocolaire et pouvait laisser un fichier monté dans Dolphin sans que le jeu accepte correctement la transition de pose.

La version Dolphin utilisée comme base traitait historiquement toute commande `A` valide comme une activation ou un keepalive. Ce comportement avait déjà prouvé sa compatibilité avec le jeu.

## Correction

Le patch conserve désormais le comportement compatible de Dolphin :

```text
Toute commande A valide
→ Activate()
```

La disponibilité réelle du périphérique reste contrôlée par le réglage d’émulation USB Skylanders dans Dolphin, et non par les alternances de l’octet de polling du jeu.

Le service SkyPortal ajoute également deux protections :

1. il refuse un chargement si le portail natif n’est pas actif au moment du précontrôle ;
2. il retire immédiatement le fichier nouvellement monté si le portail devient inactif pendant l’opération.

Ainsi, SkyPortal ne doit plus annoncer un faux succès avec un fichier ouvert mais invisible pour le jeu.

## Garde de régression

La suite JVM vérifie désormais que le patch :

- conserve explicitement la compatibilité `A 00` / `A 01` de Spyro’s Adventure ;
- ne contient plus d’appel `system.GetSkylanderPortal().Deactivate()` dans le traitement de la commande `A` ;
- protège le chargement avec `SkylanderConfig.isPortalActivated()`.

La construction complète de la paire doit encore appliquer le patch sur la révision Dolphin épinglée, compiler Dolphin et produire deux APK signés avec la même clé persistante.

## Validation matérielle attendue

Sur la Thor, après installation de la paire corrective :

1. arrêter complètement l’émulation ;
2. relancer Spyro’s Adventure avec le portail Skylanders activé et Disney Infinity désactivé ;
3. vérifier que le panneau ne clignote plus entre actif et veille ;
4. charger Sonic Boom puis Dino-Rang ;
5. confirmer que chaque personnage est détecté par le jeu ;
6. effectuer un remplacement et un retrait ;
7. contrôler Logcat pour exclure crash, ANR et erreur Binder.

Le correctif de mise en page du portail animé et l’intégration Bifrost restent volontairement suspendus jusqu’à la validation de cette correction fonctionnelle.
