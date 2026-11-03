package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.monitor.dto.AdminRiskUserQueryDTO;
import com.diabetes.monitor.mapper.AdminRiskUserMapper;
import com.diabetes.monitor.service.AdminRiskUserService;
import com.diabetes.monitor.vo.AdminRiskUserOverviewVO;
import com.diabetes.monitor.vo.AdminRiskUserVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Service
public class AdminRiskUserServiceImpl implements AdminRiskUserService {

    @Resource
    private AdminRiskUserMapper riskUserMapper;

    @Override
    public AdminRiskUserOverviewVO overview() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        LocalDate today = LocalDate.now();

        return riskUserMapper.selectRiskUserOverview(cutoff, today);
    }

    @Override
    public IPage<AdminRiskUserVO> pageQuery(AdminRiskUserQueryDTO query) {

        normalizeKeyword(query);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        LocalDate today = LocalDate.now();

        Page<AdminRiskUserVO> page =
                new Page<>(query.getPage(), query.getSize());
        return riskUserMapper.selectRiskUserPage(
                page,
                query,
                cutoff,
                today
        );
    }

    private void normalizeKeyword(AdminRiskUserQueryDTO query){
        if (!StringUtils.hasText(query.getKeyword())) {
            query.setKeyword(null);
            return;
        }

        query.setKeyword(query.getKeyword().trim());
    }
}
