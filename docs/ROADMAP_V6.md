<!--
  Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
  SPDX-License-Identifier: GPL-2.0-or-later
-->

# SkyPortal Thor V6 — Roadmap

## Vision

SkyPortal Thor V6 doit transformer l’écran inférieur et les anneaux RGB des joysticks de l’AYN Thor en un **Portal of Power numérique cohérent, animé et synchronisé avec le jeu**.

```text
Écran supérieur
└─ Dolphin SkyPortal Edition + jeu Skylanders

Écran inférieur
└─ SkyPortal Thor : portail animé, joueurs, collection et commandes

Joysticks RGB
└─ prolongement lumineux facultatif via Bifrost
```

Le socle V5 reste la référence de sûreté :

```text
SkyPortal Android
→ Binder/AIDL local
→ Dolphin SkyPortal API
→ émulation native du Portal of Power
→ fichiers .sky
```

Dolphin demeure le seul processus autorisé à écrire dans une figurine montée.

## Avancement actuel

```text
V6 Foundation       ✓ fusionnée (PR #10)
Dolphin LED API 4    ✓ fusionnée (PR #11), observée historiquement sur Thor/SSA
Portail animé       ✓ fusionné (PR #12), affichage observé historiquement sur Thor
Activation/keepalive ✓ corrigée et validée historiquement sur Thor (PR #13)
Composition Thor    ✓ revalidée puis fusionnée (PR #14), main 12d23a1
Bifrost             PR #15 brouillon ; couleurs synchronisées, restitution OFF
                    et arrêt Dolphin confirmées ; autres interruptions à valider
```

