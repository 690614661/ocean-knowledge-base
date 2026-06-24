package com.ocean.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ocean.common.BusinessException;
import com.ocean.common.PageResp;
import com.ocean.domain.Note;
import com.ocean.domain.dto.NoteSaveReq;
import com.ocean.domain.User;
import com.ocean.mapper.NoteMapper;
import com.ocean.mapper.UserMapper;
import com.ocean.util.XssFilterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Service
public class NoteService extends ServiceImpl<NoteMapper, Note> {

    @Autowired
    private SearchService searchService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 启动时将数据库中所有公开笔记批量同步到 ES 索引
     */
    @PostConstruct
    public void initNoteIndex() {
        try {
            List<Note> publicNotes = this.list(
                    new LambdaQueryWrapper<Note>().eq(Note::getIsPublic, 1));
            if (!publicNotes.isEmpty()) {
                searchService.syncNoteBatch(publicNotes);
            }
        } catch (Exception e) {
            log.warn("启动时批量同步笔记索引失败（ES可能未就绪，后续自动重试）", e);
        }
    }

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
        // 设置作者名
        for (Note note : notePage.getRecords()) {
            if (note.getUserId() != null) {
                User user = userMapper.selectById(note.getUserId());
                if (user != null) {
                    note.setAuthorName(user.getName());
                }
            }
        }
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
        Note note;
        boolean wasPublic = false;
        boolean isNew = req.getId() == null;

        if (!isNew) {
            note = this.getById(req.getId());
            if (note == null) {
                throw new BusinessException("笔记不存在");
            }
            if (!note.getUserId().equals(userId)) {
                throw new BusinessException("只能编辑自己的笔记");
            }
            wasPublic = note.getIsPublic() == 1;
        } else {
            note = new Note();
            note.setUserId(userId);
            note.setViewCount(0);
            note.setVoteCount(0);
        }

        note.setTitle(req.getTitle());
        if (req.getContent() != null) {
            note.setContent(XssFilterUtil.filterRichText(req.getContent()));
        }
        Integer newIsPublic = req.getIsPublic() != null ? req.getIsPublic() : 0;
        note.setIsPublic(newIsPublic);
        this.saveOrUpdate(note);

        // 同步 ES 索引
        boolean nowPublic = newIsPublic == 1;
        if (nowPublic) {
            // 公开笔记 → 同步到 ES
            searchService.syncNoteIndex(note.getId(), note.getTitle(), note.getContent(), userId, true);
        } else if (wasPublic && !nowPublic) {
            // 从公开改为私有 → 从 ES 删除
            searchService.deleteNoteIndex(note.getId());
        }
        // 如果一直是私有笔记，不做任何操作
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

        // 如果是公开笔记，同步删除 ES 索引
        if (note.getIsPublic() == 1) {
            searchService.deleteNoteIndex(id);
        }
    }

    /**
     * 批量删除笔记
     */
    public void deleteBatch(List<Long> ids, Long userId) {
        for (Long id : ids) {
            Note note = this.getById(id);
            if (note == null) continue;
            if (!note.getUserId().equals(userId)) continue;
            this.removeById(id);
            if (note.getIsPublic() == 1) {
                searchService.deleteNoteIndex(id);
            }
        }
    }

    public void vote(Long id, String ip) {
        Note note = this.getById(id);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }
        baseMapper.incrementVoteCount(id);
    }
}
