package com.app.Harvest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkImportRequest {

    @NotEmpty(message = "Farmers list cannot be empty")
    @Valid
    private List<FarmerRequest> farmers;
}