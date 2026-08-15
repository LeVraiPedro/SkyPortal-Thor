# Changelog — SkyPortal Thor

## V3.0.2 (0.3.2) — 16 août 2026

- Ajout d'un réglage persistant `1J / 2J` accessible depuis l'en-tête.
- Le mode solo est désormais la valeur par défaut et affiche Joueur 1 sur toute la largeur.
- Joueur 2 n'apparaît que lorsque l'option correspondante est activée.
- Revenir au mode solo retire d'abord le personnage de Joueur 2 dans Dolphin avant de masquer son slot.
- Un échec de retrait conserve le mode deux joueurs et affiche le diagnostic au lieu de masquer un personnage encore monté.

## V3.0.1 (0.3.1) — 16 août 2026

- Le portail cible maintenant toujours l'écran Android secondaire de l'AYN Thor, quel que soit l'écran depuis lequel l'icône est touchée.
- L'écran inférieur est identifié de façon stable comme l'affichage non principal/de présentation, au lieu de choisir simplement « l'autre écran ».
- L'activité de lancement se ferme après le routage afin de laisser l'écran supérieur disponible pour Dolphin.
- Si l'écran inférieur n'est pas disponible, SkyPortal reste sur un écran de diagnostic avec un bouton Réessayer au lieu de s'ouvrir silencieusement en haut.

## V3 (0.3.0) — 15 août 2026

### Interface

- Les cartes Joueur 1 et Joueur 2 deviennent les points d'entrée principaux.
- Toucher un slot vide ouvre directement une grille de personnages.
- Ajout des filtres Élément et Jeu, avec une recherche secondaire dépliable.
- Toucher un personnage lance immédiatement son placement.
- La sélection se ferme automatiquement après un succès confirmé et reste ouverte après un échec.
- Un slot occupé propose Changer, Retirer, Backup et Informations.
- Ajout d'états visuels et accessibles pour chargement, succès et erreur.

### Chargement et diagnostic

- Les erreurs SAF, partage URI, Binder et Dolphin ne sont plus masquées.
- Ajout de codes de diagnostic, détails techniques, conseils et bouton Réessayer.
- Prévalidation du fichier en lecture/écriture avant son envoi à Dolphin.
- Correction du code natif `255` : il indique un portail plein, pas un succès.
- Sérialisation des opérations et protection contre les doubles touchers.
- Ajout d'un délai maximal et des callbacks de mort/déconnexion du service Binder.
- Affichage et sélection du package Dolphin réellement ciblé lorsque Debug et Release coexistent.
- Le rafraîchissement efface maintenant les slots réellement signalés comme vides.
- Les connexions, chargements, retraits et changements de cible sont sérialisés pour éviter d'appliquer un résultat au mauvais Dolphin.
- Les autorisations de fichiers sont suivies par package et par URI, puis révoquées sans casser un autre slot.
- Les slots sont réconciliés avec leur URI source après un scan ou une recréation de l'écran.

### Collection et backups

- Les sauvegardes situées dans `99_Backups` sont exclues du scan.
- Une autorisation SAF révoquée produit maintenant une erreur explicite.
- Le backup d'un personnage actif passe par une confirmation puis un retrait sûr avant copie.

### Patch Dolphin fourni

- AIDL inchangé et compatibilité maintenue avec le service API 1 existant.
- Service API 2 facultatif avec codes URI/données/portail plein plus précis.
- Mapping logique conservé lors de la recréation du service dans le même processus.
- Journaux Logcat ajoutés côté service sans exposer l'URI complète.

### Projet

- Version Android `0.3.0` / code `3`.
- Wrapper Gradle complet, icône d'application, `.gitignore` et tests unitaires ajoutés.
- Build, tests unitaires et Android Lint validés.
- Validation sur AYN Thor : 32 dumps détectés et chargement réel de Spyro confirmé par Dolphin.
