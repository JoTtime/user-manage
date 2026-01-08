package com.app.Harvest.Controller;

import com.app.Harvest.Entity.Farmer;
import com.app.Harvest.Entity.User;
import com.app.Harvest.Repository.FarmerRepository;
import com.app.Harvest.Repository.UserRepository;
import com.app.Harvest.Service.FarmerService;
import com.app.Harvest.dto.request.BulkImportRequest;
import com.app.Harvest.dto.request.FarmerRequest;
import com.app.Harvest.dto.response.ApiResponse;
import com.app.Harvest.dto.response.BulkImportResponse;
import com.app.Harvest.dto.response.FarmerResponse;
import com.app.Harvest.dto.response.PagedResponse;
import com.app.Harvest.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cooperative/farmers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200", "http://localhost:8081"})
public class FarmerController {

    private final FarmerService farmerService;
    private final UserRepository userRepository;
    private final FarmerRepository farmerRepository;

    // ============================================================================
    // AUTHENTICATED ENDPOINTS (Require JWT Token)
    // ============================================================================

    /**
     * Get all farmers for the logged-in cooperative (with pagination, filtering, and sorting)
     * GET /api/cooperative/farmers?page=0&size=10&sortBy=name&sortOrder=asc&status=all&search=
     */
    @GetMapping
    @PreAuthorize("hasRole('COOPERATIVE')")
    public ResponseEntity<ApiResponse<PagedResponse<FarmerResponse>>> getAllFarmers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "") String search,
            Authentication authentication) {

        log.info("GET /api/cooperative/farmers - Page: {}, Size: {}, Sort: {} {}, Status: {}, Search: '{}'",
                page, size, sortBy, sortOrder, status, search);

        Long cooperativeId = getCooperativeIdFromAuth(authentication);
        PagedResponse<FarmerResponse> pagedFarmers = farmerService.getAllFarmersByCooperative(
                cooperativeId, page, size, sortBy, sortOrder, status, search);

        ApiResponse<PagedResponse<FarmerResponse>> response = ApiResponse.<PagedResponse<FarmerResponse>>builder()
                .success(true)
                .message("Farmers retrieved successfully")
                .data(pagedFarmers)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get all farmers without pagination (for backward compatibility or exports)
     * GET /api/cooperative/farmers/all
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('COOPERATIVE')")
    public ResponseEntity<ApiResponse<List<FarmerResponse>>> getAllFarmersNonPaginated(
            Authentication authentication) {

        log.info("GET /api/cooperative/farmers/all - Fetching all farmers (non-paginated)");

        Long cooperativeId = getCooperativeIdFromAuth(authentication);

        // Get all farmers by calling the paginated method with a large page size
        PagedResponse<FarmerResponse> pagedFarmers = farmerService.getAllFarmersByCooperative(
                cooperativeId, 0, Integer.MAX_VALUE, "name", "asc", "all", "");

        ApiResponse<List<FarmerResponse>> response = ApiResponse.<List<FarmerResponse>>builder()
                .success(true)
                .message("Farmers retrieved successfully")
                .data(pagedFarmers.getContent())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific farmer by ID
     * GET /api/cooperative/farmers/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('COOPERATIVE')")
    public ResponseEntity<ApiResponse<FarmerResponse>> getFarmerById(
            @PathVariable Long id,
            Authentication authentication) {

        log.info("GET /api/cooperative/farmers/{} - Fetching farmer", id);

        Long cooperativeId = getCooperativeIdFromAuth(authentication);
        FarmerResponse farmer = farmerService.getFarmerById(id, cooperativeId);

        ApiResponse<FarmerResponse> response = ApiResponse.<FarmerResponse>builder()
                .success(true)
                .message("Farmer retrieved successfully")
                .data(farmer)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Create a new farmer
     * POST /api/cooperative/farmers
     */
    @PostMapping
    @PreAuthorize("hasRole('COOPERATIVE')")
    public ResponseEntity<ApiResponse<FarmerResponse>> createFarmer(
            @Valid @RequestBody FarmerRequest request,
            Authentication authentication) {

        log.info("POST /api/cooperative/farmers - Creating new farmer: {}", request.getFullName());

        Long cooperativeId = getCooperativeIdFromAuth(authentication);
        FarmerResponse farmer = farmerService.createFarmer(request, cooperativeId);

        ApiResponse<FarmerResponse> response = ApiResponse.<FarmerResponse>builder()
                .success(true)
                .message("Farmer created successfully")
                .data(farmer)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Bulk import farmers from Excel/CSV
     * POST /api/cooperative/farmers/bulk-import
     */
    @PostMapping("/bulk-import")
    @PreAuthorize("hasRole('COOPERATIVE')")
    public ResponseEntity<ApiResponse<BulkImportResponse>> bulkImportFarmers(
            @Valid @RequestBody BulkImportRequest request,
            Authentication authentication) {

        log.info("POST /api/cooperative/farmers/bulk-import - Importing {} farmers",
                request.getFarmers().size());

        Long cooperativeId = getCooperativeIdFromAuth(authentication);
        BulkImportResponse result = farmerService.bulkImportFarmers(
                request.getFarmers(), cooperativeId);

        String message = String.format(
                "Import completed. Success: %d, Failed: %d",
                result.getSuccessCount(),
                result.getFailureCount()
        );

        ApiResponse<BulkImportResponse> response = ApiResponse.<BulkImportResponse>builder()
                .success(true)
                .message(message)
                .data(result)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing farmer
     * PUT /api/cooperative/farmers/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COOPERATIVE')")
    public ResponseEntity<ApiResponse<FarmerResponse>> updateFarmer(
            @PathVariable Long id,
            @Valid @RequestBody FarmerRequest request,
            Authentication authentication) {

        log.info("PUT /api/cooperative/farmers/{} - Updating farmer", id);

        Long cooperativeId = getCooperativeIdFromAuth(authentication);
        FarmerResponse farmer = farmerService.updateFarmer(id, request, cooperativeId);

        ApiResponse<FarmerResponse> response = ApiResponse.<FarmerResponse>builder()
                .success(true)
                .message("Farmer updated successfully")
                .data(farmer)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a farmer
     * DELETE /api/cooperative/farmers/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COOPERATIVE')")
    public ResponseEntity<ApiResponse<Void>> deleteFarmer(
            @PathVariable Long id,
            Authentication authentication) {

        log.info("DELETE /api/cooperative/farmers/{} - Deleting farmer", id);

        Long cooperativeId = getCooperativeIdFromAuth(authentication);
        farmerService.deleteFarmer(id, cooperativeId);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Farmer deleted successfully")
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Update farmer status
     * PATCH /api/cooperative/farmers/{id}/status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('COOPERATIVE')")
    public ResponseEntity<ApiResponse<FarmerResponse>> updateFarmerStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication) {

        log.info("PATCH /api/cooperative/farmers/{}/status - Updating status to {}", id, status);

        Long cooperativeId = getCooperativeIdFromAuth(authentication);
        FarmerResponse farmer = farmerService.updateFarmerStatus(id, status, cooperativeId);

        ApiResponse<FarmerResponse> response = ApiResponse.<FarmerResponse>builder()
                .success(true)
                .message("Farmer status updated successfully")
                .data(farmer)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Search farmers (deprecated - use GET /farmers with search parameter instead)
     * GET /api/cooperative/farmers/search?q={searchTerm}
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('COOPERATIVE')")
    public ResponseEntity<ApiResponse<List<FarmerResponse>>> searchFarmers(
            @RequestParam String q,
            Authentication authentication) {

        log.info("GET /api/cooperative/farmers/search?q={} - Searching farmers", q);

        Long cooperativeId = getCooperativeIdFromAuth(authentication);

        // Use paginated search with large page size for backward compatibility
        PagedResponse<FarmerResponse> pagedFarmers = farmerService.getAllFarmersByCooperative(
                cooperativeId, 0, 1000, "name", "asc", "all", q);

        ApiResponse<List<FarmerResponse>> response = ApiResponse.<List<FarmerResponse>>builder()
                .success(true)
                .message("Search completed successfully")
                .data(pagedFarmers.getContent())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get farmer statistics
     * GET /api/cooperative/farmers/statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('COOPERATIVE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFarmerStatistics(
            Authentication authentication) {

        log.info("GET /api/cooperative/farmers/statistics - Fetching statistics");

        Long cooperativeId = getCooperativeIdFromAuth(authentication);
        Map<String, Object> stats = farmerService.getFarmerStatistics(cooperativeId);

        ApiResponse<Map<String, Object>> response =
                ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Statistics retrieved successfully")
                        .data(stats)
                        .build();

        return ResponseEntity.ok(response);
    }

    // ============================================================================
    // MICROSERVICE ENDPOINTS (No JWT Required - Called by Announcement Service)
    // ============================================================================

    /**
     * Get multiple farmers by IDs (for batch operations from Announcement service)
     * POST /api/cooperative/farmers/batch
     * Body: [10, 11, 12]
     * Header: X-Cooperative-Id: 5
     */
    @PostMapping("/batch")
    public ResponseEntity<List<FarmerResponse>> getFarmersByIds(
            @RequestBody List<Long> farmerIds,
            @RequestHeader("X-Cooperative-Id") Long cooperativeId) {

        log.info("🔗 MICROSERVICE CALL - Batch request for {} farmers from cooperative {}",
                farmerIds.size(), cooperativeId);

        // Get all farmers by IDs
        List<Farmer> farmers = farmerRepository.findAllById(farmerIds);

        // Filter only farmers belonging to the specified cooperative
        // This ensures cooperatives can only access their own farmers
        List<FarmerResponse> responses = farmers.stream()
                .filter(f -> f.getCooperative().getId().equals(cooperativeId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        log.info("✅ Returning {} farmers for cooperative {}", responses.size(), cooperativeId);

        return ResponseEntity.ok(responses);
    }

    /**
     * Get farmers by location (for targeted announcements)
     * GET /api/cooperative/farmers/by-location?cooperativeId=5&location=Yaoundé
     */
    @GetMapping("/by-location")
    public ResponseEntity<List<FarmerResponse>> getFarmersByLocation(
            @RequestParam Long cooperativeId,
            @RequestParam String location) {

        log.info("🔗 MICROSERVICE CALL - Getting farmers by location: {} for cooperative {}",
                location, cooperativeId);

        // Search farmers by location
        List<Farmer> farmers = farmerRepository.searchFarmers(cooperativeId, location);

        List<FarmerResponse> responses = farmers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        log.info("✅ Found {} farmers in location: {}", responses.size(), location);

        return ResponseEntity.ok(responses);
    }

    /**
     * Search farmers with multiple criteria (for Announcement service)
     * GET /api/cooperative/farmers/microservice/search?cooperativeId=5&searchTerm=John&status=active
     */
    @GetMapping("/microservice/search")
    public ResponseEntity<List<FarmerResponse>> searchFarmersForMicroservice(
            @RequestParam Long cooperativeId,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String status) {

        log.info("🔗 MICROSERVICE CALL - Searching farmers for cooperative {} with term: {}",
                cooperativeId, searchTerm);

        List<Farmer> farmers;

        if (searchTerm != null && !searchTerm.isEmpty()) {
            farmers = farmerRepository.searchFarmers(cooperativeId, searchTerm);
        } else {
            farmers = farmerRepository.findByCooperativeId(cooperativeId);
        }

        // Apply additional filters if provided
        if (location != null && !location.isEmpty()) {
            final String searchLocation = location.toLowerCase();
            farmers = farmers.stream()
                    .filter(f -> f.getLocation().toLowerCase().contains(searchLocation))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isEmpty()) {
            final String searchStatus = status;
            farmers = farmers.stream()
                    .filter(f -> f.getStatus().equalsIgnoreCase(searchStatus))
                    .collect(Collectors.toList());
        }

        List<FarmerResponse> responses = farmers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        log.info("✅ Search returned {} farmers", responses.size());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get single farmer by ID (for Production microservice)
     * GET /cooperative/farmers/microservice/{id}
     */
    @GetMapping("/microservice/{id}")
    public ResponseEntity<FarmerResponse> getFarmerByIdForMicroservice(@PathVariable Long id) {
        log.info("🔗 MICROSERVICE CALL - Getting farmer by ID: {}", id);

        Farmer farmer = farmerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Farmer not found with id: " + id));

        FarmerResponse response = mapToResponse(farmer);

        log.info("✅ Returning farmer: {}", response.getFullName());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all farmers for a cooperative (non-paginated, for Announcement service)
     * GET /api/cooperative/farmers/microservice/all?cooperativeId=5
     */
    @GetMapping("/microservice/all")
    public ResponseEntity<List<FarmerResponse>> getAllFarmersForMicroservice(
            @RequestParam Long cooperativeId) {

        log.info("🔗 MICROSERVICE CALL - Getting all farmers for cooperative {}", cooperativeId);

        List<Farmer> farmers = farmerRepository.findByCooperativeId(cooperativeId);

        List<FarmerResponse> responses = farmers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        log.info("✅ Returning {} total farmers", responses.size());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get farmer statistics (for Announcement service dashboard)
     * GET /api/cooperative/farmers/microservice/statistics?cooperativeId=5
     */
    @GetMapping("/microservice/statistics")
    public ResponseEntity<FarmerStatisticsResponse> getFarmerStatisticsForMicroservice(
            @RequestParam Long cooperativeId) {

        log.info("🔗 MICROSERVICE CALL - Getting farmer statistics for cooperative {}", cooperativeId);

        long totalFarmers = farmerRepository.countByCooperativeId(cooperativeId);
        long activeFarmers = farmerRepository.countByCooperativeIdAndStatus(cooperativeId, "active");
        long inactiveFarmers = farmerRepository.countByCooperativeIdAndStatus(cooperativeId, "inactive");
        Double totalArea = farmerRepository.sumAreaByCooperativeId(cooperativeId);

        FarmerStatisticsResponse stats = FarmerStatisticsResponse.builder()
                .totalFarmers(totalFarmers)
                .activeFarmers(activeFarmers)
                .inactiveFarmers(inactiveFarmers)
                .totalAreaHa(totalArea != null ? totalArea : 0.0)
                .build();

        log.info("✅ Statistics: {} total, {} active, {} inactive",
                totalFarmers, activeFarmers, inactiveFarmers);

        return ResponseEntity.ok(stats);
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Extract cooperative ID from authenticated user
     * Gets the username/email from JWT token, finds the user, and returns their cooperative ID
     */
    private Long getCooperativeIdFromAuth(Authentication authentication) {
        try {
            // Get username or email from authentication principal (JWT token subject)
            String usernameOrEmail = authentication.getName();

            log.debug("Extracting cooperative ID for user: {}", usernameOrEmail);

            // Find user by username or email (since the token might contain either)
            User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                    .orElseThrow(() -> new UnauthorizedException("User not found"));

            // Ensure user has a cooperative
            if (user.getCooperative() == null) {
                log.error("User {} does not belong to any cooperative", usernameOrEmail);
                throw new UnauthorizedException("User does not belong to any cooperative");
            }

            Long cooperativeId = user.getCooperative().getId();
            log.debug("Cooperative ID {} extracted for user {}", cooperativeId, usernameOrEmail);

            return cooperativeId;

        } catch (Exception e) {
            log.error("Error extracting cooperative ID from authentication", e);
            throw new UnauthorizedException("Failed to authenticate user: " + e.getMessage());
        }
    }

    /**
     * Helper method to map Farmer entity to FarmerResponse DTO
     */
    private FarmerResponse mapToResponse(Farmer farmer) {
        return FarmerResponse.builder()
                .id(farmer.getId())
                .fullName(farmer.getFullName())
                .phoneNumber(farmer.getPhoneNumber())
                .location(farmer.getLocation())
                .language(farmer.getLanguage())  // Changed from crop to language
                .areaHa(farmer.getAreaHa())
                .status(farmer.getStatus())
                .qrCode(farmer.getQrCode())
                .cooperativeId(farmer.getCooperative().getId())
                .cooperativeName(farmer.getCooperative().getName())
                .createdAt(farmer.getCreatedAt())
                .updatedAt(farmer.getUpdatedAt())
                .isActive(farmer.getIsActive())
                .build();
    }

    // ============================================================================
    // INNER CLASSES
    // ============================================================================

    /**
     * Response DTO for farmer statistics (microservice endpoint)
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FarmerStatisticsResponse {
        private Long totalFarmers;
        private Long activeFarmers;
        private Long inactiveFarmers;
        private Double totalAreaHa;
    }
}