# Validation sur AYN Thor / Android 13

## V6.0 Bifrost — chantier autorisé après fusion de #14

La validation de #14 ci-dessous est historique. La nouvelle intégration doit
être validée avec son propre APK signé, sans remplacer Dolphin API 4.

**Reprise du 6 septembre :** campagne Bifrost suspendue à la demande de
l’utilisateur, sans clôture ni fusion de la PR #15, toujours ouverte en brouillon
à `696db59`. Le chantier distinct `agent/v6-interface-refresh` part de ce commit
pour épurer l’interface et ranger le diagnostic dans les réglages, sans masquer
les erreurs. Les premières preuves d’interface sont séparées ci-dessous ; elles
ne clôturent pas la campagne Bifrost.

### Interface épurée — PR #16, 6 septembre, hors jeu

PR #16 ouverte en brouillon, empilée sur `agent/v6-bifrost-integration` ; aucune
fusion. Les deux candidats ci-dessous restent distincts.

- [x] `46ed581`, run signé `34025548487`, installé à 11:50:16 : navigation
  J1/J2 puis solo, collection 32 figurines, filtres, 12 récents, équipes,
  emplacements supplémentaires, réglages et diagnostic vérifiés sur l’écran `4`.
- [x] Même premier candidat : Binder API 4, SAF lecture/écriture, 16 slots libres
  et éclairage ON/35 % conservés, sans jeu lancé.
- [x] `02840b0`, run signé `34025947957` et CI `34025948311` réussis : 164 tests
  JVM, zéro erreur ; Lint zéro erreur/17 avertissements ; Debug/Release compilés ;
  contrôle de licence réussi sur 95 sources.
- [x] Candidat final signé avec le certificat persistant officiel, installé par
  `adb install -r` à 12:01:52. Accueil solo et collection 32 figurines capturés
  sur `4` ; filtres dépliés laissant la grille visible, réglages et retour accueil
  vérifiés à 12:02–12:03. `0` reste à l’accueil Android. Hash installé identique
  à l’artefact, hash Dolphin recontrôlé inchangé.
- [x] Logcat UI : 4 559 lignes de 11:50:00.142 à 12:03:13.258, aucun crash,
  ANR, exception Compose/permission ou erreur explicite Binder/SAF attribuable
  à SkyPortal ; avertissements non bloquants et exceptions tierces distingués.
  L’arrêt du premier candidat est sa mise à jour, pas un crash.
- [ ] Navigation étendue intégralement rejouée sur le candidat final : les
  contrôles étendus ci-dessus portent sur `46ed581`, pas automatiquement `02840b0`.
- [ ] Validation en jeu et opérations chargement/retrait/backup/stress de la
  nouvelle interface : non effectuées dans cette campagne visuelle.
- [ ] Retour utilisateur sur cette direction visuelle.

Les hashes, certificat et périmètre exact sont dans [le suivi](docs/PROJECT_STATUS.md).
Les résultats Bifrost qui suivent conservent leur provenance historique.

- [x] Bifrost officiel 1.3.1/code 16 audité ; APK signé et SHA-256 vérifiés.
- [x] Installation autorisée sur Thor, sans remplacement Dolphin ni effacement.
- [x] Test isolé Bifrost : preset temporaire STATIC bleu gauche / rouge droite,
  Default préservé, service foreground confirmé et conservé derrière Dolphin.
  L’utilisateur a confirmé physiquement ces couleurs ; seule la baseline Bifrost
  est validée, pas la synchronisation SkyPortal ni la restitution.
- [x] Candidat `3be0796` / run `33971500140` signé officiellement, vérifié,
  installé par mise à jour ; APK réextrait identique. CI et 157 tests réussis.
- [x] Parcours **hors jeu** : écran 4, portail sans chevauchement, LED OFF/35 %,
  Binder/API4, droits SAF et 32 fichiers ; modes 1J/2J/1J et sélecteur J2.
  Les cases en jeu ci-dessous restent ouvertes.
