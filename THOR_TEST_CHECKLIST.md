# Validation V5 Smart Portal sur AYN Thor / Android 13

Cette checklist distingue les observations faites sur la console des couvertures automatisées. Une case non cochée ne doit pas être interprétée comme un échec : elle signifie que le scénario matériel reste à exécuter ou à documenter.

## Environnement relevé

| Élément | Valeur validée |
|---|---|
| Matériel | AYN Thor Max, Android 13 (API 33) |
| Écran supérieur | affichage logique `0`, 1920 × 1080 |
| Écran inférieur | affichage logique `4`, 1240 × 1080 |
| SkyPortal | `com.skyportalthor.app`, 0.5.0, code 7 |
| Dolphin testé | `org.dolphinemu.dolphinemu.debug`, API SkyPortal 3 |
| Jeu réellement lancé | Skylanders: Spyro's Adventure |
| Game ID réellement détecté | `SSPP52` |
| Collection utilisateur | 32 dumps détectés, non supprimés et non réinitialisés |
| Signature | certificat identique pour la paire Debug testée |

Le numéro de série ADB, les URI SAF et les chemins propres au PC ne sont volontairement pas publiés.

## Résultats matériels établis

- [x] SkyPortal est routé sur l'affichage logique `4` et Dolphin reste utilisable sur l'affichage `0`.
- [x] La connexion Binder s'établit avec Dolphin Debug et le diagnostic annonce l'API 3.
- [x] Spyro's Adventure est détecté avec l'ID `SSPP52` et l'état `RUNNING`.
- [x] En partant du portail désactivé dans Dolphin, SkyPortal l'active automatiquement via l'API 3.
- [x] L'en-tête atteint `Connecté | Spyro’s Adventure | Portail prêt` en quelques secondes, sans blocage indéfini.
- [x] Le diagnostic affiche le jeu, l'ID, l'émulation, le portail et les 16 slots natifs.
- [x] Lightning Rod est chargé réellement en J1, slot natif `0`, identité `3 / 0`.
- [x] Le mode 2J peut être activé et Sonic Boom est chargé réellement en J2, slot natif `1`, identité `1 / 0`.
- [x] Un double toucher rapide sur le chargement J2 ne crée qu'un seul montage natif.
- [x] Le retrait réel vide le slot côté compagnon et côté Dolphin.
- [x] Après arrêt forcé puis relance de SkyPortal, J1/J2 sont réconciliés sans duplication alors que Dolphin et le jeu restent actifs.
- [x] Après arrêt forcé de Dolphin, le compagnon reste vivant et ne conserve pas de slot faussement actif.
- [x] Après relance de Dolphin et du jeu, la connexion, le Game ID et l'état du portail sont retrouvés.
- [x] Après arrêt normal de l'émulation, le jeu et les anciens slots ne restent pas affichés comme actifs.
- [x] Écran éteint/allumé avec Lightning Rod monté : le slot actif est conservé sans duplication.
- [x] Passage par l'accueil puis retour dans SkyPortal : même slot conservé.
- [x] Force-stop/relaunch du compagnon : PID Dolphin inchangé et slot réconcilié sans second chargement.
- [x] Après correction du rebond service-only, un Logcat frais ne contient aucun crash natif/app, ANR ou spam `DeadObjectException`.

Le premier test d'arrêt brutal de Dolphin a révélé un `SIGSEGV` : le processus service-only pouvait accéder à `NativeConfig` avant l'initialisation native. Le service a été protégé par `DirectoryInitialization`, un statut transitoire `INITIALIZING` et le code `-10`. Après rebuild et réinstallation, le scénario a été rejoué : compagnon vivant, slots effacés sans fantôme, rebond du service, puis nouvelle détection de SSA `SSPP52` après relance.

Après l'arrêt forcé de Dolphin, l'accueil AYN Cocoon a temporairement recouvert l'écran inférieur. Relancer SkyPortal explicitement sur l'affichage logique `4` a restauré le compagnon et ses slots vides. Les tests ultérieurs écran éteint/allumé et accueil/retour ont conservé l'activité et le slot attendus.

## Fixtures contrôlées

Les fixtures suivantes ont été créées sur la Thor avec le Skylanders Manager de Dolphin, jamais téléchargées, puis conservées dans un dossier de test séparé de la collection principale :

| Fixture | ID / variant | Type attendu | Niveau de validation |
|---|---:|---|---|
| Tree Rex | `112 / 4614` | Giant | création Manager + visible dans `Toute la collection` |
| Pop Thorn | `3001 / 8192` | SWAP Force | création Manager + visible dans `Toute la collection` |
| Snap Shot | `462 / 12288` | Trap Master | refus SSA avant Binder, message français |
| Magic Log Holder | `210 / 12290` | Trap | refus SSA avant Binder, message français |
| Anvil Rain | `200 / 0` | Magic Item | filtre Objets + chargement/retrait/backup réels |
| Dragon's Peak | `300 / 0` | Adventure / Location | affiché par le filtre Objets SSA |
| Hot Streak | `3224 / 16384` | véhicule Land | création Manager + visible dans `Toute la collection` |
| Sky Trophy | `3500 / 16384` | Trophy | création Manager + visible dans `Toute la collection` |
| Terrabite (Sidekick) | `505 / 0` | Sidekick | affiché par le filtre Personnages SSA |
| identité inconnue | `65535 / 65535` | rejet attendu en API 3 | refus avant Binder, message français |

