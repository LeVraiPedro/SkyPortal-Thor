# Outils

## apply_dolphin_patch.py
Après avoir cloné Dolphin :

```bash
python tools/apply_dolphin_patch.py /chemin/vers/dolphin
```

Le script copie le service/AIDL et modifie le Manifest + buildFeatures de façon idempotente.

## check_licensing.py

Le contrôle léger de licence ne télécharge aucune dépendance :

```bash
python3 tools/check_licensing.py
```

Il vérifie la présence et la cohérence de `LICENSE`/`NOTICE.md`, les en-têtes SPDX des sources originales, la conservation des avis Apache du wrapper Gradle, l’absence de suppression d’avis Dolphin dans les patchs et l’absence de ROM, clé ou dump utilisateur suivi par Git.
