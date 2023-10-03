package br.com.pablowinter;

import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Singleton;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Startup
public class MemoryDatabase {

    private final Set<String> apelidos = ConcurrentHashMap.newKeySet(100_000);
    private final ConcurrentHashMap<String, Pessoa> pessoas = new ConcurrentHashMap<>(100_000);

    public Uni<Boolean> existsByApelido(String apelido) {
        return Uni.createFrom().item(() -> apelidos.contains(apelido));
    }

    public Uni<Pessoa> getPessoa(String id) {
        return Uni.createFrom().item(() -> pessoas.get(id));
    }

    public Uni<Void> save(Pessoa pessoa) {
        return Uni.createFrom().item(() -> {
            apelidos.add(pessoa.getApelido());
            pessoas.put(pessoa.getId(), pessoa);
            return null;
        });
    }

    public Multi<Pessoa> findByTerm(String term) {
        if (term == null || term.isBlank())
            return Multi.createFrom().nothing();
        return Multi.createFrom().items(pessoas.values().stream()
                .filter(pessoa -> pessoa.buildTerm().toLowerCase().contains(term.toLowerCase()))
                .toArray(Pessoa[]::new));
    }


}
