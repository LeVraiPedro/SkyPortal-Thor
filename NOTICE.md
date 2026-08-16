# Avis de licence et d’attribution

## SkyPortal Thor

Copyright © 2026 LeVraiPedro and SkyPortal Thor contributors

Sauf indication contraire dans un fichier, le code original de SkyPortal Thor est distribué sous la licence `GPL-2.0-or-later`. Le texte complet de la GNU General Public License version 2 figure dans [`LICENSE`](LICENSE) et dans [`LICENSES/GPL-2.0-or-later.txt`](LICENSES/GPL-2.0-or-later.txt).

Le texte de licence a été repris à l’identique depuis le fichier officiel `LICENSES/GPL-2.0-or-later.txt` de Dolphin au commit `54070da5851e12f2d1a4389daa528e4fb81327ce` ; seules les fins de ligne propres à la plateforme peuvent différer.

Les avis de copyright et identifiants SPDX présents dans chaque fichier doivent être conservés. Lorsqu’un fichier tiers indique une autre licence, cet avis propre au fichier prévaut pour ce composant.

## Dolphin Emulator

Le dossier [`dolphin-patch/`](dolphin-patch/) contient des fichiers ajoutés, des extraits et des modifications destinés à une révision précise de Dolphin Emulator. Le code original de Dolphin reste la propriété de ses auteurs et contributeurs. Dolphin est principalement distribué sous `GPL-2.0-or-later`, avec des composants tiers identifiés par leurs propres avis SPDX.

Les patchs SkyPortal conservent les avis et identifiants SPDX des fichiers Dolphin qu’ils modifient. Une mention SkyPortal n’accorde aucun droit de propriété sur le code Dolphin d’origine. SkyPortal Thor n’est pas une version officielle de Dolphin Emulator et n’est ni affilié à, ni approuvé par, l’équipe Dolphin.

Le wrapper Gradle suivi dans ce dépôt (`gradlew`, `gradlew.bat` et `gradle/wrapper/`) est un composant tiers distribué par le projet Gradle sous Apache-2.0. Ses avis amont sont conservés et ne sont pas remplacés par la licence du code SkyPortal.

La construction Android résout également des bibliothèques tierces, notamment AndroidX, Jetpack Compose, Kotlin et kotlinx.coroutines. Les tests utilisent notamment JUnit et `org.json`. Ces composants conservent leurs licences et avis propres ; ils ne deviennent pas la propriété des contributeurs SkyPortal du seul fait de leur utilisation. Les fichiers de dépendances ne sont pas recopiés dans le dépôt en dehors du wrapper Gradle.

## Marques et contenus tiers

Skylanders ainsi que les noms, personnages, logos, images et œuvres associés appartiennent à leurs ayants droit respectifs. Dolphin Emulator est un projet tiers. SkyPortal Thor est un projet communautaire non officiel ; il n’est affilié à, approuvé par ou sponsorisé par Activision, Toys for Bob, Microsoft, Nintendo, Sony ou l’équipe Dolphin.

Ce dépôt ne distribue pas de ROM, ISO, WBFS, jeu, clé de chiffrement, collection de dumps `.sky` provenant de tiers ni autre contenu officiel protégé. Les éventuelles fixtures de test doivent être créées localement par les outils documentés, rester séparées des collections utilisateur et ne pas être publiées sans vérification de leur provenance.

La licence GPL applicable au code n’accorde aucun droit sur les marques, personnages, illustrations, captures ou autres œuvres de tiers.

## Redistribution d’un Dolphin modifié

Toute redistribution publique d’un binaire Dolphin modifié doit respecter les licences et avis applicables, notamment la mise à disposition du code source correspondant. Les exigences techniques et les artefacts attendus sont détaillés dans [`DOLPHIN_INTEGRATION.md`](DOLPHIN_INTEGRATION.md) et [`dolphin-patch/APPLY_PATCH.md`](dolphin-patch/APPLY_PATCH.md).
