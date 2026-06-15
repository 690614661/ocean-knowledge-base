package com.ocean.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ocean.common.BusinessException;
import com.ocean.common.Constant;
import com.ocean.common.PageResp;
import com.ocean.domain.Category;
import com.ocean.domain.Ebook;
import com.ocean.domain.dto.EbookSaveReq;
import com.ocean.mapper.EbookMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EbookService extends ServiceImpl<EbookMapper, Ebook> {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public PageResp<Ebook> list(int page, int size, String name, Long category1Id, Long category2Id) {
        LambdaQueryWrapper<Ebook> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(Ebook::getName, name);
        }
        if (category1Id != null) {
            wrapper.eq(Ebook::getCategory1Id, category1Id);
        }
        if (category2Id != null) {
            wrapper.eq(Ebook::getCategory2Id, category2Id);
        }
        wrapper.orderByDesc(Ebook::getCreateTime);

        IPage<Ebook> ebookPage = this.page(new Page<>(page, size), wrapper);

        // 填充分类名称
        List<Category> allCategories = categoryService.all();
        Map<Long, String> categoryMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        ebookPage.getRecords().forEach(ebook -> {
            ebook.setCategory1Name(categoryMap.get(ebook.getCategory1Id()));
            ebook.setCategory2Name(categoryMap.get(ebook.getCategory2Id()));
        });

        // 缓存电子书列表（仅缓存第一页无筛选条件）
        if (page == 1 && !StringUtils.hasText(name) && category1Id == null && category2Id == null) {
            PageResp<Ebook> pageResp = new PageResp<>(ebookPage.getTotal(), ebookPage.getRecords());
            redisTemplate.opsForValue().set(Constant.CACHE_EBOOK_LIST, pageResp, 5, java.util.concurrent.TimeUnit.MINUTES);
        }

        return new PageResp<>(ebookPage.getTotal(), ebookPage.getRecords());
    }

    public void save(EbookSaveReq req) {
        Ebook ebook = new Ebook();
        if (req.getId() != null) {
            ebook = this.getById(req.getId());
            if (ebook == null) {
                throw new BusinessException("电子书不存在");
            }
        }
        ebook.setName(req.getName());
        ebook.setCategory1Id(req.getCategory1Id());
        ebook.setCategory2Id(req.getCategory2Id());
        ebook.setDescription(req.getDescription());
        ebook.setCover(req.getCover());
        if (req.getId() == null) {
            ebook.setDocCount(0);
            ebook.setViewCount(0);
            ebook.setVoteCount(0);
        }
        this.saveOrUpdate(ebook);
    }

    public void delete(Long id) {
        this.removeById(id);
    }
}
