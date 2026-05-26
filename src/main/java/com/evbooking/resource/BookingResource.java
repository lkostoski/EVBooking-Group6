package com.evbooking.resource;

import java.util.List;

import com.evbooking.dto.BookingRequest;
import com.evbooking.dto.BookingResponse;
import com.evbooking.dto.BookingUpdateRequest;
import com.evbooking.dto.MessageResponse;
import com.evbooking.service.BookingService;

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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"DRIVER", "ADMIN"})
public class BookingResource {

    private final BookingService service = new BookingService();

    @GET
    public List<BookingResponse> list(@Context SecurityContext sec) {
        return service.listForUser(
            sec.getUserPrincipal().getName(),
            sec.isUserInRole("ADMIN")
        );
    }

    @GET
    @Path("/{id}")
    public BookingResponse getById(@PathParam("id") Long id,
                                    @Context SecurityContext sec) {
        return service.getById(
            id,
            sec.getUserPrincipal().getName(),
            sec.isUserInRole("ADMIN")
        );
    }

    @POST
    public Response create(@Valid BookingRequest request,
                            @Context SecurityContext sec) {
        BookingResponse created = service.create(
            request,
            sec.getUserPrincipal().getName(),
            sec.isUserInRole("ADMIN")
        );
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public BookingResponse update(@PathParam("id") Long id,
                                   @Valid BookingUpdateRequest request,
                                   @Context SecurityContext sec) {
        return service.update(
            id, request,
            sec.getUserPrincipal().getName(),
            sec.isUserInRole("ADMIN")
        );
    }

    @DELETE
    @Path("/{id}")
    public MessageResponse cancel(@PathParam("id") Long id,
                                   @Context SecurityContext sec) {
        service.cancel(
            id,
            sec.getUserPrincipal().getName(),
            sec.isUserInRole("ADMIN")
        );
        return new MessageResponse("Booking cancelled");
    }
}
