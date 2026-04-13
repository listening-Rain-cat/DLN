package org.example.dln.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.dln.entity.NoteHistory;

/**
 * 包名：org.example.dln.mapper
 * 类名：NoteHistoryMapper
 * 类描述：提供笔记历史记录数据访问操作。
 * 创建人：@author Rain_润
 */
@Mapper
public interface NoteHistoryMapper extends BaseMapper<NoteHistory> {
    /**
    * 查询笔记最新历史版本。
    */
    @Select("""
            SELECT *
            FROM t_note_history
            WHERE note_id = #{noteId}
            ORDER BY version_no DESC
            LIMIT 1
            """)
    NoteHistory selectLatestByNoteId(@Param("noteId") Long noteId);
}
