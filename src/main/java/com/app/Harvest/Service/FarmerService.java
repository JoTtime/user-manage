package com.app.Harvest.Service;

import com.app.Harvest.Entity.Cooperative;
import com.app.Harvest.Entity.Farmer;
import com.app.Harvest.Entity.Project;
import com.app.Harvest.Repository.CooperativeRepository;
import com.app.Harvest.Repository.FarmerRepository;
import com.app.Harvest.Repository.ProjectRepository;
import com.app.Harvest.dto.request.CoordinatesRequest;
import com.app.Harvest.dto.request.FarmerRequest;
import com.app.Harvest.dto.request.ProjectRequest;
import com.app.Harvest.dto.response.BulkImportResponse;
import com.app.Harvest.dto.response.CoordinatesResponse;
import com.app.Harvest.dto.response.FarmerResponse;
import com.app.Harvest.dto.response.PagedResponse;
import com.app.Harvest.dto.response.ProjectResponse;
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
@Transactional
public class FarmerService {

    private final FarmerRepository farmerRepository;
    private final CooperativeRepository cooperativeRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;

    // Cameroon phone number pattern
    // Supports: +237XXXXXXXXX, 237XXXXXXXXX, or 6/2XXXXXXXX
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+?237|237)?[26]\\d{8}$");

    // Location pattern: "City, Region"
    private static final Pattern LOCATION_PATTERN = Pattern.compile("^[A-Za-zÀ-ÿ\\s-]+,\\s*[A-Za-z\\s]+$");

    // Valid Cameroon regions
    private static final Set<String> VALID_REGIONS = new HashSet<>(Arrays.asList(
            "Adamawa", "Centre", "East", "Far North", "Littoral",
            "North", "Northwest", "South", "Southwest", "West"
    ));

    // Available languages
    private static final Set<String> VALID_LANGUAGES = new HashSet<>(Arrays.asList(
            "English", "French", "Pidgin English", "Fulfulde", "Ewondo",
            "Duala", "Bamileke", "Other"
    ));

    // ==================== VALIDATION METHODS ====================

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
            throw new BadRequestException("Invalid location format. Use: City, Region");
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
     * Validate language
     */
    private void validateLanguage(String language) {
        if (language != null && !language.trim().isEmpty()) {
            boolean languageValid = VALID_LANGUAGES.stream()
                    .anyMatch(validLang -> validLang.equalsIgnoreCase(language.trim()));

            if (!languageValid) {
                throw new BadRequestException(
                        "Invalid language. Valid languages: " + String.join(", ", VALID_LANGUAGES)
                );
            }
        }
    }

    /**
     * Validate coordinates
     */
    private void validateCoordinates(CoordinatesRequest coordinates) {
        if (coordinates == null) {
            return; // Coordinates are optional
        }

        // Validate latitude range (-90 to 90)
        if (coordinates.getLatitude() < -90 || coordinates.getLatitude() > 90) {
            throw new BadRequestException("Invalid latitude. Must be between -90 and 90 degrees");
        }

        // Validate longitude range (-180 to 180)
        if (coordinates.getLongitude() < -180 || coordinates.getLongitude() > 180) {
            throw new BadRequestException("Invalid longitude. Must be between -180 and 180 degrees");
        }

        // Validate coordinates are within Cameroon (approximate bounds)
        boolean isInCameroon = coordinates.getLatitude() >= 1.65 &&
                coordinates.getLatitude() <= 13.05 &&
                coordinates.getLongitude() >= 8.38 &&
                coordinates.getLongitude() <= 16.19;

        if (!isInCameroon) {
            throw new BadRequestException("Coordinates must be within Cameroon");
        }
    }

    // ==================== UTILITY METHODS ====================

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
     * Generate unique QR code for farmer
     */
    private String generateQrCode() {
        return "QR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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
     * Map Coordinates entity to CoordinatesResponse DTO
     */
    private CoordinatesResponse mapToCoordinatesResponse(com.app.Harvest.Entity.Coordinates coordinates) {
        if (coordinates == null) {
            return null;
        }

        return CoordinatesResponse.builder()
                .latitude(coordinates.getLatitude())
                .longitude(coordinates.getLongitude())
                .address(coordinates.getAddress())
                .build();
    }

    /**
     * Map Farmer entity to FarmerResponse DTO
     * @param farmer The farmer entity
     * @param includeProjects Whether to include projects in response
     */
    private FarmerResponse mapToResponse(Farmer farmer, boolean includeProjects) {
        // Calculate allocated area from projects
        Double allocatedArea = projectRepository.sumAreaByFarmerId(farmer.getId());
        if (allocatedArea == null) {
            allocatedArea = 0.0;
        }

        // Calculate remaining area
        Double remainingArea = farmer.getAreaHa() != null ?
                farmer.getAreaHa() - allocatedArea : 0.0;

        // Ensure remaining area is not negative
        remainingArea = Math.max(remainingArea, 0.0);

        // Map projects to ProjectResponse only if requested
        List<ProjectResponse> projectResponses = null;
        if (includeProjects) {
            // Eagerly load projects
            farmer.getProjects().size(); // This triggers loading

            if (farmer.getProjects() != null && !farmer.getProjects().isEmpty()) {
                projectResponses = farmer.getProjects().stream()
                        .map(project -> ProjectResponse.builder()
                                .id(project.getId())
                                .cropName(project.getCropName())
                                .areaHa(project.getAreaHa())
                                .status(project.getStatus())
                                .plantingDate(project.getPlantingDate())
                                .expectedHarvestDate(project.getExpectedHarvestDate())
                                .notes(project.getNotes())
                                .farmerId(farmer.getId())
                                .farmerName(farmer.getFullName())
                                .createdAt(project.getCreatedAt())
                                .updatedAt(project.getUpdatedAt())
                                .build())
                        .collect(Collectors.toList());
            }
        }

        return FarmerResponse.builder()
                .id(farmer.getId())
                .fullName(farmer.getFullName())
                .phoneNumber(farmer.getPhoneNumber())
                .location(farmer.getLocation())
                .language(farmer.getLanguage())
                .areaHa(farmer.getAreaHa())
                .allocatedArea(allocatedArea)
                .remainingArea(remainingArea)
                .status(farmer.getStatus())
                .qrCode(farmer.getQrCode())
                .coordinates(mapToCoordinatesResponse(farmer.getCoordinates()))
                .cooperativeId(farmer.getCooperative().getId())
                .cooperativeName(farmer.getCooperative().getName())
                .projects(projectResponses)  // Will be null if includeProjects is false
                .createdAt(farmer.getCreatedAt())
                .updatedAt(farmer.getUpdatedAt())
                .isActive(farmer.getIsActive())
                .build();
    }

    /**
     * Convenience method - excludes projects by default (for list views)
     */
    private FarmerResponse mapToResponse(Farmer farmer) {
        return mapToResponse(farmer, false);
    }

    // ==================== BUSINESS LOGIC METHODS ====================

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

        // Map to response DTOs WITHOUT projects (for dashboard table)
        List<FarmerResponse> farmerResponses = farmerPage.getContent().stream()
                .map(farmer -> mapToResponse(farmer, false))  // false = don't include projects
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
     * Get a specific farmer by ID (only if belongs to the cooperative)
     */
    @Transactional(readOnly = true)
    public FarmerResponse getFarmerById(Long farmerId, Long cooperativeId) {
        log.info("Fetching farmer ID: {} for cooperative ID: {}", farmerId, cooperativeId);

        Farmer farmer = farmerRepository.findByIdAndCooperativeId(farmerId, cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer not found with ID: " + farmerId + " for your cooperative"));

        return mapToResponse(farmer, true);  // true = include projects for detail view
    }

    /**
     * Create a new farmer with optional projects
     */
    public FarmerResponse createFarmer(FarmerRequest request, Long cooperativeId) {
        log.info("Creating new farmer for cooperative ID: {}", cooperativeId);

        // Validate phone number format
        validatePhoneNumber(request.getPhoneNumber());

        // Validate location format
        validateLocation(request.getLocation());

        // Validate language if provided
        validateLanguage(request.getLanguage());

        // Validate coordinates if provided
        validateCoordinates(request.getCoordinates());

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

        // Validate projects if provided
        if (request.getProjects() != null && !request.getProjects().isEmpty()) {
            Double totalProjectArea = request.getProjects().stream()
                    .mapToDouble(ProjectRequest::getAreaHa)
                    .sum();

            if (totalProjectArea > request.getAreaHa()) {
                throw new BadRequestException(
                        String.format("Total project area (%.2f ha) exceeds farmer's total area (%.2f ha)",
                                totalProjectArea, request.getAreaHa()));
            }
        }

        // Generate unique QR code
        String qrCode = generateQrCode();
        while (farmerRepository.existsByQrCode(qrCode)) {
            qrCode = generateQrCode();
        }

        // Create coordinates entity if provided
        com.app.Harvest.Entity.Coordinates coordinates = null;
        if (request.getCoordinates() != null) {
            coordinates = com.app.Harvest.Entity.Coordinates.builder()
                    .latitude(request.getCoordinates().getLatitude())
                    .longitude(request.getCoordinates().getLongitude())
                    .address(request.getCoordinates().getAddress())
                    .build();
        }

        // Create farmer entity
        Farmer farmer = Farmer.builder()
                .fullName(request.getFullName().trim())
                .phoneNumber(normalizedPhone)
                .location(request.getLocation().trim())
                .language(request.getLanguage() != null ? request.getLanguage().trim() : null)
                .areaHa(request.getAreaHa())
                .status(request.getStatus() != null ? request.getStatus() : "active")
                .qrCode(qrCode)
                .coordinates(coordinates)
                .cooperative(cooperative)
                .build();

        Farmer savedFarmer = farmerRepository.save(farmer);
        log.info("Farmer created successfully with ID: {}", savedFarmer.getId());

        // Create projects if provided
        if (request.getProjects() != null && !request.getProjects().isEmpty()) {
            for (ProjectRequest projectRequest : request.getProjects()) {
                Project project = Project.builder()
                        .cropName(projectRequest.getCropName().trim())
                        .areaHa(projectRequest.getAreaHa())
                        .status(projectRequest.getStatus() != null ? projectRequest.getStatus() : "active")
                        .plantingDate(projectRequest.getPlantingDate())
                        .expectedHarvestDate(projectRequest.getExpectedHarvestDate())
                        .notes(projectRequest.getNotes() != null ? projectRequest.getNotes().trim() : null)
                        .farmer(savedFarmer)
                        .build();

                projectRepository.save(project);
            }
            log.info("Created {} projects for farmer ID: {}", request.getProjects().size(), savedFarmer.getId());
        }

        return mapToResponse(savedFarmer, true);  // true = include created projects in response
    }

    /**
     * Bulk import farmers from Excel/CSV
     */
    public BulkImportResponse bulkImportFarmers(List<FarmerRequest> farmersToImport, Long cooperativeId) {
        log.info("Starting bulk import of {} farmers for cooperative {}",
                farmersToImport.size(), cooperativeId);

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

                // Validate language if provided
                validateLanguage(farmerRequest.getLanguage());

                // Validate coordinates if provided
                validateCoordinates(farmerRequest.getCoordinates());

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

                // Create coordinates entity if provided
                com.app.Harvest.Entity.Coordinates coordinates = null;
                if (farmerRequest.getCoordinates() != null) {
                    coordinates = com.app.Harvest.Entity.Coordinates.builder()
                            .latitude(farmerRequest.getCoordinates().getLatitude())
                            .longitude(farmerRequest.getCoordinates().getLongitude())
                            .address(farmerRequest.getCoordinates().getAddress())
                            .build();
                }

                // Create farmer entity
                Farmer farmer = Farmer.builder()
                        .fullName(farmerRequest.getFullName().trim())
                        .phoneNumber(normalizedPhone)
                        .location(farmerRequest.getLocation().trim())
                        .language(farmerRequest.getLanguage() != null ? farmerRequest.getLanguage().trim() : null)
                        .areaHa(farmerRequest.getAreaHa())
                        .status(farmerRequest.getStatus() != null ? farmerRequest.getStatus() : "active")
                        .qrCode(qrCode)
                        .coordinates(coordinates)
                        .cooperative(cooperative)
                        .build();

                // Save farmer
                Farmer savedFarmer = farmerRepository.save(farmer);

                // Convert to response
                FarmerResponse farmerResponse = mapToResponse(savedFarmer, false);
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
                farmerData.put("language", farmerRequest.getLanguage());
                farmerData.put("areaHa", farmerRequest.getAreaHa());
                farmerData.put("coordinates", farmerRequest.getCoordinates());

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
     * Update an existing farmer with projects
     */
    public FarmerResponse updateFarmer(Long farmerId, FarmerRequest request, Long cooperativeId) {
        log.info("Updating farmer ID: {} for cooperative ID: {}", farmerId, cooperativeId);

        // Validate phone number format
        validatePhoneNumber(request.getPhoneNumber());

        // Validate location format
        validateLocation(request.getLocation());

        // Validate language if provided
        validateLanguage(request.getLanguage());

        // Validate coordinates if provided
        validateCoordinates(request.getCoordinates());

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

        // Validate projects if provided
        if (request.getProjects() != null && !request.getProjects().isEmpty()) {
            Double totalProjectArea = request.getProjects().stream()
                    .mapToDouble(ProjectRequest::getAreaHa)
                    .sum();

            if (totalProjectArea > request.getAreaHa()) {
                throw new BadRequestException(
                        String.format("Total project area (%.2f ha) exceeds farmer's total area (%.2f ha)",
                                totalProjectArea, request.getAreaHa()));
            }
        }

        // Update farmer details
        farmer.setFullName(request.getFullName().trim());
        farmer.setPhoneNumber(normalizedPhone);
        farmer.setLocation(request.getLocation().trim());
        farmer.setLanguage(request.getLanguage() != null ? request.getLanguage().trim() : null);
        farmer.setAreaHa(request.getAreaHa());

        // Update coordinates if provided
        if (request.getCoordinates() != null) {
            com.app.Harvest.Entity.Coordinates coordinates = com.app.Harvest.Entity.Coordinates.builder()
                    .latitude(request.getCoordinates().getLatitude())
                    .longitude(request.getCoordinates().getLongitude())
                    .address(request.getCoordinates().getAddress())
                    .build();
            farmer.setCoordinates(coordinates);
        } else {
            farmer.setCoordinates(null);
        }

        if (request.getStatus() != null) {
            farmer.setStatus(request.getStatus());
        }

        Farmer updatedFarmer = farmerRepository.save(farmer);
        log.info("Farmer updated successfully with ID: {}", updatedFarmer.getId());

        // Process project updates
        processProjectUpdates(farmerId, request.getProjects());

        // Refresh farmer to get updated projects
        Farmer refreshedFarmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer not found with ID: " + farmerId));

        return mapToResponse(refreshedFarmer, true);
    }

    /**
     * Process project updates (create, update, delete)
     */
    private void processProjectUpdates(Long farmerId, List<ProjectRequest> projectRequests) {
        if (projectRequests == null) {
            projectRequests = new ArrayList<>();
        }

        // Get existing projects for this farmer
        List<Project> existingProjects = projectRepository.findByFarmerId(farmerId);

        // Create a map of existing projects by ID for quick lookup
        Map<Long, Project> existingProjectMap = existingProjects.stream()
                .collect(Collectors.toMap(Project::getId, p -> p));

        // Track which project IDs from the request should be kept
        Set<Long> projectIdsToKeep = new HashSet<>();

        // Process each project from the request
        for (ProjectRequest projectRequest : projectRequests) {
            Project project;

            if (projectRequest.getId() != null && existingProjectMap.containsKey(projectRequest.getId())) {
                // UPDATE existing project
                project = existingProjectMap.get(projectRequest.getId());
                project.setCropName(projectRequest.getCropName().trim());
                project.setAreaHa(projectRequest.getAreaHa());
                project.setStatus(projectRequest.getStatus() != null ? projectRequest.getStatus() : "active");
                project.setPlantingDate(projectRequest.getPlantingDate());
                project.setExpectedHarvestDate(projectRequest.getExpectedHarvestDate());
                project.setNotes(projectRequest.getNotes() != null ? projectRequest.getNotes().trim() : null);

                projectIdsToKeep.add(project.getId());
                log.info("Updating existing project ID: {}", project.getId());
            } else {
                // CREATE new project
                Farmer farmer = farmerRepository.findById(farmerId)
                        .orElseThrow(() -> new ResourceNotFoundException("Farmer not found with ID: " + farmerId));

                project = Project.builder()
                        .cropName(projectRequest.getCropName().trim())
                        .areaHa(projectRequest.getAreaHa())
                        .status(projectRequest.getStatus() != null ? projectRequest.getStatus() : "active")
                        .plantingDate(projectRequest.getPlantingDate())
                        .expectedHarvestDate(projectRequest.getExpectedHarvestDate())
                        .notes(projectRequest.getNotes() != null ? projectRequest.getNotes().trim() : null)
                        .farmer(farmer)
                        .build();

                log.info("Creating new project for farmer ID: {}", farmerId);
            }

            projectRepository.save(project);
        }

        // DELETE projects that exist in the database but are not in the request
        for (Project existingProject : existingProjects) {
            if (!projectIdsToKeep.contains(existingProject.getId())) {
                log.info("Deleting project ID: {} for farmer ID: {}", existingProject.getId(), farmerId);
                projectRepository.delete(existingProject);
            }
        }

        log.info("Project updates completed for farmer ID: {}. Kept: {}, Created: {}, Deleted: {}",
                farmerId, projectIdsToKeep.size(),
                projectRequests.size() - projectIdsToKeep.size(),
                existingProjects.size() - projectIdsToKeep.size());
    }

    /**
     * Delete a farmer
     */
    public void deleteFarmer(Long farmerId, Long cooperativeId) {
        log.info("Deleting farmer ID: {} for cooperative ID: {}", farmerId, cooperativeId);

        // Get farmer (ensure it belongs to the cooperative)
        Farmer farmer = farmerRepository.findByIdAndCooperativeId(farmerId, cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer not found with ID: " + farmerId + " for your cooperative"));

        // Delete all projects first (cascade should handle this, but being explicit)
        projectRepository.deleteByFarmerId(farmerId);

        farmerRepository.delete(farmer);
        log.info("Farmer deleted successfully with ID: {}", farmerId);
    }

    /**
     * Update farmer status (active/inactive)
     */
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
        return mapToResponse(updatedFarmer, false);  // Don't include projects for status update
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

        // Calculate total allocated area from projects
        Double totalAllocatedArea = projectRepository.sumAllocatedAreaByCooperativeId(cooperativeId);
        if (totalAllocatedArea == null) {
            totalAllocatedArea = 0.0;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFarmers", totalFarmers);
        stats.put("activeFarmers", activeFarmers);
        stats.put("inactiveFarmers", inactiveFarmers);
        stats.put("totalArea", totalArea != null ? totalArea : 0.0);
        stats.put("totalAllocatedArea", totalAllocatedArea);
        stats.put("totalRemainingArea", (totalArea != null ? totalArea : 0.0) - totalAllocatedArea);

        return stats;
    }
}