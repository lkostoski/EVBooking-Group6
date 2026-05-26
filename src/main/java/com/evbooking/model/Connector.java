package com.evbooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "connectors")
public class Connector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "connector_id")
    private Long connectorId;

    @Column(name = "connector_type", nullable = false, length = 50)
    private String connectorType;

    @ManyToOne
    @JoinColumn(name = "station_id", nullable = false)
    private ChargingStation chargingStation;

    public Connector() {}

    public Connector(String connectorType, ChargingStation chargingStation) {
        this.connectorType = connectorType;
        this.chargingStation = chargingStation;
    }

    public Long getConnectorId() { return connectorId; }
    public void setConnectorId(Long connectorId) { this.connectorId = connectorId; }

    public String getConnectorType() { return connectorType; }
    public void setConnectorType(String connectorType) { this.connectorType = connectorType; }

    public ChargingStation getChargingStation() { return chargingStation; }
    public void setChargingStation(ChargingStation chargingStation) { 
        this.chargingStation = chargingStation; 
    }
}