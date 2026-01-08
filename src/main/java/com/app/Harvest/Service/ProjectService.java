package com.app.Harvest.Service;

import com.app.Harvest.Entity.Farmer;
import com.app.Harvest.Entity.Project;
import com.app.Harvest.Repository.FarmerRepository;
import com.app.Harvest.Repository.ProjectRepository;
import com.app.Harvest.dto.request.ProjectRequest;
import com.app.Harvest.dto.response.ProjectResponse;
import com.app.Harvest.exception.BadRequestException;
import com.app.Harvest.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final FarmerRepository farmerRepository;

    /**
     * Map Project entity to ProjectResponse DTO
     */
    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .cropName(project.getCropName())
                .areaHa(project.getAreaHa())
                .status(project.getStatus())
                .plantingDate(project.getPlantingDate())
                .expectedHarvestDate(project.getExpectedHarvestDate())
                .notes(project.getNotes())
                .farmerId(project.getFarmer().getId())
                .farmerName(project.getFarmer().getFullName())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    /**
     * Get all projects for a specific farmer
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByFarmer(Long farmerId, Long cooperativeId) {
        log.info("Fetching projects for farmer ID: {}", farmerId);

        // Verify farmer exists and belongs to the cooperative
        Farmer farmer = farmerRepository.findByIdAndCooperativeId(farmerId, cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer not found with ID: " + farmerId));

        List<Project> projects = projectRepository.findByFarmerId(farmerId);

        return projects.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific project by ID
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long projectId, Long farmerId, Long cooperativeId) {
        log.info("Fetching project ID: {} for farmer ID: {}", projectId, farmerId);

        // Verify farmer exists and belongs to the cooperative
        Farmer farmer = farmerRepository.findByIdAndCooperativeId(farmerId, cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer not found with ID: " + farmerId));

        Project project = projectRepository.findByIdAndFarmerId(projectId, farmerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with ID: " + projectId));

        return mapToResponse(project);
    }

    /**
     * Create a new project for a farmer
     */
    public ProjectResponse createProject(ProjectRequest request, Long farmerId, Long cooperativeId) {
        log.info("Creating new project for farmer ID: {}", farmerId);

        // Get farmer (ensure it belongs to the cooperative)
        Farmer farmer = farmerRepository.findByIdAndCooperativeId(farmerId, cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer not found with ID: " + farmerId));

        // Calculate total allocated area for this farmer
        Double totalAllocated = projectRepository.sumAreaByFarmerId(farmerId);
        Double remainingArea = farmer.getAreaHa() - (totalAllocated != null ? totalAllocated : 0.0);

        // Validate that the project area doesn't exceed available area
        if (request.getAreaHa() > remainingArea) {
            throw new BadRequestException(
                    String.format("Project area (%.2f ha) exceeds remaining available area (%.2f ha). " +
                                    "Total farm area: %.2f ha, Already allocated: %.2f ha",
                            request.getAreaHa(), remainingArea, farmer.getAreaHa(), totalAllocated));
        }

        // Create project entity
        Project project = Project.builder()
                .cropName(request.getCropName().trim())
                .areaHa(request.getAreaHa())
                .status(request.getStatus() != null ? request.getStatus() : "active")
                .plantingDate(request.getPlantingDate())
                .expectedHarvestDate(request.getExpectedHarvestDate())
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .farmer(farmer)
                .build();

        Project savedProject = projectRepository.save(project);
        log.info("Project created successfully with ID: {}", savedProject.getId());

        return mapToResponse(savedProject);
    }

    /**
     * Update an existing project
     */
    public ProjectResponse updateProject(Long projectId, ProjectRequest request, Long farmerId, Long cooperativeId) {
        log.info("Updating project ID: {} for farmer ID: {}", projectId, farmerId);

        // Verify farmer exists and belongs to the cooperative
        Farmer farmer = farmerRepository.findByIdAndCooperativeId(farmerId, cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer not found with ID: " + farmerId));

        // Get project
        Project project = projectRepository.findByIdAndFarmerId(projectId, farmerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with ID: " + projectId));

        // Calculate current allocated area excluding this project
        Double totalAllocated = projectRepository.sumAreaByFarmerId(farmerId);
        Double currentAllocatedExcludingThisProject = totalAllocated - project.getAreaHa();
        Double remainingArea = farmer.getAreaHa() - currentAllocatedExcludingThisProject;

        // If area is being changed, validate it doesn't exceed available area
        if (!project.getAreaHa().equals(request.getAreaHa())) {
            if (request.getAreaHa() > remainingArea) {
                throw new BadRequestException(
                        String.format("Updated project area (%.2f ha) exceeds available area (%.2f ha). " +
                                        "Current area: %.2f ha, Total allocated: %.2f ha",
                                request.getAreaHa(), remainingArea, project.getAreaHa(), totalAllocated));
            }
        }

        // Update project details
        project.setCropName(request.getCropName().trim());
        project.setAreaHa(request.getAreaHa());
        project.setPlantingDate(request.getPlantingDate());
        project.setExpectedHarvestDate(request.getExpectedHarvestDate());
        project.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);

        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }

        Project updatedProject = projectRepository.save(project);
        log.info("Project updated successfully with ID: {}", updatedProject.getId());

        return mapToResponse(updatedProject);
    }

    /**
     * Delete a project
     */
    public void deleteProject(Long projectId, Long farmerId, Long cooperativeId) {
        log.info("Deleting project ID: {} for farmer ID: {}", projectId, farmerId);

        // Verify farmer exists and belongs to the cooperative
        Farmer farmer = farmerRepository.findByIdAndCooperativeId(farmerId, cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer not found with ID: " + farmerId));

        // Get project
        Project project = projectRepository.findByIdAndFarmerId(projectId, farmerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with ID: " + projectId));

        projectRepository.delete(project);

        // Recalculate farmer's allocated area
        Double newTotalAllocated = projectRepository.sumAreaByFarmerId(farmerId);
        log.info("After deleting project, farmer ID: {} has total allocated area: {} ha",
                farmerId, newTotalAllocated);

        log.info("Project deleted successfully with ID: {}", projectId);
    }

    /**
     * Update project status
     */
    public ProjectResponse updateProjectStatus(Long projectId, String status, Long farmerId, Long cooperativeId) {
        log.info("Updating status for project ID: {} to {}", projectId, status);

        // Validate status
        List<String> validStatuses = List.of("active", "completed", "planned", "planning", "harvesting");
        if (!validStatuses.contains(status)) {
            throw new BadRequestException("Invalid status. Must be one of: " + String.join(", ", validStatuses));
        }

        // Verify farmer exists and belongs to the cooperative
        Farmer farmer = farmerRepository.findByIdAndCooperativeId(farmerId, cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer not found with ID: " + farmerId));

        // Get project
        Project project = projectRepository.findByIdAndFarmerId(projectId, farmerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with ID: " + projectId));

        project.setStatus(status);
        Project updatedProject = projectRepository.save(project);

        log.info("Project status updated successfully");
        return mapToResponse(updatedProject);
    }

    /**
     * Get total allocated area for a farmer
     */
    @Transactional(readOnly = true)
    public Double getTotalAllocatedArea(Long farmerId, Long cooperativeId) {
        log.info("Getting total allocated area for farmer ID: {}", farmerId);

        // Verify farmer exists and belongs to the cooperative
        Farmer farmer = farmerRepository.findByIdAndCooperativeId(farmerId, cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer not found with ID: " + farmerId));

        Double totalAllocated = projectRepository.sumAreaByFarmerId(farmerId);
        return totalAllocated != null ? totalAllocated : 0.0;
    }
}