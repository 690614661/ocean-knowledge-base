package com.ocean.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ocean.common.BusinessException;
import com.ocean.common.Constant;
import com.ocean.domain.Content;
import com.ocean.domain.Doc;
import com.ocean.domain.dto.DocSaveReq;
import com.ocean.mapper.ContentMapper;
import com.ocean.mapper.DocMapper;
import com.ocean.util.XssFilterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DocService extends ServiceImpl<DocMapper, Doc> {

    @Autowired
    private ContentMapper contentMapper;

    @Autowired
    private EbookService ebookService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SearchService searchService;

    public List<Doc> tree(Long ebookId) {
        // 尝试从缓存获取
        String cacheKey = Constant.CACHE_DOC_TREE_PREFIX + ebookId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            @SuppressWarnings("unchecked")
            List<Doc> cachedList = (List<Doc>) cached;
            return cachedList;
        }

        List<Doc> allList = this.list(new LambdaQueryWrapper<Doc>()
                .eq(Doc::getEbookId, ebookId)
                .orderByAsc(Doc::getSort));
        List<Doc> tree = buildTree(allList);

        // 缓存5分钟
        redisTemplate.opsForValue().set(cacheKey, tree, 5, java.util.concurrent.TimeUnit.MINUTES);
        return tree;
    }

    public Doc getDetail(Long id, Long userId) {
        Doc doc = getDetail(id);
        // 如果用户已登录，记录阅读历史
        if (userId != null) {
            recordHistory(userId, id, doc);
        }
        return doc;
    }

    private void recordHistory(Long userId, Long docId, Doc doc) {
        try {
            String key = Constant.HISTORY_REDIS_PREFIX + userId;
            // 去重：删除已有记录
            redisTemplate.opsForList().remove(key, 0, String.valueOf(docId));
            // 左推到列表头部（最新在最前）
            redisTemplate.opsForList().leftPush(key, String.valueOf(docId));
            // 裁剪最多50条
            redisTemplate.opsForList().trim(key, 0, Constant.HISTORY_MAX_SIZE - 1);
            // 刷新TTL
            redisTemplate.expire(key, Constant.HISTORY_TTL, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("记录阅读历史失败: userId={}, docId={}", userId, docId, e);
        }
    }

    public Doc getDetail(Long id) {
        Doc doc = this.getById(id);
        if (doc == null) {
            throw new BusinessException("文档不存在");
        }

        // 阅读数 +1
        baseMapper.incrementViewCount(id);

        // 同步更新电子书阅读数
        ebookService.incrementViewCount(doc.getEbookId());

        // Redis 实时今日阅读计数（按天自动过期）
        String todayViewKey = Constant.TODAY_VIEW_COUNT_PREFIX + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        redisTemplate.opsForValue().increment(todayViewKey);
        redisTemplate.expire(todayViewKey, 48, java.util.concurrent.TimeUnit.HOURS);

        // 查询内容
        Content content = contentMapper.selectById(id);
        if (content != null) {
            doc.setContent(content.getContent());
        }
        return doc;
    }

    @Transactional
    public void save(DocSaveReq req) {
        Doc doc = new Doc();
        if (req.getId() != null) {
            doc = this.getById(req.getId());
            if (doc == null) {
                throw new BusinessException("文档不存在");
            }
        }
        doc.setEbookId(req.getEbookId());
        doc.setParent(req.getParent() != null ? req.getParent() : 0);
        doc.setName(req.getName());
        doc.setSort(req.getSort() != null ? req.getSort() : 0);

        if (req.getId() == null) {
            doc.setViewCount(0);
            doc.setVoteCount(0);
        }
        this.saveOrUpdate(doc);

        // 清除文档目录树缓存
        redisTemplate.delete(Constant.CACHE_DOC_TREE_PREFIX + req.getEbookId());

        // 保存内容
        String filteredContent = null;
        if (req.getContent() != null) {
            filteredContent = XssFilterUtil.filterRichText(req.getContent());
            Content content = contentMapper.selectById(doc.getId());
            if (content == null) {
                content = new Content();
                content.setId(doc.getId());
                content.setContent(filteredContent);
                contentMapper.insert(content);
            } else {
                content.setContent(filteredContent);
                contentMapper.updateById(content);
            }
        }

        // 同步到ES索引
        searchService.syncDocIndex(doc.getId(), doc.getName(), filteredContent != null ? filteredContent : "", doc.getEbookId());

        // 更新电子书文档数
        long docCount = this.count(new LambdaQueryWrapper<Doc>()
                .eq(Doc::getEbookId, req.getEbookId()));
        com.ocean.domain.Ebook ebook = ebookService.getById(req.getEbookId());
        if (ebook != null) {
            ebook.setDocCount((int) docCount);
            ebookService.updateById(ebook);
        }
    }

    @Transactional
    public void delete(Long id) {
        // 先获取文档信息（删除前）
        Doc docToDelete = this.getById(id);
        if (docToDelete == null) {
            throw new BusinessException("文档不存在");
        }
        Long ebookId = docToDelete.getEbookId();

        // 递归收集所有子文档ID
        List<Long> idsToDelete = new ArrayList<>();
        collectChildIds(id, idsToDelete);
        idsToDelete.add(id);

        // 清除文档目录树缓存
        redisTemplate.delete(Constant.CACHE_DOC_TREE_PREFIX + ebookId);

        // 删除内容
        idsToDelete.forEach(contentMapper::deleteById);

        // 删除文档
        this.removeByIds(idsToDelete);

        // 从ES索引删除
        for (Long deleteId : idsToDelete) {
            searchService.deleteDocIndex(deleteId);
        }

        // 更新电子书文档数
        long docCount = this.count(new LambdaQueryWrapper<Doc>()
                .eq(Doc::getEbookId, ebookId));
        com.ocean.domain.Ebook ebook = ebookService.getById(ebookId);
        if (ebook != null) {
            ebook.setDocCount((int) docCount);
            ebookService.updateById(ebook);
        }
    }

    public void vote(Long id, String ip) {
        Doc doc = this.getById(id);
        if (doc == null) {
            throw new BusinessException("文档不存在");
        }
        baseMapper.incrementVoteCount(id);
        // 同步更新电子书点赞数
        ebookService.incrementVoteCount(doc.getEbookId());
        // Redis 实时今日点赞计数（按天自动过期）
        String todayVoteKey = Constant.TODAY_VOTE_COUNT_PREFIX + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        redisTemplate.opsForValue().increment(todayVoteKey);
        redisTemplate.expire(todayVoteKey, 48, java.util.concurrent.TimeUnit.HOURS);
    }

    private void collectChildIds(Long parentId, List<Long> ids) {
        List<Doc> children = this.list(new LambdaQueryWrapper<Doc>()
                .eq(Doc::getParent, parentId));
        for (Doc child : children) {
            ids.add(child.getId());
            collectChildIds(child.getId(), ids);
        }
    }

    private List<Doc> buildTree(List<Doc> allList) {
        Map<Long, List<Doc>> parentMap = allList.stream()
                .collect(Collectors.groupingBy(Doc::getParent));

        allList.forEach(d -> d.setChildren(parentMap.getOrDefault(d.getId(), new ArrayList<>())));
        return allList.stream()
                .filter(d -> d.getParent() == 0)
                .collect(Collectors.toList());
    }
}