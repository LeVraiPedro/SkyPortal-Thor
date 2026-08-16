# Test V5 Smart Portal sur AYN Thor / Android 13

## Smart Portal V5

- [ ] L'en-tête détecte automatiquement Spyro's Adventure et son Game ID.
- [ ] Portail désactivé : l'en-tête propose `Activer le portail` et l'API 3 l'active sans ouvrir les réglages Dolphin.
- [ ] L'état devient `Connecté | Spyro’s Adventure | Portail prêt` en moins de 5 secondes.
- [ ] Le sélecteur propose `Personnages | Objets` et masque Traps, véhicules et cristaux pour Spyro's Adventure.
- [ ] `Toute la collection` révèle les contenus masqués ; toucher un contenu incompatible affiche la raison sans appel Dolphin.
- [ ] Le diagnostic affiche jeu, ID, émulation, portail, capacité d'activation, API 3 et slots natifs.
- [ ] Après arrêt/redémarrage de Dolphin, le compagnon se reconnecte et réconcilie les slots.

## Fonctions V4

- [ ] Dans la collection, toucher `☆` ajoute le personnage aux favoris sans le charger.
- [ ] La vue `★ Favoris` ne montre que les favoris et reste identique après relance.
- [ ] Après un chargement réussi, le personnage apparaît en tête de la vue `Récents`.
- [ ] `Équipes` permet d'enregistrer les slots actifs avec un nom.
- [ ] Une équipe solo charge Joueur 1 et conserve le mode 1J.
- [ ] Une équipe duo active le mode 2J puis charge les deux personnages.
- [ ] Un fichier d'équipe déplacé ou supprimé est signalé comme introuvable.
- [ ] `Diagnostic` vérifie l'écran 4, SAF, la collection, Dolphin, la signature, Binder et l'API.
- [ ] Avec une API 1/2, le diagnostic explique que l'activation automatique exige l'API 3.

## Relevé réel du 16 août 2026

Validé sur AYN Thor Max Android 13, écrans logiques `0` (supérieur) et `4` (`Screen-2`, inférieur) :

- [x] SkyPortal V5 `0.5.0`/code `7` reste sur l'écran inférieur pendant que Dolphin et Spyro's Adventure tournent en haut.
- [x] Dolphin Debug API 3 détecte `Spyro’s Adventure`, Game ID `SSPP52`, état `RUNNING`.
- [x] En partant de `EmulateSkylanderPortal = False`, SkyPortal l'active à chaud et le réglage persistant repasse à `True`.
- [x] L'en-tête devient `Connecté | Spyro’s Adventure | Portail prêt`.
- [x] Le sélecteur Smart affiche `Personnages | Objets`, la compatibilité automatique et les métadonnées issues du dump/catalogue natif.
- [x] Chargement réel de Lightning Rod en J1, retrait réel, puis chargement de Sonic Boom en J2.
- [x] Après recréation de SkyPortal, Sonic Boom est réconcilié dans J2 ; après arrêt complet de Dolphin, les slots natifs disparus sont vidés sans faux succès.
- [x] Après redémarrage de Dolphin et du jeu, reconnexion automatique, nouvelle détection du Game ID et portail prêt.
- [x] Le diagnostic affiche API 3, jeu, ID, état d'émulation, état du portail et 16 slots natifs.
- [x] Aucune exception fatale ni ANR SkyPortal/Dolphin observée dans Logcat pendant ce parcours.

Non reproductible avec la collection présente : blocage tactile d'un dump incompatible (les 32 dumps détectés sont des personnages Spyro's Adventure). Ce cas est couvert par les tests unitaires du moteur de compatibilité.

## 1. Préparation

- Dolphin SkyPortal Edition et SkyPortal Thor doivent être signés avec la même clé.
- Avec l'API 3, laisser d'abord le portail désactivé afin de tester son activation depuis SkyPortal.
- Lancer le jeu sur l'écran supérieur et SkyPortal sur l'écran inférieur.
- Dans SkyPortal, toucher **Dossier** et sélectionner la racine qui contient les fichiers `.sky`.

Résultat attendu : la carte Collection locale indique le nombre de personnages jouables et le nombre total de fichiers, sans compter les copies de `99_Backups`.

## 2. Flux tactile Joueur 1

1. Toucher la carte **Joueur 1 — Slot vide**.
2. Vérifier que la sélection plein écran s'ouvre immédiatement.
3. Tester un filtre Élément, par exemple **Magic**.
4. Tester un filtre Jeu, par exemple **Spyro's Adventure**.
5. Déplier **Rechercher**, saisir `Spyro`, puis toucher la carte Spyro.

Résultats attendus :

- le personnage affiche `Placement…` avec un indicateur d'activité ;
- un second toucher ne lance pas une deuxième opération ;
- après succès, `Spyro est sur le portail` apparaît brièvement ;
- la sélection se ferme ;
- Joueur 1 affiche Spyro et `ACTIF` ;
- Spyro apparaît dans le jeu.

## 3. Flux Joueur 2 et changement

1. Toucher Joueur 2 et charger un second personnage.
2. Toucher le slot Joueur 1 occupé.
3. Choisir **Changer**, puis un autre personnage.

Résultat attendu : le nouveau personnage remplace Spyro dans le même slot sans passer par le manager Dolphin.

## 4. Actions d'un slot occupé

- **Informations** doit afficher jeu, élément, type, fichier, chemin et numéro de slot Dolphin.
- **Retirer** doit vider le slot dans l'application et dans le jeu.
- **Backup** doit demander confirmation, retirer le personnage, puis créer la copie dans :

```text
99_Backups/SkyPortal/<Personnage>/
```

Le personnage reste volontairement retiré après le backup.

## 5. Vérifier les erreurs visibles

### Dolphin déconnecté

Fermer Dolphin, ouvrir un slot et toucher un personnage.

Résultat attendu : la sélection reste ouverte et affiche le code `DOLPHIN_NOT_CONNECTED`, un conseil et le bouton Reconnecter.

### Autorisation de dossier révoquée

Révoquer l'accès au stockage ou déplacer le dossier, puis Scanner/charger.

Résultat attendu : une erreur SAF ou fichier inaccessible est affichée, avec l'instruction de sélectionner à nouveau le dossier.

### Portail plein

Si Dolphin renvoie le slot natif `255`, SkyPortal doit afficher `PORTAL_FULL_255` et ne jamais annoncer un succès.

## 6. Vérifier la bonne cible Dolphin

Si Debug et Release modifiés sont installés, toucher **Cible** et sélectionner le même Dolphin que celui qui affiche le jeu en haut. Le statut doit nommer la cible connectée.

## 7. Progression du fichier

1. Créer un backup avant le test.
2. Recharger le personnage.
3. Gagner un peu d'or ou d'XP.
4. Retirer puis recharger le personnage.

Résultat attendu : la progression est conservée dans le même fichier `.sky`.

## Diagnostic ADB

Sous PowerShell :

```powershell
adb shell dumpsys package org.dolphinemu.dolphinemu.debug | Select-String "SkyPortalService|PORTAL_CONTROL"
adb logcat -c
adb logcat | Select-String "SkyPortalBridge|SkyPortalService|Skylander|SecurityException"
```

À relever en cas d'échec : le code affiché par la V5, le package Dolphin ciblé, la version API, le Game ID, l'état du portail et les lignes Logcat correspondantes.
