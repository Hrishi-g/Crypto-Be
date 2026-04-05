package com.practice.firstapp.dto;

public class LoginReqDto {
    private String username;
    private String password;

    public LoginReqDto() {
    }

    public LoginReqDto(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
