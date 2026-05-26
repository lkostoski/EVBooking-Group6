package com.evbooking.dto;

import jakarta.validation.constraints.NotBlank;

public class ConnectorRequest {

    @NotBlank(message = "connectorType is required")
    private String connectorType;

    public ConnectorRequest() {}

    public String getConnectorType() { return connectorType; }
    public void setConnectorType(String connectorType) { this.connectorType = connectorType; }
}
