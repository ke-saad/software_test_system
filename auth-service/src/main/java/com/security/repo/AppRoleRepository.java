package com.security.repo;

import com.security.entities.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppRoleRepository extends JpaRepository<AppRole, Long> {

	AppRole findByRoleName(String roleName);
}
