package com.example.productmanager.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.productmanager.model.UserActivity;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

	@Query("SELECT ua FROM UserActivity ua WHERE ua.user.id = :userId ORDER BY ua.createdAt DESC")
	List<UserActivity> findRecentActivitiesByUserId(@Param("userId") Long userId, Pageable pageable);
}