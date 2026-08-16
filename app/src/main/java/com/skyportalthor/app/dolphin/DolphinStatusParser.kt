package com.skyportalthor.app.dolphin

import com.skyportalthor.app.data.EmulationState
import com.skyportalthor.app.data.DolphinServiceState
import com.skyportalthor.app.portal.NativePortalSlotState
import com.skyportalthor.app.portal.PortalProtocol
import org.json.JSONObject

internal data class ReportedLogicalSlot(
    val logicalSlot: Int,
    val actualSlot: Int,
    val label: String?,
    val uriWasReported: Boolean,
    val sourceUri: String?
)

internal data class DolphinStatusSnapshot(
    val apiVersion: Int,
    val serviceState: DolphinServiceState,
    val logicalSlots: List<ReportedLogicalSlot>,
    val nativeSlots: List<NativePortalSlotState>,
    val emulationState: EmulationState,
    val gameId: String?,
    val gameTitle: String?,
    val portalEnabled: Boolean?,
    val portalActivated: Boolean?,
    val portalProtocolActivated: Boolean?,
    val portalUsbPresent: Boolean?,
    val portalUsbAttached: Boolean?,
    val portalUsbHandshakeSeen: Boolean?,
    val conflictingUsbDevices: List<String>,
    val portalUsbStatusValid: Boolean,
    val canSetPortalEnabled: Boolean,
    val issues: List<String>
)

