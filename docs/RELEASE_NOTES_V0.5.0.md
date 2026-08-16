# SkyPortal Thor V5 — Smart Portal

SkyPortal Thor transforme l’écran inférieur de l’AYN Thor en Portal of Power numérique pour Dolphin Android.

## Nouveautés principales

- détection automatique du jeu Skylanders lancé, de son Game ID et de l’état d’émulation ;
- activation et diagnostic du Portal of Power depuis SkyPortal ;
- Dolphin SkyPortal API 3 avec preuves USB réelles et 16 slots natifs ;
- filtres adaptés au jeu, distinction Personnages / Objets et moteur de compatibilité ;
- favoris, récents, équipes rapides et mode 1 joueur / 2 joueurs ;
- backups sécurisés : retrait confirmé avant copie ;
- reconnexion Binder et réconciliation des slots après recréation d’un processus ;
- détection de la base Disney Infinity concurrente, sans faux état prêt.

## Validation sur AYN Thor

- AYN Thor Max sous Android 13 ;
- Dolphin sur l’écran supérieur logique `0` et SkyPortal sur l’écran inférieur logique `4` ;
- Spyro’s Adventure `SSPP52` détecté avec l’état `RUNNING` ;
- présence, attachement et trafic USB Skylanders confirmés ;
- chargement, remplacement en jeu et retrait validés ;
- J1, J2, reconnexion SkyPortal, mort/reprise de Dolphin et cycles écran/accueil validés ;
- conflit Disney Infinity validé : diagnostic explicite et chargement bloqué avant Binder ;
- Logcat du smoke test sans crash SkyPortal/Dolphin, ANR, faux succès, erreur SAF ou mapping périmé.

## Compatibilité

Le modèle central et les tests automatisés couvrent Spyro’s Adventure, Giants, Swap Force, Trap Team, SuperChargers et Imaginators, ainsi que les Game IDs régionaux connus et les principaux types de figurines et objets.

Seul Spyro’s Adventure `SSPP52` a été lancé et validé en jeu sur le matériel pour cette release. Les cinq autres jeux ne sont pas présentés comme testés physiquement.

## Installation

1. Installer `Dolphin_SkyPortal_API3.apk` et `SkyPortal_Thor_v0.5.0.apk`.
2. Ne pas mélanger ces APK avec un compagnon ou un Dolphin signé par une autre clé : la permission Binder exige le même certificat.
3. Lancer Dolphin sur l’écran supérieur et SkyPortal sur l’écran inférieur.
4. Dans Dolphin, activer le portail Skylanders et désactiver la base Disney Infinity, puis redémarrer complètement l’émulation si ce réglage change.
5. Dans SkyPortal, sélectionner le dossier contenant les fichiers `.sky` via le sélecteur Android.

La migration depuis une paire signée par une ancienne clé peut imposer une réinstallation et une nouvelle sélection du dossier SAF. Sauvegarder les préférences et la configuration avant cette opération ; ne jamais supprimer la collection `.sky`.

## Fichiers inclus

- `SkyPortal_Thor_v0.5.0.apk` ;
- `Dolphin_SkyPortal_API3.apk` ;
- `SkyPortal_Thor_v0.5.0_Source.zip` ;
- `Dolphin_SkyPortal_API3_Source.zip` ;
- `Dolphin_SkyPortal_API3_Rebuild_Kit.zip` ;
- `SHA256SUMS.txt` ;
- `PAIR_BUILD_INFO.txt`.

## Limitations connues

- Giants, Swap Force, Trap Team, SuperChargers et Imaginators ne sont pas validés en jeu sur cette Thor ;
- le Manager Dolphin de la révision épinglée ne permet pas de créer proprement toutes les variantes Imaginators, notamment les Creation Crystals ;
- API 1 et API 2 restent compatibles en mode dégradé, sans les garanties Smart Portal complètes de l’API 3 ;
- les équipes pendant une reconnexion, le retrait pendant un scan et l’arrêt de Dolphin exactement pendant un chargement restent des scénarios de stress non rejoués matériellement.

## Projet non officiel

SkyPortal Thor est un projet communautaire non officiel. Il n’est affilié ni à Activision et aux titulaires des marques Skylanders/Spyro, ni au projet Dolphin, ni à Disney, Nintendo ou AYN.

Aucun jeu, ROM, dump de figurine ni clé privée n’est fourni.

## Licence

`GPL-2.0-or-later`. Les sources correspondantes, notices et informations de reconstruction sont jointes à la release.
