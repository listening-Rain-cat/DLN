package org.example.dln.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.dln.entity.NoteHistory;

import java.util.List;

/**
 * 包名：org.example.dln.mapper
 * 类名：NoteHistoryMapper
 * 类描述：提供笔记历史记录数据访问操作。
 * 创建人：@author Rain_润
 */
@Mapper
public interface NoteHistoryMapper extends BaseMapper<NoteHistory> {
    /**
    * 按版本号倒序查询笔记历史版本列表。
     * @param noteId 笔记ID
    */
    @Select("""
            SELECT *
            FROM t_note_history
            WHERE note_id = #{noteId}
            ORDER BY version_no DESC, created_time DESC
            """)
    List<NoteHistory> selectByNoteIdOrderByVersionNoDesc(@Param("noteId") Long noteId);

    /**
    * 查询笔记最新历史版本。
     * @param noteId 笔记ID
    */
    @Select("""
            SELECT *
            FROM t_note_history
            WHERE note_id = #{noteId}
            ORDER BY version_no DESC
            LIMIT 1
            """)
    NoteHistory selectLatestByNoteId(@Param("noteId") Long noteId);

    /**
    * 根据笔记 ID 和历史版本 ID 查询历史版本。
     * @param noteId 笔记ID
     * @param historyId 历史版本ID
    */
    @Select("""
            SELECT *
            FROM t_note_history
            WHERE note_id = #{noteId}
              AND id = #{historyId}
            LIMIT 1
            """)
    NoteHistory selectByNoteIdAndHistoryId(@Param("noteId") Long noteId,
                                           @Param("historyId") Long historyId);

    /**
    * 根据笔记 ID 和历史版本 ID 删除历史版本。
     * @param noteId 笔记ID
     * @param historyId 历史版本ID
    */
    @Delete("""
            DELETE FROM t_note_history
            WHERE note_id = #{noteId}
              AND id = #{historyId}
            """)
    int deleteByNoteIdAndHistoryId(@Param("noteId") Long noteId,
                                   @Param("historyId") Long historyId);
}
