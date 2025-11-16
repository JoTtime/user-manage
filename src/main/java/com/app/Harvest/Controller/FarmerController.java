package com.app.Harvest.Controller;

import com.app.Harvest.Entity.User;
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

@RestController
@RequestMapping("/cooperative/farmers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class FarmerController {

    private final FarmerService farmerService;
    private final UserRepository userRepository;

    /**
     * Get all farmers for the logged-in cooperative (with pagination, filtering, and sorting)
     * GET /api/cooperative/farmers?page=0&size=10&sortBy=name&sortOrder=asc&status=all&search=
     */
    @GetMapping
    @PreAuthorize("hasAuthority('COOPERATIVE')")
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
    @PreAuthorize("hasAuthority('COOPERATIVE')")
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
    @PreAuthorize("hasAuthority('COOPERATIVE')")
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
    @PreAuthorize("hasAuthority('COOPERATIVE')")
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
    @PreAuthorize("hasAuthority('COOPERATIVE')")
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
    @PreAuthorize("hasAuthority('COOPERATIVE')")
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
    @PreAuthorize("hasAuthority('COOPERATIVE')")
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
    @PreAuthorize("hasAuthority('COOPERATIVE')")
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
    @PreAuthorize("hasAuthority('COOPERATIVE')")
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
    @PreAuthorize("hasAuthority('COOPERATIVE')")
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
    /**
     * Extract cooperative ID from authenticated user
     * Gets the email from JWT token, finds the user, and returns their cooperative ID
     */
    private Long getCooperativeIdFromAuth(Authentication authentication) {
        try {
            // Get email from authentication principal (JWT token subject)
            String email = authentication.getName();

            log.debug("Extracting cooperative ID for user: {}", email);

            // Find user by email
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UnauthorizedException("User not found"));

            // Ensure user has a cooperative
            if (user.getCooperative() == null) {
                log.error("User {} does not belong to any cooperative", email);
                throw new UnauthorizedException("User does not belong to any cooperative");
            }

            Long cooperativeId = user.getCooperative().getId();
            log.debug("Cooperative ID {} extracted for user {}", cooperativeId, email);

            return cooperativeId;

        } catch (Exception e) {
            log.error("Error extracting cooperative ID from authentication", e);
            throw new UnauthorizedException("Failed to authenticate user: " + e.getMessage());
        }
    }
}