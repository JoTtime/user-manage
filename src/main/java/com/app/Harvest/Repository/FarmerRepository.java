package com.app.Harvest.Repository;

import com.app.Harvest.Entity.Farmer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmerRepository extends JpaRepository<Farmer, Long> {

    /**
     * Find all farmers belonging to a specific cooperative
     */
    List<Farmer> findByCooperativeId(Long cooperativeId);

    /**
     * Find a farmer by ID and cooperative ID (ensures cooperative can only access their own farmers)
     */
    Optional<Farmer> findByIdAndCooperativeId(Long id, Long cooperativeId);

    /**
     * Find farmers by cooperative and status
     */
    List<Farmer> findByCooperativeIdAndStatus(Long cooperativeId, String status);

    /**
     * Check if phone number exists for a specific cooperative
     */
    boolean existsByPhoneNumberAndCooperativeId(String phoneNumber, Long cooperativeId);


    boolean existsByQrCode(String qrCode);

    /**
     * Count total farmers for a cooperative
     */
    long countByCooperativeId(Long cooperativeId);

    /**
     * Count active farmers for a cooperative
     */
    long countByCooperativeIdAndStatus(Long cooperativeId, String status);

    /**
     * Search farmers by name, phone, location, or crop within a cooperative
     */
    @Query("SELECT f FROM Farmer f WHERE f.cooperative.id = :cooperativeId " +
            "AND (LOWER(f.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(f.phoneNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(f.location) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(f.crop) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Farmer> searchFarmers(@Param("cooperativeId") Long cooperativeId,
                               @Param("searchTerm") String searchTerm);

    /**
     * Delete all farmers belonging to a cooperative
     */
    void deleteByCooperativeId(Long cooperativeId);
}