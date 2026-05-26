package com.evbooking.service;

import java.util.List;
import java.util.stream.Collectors;

import com.evbooking.dto.StationDTO;
import com.evbooking.exception.NotFoundException;
import com.evbooking.model.ChargingStation;
import com.evbooking.repository.ChargingStationDAO;

public class StationService {

    private final ChargingStationDAO stationDAO;

    public StationService() {
        this(new ChargingStationDAO());
    }

    public StationService(ChargingStationDAO stationDAO) {
        this.stationDAO = stationDAO;
    }

    public List<StationDTO> findAll() {
        return stationDAO.findAll().stream()
                .map(StationDTO::from)
                .collect(Collectors.toList());
    }

    public StationDTO findById(Long id) {
        ChargingStation s = stationDAO.findById(id);
        if (s == null) throw new NotFoundException("Station not found");
        return StationDTO.from(s);
    }

    public StationDTO create(StationDTO dto) {
        ChargingStation entity = new ChargingStation(
            dto.getName(), dto.getAddress(),
            dto.getLatitude(), dto.getLongitude()
        );
        stationDAO.save(entity);
        return StationDTO.from(entity);
    }

    public StationDTO update(Long id, StationDTO dto) {
        ChargingStation existing = stationDAO.findById(id);
        if (existing == null) throw new NotFoundException("Station not found");
        existing.setName(dto.getName());
        existing.setAddress(dto.getAddress());
        existing.setLatitude(dto.getLatitude());
        existing.setLongitude(dto.getLongitude());
        stationDAO.update(existing);
        return StationDTO.from(existing);
    }

    public void delete(Long id) {
        ChargingStation existing = stationDAO.findById(id);
        if (existing == null) throw new NotFoundException("Station not found");
        stationDAO.delete(id);
    }
}
