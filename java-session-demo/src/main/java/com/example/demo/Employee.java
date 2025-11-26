package com.example.demo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Employee model class for deeply nested object testing.
 * Contains multiple levels of nesting:
 * - Level 1: Basic fields (id, name, title, salary)
 * - Level 2: Contact object (personal contact with nested Address)
 * - Level 3: Company object (which contains another Contact with nested
 * Address)
 * - Level 2: List of Address objects (previous addresses)
 */
public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String title;
    private Double salary;
    private Contact contact;
    private Company company;
    private List<Address> previousAddresses;

    public Employee() {
        this.previousAddresses = new ArrayList<>();
    }

    public Employee(Long id, String name, String title, Double salary,
            Contact contact, Company company) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.salary = salary;
        this.contact = contact;
        this.company = company;
        this.previousAddresses = new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Double getSalary() {
        return salary;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public List<Address> getPreviousAddresses() {
        return previousAddresses;
    }

    public void setPreviousAddresses(List<Address> previousAddresses) {
        this.previousAddresses = previousAddresses;
    }
}
