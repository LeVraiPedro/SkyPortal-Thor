package com.skyportalthor.app.diagnostics

import android.content.Context
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import com.skyportalthor.app.data.Skylander
import com.skyportalthor.app.data.DolphinServiceState
import com.skyportalthor.app.display.DisplayRouter
import com.skyportalthor.app.dolphin.DolphinTargets
import com.skyportalthor.app.data.SmartPortalReadiness
import com.skyportalthor.app.data.EmulationState
import com.skyportalthor.app.portal.PortalReadinessPolicy
import com.skyportalthor.app.portal.PortalState
import java.security.MessageDigest

enum class DiagnosticLevel { SUCCESS, WARNING, ERROR, INFO }

data class DiagnosticItem(
    val title: String,
    val level: DiagnosticLevel,
    val detail: String,
    val recovery: String? = null
)

class DiagnosticAssistant(private val context: Context) {
    fun run(
        rootUri: Uri?,
        figures: List<Skylander>,
        portalState: PortalState,
        preferredDolphinPackage: String?
    ): List<DiagnosticItem> = buildList {
        add(checkLowerDisplay())
        add(checkFolderPermission(rootUri))
        add(checkCollection(rootUri, figures))

        val targetPackage = portalState.connectedPackage
            ?: preferredDolphinPackage?.takeIf(::isInstalled)
            ?: DolphinTargets.packages.firstOrNull(::isInstalled)
        add(checkDolphinInstallation(targetPackage))
        if (targetPackage != null) add(checkSignature(targetPackage))
        add(checkBinder(portalState))
        add(checkServiceState(portalState))
        add(checkApi(portalState))
        add(checkGame(portalState))
        add(checkPortal(portalState))
        add(checkNativeSlots(portalState))
    }

    private fun checkLowerDisplay(): DiagnosticItem {
        val router = DisplayRouter(context)
        val display = router.lowerDisplay()
        return if (router.supportsSecondaryActivities() && display != null) {
            DiagnosticItem(
                "Écran inférieur",
                DiagnosticLevel.SUCCESS,
                "${display.name} détecté comme Display #${display.displayId}."
            )
        } else {
            DiagnosticItem(
                "Écran inférieur",
                DiagnosticLevel.WARNING,
                "Aucun affichage Android secondaire actif n'est détecté.",
                "Allume les deux écrans de la Thor puis relance SkyPortal."
            )
        }
    }

    private fun checkFolderPermission(rootUri: Uri?): DiagnosticItem {
        if (rootUri == null) {
            return DiagnosticItem(
                "Accès au dossier",
                DiagnosticLevel.ERROR,
                "Aucun dossier Skylanders n'est sélectionné.",
                "Utilise Dossier puis autorise le dossier racine contenant les fichiers .sky."
            )
        }
        val permission = context.contentResolver.persistedUriPermissions.firstOrNull { it.uri == rootUri }
        return when {
            permission == null -> DiagnosticItem(
                "Accès au dossier",
                DiagnosticLevel.ERROR,
                "L'autorisation Android persistante n'est plus présente.",
                "Sélectionne de nouveau le dossier avec le bouton Dossier."
            )
            !permission.isReadPermission || !permission.isWritePermission -> DiagnosticItem(
                "Accès au dossier",
                DiagnosticLevel.ERROR,
                "Le dossier n'est pas autorisé en lecture et en écriture.",
                "Sélectionne de nouveau le dossier et accepte l'accès demandé."
            )
            else -> DiagnosticItem(
                "Accès au dossier",
                DiagnosticLevel.SUCCESS,
                "Autorisation SAF persistante disponible en lecture et en écriture."
            )
        }
    }

    private fun checkCollection(rootUri: Uri?, figures: List<Skylander>): DiagnosticItem = when {
        rootUri == null -> DiagnosticItem(
            "Collection",
            DiagnosticLevel.ERROR,
            "La collection ne peut pas être analysée sans dossier."
        )
        figures.isEmpty() -> DiagnosticItem(
            "Collection",
            DiagnosticLevel.WARNING,
            "Aucun fichier .sky n'a été détecté.",
            "Vérifie le dossier choisi puis utilise Scanner."
        )
        else -> DiagnosticItem(
            "Collection",
            DiagnosticLevel.SUCCESS,
            "${figures.size} fichier(s) .sky détecté(s)."
        )
    }

    private fun checkDolphinInstallation(packageName: String?): DiagnosticItem = if (packageName == null) {
        DiagnosticItem(
            "Dolphin SkyPortal",
            DiagnosticLevel.ERROR,
            "Aucune version Dolphin compatible n'est installée.",
            "Installe la build Dolphin contenant SkyPortalService."
        )
    } else {
        DiagnosticItem(
            "Dolphin SkyPortal",
            DiagnosticLevel.SUCCESS,
            "${DolphinTargets.label(packageName)} est installé ($packageName)."
        )
    }