Seul Anvil Rain a été chargé en jeu parmi ces fixtures. L'affichage ou le refus d'une autre fixture dans SSA ne prouve pas son comportement dans son jeu d'origine. Aucun fichier utilisateur portant le même nom n'a été écrasé. Aucun Creation Crystal n'a été créé, car la révision Dolphin testée n'expose pas de prise en charge Imaginators dans son Manager.

## Scénarios matériels complémentaires

- [x] Le filtre SSA affiche Terrabite dans Personnages et Anvil Rain/Dragon's Peak dans Objets.
- [x] `Toute la collection` révèle les générations futures sans désactiver la compatibilité.
- [x] Snap Shot, Magic Log Holder et l'identité inconnue sont refusés avant Binder avec une explication française ; aucun slot natif n'est créé.
- [x] Anvil Rain est chargé puis retiré réellement dans SSA.
- [x] Le backup d'Anvil Rain confirme et réussit le retrait avant une copie de 1 024 octets.
- [x] `99_Backups` n'est pas rescanné : la collection de fixtures reste à 10 fichiers après le backup.
- [x] La racine SAF utilisateur est restaurée et affiche de nouveau ses 32 fichiers.
- [x] La grille Objets, trop basse sur l'écran réel, est corrigée avec des filtres repliables puis revalidée.
- [x] Le scénario Dolphin brutal est rejoué après correction avec un Logcat frais sans crash natif/app, ANR ni spam `DeadObjectException`.

## Scénarios encore à rejouer avant une release

- [ ] Équipe pendant reconnexion.
- [ ] Retrait pendant scan.
- [ ] Arrêt de Dolphin pendant un chargement en cours.

## Jeux non testés physiquement

Giants, Swap Force, Trap Team, SuperChargers et Imaginators n'étaient pas disponibles comme jeux lancés pendant cette campagne. Leur détection, leurs Game IDs régionaux et les règles de compatibilité sont couverts par les tests unitaires, mais aucune ligne de cette checklist ne les présente comme validés en jeu sur la Thor.

## Régression V4 à préserver

- [ ] Favoris : ajout/retrait sans chargement et persistance après relance.
- [ ] Récents : mise à jour uniquement après un chargement confirmé.
- [ ] Équipe solo : chargement J1 en conservant le mode 1J.
- [ ] Équipe duo : activation 2J et chargement séquentiel sans doublon.
- [ ] Fichier d'équipe déplacé ou supprimé : message explicite, aucun faux succès.
- [ ] Sélection Dolphin Debug/Release : la cible choisie correspond au processus de jeu.
- [ ] Permission SAF persistante après redémarrage et message clair après révocation.
- [ ] Fichier déplacé/supprimé après scan : erreur claire, aucun crash.
- [ ] Progression : Dolphin reste le seul écrivain du fichier monté.
- [x] Backup actif : confirmation, retrait confirmé avant la copie de 1 024 octets dans `99_Backups`.
- [x] `99_Backups` reste exclu du scan ; les fixtures restent isolées et la racine utilisateur revient à 32 fichiers.

## Contrôles automatisés

- [x] 31 tests unitaires Debug.
- [x] Détection des six jeux et de leurs IDs régionaux connus.
- [x] Compatibilité par génération et par type.
- [x] Distinction personnage/objet et sous-types natifs.
- [x] Dump de taille incorrecte, en-tête/checksum invalide et identité inconnue.
- [x] Contrat AIDL et ordre des six méthodes historiques.
- [x] Parsing API 1, API 2, API 3 et des 16 slots natifs.
- [x] Nettoyage logique à la déconnexion et code portail plein.
- [x] Favoris, récents, équipes manquantes et exclusions de scan.
- [x] Android Lint : aucune erreur bloquante.
- [x] Compilation de l'APK Debug SkyPortal.
- [x] Compilation du Dolphin Debug patché.

## Commandes de diagnostic

Sous PowerShell :

```powershell
adb shell dumpsys package org.dolphinemu.dolphinemu.debug | Select-String "SkyPortalService|PORTAL_CONTROL"
adb logcat -c
adb logcat | Select-String "SkyPortalBridge|SkyPortalService|Skylander|AndroidRuntime|SecurityException"
```

En cas d'échec, relever le code affiché par SkyPortal, le package Dolphin ciblé, la version API, le Game ID, l'état du portail et les slots natifs. Ne pas publier l'URI SAF complète du fichier.

Voir aussi [docs/VALIDATION_V5.md](docs/VALIDATION_V5.md) et [docs/COMPATIBILITY_MATRIX.md](docs/COMPATIBILITY_MATRIX.md).
