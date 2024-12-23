package com.security.service;

import com.security.entities.AppRole;
import com.security.entities.AppUser;
import com.security.repo.AppRoleRepository;
import com.security.repo.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class AccountService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final AppRoleRepository appRoleRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public AccountService(AppUserRepository appUserRepository, AppRoleRepository appRoleRepository) {
        this.appUserRepository = appUserRepository;
        this.appRoleRepository = appRoleRepository;
        this.bCryptPasswordEncoder = new BCryptPasswordEncoder();
    }

    public BCryptPasswordEncoder getBCryptPasswordEncoder() {
        return bCryptPasswordEncoder;
    }

    public void deleteUserByUsername(String username) {
        AppUser appUser = appUserRepository.findByUsername(username);
        if (appUser != null) {
            appUserRepository.delete(appUser);
        } else {
            throw new UsernameNotFoundException("User not found");
        }
    }

    public AppUser addNewUser(AppUser appUser) {
        String pw = appUser.getPassword();
        appUser.setPassword(bCryptPasswordEncoder.encode(pw));

        Set<AppRole> roles = new HashSet<>();

        if (appUser.getRoles() != null && !appUser.getRoles().isEmpty()) {
            for (AppRole role : appUser.getRoles()) {
                AppRole existingRole = appRoleRepository.findByRoleName(role.getRoleName());
                if (existingRole != null) {
                    roles.add(existingRole);
                } else {
                    System.out.println("Role not found: " + role.getRoleName());
                }
            }
        }

        appUser.setRoles(roles);

        return appUserRepository.save(appUser);
    }

    public AppUser updateUser(String username, AppUser updatedUser) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository.findByUsername(username);
        if (appUser == null) {
            throw new UsernameNotFoundException("User not found");
        }
        if (updatedUser.getUsername() != null && !updatedUser.getUsername().isEmpty()) {
            appUser.setUsername(updatedUser.getUsername());
        }
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            appUser.setPassword(bCryptPasswordEncoder.encode(updatedUser.getPassword()));
        }
        if (updatedUser.getRoles() != null && !updatedUser.getRoles().isEmpty()) {
            Set<AppRole> roles = new HashSet<>();
            for (AppRole role : updatedUser.getRoles()) {
                AppRole existingRole = appRoleRepository.findByRoleName(role.getRoleName());
                if (existingRole != null) {
                    roles.add(existingRole);
                } else {
                    System.out.println("Role not found: " + role.getRoleName());
                }
            }
            appUser.setRoles(roles);
        }

        return appUserRepository.save(appUser);
    }


    public AppRole addNewRole(AppRole role) {
        return appRoleRepository.save(role);
    }

    public void addRoleToUser(String username, String rolename) {
        AppUser appUser = appUserRepository.findByUsername(username);
        AppRole appRole = appRoleRepository.findByRoleName(rolename);
        if (appUser != null && appRole != null) {
            appUser.getRoles().add(appRole);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository.findByUsername(username);
        if (appUser == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return new User(appUser.getUsername(), appUser.getPassword(), getAuthorities(appUser.getRoles()));
    }

    public AppUser loadSimpleUserByUsername(String username) {
        return appUserRepository.findByUsername(username);
    }

    public Collection<GrantedAuthority> getAuthorities(Collection<AppRole> roles) {
        List<GrantedAuthority> list = new ArrayList<>();
        if (roles != null) {
            roles.forEach(r -> {
                list.add(new SimpleGrantedAuthority(r.getRoleName()));
            });
        }
        return list;
    }

    public List<AppUser> listUsers() {
        return appUserRepository.findAll();
    }
}