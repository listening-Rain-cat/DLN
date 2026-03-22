package org.example.dln.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.dln.entity.Note;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
}
