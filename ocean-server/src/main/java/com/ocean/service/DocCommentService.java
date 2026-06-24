package com.ocean.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ocean.common.BusinessException;
import com.ocean.domain.DocComment;
import com.ocean.domain.User;
import com.ocean.domain.dto.DocCommentSaveReq;
import com.ocean.mapper.DocCommentMapper;
import com.ocean.mapper.UserMapper;
import com.ocean.util.SnowFlakeUtil;
import com.ocean.util.XssFilterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocCommentService extends ServiceImpl<DocCommentMapper, DocComment> {

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取文档评论列表（树形结构，两级嵌套）
     */
    public List<DocComment> listByDocId(Long docId) {
        List<DocComment> all = this.list(new LambdaQueryWrapper<DocComment>()
                .eq(DocComment::getDocId, docId)
                .orderByAsc(DocComment::getCreateTime));

        // 填充用户头像
        for (DocComment comment : all) {
            if (comment.getUserId() != null) {
                User user = userMapper.selectById(comment.getUserId());
                if (user != null) {
                    comment.setUserAvatar(user.getAvatar());
                }
            }
        }

        // 构建树：根评论 + 子评论
        List<DocComment> roots = new ArrayList<>();
        Map<Long, List<DocComment>> childrenMap = all.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(DocComment::getParentId));

        for (DocComment comment : all) {
            if (comment.getParentId() == null) {
                List<DocComment> children = childrenMap.getOrDefault(comment.getId(), new ArrayList<>());
                children.forEach(child -> child.setHasChildren(false));
                comment.setChildren(children);
                comment.setHasChildren(!children.isEmpty());
                roots.add(comment);
            }
        }

        return roots;
    }

    /**
     * 发表评论
     */
    @Transactional
    public DocComment save(DocCommentSaveReq req, Long userId, String userName) {
        // 如果是回复，验证父评论存在
        if (req.getParentId() != null) {
            DocComment parent = this.getById(req.getParentId());
            if (parent == null) {
                throw new BusinessException("回复的评论不存在");
            }
        }

        DocComment comment = new DocComment();
        comment.setId(SnowFlakeUtil.nextId());
        comment.setDocId(req.getDocId());
        comment.setParentId(req.getParentId());
        comment.setReplyToUserId(req.getReplyToUserId());
        comment.setReplyToUserName(req.getReplyToUserName());
        comment.setUserId(userId);
        comment.setUserName(userName);
        comment.setContent(XssFilterUtil.filterRichText(req.getContent()));
        this.save(comment);
        return comment;
    }

    /**
     * 删除评论（级联删除子评论）
     */
    @Transactional
    public void delete(Long id, Long userId) {
        DocComment comment = this.getById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己的评论");
        }

        // 删除子评论
        this.remove(new LambdaQueryWrapper<DocComment>()
                .eq(DocComment::getParentId, id));
        // 删除自己
        this.removeById(id);
    }
}
