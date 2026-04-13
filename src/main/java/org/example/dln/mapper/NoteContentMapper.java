package org.example.dln.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.dln.entity.NoteContent;

/**
 * 包名：org.example.dln.mapper
 * 类名：NoteContentMapper
 * 类描述：提供笔记内容数据访问操作。
 * 创建人：@author Rain_润
 */
@Mapper
public interface NoteContentMapper extends BaseMapper<NoteContent> {
    /**
    * 根据笔记 ID 查询数据。
    */
    @Select("""
            SELECT *
            FROM t_note_content
            WHERE note_id = #{noteId}
            """)
    NoteContent selectByNoteId(@Param("noteId") Long noteId);
}
