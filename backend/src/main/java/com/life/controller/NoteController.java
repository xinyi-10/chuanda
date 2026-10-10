package com.life.controller;

import com.life.common.PageResult;
import com.life.common.Result;
import com.life.common.UserContext;
import com.life.domain.Note;
import com.life.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 笔记接口。
 *
 * <p>userId 一律取自 {@link UserContext}（由登录拦截器写入），
 * 前端无法伪造，从源头保证数据隔离。</p>
 */
@RestController
@RequestMapping("/note")
public class NoteController {

    @Autowired
    private NoteService noteService;

    /** 分页查询：GET /note/page?categoryId=&keyword=&pageNum=1&pageSize=10 */
    @GetMapping("/page")
    public Result<PageResult<Note>> page(@RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(noteService.page(UserContext.getUserId(), categoryId, keyword, pageNum, pageSize));
    }

    /** 详情：GET /note/{id} */
    @GetMapping("/{id}")
    public Result<Note> detail(@PathVariable Long id) {
        return Result.ok(noteService.getById(id, UserContext.getUserId()));
    }

    /** 新建：POST /note */
    @PostMapping
    public Result<Long> create(@RequestBody Note note) {
        return Result.ok("创建成功", noteService.create(UserContext.getUserId(), note));
    }

    /** 编辑：PUT /note/{id} */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Note note) {
        note.setId(id);
        noteService.update(UserContext.getUserId(), note);
        return Result.ok("保存成功", null);
    }

    /** 删除（逻辑删）：DELETE /note/{id} */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noteService.delete(UserContext.getUserId(), id);
        return Result.ok("删除成功", null);
    }
}
