package com.evbooking.dto;

import com.evbooking.model.Connector;

public class ConnectorResponse {

    private Long connectorId;
    private String connectorType;
    private Long stationId;

    public ConnectorResponse() {}

    public static ConnectorResponse from(Connector c) {
        ConnectorResponse r = new ConnectorResponse();
        r.connectorId   = c.getConnectorId();
        r.connectorType = c.getConnectorType();
        r.stationId     = c.getChargingStation().getStationId();
        return r;
    }

    public Long getConnectorId() { return connectorId; }
    public void setConnectorId(Long connectorId) { this.connectorId = connectorId; }

    public String getConnectorType() { return connectorType; }
    public void setConnectorType(String connectorType) { this.connectorType = connectorType; }

    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }
}
