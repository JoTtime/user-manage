package com.app.Harvest.Controller;

import com.app.Harvest.Service.ProjectService;
import com.app.Harvest.dto.request.ProjectRequest;
import com.app.Harvest.dto.response.ApiResponse;
import com.app.Harvest.dto.response.ProjectResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cooperatives/{cooperativeId}/farmers/{farmerId}/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Get all projects for a farmer
     * GET /api/v1/cooperatives/{cooperativeId}/farmers/{farmerId}/projects
     */
    @GetMapping
    @PreAuthorize("hasRole('COOPERATIVE')")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getAllProjects(
            @PathVariable Long cooperativeId,
            @PathVariable Long farmerId) {

        log.info("REST request to get all projects for farmer: {}", farmerId);

        List<ProjectResponse> projects = projectService.getProjectsByFarmer(farmerId, cooperativeId);

        return ResponseEntity.ok(ApiResponse.<List<ProjectResponse>>builder()
                .success(true)
                .message("Projects retrieved successfully")
                .data(projects)
                .build());
    }

    /**
     * Get a specific project by ID
     * GET /api/v1/cooperatives/{cooperativeId}/farmers/{farmerId}/projects/{projectId}
     */
    @GetMapping("/{projectId}")
    @PreAuthorize("hasRole('COOPERATIVE')")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(
            @PathVariable Long cooperativeId,
            @PathVariable Long farmerId,
            @PathVariable Long projectId) {

        log.info("REST request to get project: {} for farmer: {}", projectId, farmerId);

        ProjectResponse project = projectService.getProjectById(projectId, farmerId, cooperativeId);

        return ResponseEntity.ok(ApiResponse.<ProjectResponse>builder()
                .success(true)
                .message("Project retrieved successfully")
                .data(project)
                .build());
    }

    /**
     * Create a new project for a farmer
     * POST /api/v1/cooperatives/{cooperativeId}/farmers/{farmerId}/projects
     */
    @PostMapping
    @PreAuthorize("hasRole('COOPERATIVE')")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @PathVariable Long cooperativeId,
            @PathVariable Long farmerId,
            @Valid @RequestBody ProjectRequest request) {

        log.info("REST request to create project for farmer: {}", farmerId);

        ProjectResponse createdProject = projectService.createProject(request, farmerId, cooperativeId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ProjectResponse>builder()
                        .success(true)
                        .message("Project created successfully")
                        .data(createdProject)
                        .build());
    }

    /**
     * Update an existing project
     * PUT /api/v1/cooperatives/{cooperativeId}/farmers/{farmerId}/projects/{projectId}
     */
    @PutMapping("/{projectId}")
    @PreAuthorize("hasRole('COOPERATIVE')")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable Long cooperativeId,
            @PathVariable Long farmerId,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectRequest request) {

        log.info("REST request to update project: {} for farmer: {}", projectId, farmerId);

        ProjectResponse updatedProject = projectService.updateProject(projectId, request, farmerId, cooperativeId);

        return ResponseEntity.ok(ApiResponse.<ProjectResponse>builder()
                .success(true)
                .message("Project updated successfully")
                .data(updatedProject)
                .build());
    }

    /**
     * Delete a project
     * DELETE /api/v1/cooperatives/{cooperativeId}/farmers/{farmerId}/projects/{projectId}
     */
    @DeleteMapping("/{projectId}")
    @PreAuthorize("hasRole('COOPERATIVE')")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable Long cooperativeId,
            @PathVariable Long farmerId,
            @PathVariable Long projectId) {

        log.info("REST request to delete project: {} for farmer: {}", projectId, farmerId);

        projectService.deleteProject(projectId, farmerId, cooperativeId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Project deleted successfully")
                .build());
    }

    /**
     * Update project status
     * PATCH /api/v1/cooperatives/{cooperativeId}/farmers/{farmerId}/projects/{projectId}/status
     */
    @PatchMapping("/{projectId}/status")
    @PreAuthorize("hasRole('COOPERATIVE')")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProjectStatus(
            @PathVariable Long cooperativeId,
            @PathVariable Long farmerId,
            @PathVariable Long projectId,
            @RequestParam String status) {

        log.info("REST request to update status of project: {} to: {} for farmer: {} in cooperative: {}",
                projectId, status, farmerId, cooperativeId);

        // Add validation
        if (status == null || status.trim().isEmpty()) {
            log.error("Status parameter is empty or null for project: {}", projectId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<ProjectResponse>builder()
                            .success(false)
                            .message("Status parameter is required and cannot be empty")
                            .build());
        }

        try {
            ProjectResponse updatedProject = projectService.updateProjectStatus(projectId, status, farmerId, cooperativeId);

            if (updatedProject == null) {
                log.error("Project not found: {} for farmer: {} in cooperative: {}",
                        projectId, farmerId, cooperativeId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<ProjectResponse>builder()
                                .success(false)
                                .message("Project not found")
                                .build());
            }

            log.info("Successfully updated project {} status to: {}", projectId, updatedProject.getStatus());

            return ResponseEntity.ok(ApiResponse.<ProjectResponse>builder()
                    .success(true)
                    .message("Project status updated successfully")
                    .data(updatedProject)
                    .build());

        } catch (Exception e) {
            log.error("Error updating project status for project: {}", projectId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<ProjectResponse>builder()
                            .success(false)
                            .message("Failed to update project status: " + e.getMessage())
                            .build());
        }
    }
}