package org.example.dln.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.dln.entity.NoteContent;

import java.util.Collection;
import java.util.List;

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
     * @param noteId 笔记ID
    */
    @Select("""
            SELECT *
            FROM t_note_content
            WHERE note_id = #{noteId}
            """)
    NoteContent selectByNoteId(@Param("noteId") Long noteId);

    /**
    * 根据笔记 ID 集合批量查询正文。
     * @param noteIds 笔记ID列表
    */
    @Select("""
            <script>
            SELECT *
            FROM t_note_content
            WHERE note_id IN
            <foreach collection="noteIds" item="noteId" open="(" separator="," close=")">
              #{noteId}
            </foreach>
            </script>
            """)
    List<NoteContent> selectByNoteIds(@Param("noteIds") Collection<Long> noteIds);
}
