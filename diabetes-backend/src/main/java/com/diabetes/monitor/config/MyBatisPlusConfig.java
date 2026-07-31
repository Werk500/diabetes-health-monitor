package com.diabetes.monitor.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 启用 MyBatis-Plus 的分页功能
 * 配置数据库类型为 MySQL
 * 让 Page 对象的分页查询生效
 */
@Configuration
public class MyBatisPlusConfig {


    /**
     * 配置 MyBatis-Plus 拦截器
     * 主要功能：添加分页插件，支持数据库物理分页
     * @return MybatisPlusInterceptor 拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 创建 MyBatis-Plus 核心拦截器
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页内部拦截器
        // PaginationInnerInterceptor：自动拦截分页查询，生成 LIMIT 语句
        // DbType.MYSQL：指定数据库类型为 MySQL，使用 MySQL 的分页语法
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        // 返回配置好的拦截器
        return interceptor;
    }
}
