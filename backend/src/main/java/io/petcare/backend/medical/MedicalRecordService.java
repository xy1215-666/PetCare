package io.petcare.backend.medical;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.petcare.backend.common.BusinessDate;
import io.petcare.backend.common.exception.BadRequestException;
import io.petcare.backend.common.exception.NotFoundException;
import io.petcare.backend.pet.PetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class MedicalRecordService {

    private final MedicalRecordMapper medicalRecordMapper;
    private final PetService petService;
    private final Clock clock;

    public MedicalRecordService(MedicalRecordMapper medicalRecordMapper, PetService petService, Clock clock) {
        this.medicalRecordMapper = medicalRecordMapper;
        this.petService = petService;
        this.clock = clock;
    }

    @Transactional
    public MedicalRecordVO create(long userId, long petId, MedicalRecordRequest request) {
        petService.requireOwnedPet(userId, petId);
        validate(request);
        MedicalRecord record = new MedicalRecord();
        record.setPetId(petId);
        apply(record, request);
        record.setDeleted(0);
        medicalRecordMapper.insert(record);
        return toView(medicalRecordMapper.selectById(record.getId()));
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordVO> list(long userId, long petId) {
        petService.requireOwnedPet(userId, petId);
        return medicalRecordMapper.selectList(new QueryWrapper<MedicalRecord>()
                        .eq("pet_id", petId)
                        .eq("deleted", 0)
                        .orderByDesc("visit_date")
                        .orderByDesc("id"))
                .stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public MedicalRecordVO get(long userId, long petId, long recordId) {
        petService.requireOwnedPet(userId, petId);
        return toView(findRecord(petId, recordId));
    }

    @Transactional
    public MedicalRecordVO update(long userId, long petId, long recordId, MedicalRecordRequest request) {
        petService.requireOwnedPet(userId, petId);
        validate(request);
        MedicalRecord record = findRecord(petId, recordId);
        apply(record, request);
        medicalRecordMapper.updateById(record);
        return toView(medicalRecordMapper.selectById(recordId));
    }

    @Transactional
    public void delete(long userId, long petId, long recordId) {
        petService.requireOwnedPet(userId, petId);
        findRecord(petId, recordId);
        medicalRecordMapper.update(null, new UpdateWrapper<MedicalRecord>()
                .eq("id", recordId).eq("pet_id", petId).eq("deleted", 0).set("deleted", 1));
    }

    private void validate(MedicalRecordRequest request) {
        LocalDate today = BusinessDate.today(clock);
        if (request.visitDate().isAfter(today)) {
            throw new BadRequestException("visitDate cannot be in the future");
        }
        if (request.nextVisitDate() != null && request.nextVisitDate().isBefore(request.visitDate())) {
            throw new BadRequestException("nextVisitDate cannot be before visitDate");
        }
    }

    private MedicalRecord findRecord(long petId, long recordId) {
        MedicalRecord record = medicalRecordMapper.selectOne(new QueryWrapper<MedicalRecord>()
                .eq("id", recordId).eq("pet_id", petId).eq("deleted", 0));
        if (record == null) {
            throw new NotFoundException("medical record was not found");
        }
        return record;
    }

    private void apply(MedicalRecord record, MedicalRecordRequest request) {
        record.setVisitDate(request.visitDate());
        record.setHospital(request.hospital());
        record.setReason(request.reason());
        record.setExamination(request.examination());
        record.setResult(request.result());
        record.setMedicine(request.medicine());
        record.setDoctorAdvice(request.doctorAdvice());
        record.setCost(request.cost());
        record.setNextVisitDate(request.nextVisitDate());
        record.setRemark(request.remark());
    }

    private MedicalRecordVO toView(MedicalRecord record) {
        return new MedicalRecordVO(record.getId(), record.getPetId(), record.getVisitDate(), record.getHospital(),
                record.getReason(), record.getExamination(), record.getResult(), record.getMedicine(),
                record.getDoctorAdvice(), record.getCost(), record.getNextVisitDate(), record.getRemark(),
                record.getCreateTime(), record.getUpdateTime());
    }
}
