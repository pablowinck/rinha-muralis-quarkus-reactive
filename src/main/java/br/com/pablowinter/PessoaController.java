package br.com.pablowinter;


import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class PessoaController {

    @Inject
    MemoryDatabase memoryDatabase;

    @POST
    @Path("/pessoas")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> insert(Pessoa pessoa) {
        if (pessoa.isUnprossessableEntity())
            return Uni.createFrom().item(Response.status(422).build());
        if (memoryDatabase.existsByApelido(pessoa.getApelido()))
            return Uni.createFrom().item(Response.status(422).build());
        pessoa.prepareToPersist();
        memoryDatabase.save(pessoa);
        return Panache.withTransaction(pessoa::persist)
                .replaceWith(Response.ok().status(201)
                        .header("Location", "/pessoas/" + pessoa.getId()).build());
    }

    @GET
    @Path("/pessoas/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @WithSession
    public Uni<Response> findById(String id) {
        if (id == null || id.isBlank())
            return Uni.createFrom().item(Response.status(400).build());
        Pessoa pessoaInMemory = memoryDatabase.getPessoa(id);
        if (pessoaInMemory != null)
            return Uni.createFrom().item(Response.ok(pessoaInMemory).build());
        return Pessoa.findById(id)
                .onItem().transform(p -> p != null ? Response.ok(p) : Response.status(404))
                .onItem().transform(Response.ResponseBuilder::build);
    }

    @GET
    @Path("/contagem-pessoas")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Long> count() {
        return Pessoa.count();
    }

    @GET
    @Path("/pessoas")
    @Produces(MediaType.APPLICATION_JSON)
    @WithSession
    public Uni<Response> find100ByTerm(@QueryParam("t") String term) {
        if (term == null || term.isBlank()) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST).build());
        }
        Pessoa pessoasInMemory = memoryDatabase.findByTerm(term);
        if (pessoasInMemory != null)
            return Uni.createFrom().item(Response.ok(pessoasInMemory).build());
        return Pessoa.<Pessoa>find("UPPER(term) like UPPER(?1)", "%" + term + "%")
                .page(0, 100).list()
                .onItem().ifNotNull().transform(entity -> Response.ok(entity).build())
                .onItem().ifNull().continueWith(Response.status(Response.Status.NOT_FOUND)::build);
    }

}
