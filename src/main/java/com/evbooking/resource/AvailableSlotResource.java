package com.evbooking.resource;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.evbooking.dto.MessageResponse;
import com.evbooking.dto.SlotRequest;
import com.evbooking.dto.SlotResponse;
import com.evbooking.exception.BadRequestException;
import com.evbooking.service.SlotService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/connectors/{connectorId}/slots")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AvailableSlotResource {

    private final SlotService service = new SlotService();

    @GET
    @RolesAllowed({"DRIVER", "ADMIN"})
    public List<SlotResponse> list(@PathParam("connectorId") Long connectorId,
                                    @QueryParam("date") String date) {
        LocalDate parsed = null;
        if (date != null && !date.isBlank()) {
            try {
                parsed = LocalDate.parse(date);
            } catch (DateTimeParseException ex) {
                throw new BadRequestException("Invalid date format, expected YYYY-MM-DD");
            }
        }
        return service.findByConnector(connectorId, parsed);
    }

    @POST
    @RolesAllowed("ADMIN")
    public Response create(@PathParam("connectorId") Long connectorId,
                            @Valid SlotRequest request) {
        SlotResponse created = service.create(connectorId, request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @DELETE
    @Path("/{slotId}")
    @RolesAllowed("ADMIN")
    public MessageResponse delete(@PathParam("connectorId") Long connectorId,
                                   @PathParam("slotId") Long slotId) {
        service.delete(slotId);
        return new MessageResponse("Slot deleted");
    }
}
