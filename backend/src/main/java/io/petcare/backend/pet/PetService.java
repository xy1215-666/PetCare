package io.petcare.backend.pet;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.petcare.backend.common.BusinessDate;
import io.petcare.backend.common.exception.NotFoundException;
import io.petcare.backend.user.UserService;
import io.petcare.backend.weight.WeightRecord;
import io.petcare.backend.weight.WeightRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

@Service
public class PetService {

    private final PetMapper petMapper;
    private final UserService userService;
    private final WeightRecordMapper weightRecordMapper;
    private final Clock clock;

    public PetService(PetMapper petMapper, UserService userService, WeightRecordMapper weightRecordMapper, Clock clock) {
        this.petMapper = petMapper;
        this.userService = userService;
        this.weightRecordMapper = weightRecordMapper;
        this.clock = clock;
    }

    @Transactional
    public PetVO create(long currentUserId, CreatePetRequest request) {
        userService.getCurrentUser(currentUserId);

        Pet pet = new Pet();
        pet.setUserId(currentUserId);
        pet.setName(request.name());
        pet.setType(request.type().name());
        pet.setBreed(emptyToNull(request.breed()));
        pet.setGender(request.gender() == null ? PetGender.UNKNOWN.name() : request.gender().name());
        pet.setBirthday(request.birthday());
        pet.setCurrentWeight(request.weight());
        pet.setNeutered(request.neutered());
        pet.setHomeDate(request.homeDate());
        pet.setRemark(emptyToNull(request.remark()));
        pet.setDeleted(0);
        petMapper.insert(pet);

        if (request.weight() != null) {
            WeightRecord initialWeight = new WeightRecord();
            initialWeight.setPetId(pet.getId());
            initialWeight.setWeight(request.weight());
            initialWeight.setRecordDate(BusinessDate.today(clock));
            initialWeight.setDeleted(0);
            weightRecordMapper.insert(initialWeight);
        }

        Pet createdPet = petMapper.selectOne(new QueryWrapper<Pet>()
                .eq("id", pet.getId())
                .eq("user_id", currentUserId)
                .eq("deleted", 0));
        if (createdPet == null) {
            throw new NotFoundException("pet was not found after creation");
        }
        return toView(createdPet);
    }

    @Transactional(readOnly = true)
    public List<PetVO> list(long currentUserId) {
        userService.getCurrentUser(currentUserId);
        return petMapper.selectList(new QueryWrapper<Pet>()
                        .eq("user_id", currentUserId)
                        .eq("deleted", 0)
                        .orderByDesc("create_time")
                        .orderByDesc("id"))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PetVO> findOwnedByName(long currentUserId, String petName) {
        userService.getCurrentUser(currentUserId);
        if (petName == null || petName.isBlank()) {
            return List.of();
        }
        return petMapper.selectList(new QueryWrapper<Pet>()
                        .eq("user_id", currentUserId)
                        .eq("name", petName.trim())
                        .eq("deleted", 0)
                        .orderByAsc("id"))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PetVO get(long currentUserId, long petId) {
        return toView(requireOwnedPet(currentUserId, petId));
    }

    @Transactional(readOnly = true)
    public Pet requireOwnedPet(long currentUserId, long petId) {
        userService.getCurrentUser(currentUserId);
        Pet pet = petMapper.selectOne(new QueryWrapper<Pet>()
                .eq("id", petId)
                .eq("user_id", currentUserId)
                .eq("deleted", 0));
        if (pet == null) {
            throw new NotFoundException("pet was not found");
        }
        return pet;
    }

    @Transactional
    public Pet requireOwnedPetForUpdate(long currentUserId, long petId) {
        userService.getCurrentUser(currentUserId);
        Pet pet = petMapper.selectOne(new QueryWrapper<Pet>()
                .eq("id", petId)
                .eq("user_id", currentUserId)
                .eq("deleted", 0)
                .last("FOR UPDATE"));
        if (pet == null) {
            throw new NotFoundException("pet was not found");
        }
        return pet;
    }

    @Transactional
    public void updateCurrentWeight(long currentUserId, long petId, BigDecimal weight) {
        int updated = petMapper.update(null, new UpdateWrapper<Pet>()
                .eq("id", petId)
                .eq("user_id", currentUserId)
                .eq("deleted", 0)
                .set("current_weight", weight));
        if (updated != 1) {
            throw new NotFoundException("pet was not found");
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private PetVO toView(Pet pet) {
        return new PetVO(
                pet.getId(),
                pet.getName(),
                PetType.valueOf(pet.getType()),
                pet.getBreed(),
                PetGender.valueOf(pet.getGender()),
                pet.getBirthday(),
                pet.getCurrentWeight(),
                pet.getNeutered(),
                pet.getHomeDate(),
                pet.getRemark(),
                pet.getCreateTime());
    }
}
