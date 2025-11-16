package com.app.Harvest.Service;

import com.app.Harvest.Entity.Cooperative;
import com.app.Harvest.Entity.Farmer;
import com.app.Harvest.Repository.CooperativeRepository;
import com.app.Harvest.Repository.FarmerRepository;
import com.app.Harvest.dto.request.FarmerRequest;
import com.app.Harvest.dto.response.BulkImportResponse;
import com.app.Harvest.dto.response.FarmerResponse;
import com.app.Harvest.dto.response.PagedResponse;
import com.app.Harvest.exception.ResourceNotFoundException;
import com.app.Harvest.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FarmerService {

    private final FarmerRepository farmerRepository;
    private final CooperativeRepository cooperativeRepository;

    // Cameroon phone number pattern
    // Supports: +237XXXXXXXXX, 237XXXXXXXXX, or 6/2XXXXXXXX
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\+?237|237)?[26]\\d{8}$"
    );

    // Location pattern: "City, Region"
    private static final Pattern LOCATION_PATTERN = Pattern.compile(
            "^[A-Za-zÀ-ÿ\\s-]+,\\s*[A-Za-z\\s]+$"
    );

    // Valid Cameroon regions
    private static final Set<String> VALID_REGIONS = new HashSet<>(Arrays.asList(
            "Adamawa", "Centre", "East", "Far North", "Littoral",
            "North", "Northwest", "South", "Southwest", "West"
    ));

    /**
     * Validate phone number format
     */
    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new BadRequestException("Phone number is required");
        }

        String cleanedPhone = phoneNumber.replaceAll("\\s", "");
        if (!PHONE_PATTERN.matcher(cleanedPhone).matches()) {
            throw new BadRequestException(
                    "Invalid phone number format. Use: +237XXXXXXXXX, 237XXXXXXXXX, or 6/2XXXXXXXX"
            );
        }
    }

    /**
     * Validate location format (City, Region)
     */
    private void validateLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new BadRequestException("Location is required");
        }

        if (!LOCATION_PATTERN.matcher(location).matches()) {
            throw new BadRequestException(
                    "Invalid location format. Use: City, Region (e.g., \"Yaoundé, Centre\")"
            );
        }

        // Extract region and validate
        String[] parts = location.split(",");
        if (parts.length != 2) {
            throw new BadRequestException(
                    "Invalid location format. Use: City, Region"
            );
        }

        String region = parts[1].trim();
        boolean regionValid = VALID_REGIONS.stream()
                .anyMatch(validRegion -> validRegion.equalsIgnoreCase(region));

        if (!regionValid) {
            throw new BadRequestException(
                    "Invalid Cameroon region. Valid regions: " + String.join(", ", VALID_REGIONS)
            );
        }
    }

    /**
     * Normalize phone number for storage
     */
    private String normalizePhoneNumber(String phoneNumber) {
        String cleaned = phoneNumber.replaceAll("\\s", "");

        // Ensure it starts with +237
        if (cleaned.startsWith("+237")) {
            return cleaned;
        } else if (cleaned.startsWith("237")) {
            return "+" + cleaned;
        } else {
            return "+237" + cleaned;
        }
    }

    /**
     * Get paginated farmers for a specific cooperative
     */
    @Transactional(readOnly = true)
    public PagedResponse<FarmerResponse> getAllFarmersByCooperative(
            Long cooperativeId,
            int page,
            int size,
            String sortBy,
            String sortOrder,
            String status,
            String searchTerm) {

        log.info("Fetching farmers for cooperative ID: {} - Page: {}, Size: {}",
                cooperativeId, page, size);

        // Verify cooperative exists
        if (!cooperativeRepository.existsById(cooperativeId)) {
            throw new ResourceNotFoundException("Cooperative not found with ID: " + cooperativeId);
        }

        // Validate page size (max 100)
        if (size > 100) {
            size = 100;
        }

        // Create sort object
        Sort sort = Sort.by(sortOrder.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC, getSortField(sortBy));

        // Create pageable object
        Pageable pageable = PageRequest.of(page, size, sort);

        // Fetch paginated farmers based on filters
        Page<Farmer> farmerPage;

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            // Search with filters
            if (status != null && !status.equals("all")) {
                farmerPage = farmerRepository.searchFarmersWithStatus(
                        cooperativeId, searchTerm, status, pageable);
            } else {
                farmerPage = farmerRepository.searchFarmersPaginated(
                        cooperativeId, searchTerm, pageable);
            }
        } else if (status != null && !status.equals("all")) {
            // Filter by status only
            farmerPage = farmerRepository.findByCooperativeIdAndStatus(
                    cooperativeId, status, pageable);
        } else {
            // No filters, get all
            farmerPage = farmerRepository.findByCooperativeId(cooperativeId, pageable);
        }

        // Map to response DTOs
        List<FarmerResponse> farmerResponses = farmerPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.<FarmerResponse>builder()
                .content(farmerResponses)
                .pageNumber(farmerPage.getNumber())
                .pageSize(farmerPage.getSize())
                .totalElements(farmerPage.getTotalElements())
                .totalPages(farmerPage.getTotalPages())
                .last(farmerPage.isLast())
                .first(farmerPage.isFirst())
                .build();
    }

    /**
     * Map sort field names to entity field names
     */
    private String getSortField(String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "name":
                return "fullName";
            case "location":
                return "location";
            case "area":
                return "areaHa";
            case "date":
                return "createdAt";
            default:
                return "fullName";
        }
    }

    /**
     * Get a specific farmer by ID (only if belongs to the cooperative)
     */
    @Transactional(readOnly = true)
    public FarmerResponse getFarmerById(Long farmerId, Long cooperativeId) {
        log.info("Fetching farmer ID: {} for cooperative ID: {}", farmerId, cooperativeId);

        Farmer farmer = farmerRepository.findByIdAndCooperativeId(farmerId, cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer not found with ID: " + farmerId + " for your cooperative"));

        return mapToResponse(farmer);
    }

    /**
     * Create a new farmer
     */
    @Transactional
    public FarmerResponse createFarmer(FarmerRequest request, Long cooperativeId) {
        log.info("Creating new farmer for cooperative ID: {}", cooperativeId);

        // Validate phone number format
        validatePhoneNumber(request.getPhoneNumber());

        // Validate location format
        validateLocation(request.getLocation());

        // Get cooperative
        Cooperative cooperative = cooperativeRepository.findById(cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException("Cooperative not found with ID: " + cooperativeId));

        // Normalize phone number
        String normalizedPhone = normalizePhoneNumber(request.getPhoneNumber());

        // Check if phone number already exists for this cooperative
        if (farmerRepository.existsByPhoneNumberAndCooperativeId(normalizedPhone, cooperativeId)) {
            throw new BadRequestException("Farmer with phone number " + normalizedPhone +
                    " already exists in your cooperative");
        }

        // Check if farmer with same name already exists in cooperative
        if (farmerRepository.existsByFullNameAndCooperativeId(
                request.getFullName().trim(), cooperativeId)) {
            throw new BadRequestException("Farmer with name \"" + request.getFullName() +
                    "\" already exists in your cooperative");
        }

        // Create farmer entity
        Farmer farmer = Farmer.builder()
                .fullName(request.getFullName().trim())
                .phoneNumber(normalizedPhone)
                .location(request.getLocation().trim())
                .crop(request.getCrop() != null ? request.getCrop().trim() : null)
                .areaHa(request.getAreaHa())
                .status(request.getStatus() != null ? request.getStatus() : "active")
                .qrCode(generateQrCode())
                .cooperative(cooperative)
                .build();

        Farmer savedFarmer = farmerRepository.save(farmer);
        log.info("Farmer created successfully with ID: {}", savedFarmer.getId());

        return mapToResponse(savedFarmer);
    }

    /**
     * Bulk import farmers from Excel/CSV
     */
    @Transactional
    public BulkImportResponse bulkImportFarmers(List<FarmerRequest> farmersToImport, Long cooperativeId) {
        log.info("Starting bulk import of {} farmers for cooperative {}", farmersToImport.size(), cooperativeId);

        BulkImportResponse response = BulkImportResponse.builder()
                .totalProcessed(farmersToImport.size())
                .successCount(0)
                .failureCount(0)
                .errors(new ArrayList<>())
                .importedFarmers(new ArrayList<>())
                .build();

        // Get cooperative
        Cooperative cooperative = cooperativeRepository.findById(cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException("Cooperative not found"));

        for (int i = 0; i < farmersToImport.size(); i++) {
            FarmerRequest farmerRequest = farmersToImport.get(i);
            int rowNumber = i + 2; // +2 because row 1 is header and array is 0-indexed

            try {
                // Validate required fields
                if (farmerRequest.getFullName() == null || farmerRequest.getFullName().trim().isEmpty()) {
                    throw new IllegalArgumentException("Full name is required");
                }

                if (farmerRequest.getPhoneNumber() == null || farmerRequest.getPhoneNumber().trim().isEmpty()) {
                    throw new IllegalArgumentException("Phone number is required");
                }

                if (farmerRequest.getLocation() == null || farmerRequest.getLocation().trim().isEmpty()) {
                    throw new IllegalArgumentException("Location is required");
                }

                if (farmerRequest.getAreaHa() == null || farmerRequest.getAreaHa() <= 0) {
                    throw new IllegalArgumentException("Area must be greater than 0");
                }

                // Validate phone number format
                validatePhoneNumber(farmerRequest.getPhoneNumber());

                // Validate location format
                validateLocation(farmerRequest.getLocation());

                // Normalize phone number
                String normalizedPhone = normalizePhoneNumber(farmerRequest.getPhoneNumber());

                // Check for duplicate phone number in the cooperative
                boolean phoneExists = farmerRepository.existsByPhoneNumberAndCooperativeId(
                        normalizedPhone, cooperativeId);

                if (phoneExists) {
                    throw new IllegalArgumentException("Phone number " + normalizedPhone +
                            " already exists in your cooperative");
                }

                // Check for duplicate name in the cooperative
                boolean nameExists = farmerRepository.existsByFullNameAndCooperativeId(
                        farmerRequest.getFullName().trim(), cooperativeId);

                if (nameExists) {
                    throw new IllegalArgumentException("Farmer with name \"" +
                            farmerRequest.getFullName() + "\" already exists in your cooperative");
                }

                // Generate QR code
                String qrCode = generateQrCode();

                // Ensure QR code is unique
                while (farmerRepository.existsByQrCode(qrCode)) {
                    qrCode = generateQrCode();
                }

                // Create farmer entity
                Farmer farmer = Farmer.builder()
                        .fullName(farmerRequest.getFullName().trim())
                        .phoneNumber(normalizedPhone)
                        .location(farmerRequest.getLocation().trim())
                        .crop(farmerRequest.getCrop() != null ? farmerRequest.getCrop().trim() : null)
                        .areaHa(farmerRequest.getAreaHa())
                        .status(farmerRequest.getStatus() != null ? farmerRequest.getStatus() : "active")
                        .qrCode(qrCode)
                        .cooperative(cooperative)
                        .build();

                // Save farmer
                Farmer savedFarmer = farmerRepository.save(farmer);

                // Convert to response
                FarmerResponse farmerResponse = mapToResponse(savedFarmer);
                response.getImportedFarmers().add(farmerResponse);
                response.setSuccessCount(response.getSuccessCount() + 1);

                log.debug("Successfully imported farmer: {} (row {})", savedFarmer.getFullName(), rowNumber);

            } catch (Exception e) {
                // Log error and add to error list
                log.error("Failed to import farmer at row {}: {}", rowNumber, e.getMessage());

                Map<String, Object> farmerData = new HashMap<>();
                farmerData.put("fullName", farmerRequest.getFullName());
                farmerData.put("phoneNumber", farmerRequest.getPhoneNumber());
                farmerData.put("location", farmerRequest.getLocation());
                farmerData.put("crop", farmerRequest.getCrop());
                farmerData.put("areaHa", farmerRequest.getAreaHa());

                BulkImportResponse.ImportError error = BulkImportResponse.ImportError.builder()
                        .row(rowNumber)
                        .farmer(farmerData)
                        .error(e.getMessage())
                        .build();

                response.getErrors().add(error);
                response.setFailureCount(response.getFailureCount() + 1);
            }
        }

        log.info("Bulk import completed. Success: {}, Failed: {}",
                response.getSuccessCount(), response.getFailureCount());

        return response;
    }

    /**
     * Update an existing farmer
     */
    @Transactional
    public FarmerResponse updateFarmer(Long farmerId, FarmerRequest request, Long cooperativeId) {
        log.info("Updating farmer ID: {} for cooperative ID: {}", farmerId, cooperativeId);

        // Validate phone number format
        validatePhoneNumber(request.getPhoneNumber());

        // Validate location format
        validateLocation(request.getLocation());

        // Get farmer (ensure it belongs to the cooperative)
        Farmer farmer = farmerRepository.findByIdAndCooperativeId(farmerId, cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer not found with ID: " + farmerId + " for your cooperative"));

        // Normalize phone number
        String normalizedPhone = normalizePhoneNumber(request.getPhoneNumber());

        // Check if phone number is being changed and if it already exists
        if (!farmer.getPhoneNumber().equals(normalizedPhone)) {
            if (farmerRepository.existsByPhoneNumberAndCooperativeId(normalizedPhone, cooperativeId)) {
                throw new BadRequestException("Farmer with phone number " + normalizedPhone +
                        " already exists in your cooperative");
            }
        }

        // Check if name is being changed and if it already exists
        if (!farmer.getFullName().equalsIgnoreCase(request.getFullName().trim())) {
            if (farmerRepository.existsByFullNameAndCooperativeId(
                    request.getFullName().trim(), cooperativeId)) {
                throw new BadRequestException("Farmer with name \"" + request.getFullName() +
                        "\" already exists in your cooperative");
            }
        }

        // Update farmer details
        farmer.setFullName(request.getFullName().trim());
        farmer.setPhoneNumber(normalizedPhone);
        farmer.setLocation(request.getLocation().trim());
        farmer.setCrop(request.getCrop() != null ? request.getCrop().trim() : null);
        farmer.setAreaHa(request.getAreaHa());

        if (request.getStatus() != null) {
            farmer.setStatus(request.getStatus());
        }

        Farmer updatedFarmer = farmerRepository.save(farmer);
        log.info("Farmer updated successfully with ID: {}", updatedFarmer.getId());

        return mapToResponse(updatedFarmer);
    }

    /**
     * Delete a farmer
     */
    @Transactional
    public void deleteFarmer(Long farmerId, Long cooperativeId) {
        log.info("Deleting farmer ID: {} for cooperative ID: {}", farmerId, cooperativeId);

        // Get farmer (ensure it belongs to the cooperative)
        Farmer farmer = farmerRepository.findByIdAndCooperativeId(farmerId, cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer not found with ID: " + farmerId + " for your cooperative"));

        farmerRepository.delete(farmer);
        log.info("Farmer deleted successfully with ID: {}", farmerId);
    }

    /**
     * Update farmer status (active/inactive)
     */
    @Transactional
    public FarmerResponse updateFarmerStatus(Long farmerId, String status, Long cooperativeId) {
        log.info("Updating status for farmer ID: {} to {} for cooperative ID: {}",
                farmerId, status, cooperativeId);

        // Validate status
        if (!status.equals("active") && !status.equals("inactive")) {
            throw new BadRequestException("Invalid status. Must be 'active' or 'inactive'");
        }

        // Get farmer (ensure it belongs to the cooperative)
        Farmer farmer = farmerRepository.findByIdAndCooperativeId(farmerId, cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer not found with ID: " + farmerId + " for your cooperative"));

        farmer.setStatus(status);
        Farmer updatedFarmer = farmerRepository.save(farmer);

        log.info("Farmer status updated successfully");
        return mapToResponse(updatedFarmer);
    }

    /**
     * Get farmer statistics for a cooperative
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFarmerStatistics(Long cooperativeId) {
        log.info("Fetching farmer statistics for cooperative ID: {}", cooperativeId);

        if (!cooperativeRepository.existsById(cooperativeId)) {
            throw new ResourceNotFoundException("Cooperative not found with ID: " + cooperativeId);
        }

        long totalFarmers = farmerRepository.countByCooperativeId(cooperativeId);
        long activeFarmers = farmerRepository.countByCooperativeIdAndStatus(cooperativeId, "active");
        long inactiveFarmers = farmerRepository.countByCooperativeIdAndStatus(cooperativeId, "inactive");
        Double totalArea = farmerRepository.sumAreaByCooperativeId(cooperativeId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFarmers", totalFarmers);
        stats.put("activeFarmers", activeFarmers);
        stats.put("inactiveFarmers", inactiveFarmers);
        stats.put("totalArea", totalArea != null ? totalArea : 0.0);

        return stats;
    }

    /**
     * Generate unique QR code for farmer
     */
    private String generateQrCode() {
        return "QR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Map Farmer entity to FarmerResponse DTO
     */
    private FarmerResponse mapToResponse(Farmer farmer) {
        return FarmerResponse.builder()
                .id(farmer.getId())
                .fullName(farmer.getFullName())
                .phoneNumber(farmer.getPhoneNumber())
                .location(farmer.getLocation())
                .crop(farmer.getCrop())
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

    /**
     * Inner class for farmer statistics
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FarmerStatistics {
        private Long totalFarmers;
        private Long activeFarmers;
        private Long inactiveFarmers;
    }
}