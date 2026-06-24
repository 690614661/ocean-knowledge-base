package com.ocean.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ocean.common.BusinessException;
import com.ocean.common.PageResp;
import com.ocean.domain.Doc;
import com.ocean.domain.Ebook;
import com.ocean.domain.Favorite;
import com.ocean.domain.Note;
import com.ocean.domain.dto.FavoriteResp;
import com.ocean.mapper.DocMapper;
import com.ocean.mapper.FavoriteMapper;
import com.ocean.util.SnowFlakeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteService extends ServiceImpl<FavoriteMapper, Favorite> {

    /** 收藏类型：文档 */
    public static final int TARGET_TYPE_DOC = 1;
    /** 收藏类型：笔记 */
    public static final int TARGET_TYPE_NOTE = 2;

    @Autowired
    private DocMapper docMapper;

    @Autowired
    private DocService docService;

    @Autowired
    private EbookService ebookService;

    @Autowired
    private NoteService noteService;

    // ==================== 文档收藏 ====================

    @Transactional
    public boolean toggleDoc(Long userId, Long docId) {
        Doc doc = docService.getById(docId);
        if (doc == null) {
            throw new BusinessException("文档不存在");
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getDocId, docId)
                .eq(Favorite::getTargetType, TARGET_TYPE_DOC);
        Favorite existing = this.getOne(wrapper);

        if (existing != null) {
            this.removeById(existing.getId());
            docMapper.decrementFavoriteCount(docId);
            return false;
        } else {
            Favorite fav = new Favorite();
            fav.setId(SnowFlakeUtil.nextId());
            fav.setUserId(userId);
            fav.setDocId(docId);
            fav.setTargetType(TARGET_TYPE_DOC);
            this.save(fav);
            docMapper.incrementFavoriteCount(docId);
            return true;
        }
    }

    public boolean checkDoc(Long userId, Long docId) {
        return this.count(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getDocId, docId)
                .eq(Favorite::getTargetType, TARGET_TYPE_DOC)) > 0;
    }

    // ==================== 笔记收藏 ====================

    @Transactional
    public boolean toggleNote(Long userId, Long noteId) {
        Note note = noteService.getById(noteId);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getDocId, noteId)
                .eq(Favorite::getTargetType, TARGET_TYPE_NOTE);
        Favorite existing = this.getOne(wrapper);

        if (existing != null) {
            this.removeById(existing.getId());
            return false;
        } else {
            Favorite fav = new Favorite();
            fav.setId(SnowFlakeUtil.nextId());
            fav.setUserId(userId);
            fav.setDocId(noteId);
            fav.setTargetType(TARGET_TYPE_NOTE);
            this.save(fav);
            return true;
        }
    }

    public boolean checkNote(Long userId, Long noteId) {
        return this.count(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getDocId, noteId)
                .eq(Favorite::getTargetType, TARGET_TYPE_NOTE)) > 0;
    }

    // ==================== 收藏列表（文档+笔记混排） ====================

    public PageResp<FavoriteResp> list(Long userId, int page, int size) {
        IPage<Favorite> favPage = this.page(new Page<>(page, size),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime));

        List<FavoriteResp> list = favPage.getRecords().stream().map(fav -> {
            FavoriteResp resp = new FavoriteResp();
            resp.setId(fav.getId());
            resp.setDocId(fav.getDocId());
            resp.setTargetType(fav.getTargetType());
            resp.setCreateTime(fav.getCreateTime());

            if (TARGET_TYPE_DOC == fav.getTargetType()) {
                Doc doc = docService.getById(fav.getDocId());
                if (doc != null) {
                    resp.setName(doc.getName());
                    resp.setEbookId(doc.getEbookId());
                    Ebook ebook = ebookService.getById(doc.getEbookId());
                    if (ebook != null) {
                        resp.setEbookName(ebook.getName());
                    }
                }
            } else if (TARGET_TYPE_NOTE == fav.getTargetType()) {
                Note note = noteService.getById(fav.getDocId());
                if (note != null) {
                    resp.setName(note.getTitle());
                    resp.setEbookId(null);
                }
            }
            return resp;
        }).collect(Collectors.toList());

        return new PageResp<>(favPage.getTotal(), list);
    }
}
