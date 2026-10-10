package com.life.service;

import com.life.domain.Todo;

import java.util.List;

/** 事务待办服务（含倒计时状态机） */
public interface TodoService {

    /** 列表（status 为 null 查全部），返回带 remainSeconds */
    List<Todo> list(Long userId, Integer status);

    Todo getById(Long id, Long userId);

    /** 新建；支持「截止时间」或「自定义时长」两种模式，统一换算为 deadline + duration */
    Long create(Long userId, Todo todo);

    /** 编辑 */
    void update(Long userId, Todo todo);

    /** 逻辑删除 */
    void delete(Long userId, Long id);

    /** 暂停：快照剩余秒数 */
    Todo pause(Long userId, Long id);

    /** 继续：按剩余秒数重算 deadline */
    Todo resume(Long userId, Long id);

    /** 重置：回到初始时长重新计时 */
    Todo reset(Long userId, Long id);

    /** 标记完成 */
    Todo finish(Long userId, Long id);
}
