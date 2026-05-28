package com.evbooking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.evbooking.dto.ConnectorRequest;
import com.evbooking.dto.ConnectorResponse;
import com.evbooking.dto.SlotRequest;
import com.evbooking.dto.StationDTO;
import com.evbooking.exception.BadRequestException;
import com.evbooking.exception.NotFoundException;
import com.evbooking.model.AvailableSlot;
import com.evbooking.model.ChargingStation;
import com.evbooking.model.Connector;
import com.evbooking.repository.AvailableSlotDAO;
import com.evbooking.repository.ChargingStationDAO;
import com.evbooking.repository.ConnectorDAO;

class CatalogServiceTest {

    @Test
    @DisplayName("station service performs create, update, list and delete")
    void stationCrud() {
        FakeStationDAO dao = new FakeStationDAO();
        StationService service = new StationService(dao);

        StationDTO created = service.create(stationDto("Central"));
        assertEquals(1, service.findAll().size());

        StationDTO update = stationDto("Updated");
        service.update(created.getStationId(), update);
        assertEquals("Updated", service.findById(created.getStationId()).getName());

        service.delete(created.getStationId());
        assertEquals(0, service.findAll().size());
    }

    @Test
    @DisplayName("connector create requires an existing station")
    void connectorCreateRequiresStation() {
        ConnectorService service = new ConnectorService(new FakeConnectorDAO(), new FakeStationDAO());
        ConnectorRequest req = new ConnectorRequest();
        req.setConnectorType("CCS");

        assertThrows(NotFoundException.class, () -> service.create(404L, req));
    }

    @Test
    @DisplayName("connector service creates and lists connectors for a station")
    void connectorCreateAndList() {
        FakeStationDAO stations = new FakeStationDAO();
        ChargingStation station = stations.add(station("Station"));
        FakeConnectorDAO connectors = new FakeConnectorDAO();
        ConnectorService service = new ConnectorService(connectors, stations);
        ConnectorRequest req = new ConnectorRequest();
        req.setConnectorType("Type 2");

        service.create(station.getStationId(), req);

        assertEquals(1, service.findByStation(station.getStationId()).size());
        assertEquals("Type 2", service.findByStation(station.getStationId()).get(0).getConnectorType());
    }

    @Test
    @DisplayName("connector service updates connector type for the selected station")
    void connectorUpdate() {
        FakeStationDAO stations = new FakeStationDAO();
        ChargingStation station = stations.add(station("Station"));
        FakeConnectorDAO connectors = new FakeConnectorDAO();
        Connector connector = connectors.add(connector("Type 2", station));
        ConnectorService service = new ConnectorService(connectors, stations);
        ConnectorRequest req = new ConnectorRequest();
        req.setConnectorType("CCS");

        ConnectorResponse updated = service.update(station.getStationId(), connector.getConnectorId(), req);

        assertEquals("CCS", updated.getConnectorType());
        assertEquals("CCS", connectors.findById(connector.getConnectorId()).getConnectorType());
    }

    @Test
    @DisplayName("connector update rejects connectors outside the selected station")
    void connectorUpdateRejectsWrongStation() {
        FakeStationDAO stations = new FakeStationDAO();
        ChargingStation station = stations.add(station("Station"));
        ChargingStation other = stations.add(station("Other"));
        FakeConnectorDAO connectors = new FakeConnectorDAO();
        Connector connector = connectors.add(connector("Type 2", other));
        ConnectorService service = new ConnectorService(connectors, stations);
        ConnectorRequest req = new ConnectorRequest();
        req.setConnectorType("CCS");

        assertThrows(NotFoundException.class,
            () -> service.update(station.getStationId(), connector.getConnectorId(), req));
    }

    @Test
    @DisplayName("slot create rejects inverted time windows")
    void slotRejectsInvalidTimeWindow() {
        SlotService service = new SlotService(new FakeSlotDAO(), new FakeConnectorDAO());
        SlotRequest req = slotRequest(LocalTime.of(10, 0), LocalTime.of(9, 0));

        assertThrows(BadRequestException.class, () -> service.create(1L, req));
    }

