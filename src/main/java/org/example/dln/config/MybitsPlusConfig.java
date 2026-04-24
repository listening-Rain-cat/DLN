package org.example.dln.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.springframework.context.annotation.Configuration;
import org.apache.ibatis.reflection.MetaObject;
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
    * 自动填充新增记录的公共字段。
     * @param metaObject MyBatis元对象
    */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        //这个fieldName要和实体类中的字段名一致并非数据库中的字段名
        this.setFieldValByName("createdTime", now, metaObject);
        this.setFieldValByName("updatedTime", now, metaObject);
    }

    /**
    * 自动填充更新记录的公共字段。
     * @param metaObject MyBatis元对象
    */
    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.setFieldValByName("updatedTime", now, metaObject);
    }
}
