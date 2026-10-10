package io.petcare.backend.ai.tool;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.petcare.backend.pet.PetVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PetProfileToolResult(
        Status status,
        Long petId,
        String name,
        String type,
        String breed,
        String gender,
        LocalDate birthday,
        BigDecimal currentWeight,
        Boolean neutered,
        List<PetMatch> matches,
        String message) {

    public PetProfileToolResult {
        Objects.requireNonNull(status, "status is required");
        matches = matches == null ? List.of() : List.copyOf(matches);
    }

    public static PetProfileToolResult found(PetVO pet) {
        return new PetProfileToolResult(
                Status.FOUND,
                pet.id(),
                pet.name(),
                pet.type().name(),
                pet.breed(),
                pet.gender().name(),
                pet.birthday(),
                pet.weight(),
                pet.neutered(),
                List.of(),
                "已找到宠物档案。");
    }

    public static PetProfileToolResult notFound(String petName) {
        return new PetProfileToolResult(
                Status.NOT_FOUND,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                "未找到名为“" + petName + "”的宠物。");
    }

    public static PetProfileToolResult ambiguous(String petName, List<PetVO> pets) {
        List<PetMatch> matches = pets.stream()
                .map(pet -> new PetMatch(pet.id(), pet.name(), pet.type().name(), pet.breed()))
                .toList();
        return new PetProfileToolResult(
                Status.AMBIGUOUS,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                matches,
                "找到 " + matches.size() + " 只名为“" + petName + "”的宠物，请用户进一步确认具体宠物。");
    }

    public static PetProfileToolResult unavailable() {
        return new PetProfileToolResult(
                Status.UNAVAILABLE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                "暂时无法读取宠物档案，请稍后再试。");
    }

    public enum Status {
        FOUND,
        NOT_FOUND,
        AMBIGUOUS,
        UNAVAILABLE
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PetMatch(long petId, String name, String type, String breed) {
    }
}
