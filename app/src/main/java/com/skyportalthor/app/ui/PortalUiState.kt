// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.ui

import com.skyportalthor.app.data.Skylander
import com.skyportalthor.app.portal.PortalResult

internal sealed interface LoadUiState {
    data object Idle : LoadUiState

    data class Loading(
        val logicalSlot: Int,
        val figure: Skylander
    ) : LoadUiState

    data class Success(
        val logicalSlot: Int,
        val figure: Skylander,
        val result: PortalResult.Success
    ) : LoadUiState

    data class Error(
        val logicalSlot: Int,
        val figure: Skylander,
        val result: PortalResult.Error
    ) : LoadUiState
}

internal enum class NoticeKind { SUCCESS, ERROR, INFO }

internal data class UiNotice(
    val text: String,
    val kind: NoticeKind = NoticeKind.INFO
)
