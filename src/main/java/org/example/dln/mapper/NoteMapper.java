package org.example.dln.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.dln.entity.Note;

import java.util.List;

/**
 * 包名：org.example.dln.mapper
 * 类名：NoteMapper
 * 类描述：提供笔记数据访问操作。
 * 创建人：@author Rain_润
 */
@Mapper
public interface NoteMapper extends BaseMapper<Note> {
    /**
    * 根据笔记 ID 查询数据。
    */
    @Select("""
            SELECT *
            FROM t_note
            WHERE id = #{noteId}
            """)
    Note selectByNoteId(@Param("noteId") Long noteId);

    /**
    * 按标题升序查询用户在知识库下的有效笔记。
    */
    @Select("""
            SELECT *
            FROM t_note
            WHERE user_id = #{userId}
              AND knowledge_base_id = #{knowledgeBaseId}
              AND status = 1
              AND delete_token = 0
            ORDER BY title ASC
            """)
    List<Note> selectActiveByUserIdAndKnowledgeBaseIdOrderByTitleAsc(
            @Param("userId") Long userId,
            @Param("knowledgeBaseId") Long knowledgeBaseId
    );

    /**
    * 按知识库和标题查询有效笔记。
    */
    @Select("""
            SELECT *
            FROM t_note
            WHERE knowledge_base_id = #{knowledgeBaseId}
              AND title = #{title}
              AND status = 1
              AND delete_token = 0
            """)
    List<Note> selectActiveByKnowledgeBaseIdAndTitle(
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("title") String title
    );

    /**
    * 查询知识库下的有效笔记列表。
    */
    @Select("""
            SELECT *
            FROM t_note
            WHERE knowledge_base_id = #{knowledgeBaseId}
              AND status = 1
              AND delete_token = 0
            """)
    List<Note> selectActiveByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);

    /**
    * 按知识库和标题查询首条有效笔记。
    */
    @Select("""
            SELECT *
            FROM t_note
            WHERE knowledge_base_id = #{knowledgeBaseId}
              AND title = #{title}
              AND status = 1
              AND delete_token = 0
            LIMIT 1
            """)
    Note selectActiveFirstByKnowledgeBaseIdAndTitle(
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("title") String title
    );

    /**
    * 统计文件夹下的有效笔记数量。
    */
    @Select("""
            SELECT COUNT(*)
            FROM t_note
            WHERE folder_id = #{folderId}
              AND status = 1
              AND delete_token = 0
            """)
    Long countActiveByFolderId(@Param("folderId") Long folderId);

    /**
    * 更新记录更新时间。
    */
    @Update("""
            UPDATE t_note
            SET updated_time = NOW()
            WHERE id = #{noteId}
            """)
    int touchUpdatedTime(@Param("noteId") Long noteId);
}
