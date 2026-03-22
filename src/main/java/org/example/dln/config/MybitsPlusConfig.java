package org.example.dln.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * 包名称： org.example.dln.config
 * 类名称：MybitsPlusConfig
 * 类描述：Mybitsplus配置类,自动填充数据库的时间字段
 * 创建人：@author Rain_润
 * 创建时间：2026-03-18 21:59
 */
@Configuration
public class MybitsPlusConfig implements MetaObjectHandler {
    @Override
    public void insertFill(org.apache.ibatis.reflection.MetaObject metaObject) {
        //TODO - 实现自动填充功能，设置createdTime和updatedTime为当前时间
        LocalDateTime now = LocalDateTime.now();
        this.setFieldValByName("createdTime", now, metaObject);
        this.setFieldValByName("updatedTime", now, metaObject);
    }

    @Override
    public void updateFill(org.apache.ibatis.reflection.MetaObject metaObject) {
        //TODO - 实现自动填充功能，更新updatedTime为当前时间
        LocalDateTime now = LocalDateTime.now();
        this.setFieldValByName("updatedTime", now, metaObject);
    }
}