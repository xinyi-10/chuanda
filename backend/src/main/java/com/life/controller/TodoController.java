package com.life.controller;

import com.life.common.Result;
import com.life.common.UserContext;
import com.life.domain.Todo;
import com.life.service.TodoService;
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

import java.util.List;

/**
 * 事务待办接口（带倒计时）。
 *
 * <p>倒计时状态操作全部走 POST，语义清晰且避免被缓存：
 * /todo/{id}/pause 暂停、/resume 继续、/reset 重置、/finish 完成。</p>
 */
@RestController
@RequestMapping("/todo")
public class TodoController {

    @Autowired
    private TodoService todoService;

    /** 列表：GET /todo/list?status= */
    @GetMapping("/list")
    public Result<List<Todo>> list(@RequestParam(required = false) Integer status) {
        return Result.ok(todoService.list(UserContext.getUserId(), status));
    }

    /** 详情：GET /todo/{id} */
    @GetMapping("/{id}")
    public Result<Todo> detail(@PathVariable Long id) {
        return Result.ok(todoService.getById(id, UserContext.getUserId()));
    }

    /** 新建：POST /todo */
    @PostMapping
    public Result<Long> create(@RequestBody Todo todo) {
        return Result.ok("创建成功", todoService.create(UserContext.getUserId(), todo));
    }

    /** 编辑：PUT /todo/{id} */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Todo todo) {
        todo.setId(id);
        todoService.update(UserContext.getUserId(), todo);
        return Result.ok("保存成功", null);
    }

    /** 删除：DELETE /todo/{id} */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        todoService.delete(UserContext.getUserId(), id);
        return Result.ok("删除成功", null);
    }

    /** 暂停倒计时 */
    @PostMapping("/{id}/pause")
    public Result<Todo> pause(@PathVariable Long id) {
        return Result.ok("已暂停", todoService.pause(UserContext.getUserId(), id));
    }

    /** 继续倒计时 */
    @PostMapping("/{id}/resume")
    public Result<Todo> resume(@PathVariable Long id) {
        return Result.ok("已继续", todoService.resume(UserContext.getUserId(), id));
    }

    /** 重置倒计时 */
    @PostMapping("/{id}/reset")
    public Result<Todo> reset(@PathVariable Long id) {
        return Result.ok("已重置", todoService.reset(UserContext.getUserId(), id));
    }

    /** 标记完成 */
    @PostMapping("/{id}/finish")
    public Result<Todo> finish(@PathVariable Long id) {
        return Result.ok("已完成", todoService.finish(UserContext.getUserId(), id));
    }
}
