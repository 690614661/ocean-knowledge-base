package com.ocean.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ocean.common.BusinessException;
import com.ocean.domain.Category;
import com.ocean.domain.dto.CategorySaveReq;
import com.ocean.mapper.CategoryMapper;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService extends ServiceImpl<CategoryMapper, Category> {

    public List<Category> tree() {
        List<Category> allList = this.list(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort));
        return buildTree(allList);
    }

    public List<Category> all() {
        return this.list(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
    }

    public void save(CategorySaveReq req) {
        if (req.getId() != null && req.getId().equals(req.getParent())) {
            throw new BusinessException("不能将自己设为父分类");
        }

        Category category = new Category();
        if (req.getId() != null) {
            category = this.getById(req.getId());
            if (category == null) {
                throw new BusinessException("分类不存在");
            }
        }
        category.setParent(req.getParent());
        category.setName(req.getName());
        category.setSort(req.getSort() != null ? req.getSort() : 0);
        this.saveOrUpdate(category);
    }

    public void delete(Long id) {
        // 检查是否有子分类
        long childCount = this.count(new LambdaQueryWrapper<Category>()
                .eq(Category::getParent, id));
        if (childCount > 0) {
            throw new BusinessException("该分类下存在子分类，无法删除");
        }
        this.removeById(id);
    }

    private List<Category> buildTree(List<Category> allList) {
        Map<Long, List<Category>> parentMap = allList.stream()
                .collect(Collectors.groupingBy(Category::getParent));

        allList.forEach(c -> c.setChildren(parentMap.getOrDefault(c.getId(), new ArrayList<>())));
        return allList.stream()
                .filter(c -> c.getParent() == 0)
                .collect(Collectors.toList());
    }
}
