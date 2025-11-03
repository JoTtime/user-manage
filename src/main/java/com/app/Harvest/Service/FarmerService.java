package com.app.Harvest.Service;

import com.app.Harvest.Entity.Cooperative;
import com.app.Harvest.Entity.Farmer;
import com.app.Harvest.Repository.CooperativeRepository;
import com.app.Harvest.Repository.FarmerRepository;
import com.app.Harvest.dto.request.FarmerRequest;
import com.app.Harvest.dto.response.BulkImportResponse;
import com.app.Harvest.dto.response.FarmerResponse;
import com.app.Harvest.exception.ResourceNotFoundException;
import com.app.Harvest.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FarmerService {

    private final FarmerRepository farmerRepository;
    private final CooperativeRepository cooperativeRepository;

    /**
     * Get all farmers for a specific cooperative
     */
    @Transactional(readOnly = true)
    public List<FarmerResponse> getAllFarmersByCooperative(Long cooperativeId) {
        log.info("Fetching all farmers for cooperative ID: {}", cooperativeId);

        // Verify cooperative exists
        if (!cooperativeRepository.existsById(cooperativeId)) {
            throw new ResourceNotFoundException("Cooperative not found with ID: " + cooperativeId);
        }

        List<Farmer> farmers = farmerRepository.findByCooperativeId(cooperativeId);
        return farmers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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

        // Get cooperative
        Cooperative cooperative = cooperativeRepository.findById(cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException("Cooperative not found with ID: " + cooperativeId));

        // Check if phone number already exists for this cooperative
        if (farmerRepository.existsByPhoneNumberAndCooperativeId(request.getPhoneNumber(), cooperativeId)) {
            throw new BadRequestException("Farmer with phone number " + request.getPhoneNumber() +
                    " already exists in your cooperative");
        }

        // Create farmer entity
        Farmer farmer = Farmer.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .location(request.getLocation())
                .crop(request.getCrop())
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

                // Check for duplicate phone number in the cooperative
                boolean phoneExists = farmerRepository.existsByPhoneNumberAndCooperativeId(
                        farmerRequest.getPhoneNumber(), cooperativeId);

                if (phoneExists) {
                    throw new IllegalArgumentException("Phone number already exists in your cooperative");
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
                        .phoneNumber(farmerRequest.getPhoneNumber().trim())
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

        // Get farmer (ensure it belongs to the cooperative)
        Farmer farmer = farmerRepository.findByIdAndCooperativeId(farmerId, cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer not found with ID: " + farmerId + " for your cooperative"));

        // Check if phone number is being changed and if it already exists
        if (!farmer.getPhoneNumber().equals(request.getPhoneNumber())) {
            if (farmerRepository.existsByPhoneNumberAndCooperativeId(request.getPhoneNumber(), cooperativeId)) {
                throw new BadRequestException("Farmer with phone number " + request.getPhoneNumber() +
                        " already exists in your cooperative");
            }
        }

        // Update farmer details
        farmer.setFullName(request.getFullName());
        farmer.setPhoneNumber(request.getPhoneNumber());
        farmer.setLocation(request.getLocation());
        farmer.setCrop(request.getCrop());
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
     * Search farmers within a cooperative
     */
    @Transactional(readOnly = true)
    public List<FarmerResponse> searchFarmers(Long cooperativeId, String searchTerm) {
        log.info("Searching farmers for cooperative ID: {} with term: {}", cooperativeId, searchTerm);

        if (!cooperativeRepository.existsById(cooperativeId)) {
            throw new ResourceNotFoundException("Cooperative not found with ID: " + cooperativeId);
        }

        List<Farmer> farmers = farmerRepository.searchFarmers(cooperativeId, searchTerm);
        return farmers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get farmer statistics for a cooperative
     */
    @Transactional(readOnly = true)
    public FarmerStatistics getFarmerStatistics(Long cooperativeId) {
        log.info("Fetching farmer statistics for cooperative ID: {}", cooperativeId);

        if (!cooperativeRepository.existsById(cooperativeId)) {
            throw new ResourceNotFoundException("Cooperative not found with ID: " + cooperativeId);
        }

        long totalFarmers = farmerRepository.countByCooperativeId(cooperativeId);
        long activeFarmers = farmerRepository.countByCooperativeIdAndStatus(cooperativeId, "active");
        long inactiveFarmers = farmerRepository.countByCooperativeIdAndStatus(cooperativeId, "inactive");

        return FarmerStatistics.builder()
                .totalFarmers(totalFarmers)
                .activeFarmers(activeFarmers)
                .inactiveFarmers(inactiveFarmers)
                .build();
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