- [x] Reprise 5 septembre 20:21–20:24 : SSA `SSPP52`, API 4 / `RUNNING`, USB prêt,
  16 slots libres ; option ON/35 %, accusé receiver dans dialogue et diagnostic.
  Aucun dump chargé ; SSA attendait encore « Appuie sur A » au dernier contrôle.
- [x] Cadence observée : 47 DISPLAY Android, 46 intervalles de 504 à 516 ms,
  moyenne 506,46 ms ; override Bifrost actif, sans expiration relevée.
  Cela ne mesure ni les couleurs physiques ni leur fréquence de rendu.
- [x] Logcat limité à 20:21–20:23:10 : aucun crash, ANR, `SecurityException`,
  `DeadObjectException` ou échec de transaction LED relevé.
- [x] Parcours en partie 20:46–20:51 : Lightning Rod chargé, remplacé par Sonic Boom,
  puis retrait ; apparitions et retrait confirmés dans SSA, 16 slots libres à la fin.
- [x] Arrêt forcé / relance SkyPortal avec Sonic Boom monté : même Dolphin vivant,
  même slot unique `#0 (1/0)`, reprise du flux LED sans second chargement demandé.
- [x] Watchdog Bifrost après arrêt forcé, puis CLEAR explicite après OFF observés
  séparément ; retour Android `resultCode=0`, aucune émission DISPLAY après OFF
  dans l’historique contrôlé. Ce ne sont pas des confirmations physiques.
- [x] Logcat 20:46–20:51 : aucune erreur critique recherchée ; avertissements Android
  de fermeture de canaux d’entrée conservés dans le rapport, sans crash.
- [x] Couleurs physiques pilotées par SkyPortal puis restitution bleu gauche /
  rouge droite après OFF confirmées explicitement par l’utilisateur le 5 septembre,
  sur `3be0796`. Preuve distincte de la baseline isolée et des accusés receiver.
- [ ] Absence de clignotement parasite observée ; la variation qualitative de
  luminosité sur le candidat suivant est documentée séparément ci-dessous.
- [x] Contrôle tiers désactivé : refus explicite sur `3be0796`, temporisation
  observée d’environ 5 s, puis réception reprise après restauration de l’autorisation.
- [x] `3be0796`, 21:06–21:16 : commandes conservées avec focus du jeu ; CLEAR
  logiciel après accueil côté Dolphin, masquage réel du compagnon (`STOPPED`)
  et veille des deux écrans. Reprise au retour ; résultat physique encore distinct.
- [x] Arrêt Bifrost : service absent, compagnon utilisable, chargement Whirlwind
  confirmé en slot natif `#0 (0/0)`, aucun faux état matériel. Relance du service
  par l’interface puis reprise des commandes. Apparition en jeu non validée :
  télécommande Wii déconnectée après veille, intervention utilisateur demandée.
- [ ] Heartbeat 2 Hz sans rafale ; réponse du receiver distinguée du résultat physique.
- [ ] Retour accueil, écran éteint et arrêt du compagnon : restitution Bifrost observée.
- [ ] Mort Dolphin et reconnexion : fin puis reprise du flux, sans doublon de figurine.
- [ ] Service Bifrost arrêté : le compagnon reste utilisable, aucun faux état physique.
- [ ] J2 et autres régressions complètes du portail sur ce candidat ; J1 couvert ci-dessus.
- [ ] Fiche d’actions ouverte puis mort Dolphin : aucune ancienne action disponible.
- [ ] Backup normal terminé après son propre retrait, sans écriture de fichier monté.
- [ ] Absence de crash/ANR et d’erreur Binder/SAF dans la fenêtre Logcat examinée.

Point d’arrêt à 21:16 : Whirlwind seul en slot natif `#0 (0/0)`, synchronisation
OFF/35 %, Binder/API 4 et SAF valides. Jeu bloqué par la commande Wii déconnectée.
Preset temporaire Bifrost actif, contrôle tiers rétabli, Default intact.
Couleurs synchronisées et restitution après OFF confirmées sur `3be0796`.

