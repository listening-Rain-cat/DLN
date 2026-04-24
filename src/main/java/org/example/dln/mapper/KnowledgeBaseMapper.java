package org.example.dln.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.dln.entity.KnowledgeBase;

import java.util.List;

/**
 * 包名：org.example.dln.mapper
 * 类名：KnowledgeBaseMapper
 * 类描述：提供知识库数据访问操作。
 * 创建人：@author Rain_润
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {
    /**
    * 根据知识库 ID 查询知识库。
     * @param knowledgeBaseId 知识库ID
    */
    @Select("""
            SELECT *
            FROM t_knowledge_base
            WHERE id = #{knowledgeBaseId}
            """)
    KnowledgeBase selectByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);

    /**
    * 按更新时间倒序查询用户的有效知识库列表。
     * @param userId 用户ID
    */
    @Select("""
            SELECT *
            FROM t_knowledge_base
            WHERE user_id = #{userId}
              AND status = 1
              AND delete_token = 0
            ORDER BY updated_time DESC
            """)
    List<KnowledgeBase> selectActiveByUserIdOrderByUpdatedTimeDesc(@Param("userId") Long userId);

    /**
    * 按用户和名称查询有效知识库。
     * @param userId 用户ID
     * @param name 名称
    */
    @Select("""
            SELECT *
            FROM t_knowledge_base
            WHERE user_id = #{userId}
              AND name = #{name}
              AND status = 1
              AND delete_token = 0
            """)
    List<KnowledgeBase> selectActiveByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);

    /**
    * 更新记录更新时间。
     * @param knowledgeBaseId 知识库ID
    */
    @Update("""
            UPDATE t_knowledge_base
            SET updated_time = NOW()
            WHERE id = #{knowledgeBaseId}
            """)
    int touchUpdatedTime(@Param("knowledgeBaseId") Long knowledgeBaseId);
}
