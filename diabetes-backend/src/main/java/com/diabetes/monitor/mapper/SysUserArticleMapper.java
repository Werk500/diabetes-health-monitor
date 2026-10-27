package com.diabetes.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diabetes.monitor.entity.SysUserArticle;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserArticleMapper extends BaseMapper<SysUserArticle> {

    @Insert("""
<script>
INSERT INTO sys_user_article(user_id,article_id,push_time)
VALUES
<foreach collection="records" item="record" separator=",">
(#{record.userId},#{record.articleId}, #{record.pushTime})
</foreach>
 ON DUPLICATE KEY UPDATE id = id
</script>
""")
    int insertIgnoreBatch(@Param("records") List<SysUserArticle> records);
}
