package com.edgareldy.springjdbctutorial.ws.payload.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Body of the create and update customer calls. Optional fields pass when null or empty, the service turns blank ones into null.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class CustomerRequest {

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @Size(max = 30)
    private String telephone;

    // @Email accepts null and the empty string, so an absent or empty email is not a validation error
    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 255)
    private String address;

    public CustomerRequest() {
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CustomerRequest that = (CustomerRequest) o;
        return Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName)
                && Objects.equals(telephone, that.telephone) && Objects.equals(email, that.email)
                && Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, telephone, email, address);
    }

    @Override
    public String toString() {
        return "CustomerRequest{" +
                "firstName=" + firstName +
                ", lastName=" + lastName +
                ", telephone=" + telephone +
                ", email=" + email +
                ", address=" + address +
                "}";
    }
}
