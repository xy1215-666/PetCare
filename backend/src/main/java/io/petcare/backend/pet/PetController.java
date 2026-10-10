package io.petcare.backend.pet;

import io.petcare.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PetVO>> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePetRequest request) {
        long currentUserId = Long.parseLong(jwt.getSubject());
        PetVO pet = petService.create(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "created", pet));
    }

    @GetMapping
    public ApiResponse<List<PetVO>> list(@AuthenticationPrincipal Jwt jwt) {
        long currentUserId = Long.parseLong(jwt.getSubject());
        return ApiResponse.success(HttpStatus.OK.value(), "success", petService.list(currentUserId));
    }

    @GetMapping("/{petId}")
    public ApiResponse<PetVO> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long petId) {
        long currentUserId = Long.parseLong(jwt.getSubject());
        return ApiResponse.success(HttpStatus.OK.value(), "success", petService.get(currentUserId, petId));
    }
}
