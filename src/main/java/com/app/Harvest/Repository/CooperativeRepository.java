package com.app.Harvest.Repository;

import com.app.Harvest.Entity.Cooperative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CooperativeRepository extends JpaRepository<Cooperative, Long> {

    Optional<Cooperative> findByName(String name);

    Optional<Cooperative> findByRegistrationNumber(String registrationNumber);

    boolean existsByName(String name);

    boolean existsByRegistrationNumber(String registrationNumber);

    Optional<Cooperative> findByEmail(String email);
}