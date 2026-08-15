package com.skyportalthor.ipc;

interface ISkylanderPortalService {
    int getApiVersion();
    boolean ping();
    int load(int logicalSlot, String uri, String displayName);
    boolean remove(int logicalSlot);
    void clear();
    String getStatusJson();
}
