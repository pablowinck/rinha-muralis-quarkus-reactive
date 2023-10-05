package br.com.pablowinter;

import io.quarkus.runtime.Startup;
import jakarta.inject.Singleton;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Startup
public class MemoryDatabase {

    private final Set<String> apelidos = new HashSet<>(100_000);
    private final LinkedHashMap<String, Pessoa> pessoas = new LinkedHashMap<>(100_000);

    public boolean existsByApelido(String apelido) {
        return apelidos.contains(apelido);
    }

    public Pessoa getPessoa(String id) {
        return pessoas.get(id);
    }

    public synchronized void save(Pessoa pessoa) {
        apelidos.add(pessoa.getApelido());
        pessoas.put(pessoa.getId(), pessoa);
    }

    public synchronized List<Pessoa> findByTerm(String term) {
        return pessoas.values().stream()
                .filter(pessoa -> pessoa.buildTerm().toLowerCase().contains(term.toLowerCase()))
                .limit(100)
                .toList();
    }


}
