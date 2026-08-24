package com.example.productmanager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.productmanager.entity.Role;
import com.example.productmanager.entity.RoleName;

public interface RoleRepository extends JpaRepository<Role, Long> {

	@Query("SELECT r FROM Role r WHERE r.name = :name")
	Optional<Role> findByName(@Param("name") RoleName name);

	@Query("SELECT r FROM Role r WHERE LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY r.name ASC")
	List<Role> searchByDescription(@Param("keyword") String keyword);

	@Query("SELECT r FROM Role r WHERE r.name IN :names ORDER BY r.name ASC")
	List<Role> findByNames(@Param("names") List<RoleName> names);
}
