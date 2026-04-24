package org.example.dln.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.example.dln.entity.Tag;

import java.util.Collection;
import java.util.List;

/**
 * 包名：org.example.dln.mapper
 * 类名：TagMapper
 * 类描述：提供标签数据访问操作。
 * 创建人：@author Rain_润
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {
    /**
    * 根据标签 ID 查询标签。
     * @param tagId 标签ID
    */
    @Select("""
            SELECT *
            FROM t_tag
            WHERE id = #{tagId}
            """)
    Tag selectByTagId(@Param("tagId") Long tagId);

    /**
    * 按名称升序查询知识库标签列表。
     * @param knowledgeBaseId 知识库ID
    */
    @Select("""
            SELECT *
            FROM t_tag
            WHERE knowledge_base_id = #{knowledgeBaseId}
            ORDER BY name ASC
            """)
    List<Tag> selectByKnowledgeBaseIdOrderByNameAsc(@Param("knowledgeBaseId") Long knowledgeBaseId);

    /**
    * 根据知识库和名称查询标签。
     * @param knowledgeBaseId 知识库ID
     * @param name 名称
    */
    @Select("""
            SELECT *
            FROM t_tag
            WHERE knowledge_base_id = #{knowledgeBaseId}
              AND name = #{name}
            """)
    List<Tag> selectByKnowledgeBaseIdAndName(
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("name") String name
    );

    /**
    * 根据标签 ID 集合查询标签列表。
     * @param tagIds 标签ID列表
    */
    @Select("""
            <script>
            SELECT *
            FROM t_tag
            WHERE id IN
            <foreach collection="tagIds" item="tagId" open="(" separator="," close=")">
              #{tagId}
            </foreach>
            </script>
            """)
    List<Tag> selectByTagIds(@Param("tagIds") Collection<Long> tagIds);

    /**
    * 按知识库 ID 删除数据。
     * @param knowledgeBaseId 知识库ID
    */
    @Delete("""
            DELETE FROM t_tag
            WHERE knowledge_base_id = #{knowledgeBaseId}
            """)
    int deleteByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);
}
