// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal.led

import org.json.JSONException
import org.json.JSONObject

enum class PortalLedParseErrorCode {
    MALFORMED_JSON,
    MISSING_FIELD,
    INVALID_FIELD,
    UNSUPPORTED_SCHEMA
}

sealed interface PortalLedParseResult {
    data class Success(
        val state: PortalLedState,
        val warnings: List<String> = emptyList()
    ) : PortalLedParseResult

    data class Failure(
        val code: PortalLedParseErrorCode,
        val message: String,
        val field: String? = null
    ) : PortalLedParseResult
}

object PortalLedStateParser {
    fun parse(json: String): PortalLedParseResult {
        if (json.isBlank()) {
            return PortalLedParseResult.Failure(
                PortalLedParseErrorCode.MALFORMED_JSON,
                "Le payload LED Dolphin est vide."
            )
        }

        val root = try {
            JSONObject(json)
        } catch (_: JSONException) {
            return PortalLedParseResult.Failure(
                PortalLedParseErrorCode.MALFORMED_JSON,
                "Le payload LED Dolphin n’est pas un objet JSON valide."
            )
        }

        return try {
            parseRoot(root)
        } catch (failure: PayloadFailure) {
            PortalLedParseResult.Failure(
                code = failure.code,
                message = failure.message ?: "Payload LED Dolphin invalide.",
                field = failure.field
            )
        }
    }

    private fun parseRoot(root: JSONObject): PortalLedParseResult.Success {
        val schemaVersion = root.requireInt("schemaVersion")
        if (schemaVersion != PortalLedState.SCHEMA_VERSION_1) {
            throw PayloadFailure(
                PortalLedParseErrorCode.UNSUPPORTED_SCHEMA,
                "Version de schéma LED non prise en charge : $schemaVersion.",
                "schemaVersion"
            )
        }

        val active = root.requireBoolean("active")
        val sequence = root.requireLong("sequence")
        if (sequence < 0L) {
            throw PayloadFailure(
                PortalLedParseErrorCode.INVALID_FIELD,
                "La séquence LED ne peut pas être négative.",
                "sequence"
            )
        }

        val warnings = mutableListOf<String>()
        val left = root.readColor("left", required = active) ?: PortalRgb.Black
        val right = root.readColor("right", required = false) ?: left.also {
            if (active) warnings += "Couleur droite absente : la couleur gauche est utilisée pour les deux côtés."
        }
        val trap = root.readColor("trap", required = false)

        return PortalLedParseResult.Success(
            state = PortalLedState(
                schemaVersion = schemaVersion,
                active = active,
                sequence = sequence,
                left = left,
                right = right,
                trap = trap
            ),
            warnings = warnings
        )
    }

    private fun JSONObject.requireBoolean(name: String): Boolean {
        val raw = requiredValue(name)
        return raw as? Boolean ?: throw PayloadFailure(
            PortalLedParseErrorCode.INVALID_FIELD,
            "Le champ $name doit être un booléen.",
            name
        )
    }

    private fun JSONObject.requireInt(name: String): Int {
        val value = requireLong(name)
        if (value !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
            throw PayloadFailure(
                PortalLedParseErrorCode.INVALID_FIELD,
                "Le champ $name est hors plage.",
                name
            )
        }
        return value.toInt()
    }

    private fun JSONObject.requireLong(name: String): Long {
        val raw = requiredValue(name)
        return raw.toStrictLong() ?: throw PayloadFailure(
            PortalLedParseErrorCode.INVALID_FIELD,
            "Le champ $name doit être un entier.",
            name
        )
    }

    private fun JSONObject.readColor(name: String, required: Boolean): PortalRgb? {
        if (!has(name) || isNull(name)) {
            if (required) {
                throw PayloadFailure(
                    PortalLedParseErrorCode.MISSING_FIELD,
                    "La couleur $name est requise lorsque le portail est actif.",
                    name
                )
            }
            return null
        }

        val colorObject = optJSONObject(name) ?: throw PayloadFailure(
            PortalLedParseErrorCode.INVALID_FIELD,
            "Le champ $name doit être un objet RGB.",
            name
        )

        return PortalRgb(
            red = colorObject.requireChannel("r", "$name.r"),
            green = colorObject.requireChannel("g", "$name.g"),
            blue = colorObject.requireChannel("b", "$name.b")
        )
    }

    private fun JSONObject.requireChannel(name: String, fieldPath: String): Int {
        val raw = if (has(name) && !isNull(name)) opt(name) else null
        val value = raw.toStrictLong() ?: throw PayloadFailure(
            if (raw == null) PortalLedParseErrorCode.MISSING_FIELD else PortalLedParseErrorCode.INVALID_FIELD,
            "Le canal $fieldPath doit être un entier entre 0 et 255.",
            fieldPath
        )
        if (value !in 0L..255L) {
            throw PayloadFailure(
                PortalLedParseErrorCode.INVALID_FIELD,
                "Le canal $fieldPath doit être compris entre 0 et 255.",
                fieldPath
            )
        }
        return value.toInt()
    }

    private fun JSONObject.requiredValue(name: String): Any {
        if (!has(name) || isNull(name)) {
            throw PayloadFailure(
                PortalLedParseErrorCode.MISSING_FIELD,
                "Le champ $name est manquant.",
                name
            )
        }
        return opt(name)
    }

    private fun Any?.toStrictLong(): Long? {
        val number = this as? Number ?: return null
        return when (number) {
            is Byte, is Short, is Int, is Long -> number.toLong()
            else -> {
                val asDouble = number.toDouble()
                if (!asDouble.isFinite() || asDouble % 1.0 != 0.0) null
                else number.toLong().takeIf { it.toDouble() == asDouble }
            }
        }
    }

    private class PayloadFailure(
        val code: PortalLedParseErrorCode,
        override val message: String,
        val field: String? = null
    ) : IllegalArgumentException(message)
}
