package com.security.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

@Data
@NoArgsConstructor
@Entity
public class AppRole {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String roleName;

	public AppRole(Long id, String roleName) {
		this.id = id;
		this.roleName = roleName;
	}
}
