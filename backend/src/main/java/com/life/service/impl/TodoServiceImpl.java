package com.life.service.impl;

import com.life.common.BizException;
import com.life.common.ResultCode;
import com.life.dao.TodoDao;
import com.life.domain.Todo;
import com.life.service.TodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** 事务待办服务实现 */
@Service
public class TodoServiceImpl implements TodoService {

    /** 重置时若无初始时长，默认 60 秒 */
    private static final int DEFAULT_DURATION_SEC = 60;

    @Autowired
    private TodoDao todoDao;

    @Override
    public List<Todo> list(Long userId, Integer status) {
        List<Todo> list = todoDao.listByUser(userId, status);
        LocalDateTime now = LocalDateTime.now();
        for (Todo t : list) {
            t.setRemainSeconds(calcRemain(t, now));
        }
        return list;
    }

    @Override
    public Todo getById(Long id, Long userId) {
        Todo todo = todoDao.selectById(id, userId);
        if (todo == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        todo.setRemainSeconds(calcRemain(todo, LocalDateTime.now()));
        return todo;
    }

    @Override
    public Long create(Long userId, Todo todo) {
        if (!StringUtils.hasText(todo.getTitle())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "待办标题不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        applyCountdown(todo, now, null);

        todo.setUserId(userId);
        todo.setStatus(0);
        if (todo.getPriority() == null) {
            todo.setPriority(0);
        }
        todoDao.insert(todo);
        return todo.getId();
    }

    @Override
    public void update(Long userId, Todo todo) {
        if (todo.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "缺少待办 ID");
        }
        Todo exist = todoDao.selectById(todo.getId(), userId);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (!StringUtils.hasText(todo.getTitle())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "待办标题不能为空");
        }
        applyCountdown(todo, LocalDateTime.now(), exist);
        todo.setUserId(userId);
        if (todo.getPriority() == null) {
            todo.setPriority(exist.getPriority());
        }
        // 编辑后回到「进行中」重新计时，并清空暂停快照（见 Mapper update）
        todoDao.update(todo);
    }

    @Override
    public void delete(Long userId, Long id) {
        todoDao.logicDelete(id, userId);
    }

    @Override
    public Todo pause(Long userId, Long id) {
        Todo t = mustGet(id, userId);
        if (t.getStatus() == null || t.getStatus() != 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "仅「进行中」的待办可以暂停");
        }
        long remain = t.getDeadline() == null ? 0
                : Math.max(Duration.between(LocalDateTime.now(), t.getDeadline()).getSeconds(), 0);
        todoDao.pause(id, userId, (int) remain);
        return getById(id, userId);
    }

    @Override
    public Todo resume(Long userId, Long id) {
        Todo t = mustGet(id, userId);
        if (t.getStatus() == null || t.getStatus() != 1) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "仅「已暂停」的待办可以继续");
        }
        int remain = t.getRemainSec() == null ? 0 : t.getRemainSec();
        todoDao.resume(id, userId, LocalDateTime.now().plusSeconds(remain));
        return getById(id, userId);
    }

    @Override
    public Todo reset(Long userId, Long id) {
        Todo t = mustGet(id, userId);
        int duration = (t.getDuration() == null || t.getDuration() <= 0)
                ? DEFAULT_DURATION_SEC : t.getDuration();
        todoDao.reset(id, userId, LocalDateTime.now().plusSeconds(duration));
        return getById(id, userId);
    }

    @Override
    public Todo finish(Long userId, Long id) {
        mustGet(id, userId);
        todoDao.finish(id, userId);
        return getById(id, userId);
    }

    /* ================= 私有方法 ================= */

    private Todo mustGet(Long id, Long userId) {
        Todo t = todoDao.selectById(id, userId);
        if (t == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return t;
    }

    /**
     * 统一换算倒计时：
     *  - 传了 deadline（截止时间模式）→ 校验晚于当前，duration = 剩余秒数
     *  - 传了 duration（自定义时长模式）→ deadline = now + duration
     *  - 都没传 → 沿用 exist 的（编辑场景），否则报错
     */
    private void applyCountdown(Todo todo, LocalDateTime now, Todo exist) {
        if (todo.getDeadline() != null) {
            long sec = Duration.between(now, todo.getDeadline()).getSeconds();
            if (sec <= 0) {
                throw new BizException(ResultCode.PARAM_ERROR.getCode(), "截止时间必须晚于当前时间");
            }
            todo.setDuration((int) sec);
        } else if (todo.getDuration() != null && todo.getDuration() > 0) {
            todo.setDeadline(now.plusSeconds(todo.getDuration()));
        } else if (exist != null) {
            todo.setDeadline(exist.getDeadline());
            todo.setDuration(exist.getDuration());
        } else {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "请设置截止时间或倒计时时长");
        }
    }

    /** 计算剩余秒数（供列表/详情回传） */
    private long calcRemain(Todo t, LocalDateTime now) {
        if (t.getStatus() == null) {
            return 0L;
        }
        if (t.getStatus() == 1) {
            return t.getRemainSec() == null ? 0L : t.getRemainSec();
        }
        if (t.getStatus() == 0 && t.getDeadline() != null) {
            return Math.max(Duration.between(now, t.getDeadline()).getSeconds(), 0);
        }
        return 0L;
    }
}
