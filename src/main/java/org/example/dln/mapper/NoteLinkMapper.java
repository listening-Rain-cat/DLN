package org.example.dln.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.dln.entity.NoteLink;

import java.util.List;

/**
 * 包名：org.example.dln.mapper
 * 类名：NoteLinkMapper
 * 类描述：提供笔记双链关系数据访问操作。
 * 创建人：@author Rain_润
 */
@Mapper
public interface NoteLinkMapper extends BaseMapper<NoteLink> {
    /**
    * 查询笔记发出的双链列表。
    */
    @Select("""
            SELECT *
            FROM t_note_link
            WHERE source_note_id = #{noteId}
            ORDER BY target_note_name ASC
            """)
    List<NoteLink> selectOutgoingByNoteId(@Param("noteId") Long noteId);

    /**
    * 查询指向指定笔记的双链列表。
    */
    @Select("""
            SELECT *
            FROM t_note_link
            WHERE target_note_id = #{noteId}
            ORDER BY source_note_id ASC
            """)
    List<NoteLink> selectIncomingByNoteId(@Param("noteId") Long noteId);

    /**
    * 按源笔记 ID 删除双链数据。
    */
    @Delete("""
            DELETE FROM t_note_link
            WHERE source_note_id = #{sourceNoteId}
            """)
    int deleteBySourceNoteId(@Param("sourceNoteId") Long sourceNoteId);

    /**
    * 根据知识库 ID 查询知识库。
    */
    @Select("""
            SELECT *
            FROM t_note_link
            WHERE knowledge_base_id = #{knowledgeBaseId}
            """)
    List<NoteLink> selectByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);
}
