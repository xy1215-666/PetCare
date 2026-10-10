package io.petcare.backend.deworming;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DewormingMapper extends BaseMapper<DewormingRecord> {
}
