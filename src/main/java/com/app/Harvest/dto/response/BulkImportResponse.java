package com.app.Harvest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkImportResponse {

    private Integer totalProcessed;
    private Integer successCount;
    private Integer failureCount;
    private List<ImportError> errors;
    private List<FarmerResponse> importedFarmers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportError {
        private Integer row;
        private Map<String, Object> farmer;
        private String error;
    }
}