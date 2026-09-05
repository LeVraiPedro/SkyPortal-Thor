# Changelog — SkyPortal Thor

## V6 — développement non publié

### Bifrost — intégration en cours, non publiée

- Synchronisation opt-in des joysticks via l’API externe locale de Bifrost 1.3.1,
  couleurs gauche/droite Dolphin API 4, luminosité réglable et cadence maximale
  de deux DISPLAY par seconde ; aucun changement Dolphin ni accès direct aux LED.
- Heartbeat borné et restitution demandée à la sortie, en veille ou si le portail
  n’est plus fiable. Aucune écriture de profil Bifrost ; fonctionnement sans Bifrost.
- Les accusés de réception ne sont pas présentés comme une preuve matérielle ;
  seules les versions dont le bail a été audité sont autorisées. Voir
  [le contrat et les limites](docs/V6_BIFROST.md).
- Fiche d’actions et confirmation backup invalidées quand leur montage disparaît
  ou change ; maintien de l’opération backup après son propre retrait sécurisé.
- Bouton « Bifrost en haut » : lancement direct sur l’écran principal pour éviter
  la relance puis fermeture de Bifrost 1.3.1 depuis l’écran inférieur. Aucun changement
  du service LED ni du Dolphin installé ; validation du binaire dans le suivi.
- Essais de cette branche suivis dans `docs/PROJECT_STATUS.md`, distincts de #14.

### Reprise du menu Dolphin — PR #14 fusionnée le 5 septembre 2026

- Deux crashs natifs du Dolphin préexistant ont interrompu le parcours Thor du
  5 septembre 2026 : les requêtes du menu Wii tentaient de créer un noyau IOS
  temporaire alors qu’un IOS était déjà présent.
- Après autorisation utilisateur, ajout de `android-menu-lifecycle.patch`
  (`23af6d0`) : lecteur commun aux trois requêtes JNI, vérification du cœur
  arrêté avant puis sous verrou et protection des lectures de TMD indisponible.
- Complément `11353ca` dans le même patch : chemins de restauration et de
  lancement neuf d’`EmulationFragment` rendus mutuellement exclusifs ; la sortie
  d’une session restaurée ne doit plus enchaîner un nouveau démarrage.
- Conservation du contrat API 4 et du comportement d’activation/keepalive du
  Portal of Power ; aucune nouvelle fonctionnalité ou version publique.
