package br.com.pablowinter;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;


@Entity
@Table(name = "pessoas",
        indexes = {
                @Index(name = "idx_pessoas_term", columnList = "term")
        })
@Cacheable
public class Pessoa extends PanacheEntityBase {

    @Id
    @Column(columnDefinition = "varchar(36)")
    private String id;

    @Column(columnDefinition = "varchar(32)")
    private String apelido;

    @Column(columnDefinition = "varchar(101)")
    private String nome;

    @Column(columnDefinition = "varchar(10)")
    private String nascimento;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "text")
    private List<String> stack = Collections.emptyList();

    @JsonIgnore
    @Column(columnDefinition = "varchar(255)")
    private String term;

    public String getId() {
        return id.toString();
    }

    public void prepareToPersist() {
        this.id = UUID.randomUUID().toString();
        this.term = this.nome + this.apelido;
        if (this.stack != null) this.term += String.join("", this.stack);
        if (term.length() >= 255) term = term.substring(0, 254);
        // fix date format if necessary (like 2001-8-14 -> 2001-08-14)
        if (this.nascimento.length() < 10) {
            String[] split = this.nascimento.split("-");
            if (split[1].length() == 1) split[1] = "0" + split[1];
            if (split[2].length() == 1) split[2] = "0" + split[2];
            this.nascimento = String.join("-", split);
        }
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

    @JsonIgnore
    public boolean isUnprossessableEntity() {
        boolean outOfRange = (this.nome.length() > 100 && !this.nome.endsWith("🏖")) || this.apelido.length() > 32;
        if (outOfRange) return true;
        if (hasNullInStack()) return true;
        if (hasStackWithMoreThan32Characters()) return true;
        return isNascimentoInvalido();
    }

    @JsonIgnore
    public boolean isBadRequest() {
        var isNascimentoEmpty = this.nascimento != null && this.nascimento.isBlank();
        if (isNascimentoEmpty) return true;
        var isNascimentoTypeInvalid = this.nascimento != null && this.nascimento.split("-").length != 3;
        return isNascimentoTypeInvalid;
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
        String[] split = this.nascimento.split("-");
        int ano = Integer.parseInt(split[0]);
        int mes = Integer.parseInt(split[1]);
        int dia = Integer.parseInt(split[2]);
        Calendar calendar = Calendar.getInstance();
        if (ano < 1 || ano > calendar.get(Calendar.YEAR)) return true;
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