    private fun checkSignature(packageName: String): DiagnosticItem {
        val own = signingDigests(context.packageName)
        val dolphin = signingDigests(packageName)
        return when {
            own.isEmpty() || dolphin.isEmpty() -> DiagnosticItem(
                "Signature Binder",
                DiagnosticLevel.WARNING,
                "Impossible de lire les certificats de signature.",
                "Vérifie que les deux APK proviennent de la même release."
            )
            own.intersect(dolphin).isNotEmpty() -> DiagnosticItem(
                "Signature Binder",
                DiagnosticLevel.SUCCESS,
                "SkyPortal et ${DolphinTargets.label(packageName)} utilisent une signature compatible."
            )
            else -> DiagnosticItem(
                "Signature Binder",
                DiagnosticLevel.ERROR,
                "Les signatures de SkyPortal et Dolphin sont différentes.",
                "Reconstruis ou réinstalle les deux APK avec la même clé de signature."
            )
        }
    }

    private fun checkBinder(state: PortalState): DiagnosticItem = if (state.connected) {
        DiagnosticItem(
            "Connexion Binder",
            DiagnosticLevel.SUCCESS,
            state.message
        )
    } else {
        DiagnosticItem(
            "Connexion Binder",
            DiagnosticLevel.ERROR,
            state.message,
            "Démarre Dolphin puis utilise Reconnecter."
        )
    }

    private fun checkServiceState(state: PortalState): DiagnosticItem = when (state.serviceState) {
        DolphinServiceState.READY -> DiagnosticItem(
            "Service Dolphin", DiagnosticLevel.SUCCESS,
            "Le service SkyPortal Dolphin est initialisé et répond."
        )
        DolphinServiceState.INITIALIZING -> DiagnosticItem(
            "Service Dolphin", DiagnosticLevel.INFO,
            "Dolphin termine son initialisation native.",
            "Patiente quelques secondes : SkyPortal actualise automatiquement cet état."
        )
        DolphinServiceState.UNKNOWN -> DiagnosticItem(
            "Service Dolphin", DiagnosticLevel.WARNING,
            if (state.connected) "Cette version de Dolphin n’expose pas explicitement l’état du service."
            else "Le service Dolphin n’est pas connecté."
        )
    }

    private fun checkApi(state: PortalState): DiagnosticItem = when (val api = state.apiVersion) {
        null -> DiagnosticItem(
            "API Dolphin",
            DiagnosticLevel.WARNING,
            "Version de l'API inconnue tant que Dolphin n'est pas connecté."
        )
        1 -> DiagnosticItem(
            "API Dolphin",
            DiagnosticLevel.WARNING,
            "API 1 compatible, avec diagnostic et restauration de slots limités.",
            "Le patch Dolphin API 3 du dépôt est recommandé."
        )
        2 -> DiagnosticItem(
            "API Dolphin",
            DiagnosticLevel.WARNING,
            "API 2 compatible pour les slots, sans détection du jeu ni activation Smart Portal.",
            "Installe le patch Dolphin API 3 pour toutes les fonctions V5."
        )
        else -> DiagnosticItem(
            "API Dolphin",
            DiagnosticLevel.SUCCESS,
            "API $api active."
        )
    }

    private fun checkGame(state: PortalState): DiagnosticItem = when {
        state.gameId == null -> DiagnosticItem(
            "Jeu Dolphin", DiagnosticLevel.INFO,
            "Aucun jeu n’est actuellement détecté (${state.emulationState})."
        )
        state.skylandersGame != null -> DiagnosticItem(
            "Jeu Dolphin", DiagnosticLevel.SUCCESS,
            "${state.skylandersGame.displayName} — ID ${state.gameId} — ${state.emulationState}. Smart Portal compatible."
        )
        else -> DiagnosticItem(
            "Jeu Dolphin", DiagnosticLevel.WARNING,
            "${state.gameTitle ?: "Jeu sans titre"} — ID ${state.gameId}. Ce jeu n’est pas reconnu comme un jeu Skylanders.",
            "Le filtre Smart Portal reste inactif pour éviter de masquer des fichiers à tort."
        )
    }

