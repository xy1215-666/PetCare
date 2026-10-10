package io.petcare.backend.ai.tool;

import io.petcare.backend.pet.PetService;
import io.petcare.backend.pet.PetVO;
import io.petcare.backend.common.exception.UnauthorizedException;
import org.springframework.dao.DataAccessException;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PetProfileTool {

    public static final String CURRENT_USER_ID_CONTEXT_KEY = "petcare.currentUserId";

    private final PetService petService;

    public PetProfileTool(PetService petService) {
        this.petService = petService;
    }

    @Tool(
            name = "getPetProfile",
            description = "根据当前已登录用户的宠物名称读取宠物基本档案。只查询当前用户自己的宠物；同名时会返回候选项并要求用户确认。")
    public PetProfileToolResult getPetProfile(
            @ToolParam(description = "要查询的宠物名称") String petName,
            ToolContext toolContext) {
        String normalizedName = petName == null ? "" : petName.trim();
        if (normalizedName.isBlank()) {
            return PetProfileToolResult.notFound(normalizedName);
        }

        long currentUserId = currentUserId(toolContext);
        List<PetVO> pets;
        try {
            pets = petService.findOwnedByName(currentUserId, normalizedName);
        } catch (DataAccessException | UnauthorizedException exception) {
            return PetProfileToolResult.unavailable();
        }
        if (pets.isEmpty()) {
            return PetProfileToolResult.notFound(normalizedName);
        }
        if (pets.size() > 1) {
            return PetProfileToolResult.ambiguous(normalizedName, pets);
        }
        return PetProfileToolResult.found(pets.getFirst());
    }

    private long currentUserId(ToolContext toolContext) {
        if (toolContext == null) {
            throw new IllegalStateException("无法确认当前登录用户，暂时无法读取宠物档案。");
        }
        Object value = toolContext.getContext().get(CURRENT_USER_ID_CONTEXT_KEY);
        if (!(value instanceof Number number) || number.longValue() <= 0) {
            throw new IllegalStateException("无法确认当前登录用户，暂时无法读取宠物档案。");
        }
        return number.longValue();
    }
}
