package br.com.pablowinter;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple3;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.redis.client.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class MemoryDatabase {

    @Inject
    ReactiveRedisDataSource reactiveDataSource;

    public Uni<Boolean> existsByApelido(String apelido) {
        return reactiveDataSource.execute("GET", apelido)
                .map(Objects::nonNull);
    }

    public Uni<Pessoa> getPessoa(String id) {
        return reactiveDataSource.execute("GET", id)
                .map(json -> json != null ? Pessoa.fromJson(json.toString()) : null);
    }

    public Uni<Tuple3<Response, Response, Response>> save(Pessoa pessoa) {
        return Uni.combine().all()
                .unis(reactiveDataSource.execute("SET", pessoa.getId(), JsonObject.mapFrom(pessoa).encode()),
                        reactiveDataSource.execute("SET", pessoa.getApelido(), pessoa.getId()),
                        reactiveDataSource.execute("SET", pessoa.getTerm(), JsonObject.mapFrom(pessoa).encode()))
                .asTuple();
    }

    public Uni<List<Pessoa>> findByTermLike(String term) {

        return reactiveDataSource.key().keys("*" + term + "*")
                .onItem().transformToMulti(keys -> Multi.createFrom().items(keys.stream()))
                .onItem().transformToUniAndMerge(key -> reactiveDataSource.execute("GET", key))
                .collect().asList()
                .onItem().transform(items -> items.stream()
                        .map(item -> Pessoa.fromJson(item.toString()))
                        .filter(Objects::nonNull)
                        .toList());
    }

}
