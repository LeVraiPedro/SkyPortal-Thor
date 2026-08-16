// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "SkyPortalThor"
include(":app")