internal object DolphinStatusParser {
    fun parse(json: String, fallbackApiVersion: Int = 1): DolphinStatusSnapshot {
        val root = JSONObject(json)
        val apiVersion = root.optInt("apiVersion", fallbackApiVersion).coerceAtLeast(1)
        val issues = mutableListOf<String>()
        val serviceState = if (!root.has("serviceState")) {
            DolphinServiceState.READY
        } else {
            runCatching {
                DolphinServiceState.valueOf(root.optString("serviceState"))
            }.getOrElse {
                issues += "état de service Dolphin inconnu"
                DolphinServiceState.UNKNOWN
            }
        }

        val logicalSlots = buildList {
            val seen = mutableSetOf<Int>()
            val slots = root.optJSONArray("slots")
            for (index in 0 until (slots?.length() ?: 0)) {
                val item = slots?.optJSONObject(index) ?: continue
                val logical = item.optInt("logicalSlot", -1)
                if (logical !in 0 until LOGICAL_SLOT_COUNT) {
                    issues += "slot logique $logical hors plage"
                    continue
                }
                if (!seen.add(logical)) {
                    issues += "slot logique $logical dupliqué"
                    continue
                }
                val actual = item.optInt("actualSlot", -1)
                if (actual != -1 && !PortalProtocol.isValidActualSlot(actual)) {
                    issues += "slot natif $actual invalide"
                }
                val uriWasReported = item.has("uri")
                add(
                    ReportedLogicalSlot(
                        logicalSlot = logical,
                        actualSlot = actual,
                        label = item.optString("label").takeIf(String::isNotBlank),
                        uriWasReported = uriWasReported,
                        sourceUri = item.optString("uri").takeIf { uriWasReported && it.isNotBlank() }
                    )
                )
            }
        }

        val nativeSlots = buildList {
            val seen = mutableSetOf<Int>()
            val slots = root.optJSONArray("nativeSlots")
            for (index in 0 until (slots?.length() ?: 0)) {
                val item = slots?.optJSONObject(index) ?: continue
                val slot = item.optInt("slot", -1)
                if (!PortalProtocol.isValidActualSlot(slot)) {
                    issues += "snapshot natif $slot hors plage"
                    continue
                }
                if (!seen.add(slot)) {
                    issues += "snapshot natif $slot dupliqué"
                    continue
                }
                add(
                    NativePortalSlotState(
                        slot = slot,
                        occupied = item.optBoolean("occupied", false),
                        status = item.optInt("status", 0),
                        figureId = item.optInt("id", -1).takeIf { it >= 0 },
                        variantId = item.optInt("variant", -1).takeIf { it >= 0 }
                    )
                )
            }
            if (apiVersion >= 3 && slots != null && size != PortalProtocol.MAX_PORTAL_SLOTS) {
                issues += "snapshot natif incomplet : $size/${PortalProtocol.MAX_PORTAL_SLOTS} slots"
            }
        }

        val conflictingDevicesJson = root.optJSONArray("conflictingUsbDevices")
        var conflictingDevicesValid = conflictingDevicesJson != null
        val conflictingUsbDevices = buildList {
            val devices = conflictingDevicesJson
            if (root.has("conflictingUsbDevices") && !root.isNull("conflictingUsbDevices") && devices == null) {
                issues += "liste des périphériques USB concurrents invalide"
            }
            for (index in 0 until (devices?.length() ?: 0)) {
                val rawCode = devices?.opt(index)
                val code = (rawCode as? String).orEmpty().trim().uppercase()
                if (code.isBlank()) {
                    conflictingDevicesValid = false
                    issues += "périphérique USB concurrent sans identifiant"
                } else if (code !in this) {
                    add(code)
                }
            }
        }
        val portalUsbPresent = root.optBooleanOrNull("portalUsbPresent")
        val portalUsbAttached = root.optBooleanOrNull("portalUsbAttached")
        val portalUsbHandshakeSeen = root.optBooleanOrNull("portalUsbHandshakeSeen")
        val usbBooleanNames = listOf("portalUsbPresent", "portalUsbAttached", "portalUsbHandshakeSeen")
        val usbBooleansStrict = usbBooleanNames.all { root.opt(it) is Boolean }
        val usbEvidence = listOf(portalUsbPresent, portalUsbAttached, portalUsbHandshakeSeen)
        if (usbEvidence.any { it != null } && usbEvidence.any { it == null }) {
            issues += "état USB du portail incomplet"
        }
        if (portalUsbHandshakeSeen == true && portalUsbAttached != true) {
            issues += "handshake USB signalé sans portail attaché"
        }
        if (portalUsbAttached == true && portalUsbPresent != true) {
            issues += "portail USB attaché mais absent du scanner"
        }
        val portalUsbStatusValid = usbBooleansStrict && conflictingDevicesValid
        if (usbEvidence.all { it != null } && !portalUsbStatusValid) {
            issues += "schéma d’état USB du portail invalide"
        }

        return DolphinStatusSnapshot(
            apiVersion = apiVersion,
            serviceState = serviceState,
            logicalSlots = logicalSlots,
            nativeSlots = nativeSlots,
            emulationState = runCatching {
                EmulationState.valueOf(root.optString("emulationState", "NONE"))
            }.getOrDefault(EmulationState.UNKNOWN),
            gameId = root.optString("gameId").takeIf(String::isNotBlank),
            gameTitle = root.optString("gameTitle").takeIf(String::isNotBlank),
            portalEnabled = root.optBooleanOrNull("portalEnabled"),
            portalActivated = root.optBooleanOrNull("portalActivated"),
            portalProtocolActivated = root.optBooleanOrNull("portalProtocolActivated"),
            portalUsbPresent = portalUsbPresent,
            portalUsbAttached = portalUsbAttached,
            portalUsbHandshakeSeen = portalUsbHandshakeSeen,
            conflictingUsbDevices = conflictingUsbDevices,
            portalUsbStatusValid = portalUsbStatusValid,
            canSetPortalEnabled = root.optBoolean("canSetPortalEnabled", false) && apiVersion >= 3,
            issues = issues
        )
    }

    private const val LOGICAL_SLOT_COUNT = 8
}

private fun JSONObject.optBooleanOrNull(name: String): Boolean? =
    if (has(name) && !isNull(name)) optBoolean(name) else null
