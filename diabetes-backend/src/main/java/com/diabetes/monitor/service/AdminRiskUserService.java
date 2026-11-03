package com.diabetes.monitor.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.diabetes.monitor.dto.AdminRiskUserQueryDTO;
import com.diabetes.monitor.vo.AdminRiskUserOverviewVO;
import com.diabetes.monitor.vo.AdminRiskUserVO;
import jakarta.validation.Valid;

public interface AdminRiskUserService {
    AdminRiskUserOverviewVO overview();

    IPage<AdminRiskUserVO> pageQuery(@Valid AdminRiskUserQueryDTO query);
}
