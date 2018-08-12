package com.forcetower.sagres.database.model;

import com.google.gson.annotations.SerializedName;

public class Person {
    private long id;
    @SerializedName(value = "nome")
    private String name;
    @SerializedName(value = "nomeExibicao")
    private String exhibitionName;
    private String cpf;
    private String email;

    public Person(long id, String name, String exhibitionName, String cpf, String email) {
        this.id = id;
        this.name = name;
        this.exhibitionName = exhibitionName;
        this.cpf = cpf;
        this.email = email;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        name = name.trim();
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExhibitionName() {
        return exhibitionName;
    }

    public void setExhibitionName(String exhibitionName) {
        this.exhibitionName = exhibitionName;
    }

    public String getCpf() {
        cpf = cpf.trim();
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "ID: " + getId() + " - Name: " + getName();
    }
}
