package com.app.Harvest.Repository;

import com.app.Harvest.Entity.Role;
import com.app.Harvest.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    List<User> findByIsApproved(Boolean isApproved);

    List<User> findByCooperativeId(Long cooperativeId);

    Optional<User> findByQrCode(String qrCode);

    Optional<User> findByRegistrationNumber(String registrationNumber);

    // ADD THESE TWO MISSING METHODS:
    List<User> findByRole(Role role);

    List<User> findByIsApprovedFalseAndRole(Role role);
}