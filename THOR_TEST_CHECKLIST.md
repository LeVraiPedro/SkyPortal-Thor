# Test V4 sur AYN Thor / Android 13

## Fonctions V4

- [ ] Dans la collection, toucher `☆` ajoute le personnage aux favoris sans le charger.
- [ ] La vue `★ Favoris` ne montre que les favoris et reste identique après relance.
- [ ] Après un chargement réussi, le personnage apparaît en tête de la vue `Récents`.
- [ ] `Équipes` permet d'enregistrer les slots actifs avec un nom.
- [ ] Une équipe solo charge Joueur 1 et conserve le mode 1J.
- [ ] Une équipe duo active le mode 2J puis charge les deux personnages.
- [ ] Un fichier d'équipe déplacé ou supprimé est signalé comme introuvable.
- [ ] `Diagnostic` vérifie l'écran 4, SAF, la collection, Dolphin, la signature, Binder et l'API.
- [ ] Le diagnostic rappelle que l'activation du portail dans le jeu reste une vérification manuelle.

## 1. Préparation

- Dolphin SkyPortal Edition et SkyPortal Thor doivent être signés avec la même clé.
- Dans Dolphin, activer `Emulated USB Devices > Skylanders Portal`.
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

À relever en cas d'échec : le code affiché par la V3, le package Dolphin ciblé, la version API du service et les lignes Logcat correspondantes.
