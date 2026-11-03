package com.diabetes.monitor.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.monitor.dto.AdminRiskUserQueryDTO;
import com.diabetes.monitor.vo.AdminRiskUserOverviewVO;
import com.diabetes.monitor.vo.AdminRiskUserVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface AdminRiskUserMapper {

    IPage<AdminRiskUserVO> selectRiskUserPage(
            Page<AdminRiskUserVO> page,
            @Param("query") AdminRiskUserQueryDTO query,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("today") LocalDate today
    );

    AdminRiskUserOverviewVO selectRiskUserOverview(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("today") LocalDate today
    );
}