- [x] Correctif `159dbe0` : 157 tests locaux, Lint zéro erreur/17 avertissements,
  Debug, licence et CI réussis. Run signé `33986789250`, certificat officiel commun
  et sommes vérifiés ; seul SkyPortal mis à jour, APK réextrait identique.
- [x] `159dbe0` sur Thor, 21:25–21:27 : bouton Bifrost testé deux fois sur `0`,
  compagnon maintenu sur `4`, Whirlwind retrouvé dans le seul slot `#0 (0/0)`,
  32 fichiers et réglages conservés. Réception/restitution logicielle/reprise
  avec le bouton vérifiées ; OFF final, Logcat du parcours sans erreur recherchée.

Point d’arrêt actualisé : compagnon `159dbe0` installé, état précédent conservé,
commande Wii toujours à réactiver. La preuve physique sur `3be0796` reste attachée
à ce binaire ; les autres restitutions physiques et cas restants ne sont pas cochés.

### Reprise `159dbe0` — 5 septembre, 21:46–21:59

Cette section actualise le point d’arrêt précédent sans réattribuer les essais
anciens. APK installé inchangé, SHA-256 relu ; aucun build ni mise à jour d’APK ici.

- [x] Retour de Whirlwind visible en partie après intervention utilisateur.
- [x] J2 dans le compagnon : Lightning Rod chargé, remplacé par Sonic Boom,
  natifs `#0 (0/0)` / `#1 (1/0)`, retrait J2 puis Whirlwind seul visible en jeu.
- [ ] Coopération réelle à deux commandes : partie restée solo, message « trop de
  jouets » avec deux personnages. Ne pas confondre avec le test de slots réussi.
- [x] Backup annulé : montage conservé, aucun fichier ajouté. Backup confirmé :
  retrait, fermeture de fiche et copie unique, aucun rechargement automatique ;
  source démontée et copie de 1 024 octets avec SHA-256 identiques. Copie conservée.
- [x] Rescan via diagnostic : 32 fichiers, backup exclu ; slots natifs libres
  après les retraits du parcours. Warnado brièvement chargé pendant navigation,
  retiré normalement, sans apparition en jeu revendiquée.
- [x] Confirmation Backup ouverte puis mort Dolphin : fermeture automatique,
  slots vidés, aucune copie supplémentaire ni ancienne action disponible.
- [x] Mort Dolphin : CLEAR accepté et retour logiciel STATIC Bifrost ; service
  automatiquement reconnecté, état « Aucun jeu », sans faux prêt.
- [x] Relance réelle SSA : API 4, `SSPP52 / RUNNING`, USB confirmé, aucun ancien
  montage restauré ; flux Bifrost repris. Écran titre, pas nouvelle partie jouée.
- [x] Whirlwind chargé après relance ; compagnon forcé à l’arrêt puis relancé :
  Dolphin vivant, unique natif `#0 (0/0)`, aucun deuxième chargement demandé,
  mode solo, SAF, collection et option ON/35 % conservés. OFF final.
- [x] Watchdog et OFF confirmés logiciellement ; échantillon final de 47 DISPLAY,
  intervalles 504–521 ms, puis aucun DISPLAY après CLEAR. Pas de preuve physique.
- [x] Logcat du parcours : 8 464 lignes, aucun crash/ANR, erreur Binder/permission,
  retrait incertain ou backup échoué recherché ; avertissements non fatals conservés.
- [x] Restitution physique après arrêt volontaire Dolphin à 21:53:47, sur
  `159dbe0` : retour bleu gauche / rouge droite confirmé explicitement par
  l’utilisateur le 5 septembre (consigné à 22:13). Aucun autre chemin déduit.
- [ ] Cause du retour du message Wii déconnectée vers 21:50 identifiée ; pas de
  causalité établie avec Bifrost ou le backup.

État historique à 21:59 : SSA sur l’écran titre, Whirlwind seul en J1, SkyPortal
OFF/35 %, écrans `0`/`4` respectés, Bifrost temporaire actif et Default intact.
Les observations complémentaires suivantes n’autorisent aucune fusion.

