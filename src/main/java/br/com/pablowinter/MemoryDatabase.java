package br.com.pablowinter;

import io.quarkus.runtime.Startup;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Startup
public class MemoryDatabase {

    private final Set<String> apelidos = ConcurrentHashMap.newKeySet(100_000);
    private final ConcurrentHashMap<String, Pessoa> pessoas = new ConcurrentHashMap<>(100_000);

    public boolean existsByApelido(String apelido) {
        return apelidos.contains(apelido);
    }

    public Pessoa getPessoa(String id) {
        return pessoas.get(id);
    }

    public void save(Pessoa pessoa) {
        apelidos.add(pessoa.getApelido());
        pessoas.put(pessoa.getId(), pessoa);
    }

    public List<Pessoa> findByTerm(String term) {
        return pessoas.values().stream()
                .filter(pessoa -> pessoa.buildTerm().toLowerCase().contains(term.toLowerCase()))
                .limit(100)
                .toList();
    }


}