    @Test
    @DisplayName("slot service creates and filters slots by connector/date")
    void slotCreateAndFilter() {
        FakeStationDAO stations = new FakeStationDAO();
        ChargingStation station = stations.add(station("Station"));
        FakeConnectorDAO connectors = new FakeConnectorDAO();
        Connector connector = connectors.add(connector("CCS", station));
        FakeSlotDAO slots = new FakeSlotDAO();
        SlotService service = new SlotService(slots, connectors);

        service.create(connector.getConnectorId(), slotRequest(LocalTime.of(8, 0), LocalTime.of(9, 0)));

        assertEquals(1, service.findByConnector(connector.getConnectorId(), LocalDate.now().plusDays(1)).size());
    }

    private static StationDTO stationDto(String name) {
        StationDTO dto = new StationDTO();
        dto.setName(name);
        dto.setAddress(name + " address");
        dto.setLatitude(40.0);
        dto.setLongitude(22.0);
        return dto;
    }

    private static ChargingStation station(String name) {
        return new ChargingStation(name, name + " address", 40.0, 22.0);
    }

    private static Connector connector(String type, ChargingStation station) {
        return new Connector(type, station);
    }

    private static SlotRequest slotRequest(LocalTime start, LocalTime end) {
        SlotRequest req = new SlotRequest();
        req.setDate(LocalDate.now().plusDays(1));
        req.setStartTime(start);
        req.setEndTime(end);
        return req;
    }

    private static final class FakeStationDAO extends ChargingStationDAO {
        private final List<ChargingStation> stations = new ArrayList<>();

        ChargingStation add(ChargingStation station) {
            station.setStationId((long) stations.size() + 1);
            stations.add(station);
            return station;
        }

        @Override
        public List<ChargingStation> findAll() { return stations; }

        @Override
        public ChargingStation findById(Long id) {
            return stations.stream().filter(s -> s.getStationId().equals(id)).findFirst().orElse(null);
        }

        @Override
        public void save(ChargingStation station) { add(station); }

        @Override
        public void update(ChargingStation station) {}

        @Override
        public void delete(Long id) {
            stations.removeIf(s -> s.getStationId().equals(id));
        }
    }

    private static final class FakeConnectorDAO extends ConnectorDAO {
        private final List<Connector> connectors = new ArrayList<>();

        Connector add(Connector connector) {
            connector.setConnectorId((long) connectors.size() + 1);
            connectors.add(connector);
            return connector;
        }

        @Override
        public List<Connector> findByStationId(Long stationId) {
            return connectors.stream()
                .filter(c -> c.getChargingStation().getStationId().equals(stationId))
                .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public Connector findById(Long id) {
            return connectors.stream().filter(c -> c.getConnectorId().equals(id)).findFirst().orElse(null);
        }

        @Override
        public void save(Connector connector) { add(connector); }

        @Override
        public void update(Connector connector) {
            Connector existing = findById(connector.getConnectorId());
            if (existing != null) {
                existing.setConnectorType(connector.getConnectorType());
            }
        }
    }

    private static final class FakeSlotDAO extends AvailableSlotDAO {
        private final List<AvailableSlot> slots = new ArrayList<>();

        @Override
        public List<AvailableSlot> findByConnectorAndDate(Long connectorId, LocalDate date) {
            return slots.stream()
                .filter(s -> s.getConnector().getConnectorId().equals(connectorId))
                .filter(s -> s.getDate().equals(date))
                .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public List<AvailableSlot> findByConnectorId(Long connectorId) {
            return slots.stream()
                .filter(s -> s.getConnector().getConnectorId().equals(connectorId))
                .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public void save(AvailableSlot slot) {
            slot.setSlotId((long) slots.size() + 1);
            slots.add(slot);
        }
    }
}
