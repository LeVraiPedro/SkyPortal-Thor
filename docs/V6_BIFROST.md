<!-- Copyright 2026 LeVraiPedro and SkyPortal Thor contributors -->
<!-- SPDX-License-Identifier: GPL-2.0-or-later -->

# V6.0 — Contrat Bifrost et limites de validation

Ce document fixe le contrat technique de l’intégration facultative. Il ne remplace
pas le [suivi du projet](PROJECT_STATUS.md), qui conserve les versions d’APK,
les tests effectivement exécutés et le point d’arrêt courant.

## Référence auditée

L’audit du 5 septembre 2026 porte sur le dépôt officiel
[Pollux-MoonBench/Bifrost, version 1.3.1](https://github.com/Pollux-MoonBench/Bifrost/tree/1.3.1),
commit `1baddf1644ff0d7edd1bd0f4ba02f7eb6c8e3cfa` :

- package Release `com.moonbench.bifrost` ;
- `versionName = 1.3.1`, `versionCode = 16` ;
- API publique externe `1` ;
- Android minimum API `33`, compatible avec la cible Android 13.

Références primaires épinglées :
[configuration Android](https://github.com/Pollux-MoonBench/Bifrost/blob/1baddf1644ff0d7edd1bd0f4ba02f7eb6c8e3cfa/app/build.gradle.kts),
[constantes API](https://github.com/Pollux-MoonBench/Bifrost/blob/1baddf1644ff0d7edd1bd0f4ba02f7eb6c8e3cfa/app/src/main/java/com/moonbench/bifrost/external/ExternalApi.kt),
[receiver](https://github.com/Pollux-MoonBench/Bifrost/blob/1baddf1644ff0d7edd1bd0f4ba02f7eb6c8e3cfa/app/src/main/java/com/moonbench/bifrost/external/ExternalApiReceiver.kt),
[service LED](https://github.com/Pollux-MoonBench/Bifrost/blob/1baddf1644ff0d7edd1bd0f4ba02f7eb6c8e3cfa/app/src/main/java/com/moonbench/bifrost/services/LEDService.kt)
et [guide d’intégration](https://github.com/Pollux-MoonBench/Bifrost/blob/1baddf1644ff0d7edd1bd0f4ba02f7eb6c8e3cfa/INTEGRATING.md).

Le support initial est volontairement limité à cette version/code audités. Une
autre version, même annonçant l’API 1, doit être réauditée avant activation :
le bail et la restauration dépendent du service, pas uniquement des constantes API.

Au contrôle initial, Bifrost était absent de la Thor. L’APK officiel a depuis été
installé après vérification et autorisation. L’utilisateur a confirmé les notifications
et la configuration du contrôle tiers, également constaté activé sur capture ; le
service reste absent. Il a été demandé de quitter le mode initial Ambient pour
Static et de démarrer « Call Heimdall ». L’intégration SkyPortal est présente dans
la [PR #15 en brouillon](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/15), code
`3be0796`, et les contrôles locaux ont réussi.
Les essais des LED physiques et de restauration restent à faire ; ni l’installation
de Bifrost ni un audit source ne constituent ces preuves.

## Périmètre SkyPortal retenu

- Synchronisation désactivée par défaut, activation volontaire dans SkyPortal,
  luminosité initiale 35 % ; bouton LED, dialogue et diagnostic dans le compagnon.
- Couleurs gauche/droite provenant du payload Dolphin API 4 valide, effet
  `STATIC`, luminosité réglable ; pas d’affectation implicite du canal Trap.
- Portail animé, collection et Binder fonctionnels indépendamment de Bifrost.
- Broadcasts locaux explicites vers
  `com.moonbench.bifrost.external.ExternalApiReceiver`, API externe `1`.
- Permission Android normale `com.moonbench.bifrost.permission.CONTROL_LEDS`
  et visibilité du package ; aucune permission root supplémentaire.
- Aucun appel direct au contrôleur matériel ou à `PServerBinder`, aucune copie
  de leur implémentation, aucune modification Dolphin pour cette intégration.
- Aucun `ACTION_INSTALL_PROFILE`, import de profil ou écrasement des presets
  utilisateur. Les effets Joueurs, Élément actif et pulsations restent hors du
  premier périmètre.

Le contrôle tiers doit être autorisé dans Bifrost et son service doit fonctionner.
SkyPortal ne démarre pas silencieusement ce service à distance.

## Réponse API et résultat matériel : deux preuves distinctes

Les commandes utilisent un broadcast ordonné, un identifiant de requête et un
délai maximal de réponse de 1 000 ms. Un résultat absent ou non corrélé ne vaut pas succès.
`RESULT_ACCEPTED = 0` signifie que le receiver a accepté la commande. Il peut
néanmoins abandonner sa transmission lorsque `LEDService.isRunning` est faux,
ou ne pas rapporter un échec d’envoi au service.

Le diagnostic doit donc afficher « commande acceptée, application matérielle
non confirmée » et **service inconnu**, jamais « LED synchronisées » à partir de
ce seul résultat. La présence du package ne prouve pas davantage la vie du service.
Une validation physique exige l’observation des anneaux gauche et droit ; le
portail Compose n’est pas cette preuve.

Les rejets connus restent explicites : contrôle tiers désactivé (`-1`), API
incompatible (`-2`), commande invalide (`-3`), appelant non résolu (`-4`), cadence
refusée (`-5`) ou action inconnue (`-6`). Les erreurs Bifrost ne doivent pas
bloquer le chargement des figurines dans Dolphin.

## Cadence, bail et libération

Le protocole initial prévoit `DISPLAY` en `STATIC`, avec `until = EXPLICIT_CLEAR`
et sans `durationMs`. SkyPortal renouvelle l’état toutes les **500 ms**, soit au
plus **2 DISPLAY/s** ; les changements intermédiaires sont regroupés en conservant
le plus récent. Une couleur inchangée doit encore produire ce heartbeat.
Les rejets imposent une temporisation de 5 s. La découverte PackageManager est
effectuée seulement lorsqu’un tick peut émettre ; l’absence de Bifrost impose
aussi 5 s avant une nouvelle recherche, sans découverte à chaque frame.
Le contrôleur suit un cycle de vie
au moins `STARTED`, l’état `isInteractive` et une fraîcheur du payload de 1 500 ms
maximum ; les essais multi-écrans restent nécessaires pour valider ce comportement.

Dans le service 1.3.1 audité, chaque commande du propriétaire renouvelle un bail
de **1 500 ms**, surveillé toutes les **400 ms** lorsqu’un override est actif.
Ce filet de sécurité dépend du service vivant et de son ordonnancement Android ;
ce n’est ni un délai matériel strict ni une promesse pour d’autres versions.

Ne pas simuler ce bail par des durées courtes répétées : le fast-path même
appelant/même effet actualise la couleur et le bail, puis retourne avant la
reprogrammation de l’expiration `durationMs`. Une ancienne échéance peut donc
interrompre l’effet malgré les renouvellements. `EXPLICIT_CLEAR` évite ce chemin.

SkyPortal doit envoyer `ACTION_CLEAR` et arrêter les renouvellements lors de :

- désactivation volontaire de la synchronisation ;
- sortie du compagnon ou passage effectif en arrière-plan ;
- extinction des écrans ;
- déconnexion Dolphin, arrêt du jeu ou disparition d’un état API 4 exploitable.

Perdre seulement le focus au profit du jeu sur l’autre écran n’équivaut pas à
quitter le compagnon visible : ce cas multi-écrans doit être testé explicitement.
La libération ne doit pas engendrer une boucle de réessai. En cas d’arrêt brutal
de SkyPortal, le bail est un recours côté Bifrost, pas un `CLEAR` dont l’envoi
serait garanti.

## Restauration et limites du tiers

La fin de l’override est gérée par Bifrost : il réévalue son profil d’application
actif ou restaure son preset/état utilisateur selon sa configuration. SkyPortal
n’installe pas un preset de remplacement et ne mémorise pas des réglages privés
pour les réécrire ensuite.

Cette restauration est attendue **si le service Bifrost reste vivant** et doit
être observée après `CLEAR` et expiration du bail. Un arrêt/crash de Bifrost peut
supprimer cet état transitoire ; aucun rétablissement matériel n’est garanti dans
ce cas. L’isolation entre plusieurs appelants n’a pas été démontrée par l’audit :
ne pas promettre qu’un `CLEAR` est incapable d’affecter une autre intégration.
Les priorités et l’identité déclarée par le receiver ne suffisent pas à établir
une isolation de sécurité. Les essais initiaux doivent éviter un autre client LED.

## Checklist à exécuter

Les cases suivantes concernent ce nouveau chantier. Les contrôles locaux
réussis ne valent pas tests matériels ; les cases physiques restent ouvertes.
Les résultats, APK exacts, empreintes et limites sont consignés dans
[`PROJECT_STATUS.md`](PROJECT_STATUS.md), sans données privées ni capture tierce.

### Contrôles automatisés et code

- [ ] Option OFF initiale et fonctionnement sans Bifrost ; aucune commande implicite.
- [ ] Version/code audités uniquement ; refus lisible des versions inconnues.
- [ ] Mapping `STATIC` gauche/droite et luminosité bornée depuis API 4.
- [ ] Deux DISPLAY/s maximum, dernière couleur utile conservée, heartbeat identique maintenu.
- [ ] `EXPLICIT_CLEAR` sans durée courte, libérations et annulation des tâches vérifiées.
- [ ] Résultats corrélés, rejets, délais et exceptions sans faux succès matériel.
- [ ] Aucune installation de profil, API matérielle directe, modification Dolphin ou écriture `.sky`.
- [x] Contrôles du code `3be0796` : 157 tests JVM réussis,
  Lint zéro erreur / 17 avertissements, compilation Debug 33 s et licence 91 sources réussies.
- [x] Construction et vérification du candidat Release `3be0796`, run `33971500140` ;
  APK installé et réextrait identique, provenance et certificat dans le suivi.

Le premier build signé `33971097637` sur `3d38933` a réussi sans installation.
Le build `33971500140` sur `3be0796` a réussi et son APK exact est installé :
contrôles hors jeu seulement à ce stade, LED physiques encore non validées.
Aucun APK précédent ne valide le nouveau code.

### Contrôles sur AYN Thor

- [x] Contrôles limités hors jeu sur ce candidat : UI inférieure, OFF/35 %,
  Binder/API4, SAF, 32 fichiers et bascule 1J/2J/1J. Aucun résultat physique LED
  ni chargement en jeu déduit de ce parcours.
- [x] Préparation : provenance, version et signature Bifrost officielles vérifiées ;
  installation `adb install -r` réussie. Cela ne valide pas une commande LED.
- [ ] Réglage utilisateur initial relevé ; aucune modification de preset/profil.
- [ ] Refus du contrôle tiers affiché clairement ; service inconnu sans faux état prêt.
- [ ] Mode OFF puis absence/arrêt de Bifrost sans régression SkyPortal ou Dolphin.
- [ ] Avec consentement et service actif, couleurs physiques gauche/droite et luminosité observées.
- [ ] Couleur constante maintenue au-delà du bail, transitions sans clignotement parasite.
- [ ] Contrôle conservé lorsque seul le focus passe à Dolphin sur l’écran supérieur.
- [ ] `CLEAR` après désactivation, sortie, veille et déconnexion ; réglage Bifrost retrouvé.
- [ ] Arrêt brutal SkyPortal : expiration du bail et restauration observées, Bifrost vivant.
- [ ] Arrêt/reprise Bifrost : absence de crash SkyPortal, limites de restauration documentées.
- [ ] Reprise après veille, reconnexion, fiches d’actions et J1/J2 vérifiés sans mapping périmé.
- [ ] Logcat examiné ; aucun crash, ANR, refus de permission ou boucle de commandes nouveau.

La validation de cette intégration ne couvre pas automatiquement les autres jeux
Skylanders : seule SSA dispose des preuves matérielles historiques du portail
API 4. Toute fusion et toute publication restent soumises à un accord distinct.