- Cinq gardes structurelles JVM ajoutées ; 120 tests locaux réussis, Lint
  sans erreur (16 avertissements), Debug et contrôle de licence réussis ; pile
  complète des patchs réversée/réappliquée avec succès. Le run intermédiaire
  `33953904485` a été annulé volontairement pour intégrer le complément, sans
  échec de compilation constaté. La paire complète du nouveau
  [run 33954214843](https://github.com/LeVraiPedro/SkyPortal-Thor/actions/runs/33954214843)
  a été construite avec succès, compilation native comprise ; source, kit,
  empreintes et certificat officiel commun vérifiés après téléchargement.
- Mise à jour du seul Dolphin correctif sur la Thor, avec le compagnon `d466536`
  conservé. Revalidation en partie SSA le 5 septembre de 12:16 à 12:33 :
  chargements/remplacements/retraits J1 et cycle J2 logique, menu avec figurine,
  restauration Android suivie d’une sortie sans redémarrage, puis mort/reprise
  Dolphin et reconnexion automatique confirmés. Aucun nouveau crash ou ANR
  dans la fenêtre Logcat examinée ; données et droits SAF conservés.
  Le compagnon produit par le workflow de paire n’a pas été installé et n’est
  pas présenté comme matériellement testé.
- Limites observées : commande Wii à réactiver après veille ; une fiche d’actions
  déjà ouverte peut conserver un ancien nom après mort Dolphin, alors que le slot
  de fond est vidé correctement. Les cas exacts et réserves, dont le scénario
  multi-client non testé, figurent dans [le suivi](docs/PROJECT_STATUS.md).
- Patch inclus dans la procédure d’application, la vérification des notices et la provenance
  du kit. Le script de préparation doit partir d’un checkout Dolphin neuf :
  sa réexécution sur un arbre déjà API 4 n’est pas prise en charge, comme le
  documente [le suivi de reprise](docs/PROJECT_STATUS.md).

### Composition du portail — PR #14 revalidée sur Thor et fusionnée

- Séparation du panneau en une zone supérieure d’état/actions, une zone centrale pour le Canvas et une bande RGB inférieure.
- Conservation des commandes Équipes et Diagnostic et des repères gauche/droite avec leurs valeurs RGB.
- Libellé de veille précisé : « Éclairage du portail en veille ».
- Après test réel : hauteur de panneau réservée (144 dp), cartes tactiles compactes,
  défilement de secours, clipping et halo entièrement contenu ; les reflets
  circulent le long de l’ellipse sans faire pivoter son axe large.
- Affichage du badge et du cristal Trap uniquement si le jeu actif déclare `GameFeature.TRAPS` et si le canal est disponible ; aucun Trap dans Spyro’s Adventure.
- Reprise de la validation matérielle le 5 septembre 2026 ; les preuves et les éventuels défauts restants sont consignés dans [PROJECT_STATUS.md](docs/PROJECT_STATUS.md). Aucun changement de version ou nouvelle release n’accompagne ce chantier.

### Activation/keepalive — PR #13 fusionnée le 19 août 2026

- Restauration du comportement Dolphin compatible : toute commande USB `A` valide agit comme une activation/keepalive, y compris `A 00` pendant le polling normal de Spyro’s Adventure.
- Suppression de l’interprétation `A 00 → Deactivate()` introduite par le premier patch LED API 4, responsable de l’alternance actif/veille et de figurines montées non détectées par le jeu.
- Précontrôle d’activation native avant chargement et retrait du nouveau montage si le portail devient inactif pendant l’opération.
- Ajout d’une garde JVM contre cette régression. La [PR #13](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/13) consigne la validation corrective historique sur la vraie Thor ; elle est distincte des tests de composition de la PR #14.

### Portail animé V6

- Ajout d’un `AnimatedPortalStateMapper` pur qui transforme l’état Smart Portal en modes déconnecté, initialisation, conflit, API historique, veille et actif.
- Ajout d’un panneau Compose dessiné intégralement au `Canvas`, sans ressource graphique officielle ou tierce.
- Reproduction des couleurs gauche, droite et Trap issues de Dolphin API 4 avec transitions progressives.
- Ajout d’un halo multicouche, d’un anneau animé, de repères lumineux, d’une respiration et d’une pulsation lors des changements de slots.
- Conservation d’un rendu dégradé lisible avec Dolphin API 1–3 et lorsque Dolphin est absent.
- Maintien des actions Équipes et Diagnostic dans le nouveau panneau afin de ne pas augmenter la hauteur totale de l’écran inférieur.
- Affichage non bloquant du dernier snapshot LED fiable lorsqu’une lecture temporaire échoue.
- Ajout d’une description sémantique détaillée pour que l’état ne repose pas uniquement sur la couleur.
- Ajout de tests JVM couvrant les modes principaux et la conservation exacte des couleurs API 4.
- Aucune commande Bifrost et aucune modification des LED physiques ne sont incluses dans cette étape.

### Dolphin LED API 4

- Ajout de `getPortalLedStateJson()` en dernière position du contrat AIDL, sans modifier l’ordre API 1–3.
- Passage du service Dolphin de l’API 3 à l’API 4 dans la branche de développement.
- Capture native verrouillée des couleurs gauche, droite et Trap déjà maintenues par le Portal of Power émulé.
- Ajout d’une séquence monotone qui avance uniquement lors d’un changement visible ou d’une transition d’activation.
- Prise en charge de l’alias gauche `0x04` de la commande audio `L`. Le traitement de `A 00` a ensuite été corrigé par la PR #13 pour préserver l’activation/keepalive compatible.
- Exposition JNI d’un snapshot compact de 12 valeurs, contrôlé puis sérialisé en JSON versionné par le service.
- Ajout d’un transport LED dédié à 100 ms, sérialisé avec les opérations du portail et borné à 750 ms par appel.
- Conservation d’un mode API 1–3 sans erreur LED et d’un comportement non bloquant en cas de payload invalide.
- Ajout des tests JVM du resolver API 4, des gardes de contrat et du nettoyage de l’état après déconnexion.
- Ajout de `portal-led-api4.patch`, appliqué après `smart-portal-core.patch`, et adaptation du workflow de paire aux artefacts API 4.
- Le transport API 4 a été fusionné dans la PR #11, puis consommé par le portail animé de la PR #12. Les observations matérielles historiques dans Spyro’s Adventure sont distinguées de la validation finale de composition dans le suivi du projet. Aucune intégration Bifrost n’est incluse.

## V5 (0.5.0) — 16 août 2026

### Licence et distribution open source

- Adoption explicite de `GPL-2.0-or-later` pour le code original SkyPortal Thor, sans changement de version Android.
- Ajout du texte officiel complet de la GPL v2 dans `LICENSE` et `LICENSES/GPL-2.0-or-later.txt`.
- Ajout de `NOTICE.md` pour distinguer les copyrights SkyPortal, Dolphin, Gradle et les marques ou œuvres tierces.
- Ajout d’en-têtes SPDX aux sources originales SkyPortal, tout en conservant les avis Dolphin et Apache-2.0 existants.
- Documentation de l’obligation de fournir le code source correspondant lors de toute distribution publique d’un Dolphin modifié.
- Renforcement des archives et workflows de release : contrôle licence/NOTICE, provenance, exclusions de données privées et sommes SHA-256.
- Ajout d’un audit des ressources graphiques ; les captures QA contenant jeux, jaquettes ou interfaces tierces restent locales et exclues des publications.

### Correctifs post-validation — état USB réel et remplacement de figurine

- Correction du faux état `Portail prêt` : le réglage Dolphin et le booléen protocolaire historique ne suffisent plus à prouver que le jeu voit le Portal of Power.
- Ajout, dans le JSON API 3, de la présence du portail dans le scanner USB, de son attachement, de la première commande Skylanders observée et de la liste des périphériques concurrents.
- Détection explicite de la base Disney Infinity. Lorsqu'elle est active en même temps que le portail Skylanders, SkyPortal affiche un conflit, n'active pas automatiquement le portail et bloque le chargement avant Binder.
- Ajout des états `Portail en initialisation`, `Portail non vérifié`, `Redémarrage requis` et `Conflit USB`, avec des conseils en français.
- Un portail configuré mais jamais interrogé par le jeu ne peut plus atteindre `READY`. Le chargement API 3 est bloqué jusqu'à une preuve USB cohérente ; API 1 et API 2 conservent leur chemin dégradé historique.
- Compatibilité préservée sans modification de l'ordre AIDL : les nouvelles clés JSON sont optionnelles. Un ancien JSON API 3 est accepté, mais reste `Portail non vérifié` au lieu de produire un faux succès.
- L'activation automatique est désarmée avec un ancien schéma API 3 qui ne peut pas révéler une base concurrente ; l'action manuelle et le diagnostic restent disponibles.
- Après un timeout Binder, l'autorisation SAF et la propriété du slot restent protégées jusqu'à une réconciliation exacte ou un retrait confirmé. Un second chargement ne peut pas écraser ce résultat incertain.
- La cause a été confirmée par l'utilisateur : désactiver Disney Infinity puis redémarrer complètement l'émulation permettait au jeu de détecter le portail Skylanders.
- Validation finale de la paire Release sur AYN Thor : conflit Disney Infinity détecté et bloqué avant Binder, retour à `Portail prêt` après restauration, remplacements Whirlwind → Sonic Boom → Lightning Rod réellement visibles en jeu, J2, reconnexion compagnon/Dolphin et Logcat sans erreur critique.
- Le snapshot des slots sépare désormais le montage réel du fichier (`FileIsOpen`) de l'état protocolaire `REMOVING / REMOVED / ADDED / READY`. Cette séparation corrige le faux échec observé lors du remplacement Lightning Rod → Sonic Boom alors que le jeu avait bien chargé la nouvelle figurine.
- L'allocation native ne considère plus un slot libre d'après le seul bit `READY` : un fichier déjà monté ne peut pas être écrasé pendant une transition, y compris par le Manager Dolphin.
- Le compagnon exige le schéma natif v2, 16 slots cohérents et l'identité attendue avant de confirmer un chargement, un retrait ou un vidage. Un ancien Dolphin API 3 est détecté et les opérations risquées sont bloquées avec un message de mise à jour.
- La bascule Dolphin Debug/Release réutilise le même vidage confirmé et ne révoque plus les autorisations SAF sur un simple retour Binder.
- Revalidation sur AYN Thor : signal USB réel, `SSPP52`, `Portail prêt`, Lightning Rod → Sonic Boom → Whirlwind sur J1, retrait final et reconnexion du compagnon avec Lightning Rod monté, sans faux succès, mapping supprimé, crash ni ANR.
- Trois tests natifs ciblés ont été exécutés sur la Thor ARM64, dont le cas `mounted=true / status=REMOVED` qui reproduit l'ancienne mauvaise allocation.

### Validation et durcissement V5.1

- Validation réelle sur AYN Thor Android 13, écrans logiques `0` et `4`, avec Dolphin Debug API 3 et Spyro's Adventure (`SSPP52`).
- Vérification du chargement/retrait natif J1-J2, de l'activation automatique du portail et de la réconciliation après recréation de SkyPortal ou redémarrage de Dolphin.
- Validation stricte des dumps de 1 024 octets : en-tête, checksums, ID et variant, sans écriture côté SkyPortal.
- Rejet avant l'appel Binder des dumps invalides, des modèles maîtres, des identités inconnues et des contenus incompatibles avec le jeu actif.
- Confirmation du slot réellement monté avant d'annoncer un succès ; prise en charge explicite des retours portail plein `255` et `-6`.
- Sérialisation renforcée des opérations, délais Binder bornés, `DeathRecipient`, invalidation des résultats obsolètes et nettoyage des slots après perte du processus Dolphin.
- Backup d'un contenu actif sérialisé avec son retrait confirmé ; une copie partielle est supprimée en cas d'échec.
- Exclusion du scan pour `99_Backups`, `device-backups`, `test-fixtures` et `.skyportal-test-fixtures`.
- Suite portée à 75 tests unitaires couvrant jeux, compatibilité, dumps, AIDL, API 1/2/3, confirmation des chargements, montages non identifiés, garde d'identité Dolphin, slots, logique de collection, parsing et décisions USB, conflit Disney Infinity, schéma natif v2, bascule Debug/Release et migration contrôlée des préférences vers la clé officielle, y compris les entrées vides héritées.
- Import ponctuel et validé des préférences sauvegardées depuis le dossier externe propre à SkyPortal, nécessaire lorsque le changement de certificat Android impose une réinstallation.
- Ajout des workflows GitHub Actions `android-ci.yml`, `release.yml` et `full-pair-build.yml` ; ce dernier exige une signature commune et une révision Dolphin épinglée.
- Création de fixtures contrôlées avec le Skylanders Manager de Dolphin pour les principales générations et catégories, hors collection utilisateur.
- Validation matérielle du filtre SSA : Terrabite côté Personnages, Anvil Rain et Dragon's Peak côté Objets, avec affichage des générations futures via `Toute la collection`.
- Refus matériel avant Binder de Snap Shot, Magic Log Holder et d'une identité inconnue ; chargement et retrait réels d'Anvil Rain.
- Validation du backup d'Anvil Rain : retrait confirmé, copie exacte de 1 024 octets et exclusion de `99_Backups` du scan.
- Validation de la reprise après écran éteint/allumé, accueil Android et recréation du compagnon, sans doublon de slot.
- Correction d'un crash natif découvert au rebond du service Dolphin avant initialisation : garde `DirectoryInitialization`, état transitoire `INITIALIZING` et code `-10`.
- Correction de la grille Objets trop basse sur l'écran inférieur réel grâce à des filtres repliables.
- Documentation de la matrice de compatibilité, de la validation matérielle et de la procédure de release.

> Pendant cette campagne, seul Spyro's Adventure a été lancé sur le matériel. Les cinq autres jeux restent validés automatiquement, sans revendication de test physique.

### Smart Portal

- API Dolphin 3 : état d'émulation, Game ID, titre, état du portail, commande d'activation et slots natifs.
- Détection centralisée des six générations de jeux et architecture extensible `SkylandersGame`.
- En-tête compact indiquant connexion, jeu et disponibilité du portail.
- Activation automatique du réglage officiel Dolphin lorsque le jeu Skylanders actif en a besoin et qu'aucune base concurrente n'est signalée ; un redémarrage de l'émulation est demandé si le jeu a déjà manqué l'énumération USB initiale.
- Reconnexion automatique après arrêt/redémarrage du processus Dolphin.

### Collection et compatibilité

- Lecture read-only des ID/variant à l'intérieur de chaque dump.
- Catalogue de métadonnées exporté depuis la table native `list_skylanders` de Dolphin.
- Distinction Personnages/Objets et types Giant, SWAP, Trap Master, Mini, Item, Trap, Vehicle et Trophy.
- Sous-filtre de type affiché automatiquement lorsque plusieurs catégories natives sont présentes.
- Filtre automatique selon le jeu actif, désactivable temporairement avec `Toute la collection`.
- Blocage avant Binder des figurines d'une génération future ou d'un type non pris en charge, avec explication en français.

### Robustesse et diagnostic

- Réconciliation du mapping logique avec un instantané verrouillé des 16 slots natifs.
- Compatibilité conservée avec API 1/API 2 en mode dégradé.
- Diagnostic du jeu, ID, état d'émulation, portail, capacité d'activation, API et slots natifs.
- Version Android `0.5.0` / code `7`.

## V4 (0.4.0) — 16 août 2026

### Collection

- Ajout d'une étoile tactile sur chaque carte pour ajouter ou retirer un favori.
- Ajout des vues Tous, Favoris et Récents dans le sélecteur.
- Les récents ne sont enregistrés qu'après un chargement confirmé par Dolphin et conservent l'ordre des 12 dernières utilisations.
- Favoris et récents sont persistants entre les lancements.

### Équipes rapides

- Enregistrement de la configuration actuelle de Joueur 1 et, si activé, Joueur 2.
- Chargement séquentiel et diagnostiqué d'une équipe en une action.
- Une équipe duo active automatiquement Joueur 2 ; une équipe solo revient proprement au mode 1J.
- Détection des fichiers d'équipe manquants après déplacement ou changement de dossier.
- Suppression et conservation persistante de dix équipes maximum.

### Assistant de diagnostic

- Contrôle de l'écran inférieur et de la prise en charge multi-écrans.
- Contrôle de l'autorisation SAF persistante en lecture et en écriture.
- Vérification de la collection détectée, de l'installation Dolphin et de la connexion Binder.
- Comparaison des signatures SHA-256 de SkyPortal et du Dolphin ciblé.
- Affichage de la version API Dolphin avec recommandation API 2 lorsque nécessaire.
- Rappel explicite que l'activation du Portal of Power dans le jeu n'est pas encore exposée par l'API.

### Projet

- Version Android `0.4.0` / code `6`.

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
