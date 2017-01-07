package com.example.models;

import java.util.Date;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class Pet {

    private String name;
    private String type;
    private Date birthDate;
    @Min(1)
    private int legs;

    public Pet(String name) {
        this.name = name;
    }

    public int getLegs() {
        return legs;
    }

    public void setLegs(int legs) {
        this.legs = legs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }
}
