package com.security;

import com.security.entities.AppRole;
import com.security.entities.AppUser;
import com.security.service.AccountService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashSet;

@SpringBootApplication
public class Authentication {

    public static void main(String[] args) {
        SpringApplication.run(Authentication.class, args);
    }

    @Bean
    CommandLineRunner start(AccountService accountService, BCryptPasswordEncoder passwordEncoder) {
        return args -> {
            accountService.addNewRole(new AppRole(null, "USER"));
            accountService.addNewRole(new AppRole(null, "ADMIN"));

            accountService.addNewUser(new AppUser(null, "user1", "1234", new HashSet<>()));
            accountService.addNewUser(new AppUser(null, "user2", "1234", new HashSet<>()));
            accountService.addNewUser(new AppUser(null, "user3", "1234", new HashSet<>()));
            accountService.addNewUser(new AppUser(null, "user4", "1234", new HashSet<>()));
            accountService.addNewUser(new AppUser(null, "admin", "1234", new HashSet<>()));

            accountService.addRoleToUser("user1", "USER");
            accountService.addRoleToUser("admin", "ADMIN");
            accountService.addRoleToUser("admin", "USER");
            accountService.addRoleToUser("user2", "USER");
            accountService.addRoleToUser("user3", "USER");
            accountService.addRoleToUser("user4", "USER");
        };
    }
}