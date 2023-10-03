package br.com.pablowinter;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/memory")
@RegisterRestClient(baseUri = "http://api2:3001")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public interface MemoryClient {

    @Path("/apelido/{apelido}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    Uni<Boolean> existsByApelido(String apelido);

    @Path("/pessoa/{id}")
    @GET
    Uni<Response> getPessoa(String id);

    @Path("/pessoa/term/{term}")
    @GET
    Uni<Response> findByTerm(String term);

    @Path("/pessoa")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<Response> save(Pessoa pessoa);
}
