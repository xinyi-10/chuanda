package com.life.controller;

import com.life.common.Result;
import com.life.common.UserContext;
import com.life.domain.NoteCategory;
import com.life.service.NoteCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 笔记分类接口 */
@RestController
@RequestMapping("/note/category")
public class NoteCategoryController {

    @Autowired
    private NoteCategoryService noteCategoryService;

    /** 分类列表（含各分类笔记数）：GET /note/category/list */
    @GetMapping("/list")
    public Result<List<NoteCategory>> list() {
        return Result.ok(noteCategoryService.list(UserContext.getUserId()));
    }

    /** 新建分类：POST /note/category */
    @PostMapping
    public Result<Long> create(@RequestBody NoteCategory category) {
        return Result.ok("创建成功",
                noteCategoryService.create(UserContext.getUserId(), category.getName(), category.getSort()));
    }

    /** 重命名 / 改排序：PUT /note/category/{id} */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody NoteCategory category) {
        noteCategoryService.update(UserContext.getUserId(), id, category.getName(), category.getSort());
        return Result.ok("保存成功", null);
    }

    /** 删除（有笔记时拒绝）：DELETE /note/category/{id} */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noteCategoryService.delete(UserContext.getUserId(), id);
        return Result.ok("删除成功", null);
    }
}
