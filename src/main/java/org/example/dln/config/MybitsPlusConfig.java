package org.example.dln.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * 包名：org.example.dln.config
 * 类名：MybitsPlusConfig
 * 类描述：配置 MyBatis-Plus 的公共字段自动填充逻辑。
 * 创建人：@author Rain_润
 */
@Configuration
public class MybitsPlusConfig implements MetaObjectHandler {
    /**
    * 在新增时填充公共字段。
    */
    @Override
    public void insertFill(org.apache.ibatis.reflection.MetaObject metaObject) {
        // TODO - 实现自动填充功能，设置 createdTime 和 updatedTime 为当前时间
        LocalDateTime now = LocalDateTime.now();
        this.setFieldValByName("createdTime", now, metaObject);
        this.setFieldValByName("updatedTime", now, metaObject);
    }

    /**
    * 在更新时填充公共字段。
    */
    @Override
    public void updateFill(org.apache.ibatis.reflection.MetaObject metaObject) {
        // TODO - 实现自动填充功能，更新 updatedTime 为当前时间
        LocalDateTime now = LocalDateTime.now();
        this.setFieldValByName("updatedTime", now, metaObject);
    }
}
