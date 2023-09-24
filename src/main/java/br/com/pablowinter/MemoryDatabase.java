package br.com.pablowinter;

import io.quarkus.runtime.Startup;
import jakarta.inject.Singleton;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Singleton
@Startup
public class MemoryDatabase {

    private final Set<String> apelidos = new HashSet<>(10_000);
    private final LinkedHashMap<String, Pessoa> pessoas = new LinkedHashMap<>(10_000);

    public boolean existsByApelido(String apelido) {
        return apelidos.contains(apelido);
    }

    public Pessoa getPessoa(String id) {
        return pessoas.get(id);
    }

    public synchronized void save(Pessoa pessoa) {
        pessoas.put(pessoa.getId(), pessoa);
        apelidos.add(pessoa.getApelido());
    }

    public List<Pessoa> findByTerm(String term) {
        return pessoas.values().stream()
                .filter(p -> p.getTerm().toLowerCase().contains(term.toLowerCase()))
                .limit(100)
                .toList();
    }

}
