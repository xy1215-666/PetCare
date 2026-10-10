package io.petcare.backend.deworming;

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
@RequestMapping("/api/pets/{petId}/dewormings")
public class DewormingController {

    private final DewormingService dewormingService;

    public DewormingController(DewormingService dewormingService) {
        this.dewormingService = dewormingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DewormingVO>> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long petId,
            @Valid @RequestBody DewormingRequest request) {
        DewormingVO record = dewormingService.create(userId(jwt), petId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "created", record));
    }

    @GetMapping
    public ApiResponse<List<DewormingVO>> list(@AuthenticationPrincipal Jwt jwt, @PathVariable long petId) {
        return ApiResponse.success(HttpStatus.OK.value(), "success", dewormingService.list(userId(jwt), petId));
    }

    @GetMapping("/{recordId}")
    public ApiResponse<DewormingVO> get(
            @AuthenticationPrincipal Jwt jwt, @PathVariable long petId, @PathVariable long recordId) {
        return ApiResponse.success(HttpStatus.OK.value(), "success",
                dewormingService.get(userId(jwt), petId, recordId));
    }

    @PutMapping("/{recordId}")
    public ApiResponse<DewormingVO> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long petId,
            @PathVariable long recordId,
            @Valid @RequestBody DewormingRequest request) {
        return ApiResponse.success(HttpStatus.OK.value(), "updated",
                dewormingService.update(userId(jwt), petId, recordId, request));
    }

    @DeleteMapping("/{recordId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal Jwt jwt, @PathVariable long petId, @PathVariable long recordId) {
        dewormingService.delete(userId(jwt), petId, recordId);
        return ApiResponse.success(HttpStatus.OK.value(), "deleted", null);
    }

    private long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
