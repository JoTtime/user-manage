package com.app.Harvest.Repository;

import java.util.Optional;

import com.app.Harvest.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import com.app.Harvest.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
