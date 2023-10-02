package br.com.pablowinter;

import io.quarkus.runtime.Startup;
import jakarta.inject.Singleton;

import java.util.LinkedHashMap;

@Singleton
@Startup
public class MemoryDatabase {

    private final LinkedHashMap<String, Pessoa> pessoas = new LinkedHashMap<>(100_000);

    public Pessoa findById(String id) {
        return pessoas.get(id);
    }

    public void save(Pessoa pessoa) {
        pessoas.put(pessoa.getId(), pessoa);
    }

}