### Complément `159dbe0` — 5 septembre, 22:43–23:11

- [x] Variation qualitative 0 % → 70 % → 35 % à 22:43–22:44 : anneaux éteints,
  rallumés, puis moins lumineux, confirmés par l’utilisateur (consigné à 22:58).
  Pas de calibration lumineuse ni de preuve d’absence de clignotement.
- [x] Veille à 22:59:11 : écrans `0`/`4` OFF, service Bifrost vivant ; dernier
  DISPLAY accepté à 22:59:10.950, CLEAR accepté à 22:59:11.254 en 5 ms, STATIC
  à 22:59:11.535. Retour physique bleu gauche / rouge droite pendant la veille
  confirmé par l’utilisateur à 23:11.
- [x] Logcat 22:59:00.052–22:59:16.419 : aucun crash/ANR, `SecurityException`,
  `DeadObjectException` ou erreur Binder recherché ; bruit vendor audio/Bluetooth/CPU.
- [x] Contrôle de reprise à 23:11:41 : Whirlwind visible en partie et seul natif
  `#0 (0/0)`, USB présent/attaché/protocole, Dolphin `0`, SkyPortal `4`, ON/35 %.
  La Thor était déjà `Awake` avant la commande `224` : pas de réveil contrôlé prouvé.
- [ ] Restitution physique après arrêt forcé du compagnon / watchdog : test
  préparé mais non rejoué après l’interruption ; campagne suspendue le 6 septembre.
- [ ] Déconnexion Wii récurrente résolue : le retour en partie ne prouve pas un correctif.

Les autres cases ouvertes restent en attente. Aucun nouveau build, changement
Dolphin, version ou clé n’est déduit de ces observations.

## Reprise V6 / PR #14 — 5 septembre 2026

Le [suivi courant](docs/PROJECT_STATUS.md) identifie commits, APK, certificat et
preuves de cette session. La section V5 ci-dessous reste historique.

### Premiers essais avec l’ancien Dolphin — historique conservé

Les cases non cochées de ce premier parcours décrivent ses limites à 09:37.
La revalidation du Dolphin correctif figure dans la section suivante.

- [x] ADB autorisé, écrans logiques `0` supérieur et `4` inférieur vérifiés.
- [x] APK préexistants conservés, certificat officiel commun vérifié.
- [x] Dolphin API 4 préexistant conservé pendant les essais initiaux du compagnon,
  sans remplacement ni suppression de données ; la paire corrective ultérieure
  dispose d’une validation distincte ci-dessous.
- [x] APK historique `final-compose` réinstallé par `adb install -r` ; défaut
  restant reproduit : panneau trop court, portail invisible.
- [x] Correction compagnon : hauteur réservée, défilement de secours, régions
  mesurées, clipping, arcs horizontaux et accessibilité Trap conditionnelle.
- [x] JVM : 115 tests réussis ; Lint : aucune erreur ; Debug : compilation réussie.
- [x] Nouvel APK Release signé exact vérifié et mis à jour sur la Thor (`d466536`,
  run `33952416415`) ; APK installé identique par SHA-256.
- [x] SSA : aucun badge/cristal Trap dans les captures ; description couverte par JVM.
- [x] Portail central visible, texte et bande RGB séparés à plusieurs phases animées.
- [x] RGB gauche/droite lisibles ; Équipes et Diagnostic accessibles.
- [ ] J1 : chargement, remplacement et retrait réels, sans faux succès ni doublon.
- [ ] J2 activé : nouveau chargement/retrait sur le dernier APK (montage préexistant
  réconcilié, retrait Warnado et retour solo réussis ; pas de partie coop validée).
- [x] Sélecteur Slot 3 et collection accessibles ; aucun montage supplémentaire tenté.
- [x] Recréation du compagnon et reconnexion manuelle avec Whirlwind : un seul slot natif.
- [ ] Retour accueil/veille/Dolphin sans incident : **échec, crash natif du menu Dolphin**.
- [ ] Logcat ciblé : aucun crash, ANR, erreur Binder/SAF ou alternance anormale actif/veille.
- [ ] Preuves actualisées dans la PR ; accord explicite de fusion obtenu séparément.

