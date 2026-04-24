package org.example.dln.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.dln.entity.NoteAttachment;

import java.util.List;

/**
 * 包名：org.example.dln.mapper
 * 类名：NoteAttachmentMapper
 * 类描述：提供笔记附件数据访问操作。
 * 创建人：@author Rain_润
 */
@Mapper
public interface NoteAttachmentMapper extends BaseMapper<NoteAttachment> {
    /**
    * 根据附件 ID 查询附件。
     * @param attachmentId 附件ID
    */
    @Select("""
            SELECT *
            FROM t_note_attachment
            WHERE id = #{attachmentId}
            """)
    NoteAttachment selectByAttachmentId(@Param("attachmentId") Long attachmentId);

    /**
    * 按创建时间倒序查询笔记附件列表。
     * @param noteId 笔记ID
    */
    @Select("""
            SELECT *
            FROM t_note_attachment
            WHERE note_id = #{noteId}
            ORDER BY created_time DESC
            """)
    List<NoteAttachment> selectByNoteIdOrderByCreatedTimeDesc(@Param("noteId") Long noteId);
}
