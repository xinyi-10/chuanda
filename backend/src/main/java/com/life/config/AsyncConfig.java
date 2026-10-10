package com.life.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步与定时任务配置（P0 铁律 H4）。
 *
 * <p>关键操作（AI 穿搭推荐等长耗时任务）走独立线程池，不阻塞 Web 请求线程；
 * 同时启用定时任务，用于<b>异步任务超时补偿</b>——扫描 status=1 且已超过
 * timeout_at 的任务置为「超时」终态，杜绝「永久执行中」。</p>
 *
 * <p>注意：@Async 方法必须由<b>外部调用</b>（同类内部调用不走代理，注解失效）；
 * 且方法内必须 try-catch，否则异常会让任务状态卡死。</p>
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /** 穿搭推荐专用线程池：与主业务线程隔离，避免外部 API 拖垮全站 */
    @Bean("outfitExecutor")
    public ThreadPoolTaskExecutor outfitExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(16);
        ex.setQueueCapacity(100);
        ex.setKeepAliveSeconds(60);
        ex.setThreadNamePrefix("outfit-");
        // 队列满时不丢任务，交由调用线程执行（背压），避免无声吞掉请求
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.initialize();
        return ex;
    }

    /** 定时任务调度器：异步任务超时补偿等 */
    @Bean("taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("life-sched-");
        scheduler.initialize();
        return scheduler;
    }
}
