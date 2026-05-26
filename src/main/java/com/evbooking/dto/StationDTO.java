package com.evbooking.dto;

import com.evbooking.model.ChargingStation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public class StationDTO {

    private Long stationId;

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "address is required")
    private String address;

    @DecimalMin(value = "-90.0",  message = "latitude must be between -90 and 90")
    @DecimalMax(value = "90.0",   message = "latitude must be between -90 and 90")
    private double latitude;

    @DecimalMin(value = "-180.0", message = "longitude must be between -180 and 180")
    @DecimalMax(value = "180.0",  message = "longitude must be between -180 and 180")
    private double longitude;

    public StationDTO() {}

    public static StationDTO from(ChargingStation s) {
        StationDTO d = new StationDTO();
        d.stationId = s.getStationId();
        d.name      = s.getName();
        d.address   = s.getAddress();
        d.latitude  = s.getLatitude();
        d.longitude = s.getLongitude();
        return d;
    }

    public ChargingStation toEntity() {
        ChargingStation s = new ChargingStation(name, address, latitude, longitude);
        s.setStationId(stationId);
        return s;
    }

    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
