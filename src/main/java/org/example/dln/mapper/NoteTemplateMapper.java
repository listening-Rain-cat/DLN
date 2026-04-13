package org.example.dln.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.dln.entity.NoteTemplate;

import java.util.List;

/**
 * 包名：org.example.dln.mapper
 * 类名：NoteTemplateMapper
 * 类描述：提供笔记模板数据访问操作。
 * 创建人：@author Rain_润
 */
@Mapper
public interface NoteTemplateMapper extends BaseMapper<NoteTemplate> {
    /**
    * 根据模板 ID 和用户 ID 查询模板。
    */
    @Select("""
            SELECT *
            FROM t_note_template
            WHERE id = #{templateId}
              AND user_id = #{userId}
            """)
    NoteTemplate selectByTemplateIdAndUserId(
            @Param("templateId") Long templateId,
            @Param("userId") Long userId
    );

    /**
    * 按更新时间倒序查询用户模板列表。
    */
    @Select("""
            SELECT *
            FROM t_note_template
            WHERE user_id = #{userId}
            ORDER BY updated_time DESC
            """)
    List<NoteTemplate> selectByUserIdOrderByUpdatedTimeDesc(@Param("userId") Long userId);

    /**
    * 根据用户 ID 和名称查询模板。
    */
    @Select("""
            SELECT *
            FROM t_note_template
            WHERE user_id = #{userId}
              AND name = #{name}
            """)
    List<NoteTemplate> selectByUserIdAndName(
            @Param("userId") Long userId,
            @Param("name") String name
    );
}
