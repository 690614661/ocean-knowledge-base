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

    @Autowired
    private DocMapper docMapper;

    @Autowired
    private DocService docService;

    @Autowired
    private EbookService ebookService;

    @Transactional
    public boolean toggle(Long userId, Long docId) {
        // 检查文档是否存在
        Doc doc = docService.getById(docId);
        if (doc == null) {
            throw new BusinessException("文档不存在");
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getDocId, docId);
        Favorite existing = this.getOne(wrapper);

        if (existing != null) {
            // 取消收藏
            this.removeById(existing.getId());
            docMapper.decrementFavoriteCount(docId);
            return false;
        } else {
            // 添加收藏
            Favorite fav = new Favorite();
            fav.setId(SnowFlakeUtil.nextId());
            fav.setUserId(userId);
            fav.setDocId(docId);
            this.save(fav);
            docMapper.incrementFavoriteCount(docId);
            return true;
        }
    }

    public PageResp<FavoriteResp> list(Long userId, int page, int size) {
        IPage<Favorite> favPage = this.page(new Page<>(page, size),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime));

        List<FavoriteResp> list = favPage.getRecords().stream().map(fav -> {
            FavoriteResp resp = new FavoriteResp();
            resp.setId(fav.getId());
            resp.setDocId(fav.getDocId());
            resp.setCreateTime(fav.getCreateTime());

            Doc doc = docService.getById(fav.getDocId());
            if (doc != null) {
                resp.setDocName(doc.getName());
                resp.setEbookId(doc.getEbookId());
                Ebook ebook = ebookService.getById(doc.getEbookId());
                if (ebook != null) {
                    resp.setEbookName(ebook.getName());
                }
            }
            return resp;
        }).collect(Collectors.toList());

        return new PageResp<>(favPage.getTotal(), list);
    }

    public boolean check(Long userId, Long docId) {
        return this.count(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getDocId, docId)) > 0;
    }
}
