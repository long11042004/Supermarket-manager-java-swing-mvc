package com.example.productmanager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.productmanager.model.RoleName;
import com.example.productmanager.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

	@Query("SELECT u FROM User u WHERE u.username = :username")
	Optional<User> findByUsername(@Param("username") String username);

	@Query("SELECT u FROM User u WHERE u.email = :email")
	Optional<User> findByEmail(@Param("email") String email);

	@Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username")
	boolean existsByUsername(@Param("username") String username);

	@Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email")
	boolean existsByEmail(@Param("email") String email);

	@Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY u.fullName ASC")
	List<User> searchByFullName(@Param("keyword") String keyword);

	@Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
	List<User> findByRoleName(@Param("roleName") RoleName roleName);
}
