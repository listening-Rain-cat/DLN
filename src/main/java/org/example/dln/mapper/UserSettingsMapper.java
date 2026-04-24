package org.example.dln.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.dln.entity.UserSettings;

/**
 * 包名：org.example.dln.mapper
 * 类名：UserSettingsMapper
 * 类描述：提供用户设置数据访问操作。
 * 创建人：@author Rain_润
 */
@Mapper
public interface UserSettingsMapper extends BaseMapper<UserSettings> {
    /**
    * 根据设置 ID 查询用户设置。
     * @param id ID
    */
    @Select("""
            SELECT *
            FROM t_user_settings
            WHERE id = #{id}
            """)
    UserSettings selectBySettingsId(@Param("id") Long id);

    /**
    * 根据用户 ID 查询数据。
     * @param userId 用户ID
    */
    @Select("""
            SELECT *
            FROM t_user_settings
            WHERE user_id = #{userId}
            """)
    UserSettings selectByUserId(@Param("userId") Long userId);
}
