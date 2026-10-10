package io.petcare.backend.vaccination;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VaccinationMapper extends BaseMapper<VaccinationRecord> {
}
