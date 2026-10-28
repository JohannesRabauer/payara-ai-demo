package com.jconbrazil.cfp;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("submissions")
@Produces(MediaType.APPLICATION_JSON)
public class SubmissionResource {

    @Inject
    SubmissionStore store;

    @Inject
    Event<ReviewRequested> reviewRequested;

    @GET
    public List<Submission> list() {
        return store.all();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid NewSubmission newSubmission) {
        Submission created = store.add(newSubmission);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    // The agent workflow runs synchronously inside fire(), so the review is stored when it returns.
    @POST
    @Path("{id}/review")
    public Response review(@PathParam("id") long id) {
        if (store.find(id).isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "No submission with id " + id))
                    .build();
        }
        reviewRequested.fire(new ReviewRequested(id));
        return Response.ok(store.find(id).orElseThrow()).build();
    }
}
