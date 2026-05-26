package com.evbooking.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.evbooking.model.AvailableSlot;

public class SlotResponse {

    private Long slotId;
    private Long connectorId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    public SlotResponse() {}

    public static SlotResponse from(AvailableSlot s) {
        SlotResponse r = new SlotResponse();
        r.slotId      = s.getSlotId();
        r.connectorId = s.getConnector().getConnectorId();
        r.date        = s.getDate();
        r.startTime   = s.getStartTime();
        r.endTime     = s.getEndTime();
        return r;
    }

    public Long getSlotId() { return slotId; }
    public void setSlotId(Long slotId) { this.slotId = slotId; }

    public Long getConnectorId() { return connectorId; }
    public void setConnectorId(Long connectorId) { this.connectorId = connectorId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}
