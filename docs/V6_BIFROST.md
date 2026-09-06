<!-- Copyright 2026 LeVraiPedro and SkyPortal Thor contributors -->
<!-- SPDX-License-Identifier: GPL-2.0-or-later -->

# V6.0 — Contrat Bifrost et limites de validation

Ce document fixe le contrat technique de l’intégration facultative. Il ne remplace
pas le [suivi du projet](PROJECT_STATUS.md), qui conserve les versions d’APK,
les tests effectivement exécutés et le point d’arrêt courant.

Le 6 septembre, l’utilisateur suspend la campagne matérielle pour passer à un
chantier distinct d’interface épurée sur `agent/v6-interface-refresh`, depuis
`696db59`. La PR #15 reste ouverte en brouillon à cette reprise. Les résultats
ci-dessous ne valident ni les scénarios encore ouverts ni la nouvelle interface.
Celle-ci dispose désormais de ses propres preuves **hors jeu** dans le suivi :
PR #16 ouverte en brouillon, navigation sur `46ed581`, puis accueil/collection
sur `02840b0` signé et installé (164 tests JVM réussis, Lint sans erreur).
Aucune nouvelle campagne LED, opération de figurine ou partie n’est déduite de
ces contrôles visuels ; Dolphin, version et clé sont conservés.

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
service a ensuite été démarré pour un test STATIC isolé, avec un preset temporaire
bleu gauche / rouge droite et Default préservé. Sa présence en arrière-plan est
confirmée par ADB ; l’utilisateur a depuis confirmé physiquement ces deux couleurs
du test isolé. L’intégration SkyPortal est présente dans
la [PR #15 en brouillon](https://github.com/LeVraiPedro/SkyPortal-Thor/pull/15), code
`3be0796`, et les contrôles locaux ont réussi.
Le 5 septembre à 20:21–20:24 (Paris), les commandes SkyPortal ont été reçues dans
SSA `SSPP52`, API 4 / `RUNNING`, à 35 %. L’échantillon Android de 47 DISPLAY donne
46 intervalles de 504 à 516 ms, moyenne 506,46 ms ; Bifrost signale l’override actif.
Cet échantillon confirme la réception et la cadence observée, **pas** les couleurs
physiques. Une preuve distincte a été apportée ensuite par l’utilisateur le
5 septembre, sur le même candidat `3be0796` : les couleurs physiques suivent le
portail, puis reviennent au bleu gauche / rouge droite après OFF. Cette
confirmation lève ces deux attentes ciblées ; elle ne couvre pas la restitution
après watchdog, veille, perte Dolphin ou arrêt Bifrost, ni la calibration de luminosité.

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

Le bouton « Bifrost en haut » cible l’écran principal (`Display.DEFAULT_DISPLAY`).
Le lancement depuis l’écran inférieur déclenchait le routage interne de Bifrost
1.3.1 puis la disparition de son activité. Le correctif `159dbe0` ne change pas
le contrat LED. La préférence privée Bifrost forçant son écran inférieur reste
une configuration non validée ; le compagnon ne la lit ni ne la modifie.

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
réussis ne valent pas tests matériels ; chaque preuve physique est identifiée
séparément, sans étendre un résultat OFF à tous les scénarios de restitution.
Les résultats, APK exacts, empreintes et limites sont consignés dans
[`PROJECT_STATUS.md`](PROJECT_STATUS.md), sans données privées ni capture tierce.

### Contrôles automatisés et code

Les preuves ci-dessous portent sur le code `3be0796`, inchangé dans `92d063d`.
Les rapports JVM existants du 5 septembre à 14:18 UTC donnent **9 tests
`BifrostFramePolicyTest` et 19 tests `BifrostSessionTest` réussis**, inclus dans les
157 tests du projet. Leur relecture n’est pas une nouvelle exécution.

- Option initiale et absence de Bifrost :

  - [x] JVM : modèle OFF, aucune frame sans consentement ; absence, apparition,
    disparition et refus simulés sans commande implicite.
  - [ ] Android automatisé : lecture/écriture et restauration des préférences.
- Version/code audités uniquement :

  - [x] JVM : absence de DISPLAY pour une disponibilité « version non prise en charge ».
  - [x] Inspection : filtre exact `1.3.1 / 16`, état du receiver et message français.
  - [ ] Android automatisé : PackageManager et variantes version/code/receiver réelles.
- Couleurs et luminosité :

  - [x] JVM : `STATIC`, gauche/droite indépendantes, Trap ignoré, conversion
    0/35/100 % vers 0/89/255 ; API ancienne, état non prêt ou données périmées exclus.
  - [x] Inspection : valeurs bornées et extras Android gauche/droite/intensité correspondants.
  - [ ] Android automatisé : vérification de l’Intent effectivement envoyé.
- [x] JVM : intervalle DISPLAY de 500 ms, dernière frame utile, heartbeat identique,
  absence de rattrapage ; découverte limitée et backoff de 5 s après refus/absence.
- Libération et cycle de vie :

  - [x] JVM : CLEAR unique après tentative, même pendant le backoff ; sérialisation
    des opérations et libération depuis `finally` après annulation.
  - [x] Inspection : `EXPLICIT_CLEAR` sans durée courte, cycle `STARTED`,
    `isInteractive` et `finally` du contrôleur.
  - [ ] Android automatisé : callbacks de cycle de vie, multi-écrans et veille.
- Résultats et erreurs :

  - [x] JVM : sentinelle, écho exact, rejets connus, erreurs simulées et nettoyage
    sans faux succès ni promesse de restitution physique.
  - [x] Inspection : timeout Android de 1 000 ms, continuation active et exceptions.
  - [ ] Android automatisé : timeout réel et callback tardif du broadcast ordonné.
- [x] Inspection du périmètre Bifrost : aucune installation de profil, API matérielle
  directe, modification Dolphin ou écriture `.sky` ; pas de test automatique dédié
  garantissant cette absence.
- [x] Contrôles du code `3be0796` : 157 tests JVM réussis,
  Lint zéro erreur / 17 avertissements, compilation Debug 33 s et licence 91 sources réussies.
- [x] Construction et vérification du candidat Release `3be0796`, run `33971500140` ;
  APK installé et réextrait identique, provenance et certificat dans le suivi.

Le premier build signé `33971097637` sur `3d38933` a réussi sans installation.
Le build `33971500140` sur `3be0796` a réussi et son APK exact a été installé :
contrôles hors jeu puis réception des commandes dans SSA exécutés. Les couleurs
physiques synchronisées et leur restitution après OFF ont ensuite été confirmées
par l’utilisateur le 5 septembre ; les autres restitutions restent à vérifier.
Aucun APK précédent ne valide le nouveau code.

Complément `159dbe0`, limité au routage du bouton : 157 tests locaux réexécutés,
Lint zéro erreur/17 avertissements, Debug et licence réussis. Run signé
`33986789250` réussi, APK exact installé le 5 septembre à 21:25 puis réextrait
identique. Deux ouvertures réelles de Bifrost sur `0`, maintien du compagnon sur
`4`, réconciliation de Whirlwind sans doublon et cycle de réception/restitution
logicielle/reprise vérifiés. Les couleurs physiques ne sont pas automatiquement
revalidées pour ce nouveau binaire ; la commande Wii déconnectée limite les essais
en jeu. Provenance et empreintes complètes dans le suivi.

Parcours supplémentaire sur ce même `159dbe0`, 21:46–21:59 : Whirlwind visible
en partie, opérations J2 confirmées nativement (pas de coop à deux commandes),
backup après retrait comparé read-only, invalidation de sa confirmation à la mort
Dolphin, reconnexion du service puis relance SSA, et recréation du compagnon sans
doublon. CLEAR après mort Dolphin est accepté et Bifrost reprend STATIC. L’utilisateur
a ensuite confirmé le retour physique bleu gauche / rouge droite après cet arrêt
volontaire à 21:53:47 sur `159dbe0` (confirmation consignée le 5 septembre à 22:13).
Les autres chemins de restitution restent distincts. Aucun APK modifié pour ces essais.
Le message Wii déconnectée a réapparu pendant le parcours ; sa cause reste inconnue.
Le jeu relancé est laissé au titre, Whirlwind monté et synchronisation OFF/35 %.

Complément sur `159dbe0`, le 5 septembre : variation qualitative 0 % → 70 % →
35 % à 22:43–22:44, confirmée à 22:58 (éteint, rallumé, puis moins lumineux).
La restitution physique bleu gauche / rouge droite pendant la veille de 22:59:11
est confirmée à 23:11. CLEAR est accepté à 22:59:11.254, puis STATIC reprend à
22:59:11.535, service Bifrost vivant et les deux écrans OFF. Ces deux preuves
ne sont ni une calibration ni une observation d’absence de clignotement.
À 23:11:41, Whirlwind est visible en partie, seul slot natif `#0 (0/0)`, USB prêt,
compagnon ON/35 % sur `4` et jeu sur `0`. La Thor étant déjà `Awake` avant la
commande `224`, aucun réveil contrôlé ni correctif Wii n’est revendiqué.

### Contrôles sur AYN Thor

- [x] Contrôles limités hors jeu sur ce candidat : UI inférieure, OFF/35 %,
  Binder/API4, SAF, 32 fichiers et bascule 1J/2J/1J. Aucun résultat physique LED
  ni chargement en jeu déduit de ce parcours.
- [x] Préparation : provenance, version et signature Bifrost officielles vérifiées ;
  installation `adb install -r` réussie. Cela ne valide pas une commande LED.
- [x] Réglage initial relevé ; Default préservé, seul un preset temporaire créé
  par l’interface Bifrost pour le test isolé (pas par SkyPortal).
- [x] Service démarré et conservé derrière Dolphin ; test STATIC isolé bleu gauche /
  rouge droite confirmé physiquement par l’utilisateur. Cette attente initiale
  est levée, sans valider les couleurs commandées par SkyPortal.
- [x] Réception dans SSA le 5 septembre à 20:21–20:24 : API 4, `SSPP52`, `RUNNING`,
  USB prêt, 16 slots libres ; accusé receiver et cadence observée d’environ 1,97 Hz
  sur 47 DISPLAY. Aucun chargement de dump pendant ce parcours.
- [x] Fenêtre Logcat 20:21–20:23:10 sans crash, ANR, erreur de permission/Binder
  ni erreur de transaction LED relevée ; les interruptions restent à tester.
- [x] Parcours SSA 20:46–20:51 : chargement/remplacement/retrait J1 et reconnexion
  après arrêt forcé du compagnon sans doublon de slot ; reprise des commandes.
- [x] Watchdog après arrêt forcé et CLEAR explicite après OFF documentés séparément,
  retour Android `resultCode=0` et absence de DISPLAY ultérieur ; leur résultat
  physique n’est pas déduit de ces traces. Logcat du parcours sans erreur critique recherchée.
- [x] Sur `3be0796`, contrôle tiers réellement refusé : message français explicite,
  temporisation observée d’environ 5 s puis reprise après autorisation rétablie.
- [x] Sur `3be0796`, masquage réel du compagnon (`STOPPED`) et veille : CLEAR
  logiciel accepté, arrêt des DISPLAY, reprise au retour. Cette preuve n’est pas
  une observation physique des restitutions ; voir les cases distinctes ci-dessous.
- [x] Sur `3be0796`, service Bifrost absent après arrêt forcé : compagnon réactif,
  Whirlwind monté en slot natif, sans affirmation d’éclairage matériel ; service
  relancé manuellement et commandes reprises. Le jeu attendait sa commande Wii,
  donc l’apparition de Whirlwind n’est pas validée par ce parcours.
- [ ] Mode OFF puis absence/arrêt de Bifrost sans régression SkyPortal ou Dolphin.
- [x] Avec consentement et service actif, couleurs physiques suivant le portail
  confirmées par l’utilisateur le 5 septembre sur `3be0796`, distinctes du test isolé Bifrost.
- [x] Variation qualitative de luminosité sur `159dbe0`, le 5 septembre : 0 %
  éteint → 70 % rallumé → 35 % moins lumineux, confirmée par l’utilisateur.
  Aucune calibration lumineuse ni absence de clignotement déduite.
- [ ] Couleur constante maintenue au-delà du bail, transitions sans clignotement parasite.
- [ ] Contrôle conservé lorsque seul le focus passe à Dolphin sur l’écran supérieur.
- [x] Après désactivation OFF : retour physique bleu gauche / rouge droite confirmé
  par l’utilisateur le 5 septembre sur `3be0796`, en complément du CLEAR observé.
- [x] Arrêt volontaire Dolphin à 21:53:47 sur `159dbe0` : retour physique bleu
  gauche / rouge droite confirmé explicitement par l’utilisateur le 5 septembre,
  en complément du CLEAR. Ne valide pas les autres causes de déconnexion.
- [x] Veille à 22:59:11 sur `159dbe0` : CLEAR logiciel accepté et retour physique
  bleu gauche / rouge droite confirmé à 23:11, Bifrost vivant, écrans `0`/`4` OFF.
- [ ] `CLEAR` après sortie et autres déconnexions : réglage Bifrost retrouvé
  pour chacun de ces chemins, indépendamment des résultats OFF, arrêt Dolphin et veille.
- [ ] Arrêt brutal SkyPortal : expiration du bail et restauration physique observées,
  Bifrost vivant. Test non rejoué après interruption ; campagne désormais suspendue.
- [ ] Arrêt/reprise Bifrost : absence de crash SkyPortal, limites de restauration documentées.
- [ ] Reprise après veille, reconnexion, fiches d’actions et J1/J2 vérifiés sans mapping périmé.
- [ ] Logcat examiné ; aucun crash, ANR, refus de permission ou boucle de commandes nouveau.

La validation de cette intégration ne couvre pas automatiquement les autres jeux
Skylanders : seule SSA dispose des preuves matérielles historiques du portail
API 4. Toute fusion et toute publication restent soumises à un accord distinct.
