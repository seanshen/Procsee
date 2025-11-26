package com.example.demo;

import java.io.Serializable;

/**
 * Contact model class for nested object testing.
 * Contains phone, email, and a nested Address object.
 */
public class Contact implements Serializable {
    private static final long serialVersionUID = 1L;

    private String phone;
    private String email;
    private Address address;

    public Contact() {
    }

    public Contact(String phone, String email, Address address) {
        this.phone = phone;
        this.email = email;
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}
