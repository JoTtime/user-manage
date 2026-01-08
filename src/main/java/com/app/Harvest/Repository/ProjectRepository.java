package com.app.Harvest.Repository;

import com.app.Harvest.Entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Find all projects for a specific farmer
    List<Project> findByFarmerId(Long farmerId);

    // Find a project by ID and farmer ID
    Optional<Project> findByIdAndFarmerId(Long projectId, Long farmerId);

    // Find projects by farmer and status
    List<Project> findByFarmerIdAndStatus(Long farmerId, String status);

    // Calculate total allocated area for a farmer
    @Query("SELECT COALESCE(SUM(p.areaHa), 0.0) FROM Project p WHERE p.farmer.id = :farmerId")
    Double sumAreaByFarmerId(@Param("farmerId") Long farmerId);

    // Count projects for a farmer
    long countByFarmerId(Long farmerId);

    // Count projects by status for a farmer
    long countByFarmerIdAndStatus(Long farmerId, String status);

    // Find all projects for a cooperative (through farmer relationship)
    @Query("SELECT p FROM Project p WHERE p.farmer.cooperative.id = :cooperativeId")
    List<Project> findByCooperativeId(@Param("cooperativeId") Long cooperativeId);

    // Add this method to ProjectRepository.java
    @Query("SELECT COALESCE(SUM(p.areaHa), 0.0) FROM Project p WHERE p.farmer.cooperative.id = :cooperativeId")
    Double sumAllocatedAreaByCooperativeId(@Param("cooperativeId") Long cooperativeId);

    // Find projects by crop name for a cooperative
    @Query("SELECT p FROM Project p WHERE p.farmer.cooperative.id = :cooperativeId AND LOWER(p.cropName) LIKE LOWER(CONCAT('%', :cropName, '%'))")
    List<Project> findByCropNameAndCooperativeId(@Param("cropName") String cropName, @Param("cooperativeId") Long cooperativeId);

    // Delete all projects for a farmer
    void deleteByFarmerId(Long farmerId);
}