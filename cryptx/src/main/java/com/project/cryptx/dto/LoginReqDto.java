package com.project.cryptx.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class LoginReqDto {
    @JsonAlias({ "username", "email" })
    private String email;
    private String password;

    public LoginReqDto() {
    }

    public LoginReqDto(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
