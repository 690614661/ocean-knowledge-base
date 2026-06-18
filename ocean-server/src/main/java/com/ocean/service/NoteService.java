package com.ocean.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ocean.common.BusinessException;
import com.ocean.common.PageResp;
import com.ocean.domain.Note;
import com.ocean.domain.dto.NoteSaveReq;
import com.ocean.mapper.NoteMapper;

import com.ocean.util.XssFilterUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NoteService extends ServiceImpl<NoteMapper, Note> {

    public PageResp<Note> myList(int page, int size, Long userId) {
        IPage<Note> notePage = this.page(new Page<>(page, size),
                new LambdaQueryWrapper<Note>()
                        .eq(Note::getUserId, userId)
                        .orderByDesc(Note::getUpdateTime));
        return new PageResp<>(notePage.getTotal(), notePage.getRecords());
    }

    public PageResp<Note> publicList(int page, int size, String keyword) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<Note>()
                .eq(Note::getIsPublic, 1);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Note::getTitle, keyword);
        }
        wrapper.orderByDesc(Note::getUpdateTime);
        IPage<Note> notePage = this.page(new Page<>(page, size), wrapper);
        return new PageResp<>(notePage.getTotal(), notePage.getRecords());
    }

    public Note getDetail(Long id) {
        Note note = this.getById(id);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }
        baseMapper.incrementViewCount(id);
        return note;
    }

    public void save(NoteSaveReq req, Long userId) {
        Note note = new Note();
        if (req.getId() != null) {
            note = this.getById(req.getId());
            if (note == null) {
                throw new BusinessException("笔记不存在");
            }
            if (!note.getUserId().equals(userId)) {
                throw new BusinessException("只能编辑自己的笔记");
            }
        } else {
            note.setUserId(userId);
            note.setViewCount(0);
            note.setVoteCount(0);
        }
        note.setTitle(req.getTitle());
        if (req.getContent() != null) {
            note.setContent(XssFilterUtil.filterRichText(req.getContent()));
        }
        note.setIsPublic(req.getIsPublic() != null ? req.getIsPublic() : 0);
        this.saveOrUpdate(note);
    }

    public void delete(Long id, Long userId) {
        Note note = this.getById(id);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己的笔记");
        }
        this.removeById(id);
    }

    public void vote(Long id, String ip) {
        Note note = this.getById(id);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }
        baseMapper.incrementVoteCount(id);
    }
}
