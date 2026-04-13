package org.example.dln.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.dln.entity.NoteTag;

import java.util.Collection;
import java.util.List;

/**
 * 包名：org.example.dln.mapper
 * 类名：NoteTagMapper
 * 类描述：提供笔记标签关联数据访问操作。
 * 创建人：@author Rain_润
 */
@Mapper
public interface NoteTagMapper extends BaseMapper<NoteTag> {
    /**
    * 按标签 ID 删除关联数据。
    */
    @Delete("""
            DELETE FROM t_note_tag
            WHERE tag_id = #{tagId}
            """)
    int deleteByTagId(@Param("tagId") Long tagId);

    /**
    * 按笔记 ID 删除数据。
    */
    @Delete("""
            DELETE FROM t_note_tag
            WHERE note_id = #{noteId}
            """)
    int deleteByNoteId(@Param("noteId") Long noteId);

    /**
    * 根据笔记 ID 查询数据。
    */
    @Select("""
            SELECT *
            FROM t_note_tag
            WHERE note_id = #{noteId}
            """)
    List<NoteTag> selectByNoteId(@Param("noteId") Long noteId);

    /**
    * 根据笔记 ID 集合查询关联数据。
    */
    @Select("""
            <script>
            SELECT *
            FROM t_note_tag
            WHERE note_id IN
            <foreach collection="noteIds" item="noteId" open="(" separator="," close=")">
              #{noteId}
            </foreach>
            </script>
            """)
    List<NoteTag> selectByNoteIds(@Param("noteIds") Collection<Long> noteIds);
}
