package org.example.dln.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.dln.entity.Folder;

import java.util.List;

/**
 * 包名：org.example.dln.mapper
 * 类名：FolderMapper
 * 类描述：提供文件夹数据访问操作。
 * 创建人：@author Rain_润
 */
@Mapper
public interface FolderMapper extends BaseMapper<Folder> {
    /**
    * 根据文件夹 ID 查询文件夹。
    */
    @Select("""
            SELECT *
            FROM t_folder
            WHERE id = #{folderId}
            """)
    Folder selectByFolderId(@Param("folderId") Long folderId);

    /**
    * 按名称升序查询知识库下的有效文件夹。
    */
    @Select("""
            SELECT *
            FROM t_folder
            WHERE knowledge_base_id = #{knowledgeBaseId}
              AND status = 1
              AND delete_token = 0
            ORDER BY name ASC
            """)
    List<Folder> selectActiveByKnowledgeBaseIdOrderByNameAsc(@Param("knowledgeBaseId") Long knowledgeBaseId);

    /**
    * 统计父级目录下的有效文件夹数量。
    */
    @Select("""
            SELECT COUNT(*)
            FROM t_folder
            WHERE parent_id = #{parentId}
              AND status = 1
              AND delete_token = 0
            """)
    Long countActiveByParentId(@Param("parentId") Long parentId);

    /**
    * 按知识库、父级目录和名称查询有效文件夹。
    */
    @Select("""
            <script>
            SELECT *
            FROM t_folder
            WHERE knowledge_base_id = #{knowledgeBaseId}
              AND name = #{name}
              AND status = 1
              AND delete_token = 0
            <if test="parentId == null">
              AND parent_id IS NULL
            </if>
            <if test="parentId != null">
              AND parent_id = #{parentId}
            </if>
            </script>
            """)
    List<Folder> selectActiveByKnowledgeBaseIdAndParentIdAndName(
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("parentId") Long parentId,
            @Param("name") String name
    );
}
