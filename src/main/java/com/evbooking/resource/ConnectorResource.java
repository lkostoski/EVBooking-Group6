package com.evbooking.resource;

import java.util.List;

import com.evbooking.dto.ConnectorRequest;
import com.evbooking.dto.ConnectorResponse;
import com.evbooking.dto.MessageResponse;
import com.evbooking.service.ConnectorService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/stations/{stationId}/connectors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConnectorResource {

    private final ConnectorService service = new ConnectorService();

    @GET
    @RolesAllowed({"DRIVER", "ADMIN"})
    public List<ConnectorResponse> list(@PathParam("stationId") Long stationId) {
        return service.findByStation(stationId);
    }

    @POST
    @RolesAllowed("ADMIN")
    public Response create(@PathParam("stationId") Long stationId,
                            @Valid ConnectorRequest request) {
        ConnectorResponse created = service.create(stationId, request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{connectorId}")
    @RolesAllowed("ADMIN")
    public ConnectorResponse update(@PathParam("stationId") Long stationId,
                                     @PathParam("connectorId") Long connectorId,
                                     @Valid ConnectorRequest request) {
        return service.update(stationId, connectorId, request);
    }

    @DELETE
    @Path("/{connectorId}")
    @RolesAllowed("ADMIN")
    public MessageResponse delete(@PathParam("stationId") Long stationId,
                                   @PathParam("connectorId") Long connectorId) {
        service.delete(connectorId);
        return new MessageResponse("Connector deleted");
    }
}
