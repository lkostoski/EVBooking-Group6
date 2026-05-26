package com.evbooking.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.evbooking.dto.SlotRequest;
import com.evbooking.dto.SlotResponse;
import com.evbooking.exception.BadRequestException;
import com.evbooking.exception.NotFoundException;
import com.evbooking.model.AvailableSlot;
import com.evbooking.model.Connector;
import com.evbooking.repository.AvailableSlotDAO;
import com.evbooking.repository.ConnectorDAO;

public class SlotService {

    private final AvailableSlotDAO slotDAO;
    private final ConnectorDAO connectorDAO;

    public SlotService() {
        this(new AvailableSlotDAO(), new ConnectorDAO());
    }

    public SlotService(AvailableSlotDAO slotDAO, ConnectorDAO connectorDAO) {
        this.slotDAO = slotDAO;
        this.connectorDAO = connectorDAO;
    }

    public List<SlotResponse> findByConnector(Long connectorId, LocalDate date) {
        List<AvailableSlot> slots = (date != null)
            ? slotDAO.findByConnectorAndDate(connectorId, date)
            : slotDAO.findByConnectorId(connectorId);
        return slots.stream().map(SlotResponse::from).collect(Collectors.toList());
    }

    public SlotResponse create(Long connectorId, SlotRequest req) {
        if (!req.getStartTime().isBefore(req.getEndTime())) {
            throw new BadRequestException("startTime must be before endTime");
        }
        Connector connector = connectorDAO.findById(connectorId);
        if (connector == null) throw new NotFoundException("Connector not found");
        AvailableSlot slot = new AvailableSlot(
            connector, req.getDate(), req.getStartTime(), req.getEndTime()
        );
        slotDAO.save(slot);
        return SlotResponse.from(slot);
    }

    public void delete(Long slotId) {
        slotDAO.delete(slotId);
    }
}