    private fun checkPortal(state: PortalState): DiagnosticItem = when {
        state.apiVersion != null && state.apiVersion < 3 -> DiagnosticItem(
            "Portal of Power", DiagnosticLevel.WARNING,
            "API ${state.apiVersion} : le chargement reste compatible, mais la détection USB réelle du portail ne peut pas être vérifiée.",
            "Utilise la paire SkyPortal/Dolphin API 3 à jour pour obtenir un état fiable."
        )
        state.readiness == SmartPortalReadiness.PORTAL_CONFLICT -> {
            val conflict = PortalReadinessPolicy.conflictSummary(state.conflictingUsbDevices)
                .ifBlank { "une autre base USB émulée" }
            DiagnosticItem(
                "Portal of Power", DiagnosticLevel.ERROR,
                "Le portail est en conflit avec $conflict. Le jeu peut alors annoncer qu’aucun portail n’est détecté.",
                "Désactive la base concurrente, arrête complètement l’émulation puis relance le jeu."
            )
        }
        state.portalEnabled == false -> DiagnosticItem(
            "Portal of Power", DiagnosticLevel.ERROR,
            "Le portail émulé est désactivé. Activation par API : ${if (state.canSetPortalEnabled) "disponible" else "indisponible"}.",
            if (state.canSetPortalEnabled) "Utilise Activer le portail dans l’en-tête." else "Active-le dans Dolphin ou installe l’API 3."
        )
        state.readiness == SmartPortalReadiness.PORTAL_RESTART_REQUIRED -> DiagnosticItem(
            "Portal of Power", DiagnosticLevel.ERROR,
            "Le portail est configuré, mais le jeu ne l’a ni attaché ni interrogé sur USB.",
            "Arrête complètement l’émulation, vérifie que seul le portail Skylanders est activé, puis relance le jeu."
        )
        state.readiness == SmartPortalReadiness.PORTAL_UNVERIFIED -> when {
            !state.portalUsbStatusValid -> DiagnosticItem(
                "Portal of Power", DiagnosticLevel.WARNING,
                "Cette build Dolphin API 3 n’expose pas les preuves USB nécessaires. « Portail activé » ne signifie pas que le jeu le détecte.",
                "Mets à jour ensemble SkyPortal et Dolphin avant de charger une figurine."
            )
            state.serviceState == DolphinServiceState.UNKNOWN -> DiagnosticItem(
                "Portal of Power", DiagnosticLevel.WARNING,
                "L’état du service Dolphin est inconnu : le portail ne peut pas être déclaré prêt.",
                "Attends l’actualisation ou reconnecte Dolphin."
            )
            state.emulationState == EmulationState.UNKNOWN -> DiagnosticItem(
                "Portal of Power", DiagnosticLevel.WARNING,
                "L’état de l’émulation est inconnu : le portail ne peut pas être déclaré prêt.",
                "Attends l’actualisation ou redémarre l’émulation."
            )
            else -> DiagnosticItem(
                "Portal of Power", DiagnosticLevel.WARNING,
                "Les informations USB du portail sont incohérentes et ne permettent pas de confirmer sa détection.",
                "Actualise Dolphin avant de charger une figurine."
            )
        }
        state.readiness == SmartPortalReadiness.PORTAL_INITIALIZING -> DiagnosticItem(
            "Portal of Power", DiagnosticLevel.INFO,
            "Le périphérique USB est attaché, mais aucune commande Skylanders du jeu n’a encore été observée.",
            "Patiente quelques secondes. Si cet état persiste, redémarre complètement l’émulation."
        )
        state.readiness == SmartPortalReadiness.READY && state.portalUsbHandshakeSeen == true -> DiagnosticItem(
            "Portal of Power", DiagnosticLevel.SUCCESS,
            "Portail confirmé par une commande USB Skylanders du jeu (présent=${state.portalUsbPresent}, attaché=${state.portalUsbAttached}, protocole=${state.portalProtocolActivated})."
        )
        state.portalEnabled == true -> DiagnosticItem(
            "Portal of Power", DiagnosticLevel.WARNING,
            "Le portail est configuré dans Dolphin, mais sa détection réelle par le jeu n’est pas confirmée.",
            "Attends la détection USB ou consulte les réglages des périphériques émulés."
        )
        else -> DiagnosticItem(
            "Portal of Power", DiagnosticLevel.WARNING,
            "État du portail non exposé par cette version de Dolphin.",
            "Utilise la paire SkyPortal/Dolphin à jour."
        )
    }

    private fun checkNativeSlots(state: PortalState): DiagnosticItem {
        if (state.apiVersion == null || state.apiVersion < 3) {
            return DiagnosticItem("Slots natifs", DiagnosticLevel.INFO, "Instantané natif indisponible avec l’API ${state.apiVersion ?: "inconnue"}.")
        }
        val occupied = state.nativeSlots.filter { it.occupied }
        return DiagnosticItem(
            "Slots natifs", DiagnosticLevel.SUCCESS,
            if (occupied.isEmpty()) "Les 16 slots natifs sont libres."
            else occupied.joinToString(prefix = "Occupés : ") { "#${it.slot} (${it.figureId}/${it.variantId})" }
        )
    }

    private fun isInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getPackageInfo(packageName, 0)
    }.isSuccess

    private fun signingDigests(packageName: String): Set<String> = runCatching {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            modernSignatures(packageName)
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures.orEmpty()
        }
        signatures.mapTo(mutableSetOf()) { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .joinToString("") { byte -> "%02x".format(byte) }
        }
    }.getOrDefault(emptySet())

    @TargetApi(Build.VERSION_CODES.P)
    private fun modernSignatures(packageName: String): Array<Signature> {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        }
        val signingInfo = info.signingInfo ?: return emptyArray()
        return if (signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners
        } else {
            signingInfo.signingCertificateHistory
        }
    }
}
