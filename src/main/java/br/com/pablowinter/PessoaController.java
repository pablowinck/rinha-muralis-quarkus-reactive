package br.com.pablowinter;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Objects;

@Path("/")
public class PessoaController {

    @Inject
    MemoryDatabase memoryDatabase;

    private static final List<String> FIELDS = List.of("apelido", "nome", "nascimento");

    @POST
    @Path("/pessoas")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
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
        if (stackHasTypeNullInArray(jsonObject))
            return Uni.createFrom().item(Response.status(422).build());
        if (stackHasTypeInvalidInArray(jsonObject))
            return Uni.createFrom().item(Response.status(400).build());
        Pessoa pessoa = jsonObject.mapTo(Pessoa.class);
        if (pessoa.getNascimento() == null)
            return Uni.createFrom().item(Response.status(422).build());
        if (pessoa.getNascimento().length() != 10 || pessoa.getNascimento().split("-").length != 3)
            return Uni.createFrom().item(Response.status(400).build());
        if (pessoa.isBadRequest())
            return Uni.createFrom().item(Response.status(400).build());
        if (pessoa.isUnprossessableEntity())
            return Uni.createFrom().item(Response.status(422).build());
        if (memoryDatabase.existsByApelido(pessoa.getApelido()))
            return Uni.createFrom().item(Response.status(422).build());
        return finishPersist(pessoa);
    }

    private Uni<Response> finishPersist(Pessoa pessoa) {
        pessoa.prepareToPersist();
        memoryDatabase.save(pessoa);
        return Panache.withTransaction(pessoa::persist)
                .replaceWith(Response.ok().status(201)
                        .header("Location", "/pessoas/" + pessoa.getId()).build())
                .onFailure().recoverWithUni((it) -> Uni.createFrom().item(Response.status(422).build()));
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

    private boolean stackHasTypeNullInArray(JsonObject jsonObject) {
        if (!jsonObject.containsKey("stack") || jsonObject.getValue("stack") == null)
            return false;
        return jsonObject.getJsonArray("stack").stream()
                .anyMatch(Objects::isNull);
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
    public Uni<Response> findById(@PathParam("id") String id) {
        if (id == null || id.isBlank()) {
            return Uni.createFrom().item(Response.status(400).build());
        }
        Pessoa pessoa = memoryDatabase.getPessoa(id);
        if (pessoa != null) {
            return Uni.createFrom().item(Response.ok(pessoa).build());
        }
        return findPessoaByIdInDatabase(id);
    }

    private static Uni<Response> findPessoaByIdInDatabase(String id) {
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
        List<Pessoa> pessoaByTermInMemory = memoryDatabase.findByTerm(term);
        if (pessoaByTermInMemory != null && !pessoaByTermInMemory.isEmpty() && pessoaByTermInMemory.size() > 15) {
            return multiplyList(pessoaByTermInMemory).toUni()
                    .onItem().transform(Response.ok()::entity)
                    .onItem().transform(Response.ResponseBuilder::build);
        }
        return findPessoaByTermInDatabase(term);
    }

    private Multi<List<Pessoa>> multiplyList(List<Pessoa> entity) {
        if (entity.size() > 100) {
            return Multi.createFrom().items(entity);
        }
        if (entity.size() > 50) {
            return Multi.createFrom().items(entity, entity);
        }
        return Multi.createFrom().items(entity, entity, entity, entity, entity);
    }

    private static Uni<Response> findPessoaByTermInDatabase(String term) {
        return Pessoa.<Pessoa>find("UPPER(term) like UPPER(?1)", "%" + term + "%")
                .page(0, 100).list()
                .onItem().ifNotNull().transform(entity -> {
                    if (entity.isEmpty()) {
                        return Response.ok(List.of()).build();
                    }
                    return Response.ok(entity).build();
                })
                .onItem().ifNull().continueWith(Response.ok(List.of())::build);
    }

}
