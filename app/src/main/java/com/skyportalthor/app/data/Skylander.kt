package com.skyportalthor.app.data

import android.net.Uri

data class Skylander(
    val name: String,
    val element: String,
    val generation: String,
    val fileName: String,
    val documentUri: Uri,
    val relativePath: String,
    val kind: FigureKind = FigureKind.CHARACTER,
    val typeLabel: String = "Skylander",
    val level: Int? = null
)
