# Matrice de compatibilité V5

## Légende

- **Matériel** : scénario observé sur une AYN Thor avec le jeu réellement lancé.
- **Fixture Thor** : fichier créé avec le Skylanders Manager de Dolphin sur la console, sans validation dans un jeu.
- **Automatisé** : comportement couvert par un test déterministe, sans lancement matériel du jeu.
- **Non testé** : aucune preuve matérielle ou automatisée suffisante.
- **Incompatible par conception** : le moteur doit refuser le chargement avant Binder.

## Jeux

| Jeu | Détection/IDs | Filtre et compatibilité | Test en jeu sur Thor |
|---|---|---|---|
| Spyro's Adventure | matériel : `SSPP52` ; IDs régionaux automatisés | matériel pour personnages, objets et refus de fixtures | oui |
| Giants | automatisé | automatisé | non |
| Swap Force | automatisé | automatisé | non |
| Trap Team | automatisé | automatisé | non |
| SuperChargers | automatisé | automatisé | non |
| Imaginators | automatisé | automatisé | non ; non pris en charge par le Manager de la révision Dolphin testée |

Le modèle central `SkylandersGame` regroupe les noms, générations, IDs régionaux, éléments, types et fonctions spéciales. Cette table évite de disperser les décisions de compatibilité dans l'interface.

## Générations et objets

| Contenu représentatif | Type reconnu | Preuve disponible | Spyro's Adventure | Giants | Swap Force | Trap Team | SuperChargers | Imaginators |
|---|---|---|---|---|---|---|---|---|
| Lightning Rod / Sonic Boom | personnage SSA | chargement matériel | compatible | compatible | compatible | compatible | compatible | compatible selon le modèle |
| Tree Rex | Giant | fixture Thor, visible en `Toute la collection` | incompatible par conception | compatible | compatible | compatible | compatible | compatible selon le modèle |
| Pop Thorn | SWAP Force | fixture Thor, visible en `Toute la collection` | incompatible par conception | incompatible par conception | compatible | compatible | compatible | compatible selon le modèle |
| Snap Shot | Trap Master | refus matériel SSA avant Binder | refus matériel confirmé | incompatible par conception | incompatible par conception | compatible | compatible | compatible selon le modèle |
| Magic Log Holder | Trap | refus matériel SSA avant Binder | refus matériel confirmé | incompatible par conception | incompatible par conception | compatible | compatible selon le modèle | compatible selon le modèle |
| Anvil Rain | Magic Item | chargement/retrait/backup matériels | compatible matériel | compatible selon le modèle | compatible selon le modèle | compatible selon le modèle | compatible selon le modèle | compatible selon le modèle |
| Dragon's Peak | Adventure / Location | affiché dans Objets sur matériel | affiché compatible sur matériel | compatible selon le modèle | compatible selon le modèle | compatible selon le modèle | compatible selon le modèle | compatible selon le modèle |
| Hot Streak | véhicule Land | fixture Thor, visible en `Toute la collection` | incompatible par conception | incompatible par conception | incompatible par conception | incompatible par conception | compatible | compatible selon le modèle |
| Sky Trophy | Trophy | fixture Thor, visible en `Toute la collection` | incompatible par conception | incompatible par conception | incompatible par conception | incompatible par conception | compatible | compatible selon le modèle |
| Terrabite | Sidekick | affiché dans Personnages sur matériel | affiché compatible sur matériel | compatible selon le modèle | compatible selon le modèle | compatible selon le modèle | compatible selon le modèle | compatible selon le modèle |
| Creation Crystal | Creation Crystal | automatisé uniquement | incompatible par conception | incompatible par conception | incompatible par conception | incompatible par conception | incompatible par conception | compatible selon le modèle |
| ID/variant inconnu | non reconnu | refus matériel SSA avant Binder + automatisé | refus matériel confirmé | refus API 3 | refus API 3 | refus API 3 | refus API 3 | refus API 3 |

« Compatible selon le modèle » signifie que la règle centrale autorise ce type pour cette génération ; cela ne remplace pas un test en jeu sur le matériel. Les variantes particulières peuvent ajouter des restrictions que le catalogue natif doit identifier.

## Modes d'affichage

| Mode | Contenu visible | Protection au chargement |
|---|---|---|
| Filtre automatique | uniquement les types pertinents pour le jeu détecté | compatibilité obligatoire |
| Toute la collection | y compris les contenus normalement masqués | compatibilité toujours obligatoire ; consultation permise, chargement refusé si nécessaire |
| API 1/2 | collection disponible sans faits Smart complets | contrôles locaux disponibles, fonctions dépendant du jeu et backup sécurisé désactivés ; ne pas utiliser le Manager simultanément |
| API 3 | catalogue natif, jeu, portail et slots disponibles | double contrôle compagnon + Dolphin |

Sur la Thor, le filtre automatique SSA a montré Terrabite côté Personnages et Anvil Rain/Dragon's Peak côté Objets. `Toute la collection` a rendu les générations futures visibles, mais Snap Shot, Magic Log Holder et l'identité inconnue ont conservé leur protection et ont été refusés avant Binder avec un message français.

## API Dolphin

| Version | Couverture automatisée | Couverture matérielle | Limites |
|---|---|---|---|
| API 1 | oui | non pendant cette campagne | pas de jeu/portail/catalogue Smart |
| API 2 | oui | non pendant cette campagne | pas de jeu/portail/catalogue Smart complet |
| API 3 | oui | oui, Dolphin Debug + SSA | mode Smart Portal complet |

La matrice doit être mise à jour seulement avec des preuves reproductibles. En particulier, créer une fixture Trap Team ou SuperChargers dans le Manager n'autorise pas à déclarer le jeu correspondant « testé sur matériel ».
