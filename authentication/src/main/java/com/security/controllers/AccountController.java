package com.security.controllers;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.security.entities.AppRole;
import com.security.entities.AppUser;
import com.security.entities.UserRoleForm;
import com.security.service.AccountService;
import com.security.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AccountController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    public AccountService accountService;

    @GetMapping(path = "/users")
    @PostAuthorize("hasAuthority('USER')")
    public List<AppUser> appUsers() {
        return accountService.listUsers();
    }

    @GetMapping(path = "/users/{username}")
    @PostAuthorize("hasAuthority('USER')")
    public AppUser getUserByUsername(@PathVariable String username) {
        return accountService.loadSimpleUserByUsername(username);
    }

    @DeleteMapping(path = "/users/{username}")
    @PostAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable String username) {
        try {
            accountService.deleteUserByUsername(username);
            return ResponseEntity.ok("User deleted successfully");
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }



    @PostMapping(path = "/addUser")
    @PostAuthorize("hasAuthority('ADMIN')")
    public AppUser saveUser(@RequestBody AppUser appUser) {
        return accountService.addNewUser(appUser);
    }

    @PutMapping(path = "/users/updateUser/{username}")
    @PostAuthorize("hasAuthority('ADMIN')")
    public AppUser updateUser(@PathVariable String username, @RequestBody AppUser updateAppUser) {
        return accountService.updateUser(username, updateAppUser);
    }

    @PostMapping(path = "/addRole")
    public AppRole saveRole(@RequestBody AppRole appRole) {
        return accountService.addNewRole(appRole);
    }

    @PostMapping(path = "/addRoleToUser")
    public void addRoleToUser(@RequestBody UserRoleForm userRoleForm) {
        accountService.addRoleToUser(userRoleForm.getUser(), userRoleForm.getRole());
    }

    @GetMapping(path = "/refreshToken")
    public void refresh(HttpServletRequest request, HttpServletResponse response) {
        String authToken = request.getHeader("Authorization");

        if (authToken == null || !authToken.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String refreshToken = authToken.substring(7);

        try {
            DecodedJWT decodedJwt = jwtUtil.verifySignature(refreshToken);

            if (decodedJwt != null) {
                String userName = jwtUtil.getUsername(decodedJwt);
                User user = (User) accountService.loadUserByUsername(userName);

                if (user != null) {
                    String accessToken = jwtUtil.generateAccessToken(user);
                    Map<String, String> idToken = new HashMap<>();
                    idToken.put("refresh-token", refreshToken);
                    idToken.put("access-token", accessToken);

                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    new ObjectMapper().writeValue(response.getOutputStream(), idToken);
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                }
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }

        } catch (JWTVerificationException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
