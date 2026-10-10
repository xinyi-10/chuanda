package com.life.service;

import com.life.dao.TodoDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 待办倒计时「到期补偿」定时任务。
 *
 * <p>每 60 秒把「进行中且已过 deadline」的待办批量置为「已到期」，保证即使前端没打开，
 * 状态也不会永远停留在「进行中」。</p>
 *
 * <p><b>为什么单独成类且不实现接口：</b>
 * Spring 对实现了接口的 Bean 默认使用 JDK 动态代理，代理对象只暴露接口方法。
 * 若把 @Scheduled 方法写在 {@code TodoServiceImpl}（实现了 TodoService 接口）里，
 * 该方法不在接口上，启动时会直接抛：
 * <pre>
 * IllegalStateException: Need to invoke method 'scanExpired' declared on target class
 * 'TodoServiceImpl', but not found in any interface(s) of the exposed proxy type.
 * </pre>
 * 独立为无接口的 @Component 即可彻底规避，同时让「业务」与「调度」职责分离。</p>
 */
@Component
public class TodoScheduler {

    private static final Logger log = LoggerFactory.getLogger(TodoScheduler.class);

    @Autowired
    private TodoDao todoDao;

    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void scanExpired() {
        try {
            int n = todoDao.markExpired();
            if (n > 0) {
                log.info("[todo] 标记 {} 条待办为已到期", n);
            }
        } catch (Exception e) {
            log.error("[todo] 到期扫描失败", e);
        }
    }
}
