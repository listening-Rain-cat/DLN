package org.example.dln.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.dln.entity.User;

/**
 * 包名：org.example.dln.mapper
 * 类名：UserMapper
 * 类描述：提供用户数据访问操作。
 * 创建人：@author Rain_润
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
    * 根据用户 ID 查询数据。
     * @param userId 用户ID
    */
    @Select("""
            SELECT *
            FROM t_user
            WHERE id = #{userId}
            """)
    User selectByUserId(@Param("userId") Long userId);

    /**
    * 根据用户名查询用户。
     * @param username 用户名
    */
    @Select("""
            SELECT *
            FROM t_user
            WHERE username = #{username}
            """)
    User selectByUsername(@Param("username") String username);

    /**
    * 根据邮箱查询用户。
     * @param email 邮箱
    */
    @Select("""
            SELECT *
            FROM t_user
            WHERE email = #{email}
            """)
    User selectByEmail(@Param("email") String email);
}
