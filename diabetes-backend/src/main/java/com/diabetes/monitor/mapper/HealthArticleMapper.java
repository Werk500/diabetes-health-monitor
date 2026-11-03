package com.diabetes.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diabetes.monitor.entity.HealthArticle;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface HealthArticleMapper extends BaseMapper<HealthArticle> {


    @Select("""
    SELECT category,COUNT(*) AS total
    FROM health_article
    WHERE category IS NOT NULL
    GROUP BY category
""")
    List<Map<String,Object>> countGroupedByCategory();
}