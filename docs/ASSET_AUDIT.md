# Audit des ressources et des marques

État vérifié pour la préparation de la licence `GPL-2.0-or-later`.

## Ressources livrées dans l’application

| Ressource | Classification | État |
|---|---|---|
| `app/src/main/res/drawable/ic_skyportal.xml` | Icône vectorielle géométrique SkyPortal | Ajoutée à l’historique du projet par LeVraiPedro ; aucun logo, personnage ou mot-symbole Skylanders/Dolphin. Elle est couverte par la licence du projet sous réserve de confirmation humaine de sa provenance. |
| `app/src/main/res/values/styles.xml` | Configuration Android | Utilise uniquement un thème et une police système Android ; aucun média tiers embarqué. |
| Interface Compose | Code et glyphes génériques | Aucun portrait, logo, jaquette ou illustration de personnage chargé par l’application. |

Le dépôt suivi par Git ne contient actuellement aucun fichier raster (`.png`, `.jpg`, `.webp`, etc.) ni dossier `assets/` dans l’application.

## Captures de validation locales

Les dossiers locaux `artifacts/`, `captures/`, `device-backups/` ainsi que les captures `skyportal-*.png` sont ignorés par Git. Certaines preuves QA locales peuvent montrer des écrans de jeux, jaquettes, lanceurs Android ou interfaces Dolphin contenant des œuvres et marques tierces.

Ces captures ne sont pas couvertes par la GPL du code et ne doivent pas être ajoutées automatiquement au dépôt, aux archives sources ou aux releases. Avant toute publication, elles doivent faire l’objet d’une revue humaine, être recadrées ou remplacées par une capture ne montrant que l’interface originale SkyPortal.

## Marques citées textuellement

Le code et la documentation utilisent notamment les noms Skylanders, Spyro, Disney Infinity, Dolphin Emulator, Wii, GameCube, Nintendo et AYN Thor pour décrire la compatibilité technique. Cet usage ne signifie aucune affiliation. Le projet ne revendique aucun droit sur ces marques, personnages ou œuvres ; voir [`NOTICE.md`](../NOTICE.md).

## Vérification avant release

- Ne pas ajouter de logo officiel Skylanders, Dolphin, Activision, Nintendo ou d’un constructeur à l’icône ou à la bannière du projet.
- Vérifier l’origine de toute nouvelle image avant de la suivre dans Git.
- Contrôler qu’une capture ne révèle ni URI SAF complète, ni nom utilisateur, ni identifiant d’appareil.
- Conserver les preuves matérielles contenant des œuvres tierces dans les dossiers QA locaux ignorés.
