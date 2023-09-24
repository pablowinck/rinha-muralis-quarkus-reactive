package br.com.pablowinter;

import java.util.List;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class PessoaController {

    @Inject
    MemoryDatabase memoryDatabase;

    private static final List<String> FIELDS = List.of("apelido", "nome", "nascimento");

    @POST
    @Path("/pessoas")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> insert(JsonObject jsonObject) {
        if (FIELDS.stream()
                .anyMatch(field -> fieldIsUndefinedOrNull(jsonObject, field)))
            return Uni.createFrom().item(Response.status(422).build());
        if (FIELDS.stream()
                .anyMatch(field -> fieldIsTypeIncorrect(jsonObject, field, String.class)))
            return Uni.createFrom().item(Response.status(400).build());
        if (stackIsASingleObject(jsonObject))
            return Uni.createFrom().item(Response.status(400).build());
        if (fieldIsTypeIncorrect(jsonObject, "stack", JsonArray.class))
            return Uni.createFrom().item(Response.status(400).build());
        if (stackHasTypeInvalidInArray(jsonObject))
            return Uni.createFrom().item(Response.status(400).build());
        Pessoa pessoa = jsonObject.mapTo(Pessoa.class);
        if (pessoa.getNascimento() == null)
            return Uni.createFrom().item(Response.status(422).build());
        if (pessoa.isBadRequest())
            return Uni.createFrom().item(Response.status(400).build());
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

    private boolean fieldIsUndefinedOrNull(JsonObject jsonObject, String field) {
        return !jsonObject.containsKey(field) || jsonObject.getValue(field) == null;
    }

    private boolean fieldIsTypeIncorrect(JsonObject jsonObject, String field, Class<?> type) {
        return jsonObject.containsKey(field) && jsonObject.getValue(field) != null
                && !type.isInstance(jsonObject.getValue(field));
    }

    private boolean stackIsASingleObject(JsonObject jsonObject) {
        return jsonObject.containsKey("stack") && jsonObject.getValue("stack") != null
                && jsonObject.getValue("stack") instanceof JsonObject;
    }

    private boolean stackHasTypeInvalidInArray(JsonObject jsonObject) {
        if (!jsonObject.containsKey("stack") || jsonObject.getValue("stack") == null)
            return false;
        return jsonObject.getJsonArray("stack").stream()
                .anyMatch(value -> !(value instanceof String));
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
        List<Pessoa> pessoasInMemory = memoryDatabase.findByTerm(term);
        if (!pessoasInMemory.isEmpty())
            return Uni.createFrom().item(Response.ok(pessoasInMemory).build());
        return Pessoa.<Pessoa>find("UPPER(term) like UPPER(?1)", "%" + term + "%")
                .page(0, 100).list()
                .onItem().ifNotNull().transform(entity -> Response.ok(entity).build())
                .onItem().ifNull().continueWith(Response.status(Response.Status.NOT_FOUND)::build);
    }

}
