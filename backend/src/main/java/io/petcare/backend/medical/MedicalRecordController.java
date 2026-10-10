package io.petcare.backend.medical;

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
@RequestMapping("/api/pets/{petId}/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MedicalRecordVO>> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long petId,
            @Valid @RequestBody MedicalRecordRequest request) {
        MedicalRecordVO record = medicalRecordService.create(userId(jwt), petId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "created", record));
    }

    @GetMapping
    public ApiResponse<List<MedicalRecordVO>> list(@AuthenticationPrincipal Jwt jwt, @PathVariable long petId) {
        return ApiResponse.success(HttpStatus.OK.value(), "success",
                medicalRecordService.list(userId(jwt), petId));
    }

    @GetMapping("/{recordId}")
    public ApiResponse<MedicalRecordVO> get(
            @AuthenticationPrincipal Jwt jwt, @PathVariable long petId, @PathVariable long recordId) {
        return ApiResponse.success(HttpStatus.OK.value(), "success",
                medicalRecordService.get(userId(jwt), petId, recordId));
    }

    @PutMapping("/{recordId}")
    public ApiResponse<MedicalRecordVO> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long petId,
            @PathVariable long recordId,
            @Valid @RequestBody MedicalRecordRequest request) {
        return ApiResponse.success(HttpStatus.OK.value(), "updated",
                medicalRecordService.update(userId(jwt), petId, recordId, request));
    }

    @DeleteMapping("/{recordId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal Jwt jwt, @PathVariable long petId, @PathVariable long recordId) {
        medicalRecordService.delete(userId(jwt), petId, recordId);
        return ApiResponse.success(HttpStatus.OK.value(), "deleted", null);
    }

    private long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
