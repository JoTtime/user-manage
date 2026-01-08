package com.app.Harvest.Repository;

import com.app.Harvest.Entity.Role;
import com.app.Harvest.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    List<User> findByRole(Role role);

    List<User> findByCooperativeId(Long cooperativeId);

    List<User> findByRoleAndIsApproved(Role role, Boolean isApproved);

    long countByRole(Role role);

    long countByRoleAndIsApproved(Role role, Boolean isApproved);

    // NEW METHODS FOR PASSWORD RESET
    Optional<User> findByResetToken(String resetToken);

    Optional<User> findByResetTokenAndResetTokenExpiryAfter(String resetToken, LocalDateTime now);
}