package io.petcare.backend.weight;

import io.petcare.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pets/{petId}/weight-records")
public class WeightRecordController {

    private final WeightRecordService weightRecordService;

    public WeightRecordController(WeightRecordService weightRecordService) {
        this.weightRecordService = weightRecordService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WeightRecordVO>> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long petId,
            @Valid @RequestBody WeightRecordRequest request) {
        WeightRecordVO record = weightRecordService.create(currentUserId(jwt), petId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "created", record));
    }

    @GetMapping
    public ApiResponse<List<WeightRecordVO>> list(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long petId) {
        return ApiResponse.success(HttpStatus.OK.value(), "success",
                weightRecordService.list(currentUserId(jwt), petId));
    }

    @PutMapping("/{recordId}")
    public ApiResponse<WeightRecordVO> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long petId,
            @PathVariable long recordId,
            @Valid @RequestBody WeightRecordRequest request) {
        return ApiResponse.success(HttpStatus.OK.value(), "updated",
                weightRecordService.update(currentUserId(jwt), petId, recordId, request));
    }

    @DeleteMapping("/{recordId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long petId,
            @PathVariable long recordId) {
        weightRecordService.delete(currentUserId(jwt), petId, recordId);
        return ApiResponse.success(HttpStatus.OK.value(), "deleted", null);
    }

    private long currentUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
