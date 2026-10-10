package com.life.dao;

import com.life.domain.Todo;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 事务待办数据访问接口（对应 mapper/TodoMapper.xml） */
public interface TodoDao {

    /** 本人待办列表（status 为 null 时查全部） */
    List<Todo> listByUser(@Param("userId") Long userId, @Param("status") Integer status);

    Todo selectById(@Param("id") Long id, @Param("userId") Long userId);

    int insert(Todo todo);

    /** 编辑（标题/内容/优先级/时间） */
    int update(Todo todo);

    /** 逻辑删除 */
    int logicDelete(@Param("id") Long id, @Param("userId") Long userId);

    /** 暂停：写入剩余秒数快照，状态置 1 */
    int pause(@Param("id") Long id, @Param("userId") Long userId,
              @Param("remainSec") Integer remainSec);

    /** 恢复：根据剩余秒数重算 deadline，状态置 0 */
    int resume(@Param("id") Long id, @Param("userId") Long userId,
               @Param("deadline") LocalDateTime deadline);

    /** 重置：deadline = now + duration，状态置 0，清空暂停快照 */
    int reset(@Param("id") Long id, @Param("userId") Long userId,
              @Param("deadline") LocalDateTime deadline);

    /** 标记完成：状态置 2 */
    int finish(@Param("id") Long id, @Param("userId") Long userId);

    /** 定时任务：把所有「进行中且已过 deadline」的待办置为已到期（3）。无用户上下文，全局扫描。 */
    int markExpired();
}