Deux `SIGTRAP` Dolphin ont été observés à 09:34 et 09:37 lors du retour à son
menu principal : `onPrepareOptionsMenu → isSystemMenuInstalled → IOS::HLE::Kernel`,
avec un IOS déjà présent. La PR était alors maintenue en brouillon. Le compagnon a refusé le
chargement après perte du handshake, sans faux succès. Voir les limites et les
résultats détaillés dans le suivi ; ne pas cocher le parcours global à partir
des seules réussites de disposition, de CI ou de reconnexion du compagnon.

### Reprise autorisée du correctif de menu Dolphin

Le candidat `11353ca7cabf28bc4dccbfbefa0593fb321def2f` inclut les gardes JNI du
menu Wii de `23af6d0` et rend les chemins de restauration/lancement neuf de
`EmulationFragment` mutuellement exclusifs. Les deux crashs ci-dessus
restent des observations de l’ancien Dolphin ; aucune case matérielle ne peut
être cochée pour la nouvelle paire avant installation et rejeu.

- [x] Autorisation utilisateur obtenue pour le correctif Dolphin ciblé.
- [x] Correctif isolé dans `android-menu-lifecycle.patch`, sans changement API 4
  ni remise en cause de l’activation/keepalive `A 00`.
- [x] Contrôles locaux réexécutés sur `11353ca` : 120 tests JVM réussis, Lint 0 erreur / 16
  avertissements, compilation Debug et contrôle de licence réussis.
- [x] Pile complète de patchs réversée puis réappliquée sur la base épinglée
  dans un arbre contrôlé. Reconstruction prévue depuis un checkout **neuf** :
  le script complet ne peut pas être relancé tel quel sur un arbre déjà API 4
  (échec reproduit et arbre contrôlé réparé, détails dans le suivi).
- [x] Run intermédiaire `33953904485` annulé volontairement avant candidat pour
  intégrer le second correctif, sans le présenter comme un échec de compilation.