État courant du 5 septembre 2026 : `v0.5.0` reste la release publique API 3 ; la V6/API 4 est un développement non publié. La [PR #14](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/14), initialement ouverte en brouillon, a été fusionnée après autorisation dans `12d23a1db1b0fb9214d4386072dcfc44c1858f2f`. La [CI de main](https://github.com/LeVraiPedro/SkyPortal-Thor/actions/runs/33962044116) a réussi. Les preuves de cette étape et le nouveau point de reprise restent dans [`PROJECT_STATUS.md`](PROJECT_STATUS.md) ; ils ne valident pas automatiquement les modifications Bifrost.

Après les incidents du menu Dolphin, un correctif ciblé a été autorisé, construit
et installé. La revalidation du 5 septembre, de 12:16 à 12:33, a confirmé dans
SSA la composition, les opérations J1/J2 logiques, les menus, la restauration
Android et les reconnexions. Les preuves, les deux APK utilisés et les limites
(commande Wii après veille, fiche d’actions pouvant conserver un ancien nom)
figurent dans le suivi. Après fusion de #14, l’utilisateur a autorisé V6.0 Bifrost
avec fiabilisation préalable. La branche `agent/v6-bifrost-integration` part de
`12d23a1` ; l’audit du source officiel Bifrost 1.3.1 est terminé. Absent au contrôle initial de la Thor, Bifrost
officiel a depuis été installé après autorisation. Les notifications et le contrôle
tiers sont configurés ; le service est désormais actif pour un test STATIC isolé,
Default préservé. L’utilisateur a confirmé physiquement la baseline bleu gauche /
rouge droite, distincte de la synchronisation SkyPortal. Le compagnon dans la [PR #15 en brouillon](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/15) inclut désormais le
transport borné, la session 2 Hz, le contrôle de cycle de vie/fraîcheur, le réglage
OFF / 35 %, le diagnostic et la protection de fiche d’actions périmée. Les
contrôles locaux ont réussi sur `3be0796` ; le candidat signé est installé et les
contrôles hors jeu ont réussi. Le parcours 20:21–20:24 du 5 septembre confirme
la réception des commandes dans SSA et une cadence observée d’environ 1,97 Hz.
Le parcours 20:46–20:51 ajoute J1 en partie, reconnexion du compagnon sans doublon,
watchdog et CLEAR logiciel. L’utilisateur a ensuite confirmé, le 5 septembre sur
le même candidat `3be0796`, les couleurs physiques suivant le portail puis le
retour bleu gauche / rouge droite après OFF. Cette confirmation ne couvre pas la
restitution après watchdog, veille, perte Dolphin ou arrêt Bifrost, ni la calibration
de luminosité. Les autres régressions restent à vérifier ; les builds et leurs
provenances figurent dans le suivi.

Le candidat `159dbe0` remplace ensuite `3be0796` sur la Thor : correction ciblée
du bouton « Bifrost en haut », contrôles locaux/CI réussis, APK officiel du run
`33986789250` installé et bouton revalidé. Le moteur LED et Dolphin sont inchangés.
Les preuves physiques précédentes restent attachées à leur candidat. Le parcours
suivant (21:46–21:59) ajoute Whirlwind visible en partie, les opérations du slot J2
(sans coop à deux commandes), backup sécurisé vérifié et reconnexions Dolphin /
compagnon. Le retour physique bleu gauche / rouge droite après l’arrêt volontaire
Dolphin à 21:53:47 sur `159dbe0` est ensuite confirmé par l’utilisateur le 5 septembre.
Veille, arrêt du compagnon et luminosité restent à observer ; un nouveau message
Wii déconnectée a été constaté, sans cause établie. Détails et
point d’arrêt dans le suivi, PR #15 toujours en brouillon.

Le contrat technique API 4 est documenté dans [`V6_LED_API4.md`](V6_LED_API4.md).

## Principes non négociables

- aucune régression des fonctions V5 ;
- aucune écriture directe par SkyPortal dans un fichier `.sky` monté ;
- fonctionnement sans Internet et sans serveur externe ;
- communication locale uniquement ;
- fonctionnement complet de base sans Bifrost ;
- libération explicite du contrôle LED, avec restauration preset/profil déléguée
  à Bifrost tant que son service fonctionne ; aucune garantie après son arrêt/crash ;
- aucune ROM, aucun jeu et aucun dump tiers distribué ;
- aucune ressource officielle non autorisée intégrée ;
- compatibilité prioritaire avec l’AYN Thor Max sous Android 13 ;
- validation réelle sur les écrans logiques `0` et `4` ;
- fonctions matérielles désactivables individuellement ;
- toute nouvelle méthode AIDL est ajoutée à la fin du contrat existant.

---

# V6 Foundation — chantier préparatoire

## Objectif

Préparer l’architecture V6 sans modifier le comportement de la version stable 0.5.0.

Cette phase doit fournir :

- la présente roadmap versionnée ;
- un modèle pur `PortalLedState` ;
- un parseur strict et testable du futur payload Dolphin LED ;
- une abstraction `LedOutputBridge` indépendante de Bifrost ;
- une sortie `NoOp` lorsque les LED ne sont pas disponibles ;
- une politique déterministe de limitation de fréquence ;
- des tests JVM sans dépendance au matériel ;
- une frontière claire entre l’état lumineux Dolphin, le rendu Compose et la sortie physique.

Cette phase ne doit pas encore :

- modifier l’AIDL publié ;
- passer Dolphin en API 4 ;
- envoyer de commande à Bifrost ;
- modifier les LED de la Thor ;
- changer l’interface visible ;
- augmenter le numéro de version Android ;
- publier une release.

## Architecture cible

```text
Dolphin API 4
└─ payload LED versionné
   ↓
PortalLedStateParser
   ↓
PortalLedState
   ├─ AnimatedPortal (Compose)
   └─ LedOutputCoordinator
      ├─ LedCommandRateLimiter
      ├─ BifrostLedOutputBridge
      └─ NoOpLedOutputBridge
```

Le même état doit alimenter l’écran et les joysticks afin d’éviter toute divergence visuelle.

---

# V6.0 — Portal Experience

## Objectif

Créer la nouvelle identité visuelle de SkyPortal et synchroniser en temps réel :

- l’état lumineux émulé dans Dolphin ;
- le portail animé sur l’écran inférieur ;
- les LED gauche et droite de l’AYN Thor via Bifrost.

## Interface

- portail central animé ;
- cartes J1 et J2 clairement séparées ;
- états de connexion compacts ;
- couleurs par élément ;
- animations de pose, remplacement et retrait ;
- favoris, récents, équipes rapides et mode 1J/2J conservés ;
- navigation tactile adaptée à l’écran inférieur ;
- mode d’animations réduites pour l’accessibilité et la batterie.

## Dolphin SkyPortal API 4

**État : fusionné dans `main` (PR #11).** Les validations historiques sur la Thor avec Spyro’s Adventure ont confirmé les séquences LED et valeurs RGB gauche/droite. La régression d’activation/keepalive a été corrigée et validée sur matériel dans la PR #13. Le canal Trap et les autres jeux n’ont pas de validation matérielle revendiquée.

Le patch Dolphin expose un état lumineux versionné comprenant au minimum :

- couleur gauche ;
- couleur droite ;
- couleur de Trap facultative ;
- portail actif ou éteint ;
- compteur de séquence monotone ;
- version du schéma.

Contrat de schéma 1 implémenté :

```json
{
  "schemaVersion": 1,
  "active": true,
  "sequence": 42,
  "left": { "r": 160, "g": 64, "b": 255 },
  "right": { "r": 160, "g": 64, "b": 255 },
  "trap": null
}
```

Le contrat API 1–3 existant reste inchangé. La méthode LED est ajoutée à la fin et n’est appelée qu’après confirmation d’une API 4 compatible.

## Portail animé

**État : rendu initial et composition corrigée fusionnés dans `main` (PR #12 et #14).** La mise en page de #14 a été revalidée sur la vraie Thor dans SSA le 5 septembre, puis fusionnée après accord utilisateur dans `12d23a1`. Le suivi conserve les résultats matériels et les limites séparément de la CI ; le nouveau chantier Bifrost doit apporter ses propres preuves.

Le rendu Compose doit prendre en charge :

- activation et extinction progressives ;
- changements de couleur fluides ;
- couleurs gauche et droite distinctes ;
- couleur de Trap lorsque le canal est disponible et que le jeu actif déclare `GameFeature.TRAPS` ;
- pulsation de pose ;
- fondu de retrait ;
- conflit USB ;
- attente de handshake ;
- perte et reprise de Dolphin.

Le rendu peut rester fluide à la fréquence de l’écran, indépendamment de la cadence des LED physiques.

## Intégration Bifrost

Cette étape a été explicitement autorisée après la clôture de la PR #14. Le
[contrat technique audité](V6_BIFROST.md) cible exclusivement Bifrost `1.3.1` / code
`16`, API externe `1`, commit officiel `1baddf1644ff0d7edd1bd0f4ba02f7eb6c8e3cfa`
(Android minimum API 33). Toute autre version reste désactivée jusqu’à un nouvel
audit. La fiabilisation préalable et l’intégration se poursuivent sur
`agent/v6-bifrost-integration`. Le périmètre initial ci-dessous est implémenté dans
la branche et a passé les contrôles locaux. La baseline physique Bifrost, la
réception des commandes SkyPortal, puis les couleurs physiques synchronisées et
leur restitution après OFF ont leurs preuves distinctes sur le candidat `3be0796`.
Le retour après arrêt volontaire Dolphin a ensuite sa confirmation physique
distincte sur `159dbe0`. Les autres restitutions et la variation de luminosité
restent à vérifier. Les modes LED J1/J2 et priorité J1
ne sont pas ajoutés : V6.0 n’est pas déclarée achevée. Une fusion ou publication
nécessitera son autorisation propre.

SkyPortal utilisera l’API publique de broadcasts Android de Bifrost, sans copier
son contrôleur matériel ni appeler directement `PServerBinder`. Aucun profil
Bifrost ne sera installé, aucune permission root supplémentaire ne sera demandée
et Dolphin ne sera pas modifié pour cette intégration.

États à gérer :

- Bifrost absent ;
- Bifrost installé, état réel du service inconnu ;
- contrôle tiers désactivé ;
- commande acceptée par le receiver, sans confirmation matérielle ;
- commande rejetée ou limitée ;
- API Bifrost incompatible.

Périmètre initial retenu :

- **Désactivé par défaut** : portail animé uniquement, fonctionnement autonome
  lorsque Bifrost est absent ;
- **Portal of Power** après activation volontaire : couleurs API 4 gauche → stick
  gauche, droite → stick droit, effet `STATIC` et luminosité réglable ;
- `ACTION_CLEAR` pour rendre le contrôle à Bifrost, sans écraser ses réglages.

Les modes Joueurs/Élément actif, les pulsations et les effets d’erreur restent des
possibilités ultérieures, pas des fonctions promises par cette première intégration.
La restauration du preset ou du profil actif est gérée par Bifrost tant que son
service reste vivant. L’arrêt/crash de Bifrost et l’isolation entre appelants
ne sont pas des garanties offertes par le contrat audité.

## Fréquence et sécurité LED

- au plus 2 commandes `DISPLAY` par seconde, avec heartbeat toutes les 500 ms ;
- regroupement des changements rapides, mais maintien du heartbeat même si la
  couleur est identique pour renouveler le bail ;
- dernière couleur utile conservée ;
- terminator `EXPLICIT_CLEAR` ; Bifrost 1.3.1 surveille son bail de 1 500 ms
  toutes les 400 ms tant que son service fonctionne ;
- aucune durée courte répétée : le fast-path de renouvellement ne réarme pas
  son échéance dans la version auditée ;
- `ACTION_CLEAR` à l’arrêt du jeu, à la déconnexion Dolphin, à la désactivation,
  à la sortie du compagnon ou à l’extinction des écrans, sans boucle de réessai ;
- aucune erreur bloquante si Bifrost est absent.

## Diagnostic V6.0

Le diagnostic doit afficher :

- version API Dolphin ;
- version du schéma LED ;
- séquence reçue ;
- couleurs gauche, droite et Trap ;
- état actif ;
- présence et version de Bifrost ;
- état du service explicitement inconnu lorsque l’API ne le fournit pas ;
- contrôle tiers autorisé ou non ;
- dernière commande et dernier code receiver, sans les présenter comme un
  acquittement de l’application matérielle ;
- cadence effective ;
- mode et luminosité choisis.

## Validation V6.0

- portail animé conforme au payload Dolphin ;
- couleurs gauche et droite distinctes validées ;
- absence de clignotement parasite ;
- limite de fréquence respectée ;
- restauration Bifrost après `ACTION_CLEAR` et expiration du bail, service vivant ;
- arrêt/crash Bifrost documenté séparément, sans promesse de restauration ;
- absence de crash lorsque Bifrost manque ;
- aucune perte de performance perceptible dans Dolphin ;
- écran inférieur fluide ;
- modes 1J/2J et fonctions V5 inchangés ;
- tests réels sur AYN Thor et contrôle Logcat.

Les couleurs synchronisées et le retour bleu gauche / rouge droite après OFF
ont été confirmés physiquement par l’utilisateur le 5 septembre sur `3be0796` ;
le retour après arrêt volontaire Dolphin est confirmé séparément sur `159dbe0`.
Les autres critères matériels, notamment veille, arrêt du compagnon et luminosité,
restent distincts et non validés par ces confirmations.
Les contrôles locaux sur
le code `3be0796` ont réussi : 157 tests, Lint sans erreur bloquante, compilation
Debug et licence. La checklist détaillée figure dans [`V6_BIFROST.md`](V6_BIFROST.md) ;
les résultats et leur provenance restent dans le [suivi courant](PROJECT_STATUS.md).

---

# V6.1 — Créateur de figurines

## Objectif

Créer une figurine depuis SkyPortal sans ouvrir le gestionnaire Dolphin.

```text
Jeu d’origine
→ élément
→ personnage
→ variante
→ nom du fichier
→ dossier de destination
→ création par Dolphin
```

Dolphin doit :

- valider ID et variant ;
- générer un dump vierge valide ;
- écrire via SAF ;
- refuser tout écrasement involontaire ;
- retourner le résultat réel.

SkyPortal choisit et affiche ; Dolphin génère et écrit ; SkyPortal rescanne.

---

# V6.2 — Objets temporaires

## Objectif

Permettre d’utiliser sans fichier permanent les contenus qui ne nécessitent pas de progression durable :

- Magic Items ;
- Adventure Packs et Location Pieces ;
- Sidekicks ;
- autres objets explicitement reconnus comme temporaires.

Les personnages, Traps, Creation Crystals et véhicules avec progression conservent un fichier permanent.

API envisagée :

```text
loadTemporaryFigure(slot, figureId, variantId, displayName)
removeTemporaryFigure(slot)
```

La génération et la mémoire temporaire restent sous la responsabilité de Dolphin.

---

# V6.3 — Lecture de progression

## Objectif

Afficher en lecture seule les informations contenues dans les dumps :

- niveau ;
- expérience ;
- or ;
- chemin d’amélioration ;
- améliorations débloquées ;
- chapeau ;
- surnom ;
- défis héroïques ;
- génération et variante ;
- état du fichier et checksums ;
- dernière modification ;
- backup le plus récent.

Une figurine active ne doit jamais être lue au milieu d’une écriture. SkyPortal utilisera soit un snapshot cohérent fourni par Dolphin, soit une lecture hors montage.

---

# V6.4 — Collection avancée et finition

Fonctions envisagées :

- fiches détaillées ;
- tris par niveau, élément, jeu et dernière utilisation ;
- détection des doublons ;
- comparaison de variantes ;
- progression globale de collection ;
- statistiques par élément ;
- personnages jamais utilisés ;
- filtres enregistrés ;
- recherche tolérante ;
- backups automatiques configurables ;
- restauration guidée ;
- export JSON ;
- mode galerie ;
- optimisation batterie et accessibilité.

---

# Ordre officiel

```text
v0.5.0 stable et figée
    ↓
V6 Foundation
    ↓
V6.0 — Portal Experience
    ↓
V6.1 — Créateur de figurines
    ↓
V6.2 — Objets temporaires
    ↓
V6.3 — Lecture de progression
    ↓
V6.4 — Collection avancée et finition
```

Chaque étape doit passer par une branche `agent/*`, une pull request vers `main` et le contrôle obligatoire `validate`. Les tags `v*` publiés ne doivent jamais être déplacés ni réutilisés.
