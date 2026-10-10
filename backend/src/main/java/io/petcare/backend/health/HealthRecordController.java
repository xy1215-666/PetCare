package io.petcare.backend.health;

import io.petcare.backend.common.ApiResponse;
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

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/pets/{petId}/health-records")
public class HealthRecordController {

    private final HealthRecordService healthRecordService;

    public HealthRecordController(HealthRecordService healthRecordService) {
        this.healthRecordService = healthRecordService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HealthRecordVO>> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long petId,
            @Valid @RequestBody HealthRecordRequest request) {
        HealthRecordVO record = healthRecordService.create(currentUserId(jwt), petId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "created", record));
    }

    @GetMapping
    public ApiResponse<List<HealthRecordVO>> list(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long petId) {
        return ApiResponse.success(HttpStatus.OK.value(), "success",
                healthRecordService.list(currentUserId(jwt), petId));
    }

    @GetMapping("/{recordId}")
    public ApiResponse<HealthRecordVO> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long petId,
            @PathVariable long recordId) {
        return ApiResponse.success(HttpStatus.OK.value(), "success",
                healthRecordService.get(currentUserId(jwt), petId, recordId));
    }

    @PutMapping("/{recordId}")
    public ApiResponse<HealthRecordVO> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long petId,
            @PathVariable long recordId,
            @Valid @RequestBody HealthRecordRequest request) {
        return ApiResponse.success(HttpStatus.OK.value(), "updated",
                healthRecordService.update(currentUserId(jwt), petId, recordId, request));
    }

    @DeleteMapping("/{recordId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long petId,
            @PathVariable long recordId) {
        healthRecordService.delete(currentUserId(jwt), petId, recordId);
        return ApiResponse.success(HttpStatus.OK.value(), "deleted", null);
    }

    private long currentUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
