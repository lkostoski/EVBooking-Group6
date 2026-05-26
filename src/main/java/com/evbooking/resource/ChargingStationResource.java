package com.evbooking.resource;

import java.util.List;

import com.evbooking.dto.MessageResponse;
import com.evbooking.dto.StationDTO;
import com.evbooking.service.StationService;

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

@Path("/stations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChargingStationResource {

    private final StationService service = new StationService();

    @GET
    @RolesAllowed({"DRIVER", "ADMIN"})
    public List<StationDTO> list() {
        return service.findAll();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"DRIVER", "ADMIN"})
    public StationDTO getById(@PathParam("id") Long id) {
        return service.findById(id);
    }

    @POST
    @RolesAllowed("ADMIN")
    public Response create(@Valid StationDTO station) {
        StationDTO created = service.create(station);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public StationDTO update(@PathParam("id") Long id, @Valid StationDTO station) {
        return service.update(id, station);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public MessageResponse delete(@PathParam("id") Long id) {
        service.delete(id);
        return new MessageResponse("Station deleted");
    }
}
