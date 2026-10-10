package io.petcare.backend.bootstrap;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BootstrapProbeMapper {

    @Select("SELECT 1")
    int selectOne();
}