- [x] Nouveau run de paire [33954214843](https://github.com/LeVraiPedro/SkyPortal-Thor/actions/runs/33954214843)
  terminé avec succès en 31 min 32 s, compilation native comprise ; source et
  kit disponibles, contenu corrigé vérifié et CRC des deux ZIP valides.
- [x] Nouveaux APK exacts téléchargés, six SHA-256 du manifeste et certificat officiel commun
  vérifiés, mode `PERSISTENT_RELEASE_KEY` confirmé.
- [x] Dolphin correctif seul installé par mise à jour à 10:36:58, sans effacement ;
  APK réextrait identique par SHA-256. Compagnon `d466536` conservé : le compagnon
  construit par le run de paire n’est pas installé ni revendiqué testé sur Thor.
- [x] Premier diagnostic : API 4, SSA `SSPP52`, `RUNNING`, trois preuves USB à
  `true`, 16 slots libres, 32 fichiers et droits SAF conservés.
- [x] À 10:42, « Dolphin en haut » depuis l’écran-titre SSA ouvre le menu
  principal ; Retour Android retrouve le même écran-titre et le même processus,
  sans nouveau démarrage. Aucun personnage n’est monté pendant cet essai.
- [x] Logcat 10:37–10:43 sans nouvelle erreur fatale, assertion, ANR,
  `SecurityException` ou `DeadObjectException` ; un seul lancement `Running`,
  celui de 10:37:28. Historique des sorties inchangé depuis la mise à jour.
- [x] Partie SSA ouverte par l’utilisateur ; premier chargement de figurine
  avec le nouveau Dolphin avant poursuite des scénarios.
- [x] Retour au menu Dolphin pendant la partie avec personnage monté puis après arrêt d’émulation sans
  nouveau `SIGTRAP`, crash ou ANR.
- [x] Sortie d’une session restaurée depuis l’état temporaire sans nouveau
  démarrage involontaire de l’émulation.
- [x] Chargement/remplacement/retrait J1 et chargement/retrait J2 rejoués ;
  disposition, reconnexion, accès collection et retour solo préservés.
- [x] Accueil/veille/reprise rejoués et fenêtre Logcat du nouveau candidat analysée
  (commande Wii interrompue après veille, distincte de Binder et du rendu).
- [x] Mort du processus Dolphin avec figurine montée, retour automatique du service,
  relance SSA et 16 slots libres sans remontage ni mapping fantôme.
- [x] Preuves de la revalidation 12:16–12:33 consignées dans le suivi et la PR #14.
- [ ] Accord explicite de fusion obtenu séparément ; Bifrost reste soumis à une
  autorisation distincte.

Deux avertissements Wii SSL (`clientca.pem`, `clientcakey.pem` manquants) ont
été acquittés individuellement ; aucune option d’ignorance globale activée.
Une `DeadObjectException` à 10:36:59 correspond au remplacement du processus
par l’installation. Le Logcat des scénarios suivants doit être contrôlé séparément.
Le succès initial depuis l’écran-titre n’a pas été utilisé comme substitut aux
essais en partie : ceux-ci ont été exécutés ensuite de 12:16 à 12:33. Lightning Rod,
Sonic Boom et Bash en J1, puis Warnado via J2, ont été présentés réellement par SSA.
J2 désigne ici le slot logique du compagnon, pas une validation coopérative.

La restauration a été prouvée par un processus neuf et l’OSD « Loaded State from
temp.sav » après `am kill` en arrière-plan et retour par Récents. La sortie n’a
pas relancé de session pendant plus de 30 secondes. Logcat 12:16–12:33 : aucun
nouveau crash, ANR, assertion, `SecurityException`, `DeadObjectException` ou erreur
du pont SkyPortal. Aucun nouvel APK installé dans ce parcours.

Limites consignées dans le suivi : commande Wii après veille ; nom périmé dans
une fiche d’actions restée ouverte pendant la mort Dolphin, malgré un slot de fond
correctement vidé (Backup revalide le montage). Ni les objets, ni les autres jeux,
ni la coopération à deux commandes ne sont validés par ces essais. La Thor est
laissée en solo, slots vides, bibliothèque Dolphin sur 0 et compagnon sur 4.

## Historique V5 Smart Portal

Cette checklist distingue les observations faites sur la console des couvertures automatisées. Une case non cochée ne doit pas être interprétée comme un échec : elle signifie que le scénario matériel reste à exécuter ou à documenter.

## Environnement relevé

| Élément | Valeur validée |
|---|---|
| Matériel | AYN Thor Max, Android 13 (API 33) |
| Écran supérieur | affichage logique `0`, 1920 × 1080 |
| Écran inférieur | affichage logique `4`, 1240 × 1080 |
| SkyPortal | `com.skyportalthor.app`, 0.5.0, code 7 |
| Dolphin testé | `org.dolphinemu.dolphinemu`, Dolphin SkyPortal API 3 Release |
| Jeu réellement lancé | Skylanders: Spyro's Adventure |
| Game ID réellement détecté | `SSPP52` |
| Collection utilisateur | 32 dumps détectés, non supprimés et non réinitialisés |
| Signature | certificat de release persistant identique pour les deux APK |

Le numéro de série ADB, les URI SAF et les chemins propres au PC ne sont volontairement pas publiés.

> **Portée des preuves :** les résultats historiques sont complétés par la validation de la paire Release officielle le 16 août 2026. Le chemin USB normal, les remplacements en jeu et le conflit volontaire avec Disney Infinity ont été rejoués de bout en bout avec les APK exacts destinés à la publication.

## Résultats matériels établis

- [x] SkyPortal est routé sur l'affichage logique `4` et Dolphin reste utilisable sur l'affichage `0`.
- [x] La connexion Binder s'établit avec Dolphin Debug et le diagnostic annonce l'API 3.
- [x] Spyro's Adventure est détecté avec l'ID `SSPP52` et l'état `RUNNING`.
- [x] En partant du portail désactivé dans Dolphin, SkyPortal l'active automatiquement via l'API 3.
- [x] Pendant la campagne initiale, l'en-tête a atteint `Connecté | Spyro’s Adventure | Portail prêt` en quelques secondes et des chargements réels ont ensuite réussi. Ce résultat historique ne constitue pas la validation du nouveau signal de handshake USB.
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

## Correctifs USB et remplacement — revalidation ciblée

Le correctif sépare désormais quatre informations : réglage du portail, présence dans le scanner Dolphin, attachement USB et première commande Skylanders reçue. Il expose aussi `DISNEY_INFINITY_BASE` comme périphérique concurrent. Le snapshot natif v2 sépare en plus le fichier réellement monté de l'état protocolaire transitoire.

- [x] Avec portail Skylanders et base Disney Infinity activés, l'en-tête affiche le conflit et ne passe jamais à `Portail prêt`.
- [x] Dans ce conflit, l'activation automatique et le chargement d'un `.sky` sont bloqués avant l'appel Binder, avec un message demandant de désactiver Disney Infinity puis de redémarrer l'émulation.
- [x] Après désactivation de Disney Infinity et redémarrage complet de l'émulation, le jeu effectue le handshake USB et l'en-tête atteint `Portail prêt`.
- [x] Avec le seul portail Skylanders actif, chargement et retrait réels fonctionnent toujours sans doublon ni faux succès.
- [x] En jeu, J1 remplace Whirlwind par Sonic Boom puis Lightning Rod sans `Dropping stale logical portal mapping`, faux échec ou slot dupliqué ; le HUD et le personnage Dolphin changent réellement.
- [x] Le diagnostic affiche le schéma natif v2 fiable et 16 slots libres après le retrait final.
- [x] Trois tests natifs ARM64 ciblent le remplacement et l'allocation pendant `mounted=true / status=REMOVED`.
- [x] L'arrêt brutal de Dolphin efface le jeu, les slots et la preuve USB ; après relance, `SSPP52` et les preuves USB sont redétectés sans état prêt fantôme.
- [ ] Un changement du réglage USB pendant une émulation déjà lancée affiche `Redémarrage requis` au lieu de promettre une activation à chaud suffisante.

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

- [x] 75 tests unitaires Debug, dont le format de migration des préférences et les entrées vides héritées.
- [x] Détection des six jeux et de leurs IDs régionaux connus.
- [x] Compatibilité par génération et par type.
- [x] Distinction personnage/objet et sous-types natifs.
- [x] Dump de taille incorrecte, en-tête/checksum invalide et identité inconnue.
- [x] Contrat AIDL et ordre des six méthodes historiques.
- [x] Parsing API 1, API 2, API 3 et des 16 slots natifs.
- [x] Parsing des preuves USB tri-state, ancien JSON API 3, décisions sans faux `READY`, conflit Disney Infinity et chemin dégradé API 1/2.
- [x] Nettoyage logique à la déconnexion et code portail plein.
- [x] Favoris, récents, équipes manquantes et exclusions de scan.
- [x] Android Lint : aucune erreur bloquante.
- [x] Compilation de l'APK Debug SkyPortal.
- [x] Campagne initiale : compilation et installation du Dolphin Debug patché.
- [x] Correctifs USB/montage : paire installée sans effacement, parcours ciblé et reconnexion rejoués sur la Thor.

## Commandes de diagnostic

Sous PowerShell :

```powershell
adb shell dumpsys package org.dolphinemu.dolphinemu.debug | Select-String "SkyPortalService|PORTAL_CONTROL"
adb logcat -c
adb logcat | Select-String "SkyPortalBridge|SkyPortalService|Skylander|AndroidRuntime|SecurityException"
```

En cas d'échec, relever le code affiché par SkyPortal, le package Dolphin ciblé, la version API, le Game ID, l'état du portail et les slots natifs. Ne pas publier l'URI SAF complète du fichier.

Voir aussi [docs/VALIDATION_V5.md](docs/VALIDATION_V5.md) et [docs/COMPATIBILITY_MATRIX.md](docs/COMPATIBILITY_MATRIX.md).
