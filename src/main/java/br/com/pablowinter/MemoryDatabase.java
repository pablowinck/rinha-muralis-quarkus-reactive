package br.com.pablowinter;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.tuples.Tuple3;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.redis.client.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Objects;

@ApplicationScoped
public class MemoryDatabase {

    @Inject
    ReactiveRedisDataSource reactiveDataSource;

    public Uni<Pessoa> findById(String id) {
        return reactiveDataSource.execute("GET", id)
                .map(json -> json != null ? Pessoa.fromJson(json.toString()) : null);
    }

    public Uni<Response> save(Pessoa pessoa) {
        return reactiveDataSource.execute("SET", pessoa.getId(), JsonObject.mapFrom(pessoa).encode());
    }

}
