// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.ipc;

interface ISkylanderPortalService {
    int getApiVersion();
    boolean ping();
    int load(int logicalSlot, String uri, String displayName);
    boolean remove(int logicalSlot);
    void clear();
    String getStatusJson();
    int setPortalEnabled(boolean enabled);
    String getFigureCatalogJson();
    String getPortalLedStateJson();
}
