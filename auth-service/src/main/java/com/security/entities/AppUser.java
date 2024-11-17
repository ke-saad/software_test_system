package com.security.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@Entity
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String username;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<AppRole> roles = new HashSet<>();

    public AppUser(String username, String password, Set<AppRole> roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
    }

    public AppUser(Long id, String username, String password, Set<AppRole> roles) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.roles = roles != null ? roles : new HashSet<>();
    }

    public AppUser(String username, String password) {
    }
}
