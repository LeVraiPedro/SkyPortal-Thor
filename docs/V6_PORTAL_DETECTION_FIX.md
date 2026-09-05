<!--
  Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
  SPDX-License-Identifier: GPL-2.0-or-later
-->

# SkyPortal Thor V6 — correctif de détection des figurines

## État historique confirmé à la reprise

La [PR #13](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/13) a été fusionnée le 19 août 2026 dans le commit `ffc1e7158e63abf3dae4a6f08aa372c66d8f35d1`. Son corps confirme la validation corrective sur l’AYN Thor Max Android 13 : la détection des figurines dans Spyro’s Adventure fonctionnait de nouveau et le problème ne se reproduisait plus. Il s’agit d’une validation historique, distincte de la session du 5 septembre 2026 et des corrections de composition de la PR #14.

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

La paire corrective a été construite avec succès par le [run 32197254322](https://github.com/LeVraiPedro/SkyPortal-Thor/actions/runs/32197254322), au commit SkyPortal `1635709f22199d5b2d038f36b7b7d8bca77bdc72`, sur la révision Dolphin épinglée `54070da5851e12f2d1a4389daa528e4fb81327ce`. La PR consigne la vérification de la signature persistante commune et l’installation de cette paire par-dessus la précédente. Cette provenance historique ne doit pas être attribuée à un nouvel APK.

## Protocole de non-régression à conserver

Sur la Thor, après installation de la paire corrective :

1. arrêter complètement l’émulation ;
2. relancer Spyro’s Adventure avec le portail Skylanders activé et Disney Infinity désactivé ;
3. vérifier que le panneau ne clignote plus entre actif et veille ;
4. charger Sonic Boom puis Dino-Rang ;
5. confirmer que chaque personnage est détecté par le jeu ;
6. effectuer un remplacement et un retrait ;
7. contrôler Logcat pour exclure crash, ANR et erreur Binder.

## Revalidation distincte du 5 septembre 2026

La correction fonctionnelle a permis de reprendre la mise en page dans la [PR #14](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/14). Pendant cette campagne, les incidents du menu Android Dolphin ont conduit à une extension ciblée autorisée : gardes des requêtes JNI et chemins de restauration/lancement rendus exclusifs, sans modification du comportement d’activation/keepalive documenté ici.

Avec le compagnon `d466536` et le Dolphin correctif `11353ca`, la revalidation ciblée SSA de 12:16 à 12:33 a confirmé les opérations J1/J2 logiques, le retour du menu avec figurine, la sortie d’une session restaurée sans nouveau démarrage et la reconnexion automatique après mort Dolphin. Aucun nouveau crash ou ANR n’a été relevé dans cette fenêtre. Ces observations sont distinctes de celles de la paire corrective historique de la PR #13 et ne revendiquent aucun test des autres jeux.

Les résultats complets, les limites de commande Wii et de fiche d’actions périmée, ainsi que les deux APK effectivement utilisés sont identifiés dans [PROJECT_STATUS.md](PROJECT_STATUS.md). La validation ciblée est achevée ; la PR #14 reste ouverte, avec revue et accord de fusion attendus. Bifrost reste suspendu jusqu’à la clôture de cette étape et une autorisation explicite distincte de l’utilisateur.
