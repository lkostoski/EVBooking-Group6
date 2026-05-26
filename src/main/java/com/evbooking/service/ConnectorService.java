package com.evbooking.service;

import java.util.List;
import java.util.stream.Collectors;

import com.evbooking.dto.ConnectorRequest;
import com.evbooking.dto.ConnectorResponse;
import com.evbooking.exception.NotFoundException;
import com.evbooking.model.ChargingStation;
import com.evbooking.model.Connector;
import com.evbooking.repository.ChargingStationDAO;
import com.evbooking.repository.ConnectorDAO;

public class ConnectorService {

    private final ConnectorDAO connectorDAO;
    private final ChargingStationDAO stationDAO;

    public ConnectorService() {
        this(new ConnectorDAO(), new ChargingStationDAO());
    }

    public ConnectorService(ConnectorDAO connectorDAO, ChargingStationDAO stationDAO) {
        this.connectorDAO = connectorDAO;
        this.stationDAO = stationDAO;
    }

    public List<ConnectorResponse> findByStation(Long stationId) {
        return connectorDAO.findByStationId(stationId).stream()
                .map(ConnectorResponse::from)
                .collect(Collectors.toList());
    }

    public ConnectorResponse create(Long stationId, ConnectorRequest req) {
        ChargingStation station = stationDAO.findById(stationId);
        if (station == null) throw new NotFoundException("Station not found");
        Connector c = new Connector(req.getConnectorType(), station);
        connectorDAO.save(c);
        return ConnectorResponse.from(c);
    }

    public ConnectorResponse update(Long stationId, Long connectorId, ConnectorRequest req) {
        ChargingStation station = stationDAO.findById(stationId);
        if (station == null) throw new NotFoundException("Station not found");
        Connector existing = connectorDAO.findById(connectorId);
        if (existing == null
                || !existing.getChargingStation().getStationId().equals(stationId)) {
            throw new NotFoundException("Connector not found");
        }
        existing.setConnectorType(req.getConnectorType());
        connectorDAO.update(existing);
        return ConnectorResponse.from(existing);
    }

    public void delete(Long connectorId) {
        Connector existing = connectorDAO.findById(connectorId);
        if (existing == null) throw new NotFoundException("Connector not found");
        connectorDAO.delete(connectorId);
    }
}
