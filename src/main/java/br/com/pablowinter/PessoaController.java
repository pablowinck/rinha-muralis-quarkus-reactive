package br.com.pablowinter;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.Objects;

@Path("/")
public class PessoaController {

    @Inject
    MemoryDatabase memoryDatabase;

    @RestClient
    MemoryClient memoryClient;

    @ConfigProperty(name = "quarkus.http.port")
    Integer port;

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
        if (isMemoryClient()) {
            return memoryDatabase.existsByApelido(pessoa.getApelido())
                    .onItem().ifNotNull().transformToUni(exists -> {
                        if (exists)
                            return Uni.createFrom().item(Response.status(422).build());
                        return finishPersist(pessoa);
                    })
                    .onItem().ifNull().continueWith(Response.status(422)::build);
        }
        try {
            return memoryClient.existsByApelido(pessoa.getApelido())
                    .onItem().ifNotNull().transformToUni(exists -> {
                        if (exists)
                            return Uni.createFrom().item(Response.status(422).build());
                        return finishPersist(pessoa);
                    })
                    .onItem().ifNull().continueWith(Response.status(422)::build);
        } catch (WebApplicationException e) {
            return finishPersist(pessoa);
        }
    }

    private Uni<Response> finishPersist(Pessoa pessoa) {
        pessoa.prepareToPersist();
        try {
            if (isMemoryClient()) {
                return memoryDatabase.save(pessoa)
                        .onItem().transformToUni((it) -> Panache.withTransaction(pessoa::persist)
                                .replaceWith(Response.ok().status(201)
                                        .header("Location", "/pessoas/" + pessoa.getId()).build()));

            } else {
                return memoryClient.save(pessoa)
                        .onItem().transformToUni(response -> Panache.withTransaction(pessoa::persist)
                                .replaceWith(Response.ok().status(201)
                                        .header("Location", "/pessoas/" + pessoa.getId()).build()));
            }
        } catch (WebApplicationException e) {
            return Panache.withTransaction(pessoa::persist)
                    .replaceWith(Response.ok().status(201)
                            .header("Location", "/pessoas/" + pessoa.getId()).build());
        }
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
        try {
            if (isMemoryClient()) {
                return memoryDatabase.getPessoa(id)
                        .onItem().ifNotNull().transform(pessoaInMemory -> Response.ok(pessoaInMemory).build())
                        .onItem().ifNull().switchTo(findPessoaByIdInDatabase(id));
            } else {
                return memoryClient.getPessoa(id)
                        .onItem().ifNotNull().transformToUni(entity -> {
                            if (entity.getStatus() == 200)
                                return Uni.createFrom().item(entity);
                            else
                                return findPessoaByIdInDatabase(id);
                        })
                        .onItem().ifNull().switchTo(findPessoaByIdInDatabase(id));
            }
        } catch (WebApplicationException e) {
            return findPessoaByIdInDatabase(id);
        }
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
        try {
            if (isMemoryClient()) {
                return memoryDatabase.findByTerm(term).collect().asList().onItem().transformToUni(pessoasInMemory -> {
                    if (pessoasInMemory != null && !pessoasInMemory.isEmpty()) {
                        return Uni.createFrom().item(Response.ok(pessoasInMemory).build());
                    } else {
                        return findPessoaByTermInDatabase(term);
                    }
                });
            } else {
                return memoryClient.findByTerm(term)
                        .onItem().ifNotNull().transformToUni(entity -> {
                            if (entity.getStatus() == 200)
                                return Uni.createFrom().item(entity);
                            else
                                return findPessoaByTermInDatabase(term);
                        })
                        .onItem().ifNull().switchTo(findPessoaByTermInDatabase(term));
            }
        } catch (WebApplicationException e) {
            return findPessoaByTermInDatabase(term);
        }
    }

    private static Uni<Response> findPessoaByTermInDatabase(String term) {
        return Pessoa.<Pessoa>find("UPPER(term) like UPPER(?1)", "%" + term + "%")
                .page(0, 100).list()
                .onItem().ifNotNull().transform(entity -> Response.ok(entity).build())
                .onItem().ifNull().continueWith(Response.status(Response.Status.NOT_FOUND)::build);
    }

    private boolean isMemoryClient() {
        return port == 3001;
    }

    @Path("/memory/apelido/{apelido}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Boolean> existsByApelido(@PathParam("apelido") String apelido) {
        return memoryDatabase.existsByApelido(apelido);
    }

    @Path("/memory/pessoa/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> getPessoa(String id) {
        return memoryDatabase.getPessoa(id)
                .onItem().ifNotNull().transform(pessoa -> Response.ok(pessoa).build())
                .onItem().ifNull().continueWith(Response.status(404)::build);
    }

    @Path("/memory/pessoa/term/{term}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> findByTerm(String term) {
        return memoryDatabase.findByTerm(term)
                .collect().asList().onItem().transform(pessoas -> {
                    if (pessoas != null && !pessoas.isEmpty()) {
                        return Response.ok(pessoas).build();
                    } else {
                        return Response.status(404).build();
                    }
                });
    }

    @Path("/memory/pessoa")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> save(Pessoa pessoa) {
        return memoryDatabase.save(pessoa)
                .onItem().transform(response -> Response.ok().status(201).build());
    }

}
