package com.example.demo;

import java.io.Serializable;

/**
 * Company model class for nested object testing.
 * Contains name, industry, and a nested Contact object.
 */
public class Company implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String industry;
    private Contact contact;

    public Company() {
    }

    public Company(String name, String industry, Contact contact) {
        this.name = name;
        this.industry = industry;
        this.contact = contact;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }
}
