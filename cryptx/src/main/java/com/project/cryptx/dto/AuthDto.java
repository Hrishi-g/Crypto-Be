package com.project.cryptx.dto;

import java.util.List;

public class AuthDto {
    private Long id;
    private List<String> role;

    public AuthDto(Long id, List<String> role) {
        this.id = id;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<String> getRole() {
        return role;
    }

    public void setRole(List<String> role) {
        this.role = role;
    }
}
