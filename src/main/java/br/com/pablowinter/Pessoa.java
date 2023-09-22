package br.com.pablowinter;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Entity
@Table(name = "pessoas",
        indexes = {
                @Index(name = "idx_pessoas_term", columnList = "term")
        })
@Cacheable
public class Pessoa extends PanacheEntityBase {

    @Id
    private String id;

    private String apelido;

    private String nome;

    private String nascimento;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "text")
    private List<String> stack = Collections.emptyList();

    private String term;

    public String getId() {
        return id.toString();
    }

    public void prepareToPersist() {
        this.id = UUID.randomUUID().toString();
        this.term = this.nome + this.apelido;
        if (this.stack != null) this.term += String.join("", this.stack);
        if (term.length() >= 255) term = term.substring(0, 254);
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getApelido() {
        return apelido;
    }

    public void setApelido(String apelido) {
        this.apelido = apelido;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNascimento() {
        return nascimento;
    }

    public void setNascimento(String nascimento) {
        this.nascimento = nascimento;
    }

    public List<String> getStack() {
        return stack;
    }

    public void setStack(List<String> stack) {
        this.stack = stack;
    }

    @JsonbTransient
    public boolean isUnprossessableEntity() {
        boolean empty = this.nome == null || this.apelido == null || this.apelido.isBlank();
        if (empty) return true;
        boolean outOfRange = this.nome.length() > 100 || this.apelido.length() > 32;
        if (outOfRange) return true;
        if (hasNullInStack()) return true;
        if (hasStackWithMoreThan32Characters()) return true;
        return isNascimentoInvalido();
    }

    private boolean hasNullInStack() {
        if (stack == null) return false;
        return stack.stream().anyMatch(Objects::isNull);
    }

    private boolean hasStackWithMoreThan32Characters() {
        if (stack == null) return false;
        return stack.stream().anyMatch(s -> s != null && s.length() > 32);
    }

    private boolean isNascimentoInvalido() {
        if (this.nascimento == null || this.nascimento.isBlank()) return false;
        String[] split = this.nascimento.split("-");
        if (split.length != 3) return true;
        int ano = Integer.parseInt(split[0]);
        int mes = Integer.parseInt(split[1]);
        int dia = Integer.parseInt(split[2]);
        if (ano < 1900 || ano > 2021) return true;
        if (mes < 1 || mes > 12) return true;
        if (dia < 1 || dia > 31) return true;
        if (mes == 2 && dia > 29) return true;
        if (mes == 4 && dia > 30) return true;
        if (mes == 6 && dia > 30) return true;
        if (mes == 9 && dia > 30) return true;
        if (!yearFebruaryHas29Days(ano) && mes == 2 && dia > 28) return true;
        return mes == 11 && dia > 30;
    }

    private boolean yearFebruaryHas29Days(int ano) {
        return ano % 4 == 0 && ano % 100 != 0 || ano % 400 == 0;
    }

}
