package com.firstProject.authProject.repository;

import com.firstProject.authProject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;


public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    @Query("select u from User u left join fetch u.roles where u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") UUID id);
}