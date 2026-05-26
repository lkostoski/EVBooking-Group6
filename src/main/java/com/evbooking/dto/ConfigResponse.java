package com.evbooking.dto;

public class ConfigResponse {

    private String mapsKey;
    private String clientId;

    public ConfigResponse() {}

    public ConfigResponse(String mapsKey, String clientId) {
        this.mapsKey = mapsKey;
        this.clientId = clientId;
    }

    public String getMapsKey() { return mapsKey; }
    public void setMapsKey(String mapsKey) { this.mapsKey = mapsKey; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
}
