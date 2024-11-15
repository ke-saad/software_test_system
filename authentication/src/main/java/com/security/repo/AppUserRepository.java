package com.security.repo;

import com.security.entities.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
	AppUser findByUsername(String username);
}
