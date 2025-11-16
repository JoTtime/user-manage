package com.app.Harvest.Repository;

import com.app.Harvest.Entity.Farmer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmerRepository extends JpaRepository<Farmer, Long> {

    /**
     * Find all farmers belonging to a specific cooperative (paginated)
     */
    Page<Farmer> findByCooperativeId(Long cooperativeId, Pageable pageable);

    /**
     * Find all farmers belonging to a specific cooperative (non-paginated)
     */
    List<Farmer> findByCooperativeId(Long cooperativeId);

    /**
     * Find a farmer by ID and cooperative ID (ensures cooperative can only access their own farmers)
     */
    Optional<Farmer> findByIdAndCooperativeId(Long id, Long cooperativeId);

    /**
     * Find farmers by cooperative and status (paginated)
     */
    Page<Farmer> findByCooperativeIdAndStatus(Long cooperativeId, String status, Pageable pageable);

    /**
     * Find farmers by cooperative and status (non-paginated)
     */
    List<Farmer> findByCooperativeIdAndStatus(Long cooperativeId, String status);

    /**
     * Check if phone number exists for a specific cooperative
     */
    boolean existsByPhoneNumberAndCooperativeId(String phoneNumber, Long cooperativeId);

    /**
     * Check if full name exists for a specific cooperative
     */
    boolean existsByFullNameAndCooperativeId(String fullName, Long cooperativeId);

    /**
     * Check if QR code exists
     */
    boolean existsByQrCode(String qrCode);

    /**
     * Count total farmers for a cooperative
     */
    long countByCooperativeId(Long cooperativeId);

    /**
     * Count farmers by cooperative and status
     */
    long countByCooperativeIdAndStatus(Long cooperativeId, String status);

    /**
     * Sum total area (hectares) for all farmers in a cooperative
     * Returns 0.0 if no farmers exist or all area values are null
     */
    @Query("SELECT COALESCE(SUM(f.areaHa), 0.0) FROM Farmer f WHERE f.cooperative.id = :cooperativeId")
    Double sumAreaByCooperativeId(@Param("cooperativeId") Long cooperativeId);

    /**
     * Search farmers by name, phone, location, or crop within a cooperative (paginated)
     */
    @Query("SELECT f FROM Farmer f WHERE f.cooperative.id = :cooperativeId " +
            "AND (LOWER(f.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(f.phoneNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(f.location) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(f.crop) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Farmer> searchFarmersPaginated(@Param("cooperativeId") Long cooperativeId,
                                        @Param("searchTerm") String searchTerm,
                                        Pageable pageable);

    /**
     * Search farmers with status filter (paginated)
     */
    @Query("SELECT f FROM Farmer f WHERE f.cooperative.id = :cooperativeId " +
            "AND f.status = :status " +
            "AND (LOWER(f.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(f.phoneNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(f.location) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(f.crop) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Farmer> searchFarmersWithStatus(@Param("cooperativeId") Long cooperativeId,
                                         @Param("searchTerm") String searchTerm,
                                         @Param("status") String status,
                                         Pageable pageable);

    /**
     * Search farmers by name, phone, location, or crop within a cooperative (non-paginated)
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