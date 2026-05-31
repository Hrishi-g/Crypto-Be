package com.project.cryptx.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class LoginReqDto {
    @JsonAlias({ "username", "email" })
    private String identifier;
    private String password;

    public LoginReqDto() {
    }

    public LoginReqDto(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
