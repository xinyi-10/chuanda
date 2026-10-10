package com.life.config;

import com.life.common.BizException;
import com.life.common.UserContext;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * 数据隔离拦截器（P0 铁律 H2）。
 *
 * <p>越权（漏写 user_id）是隐私类应用最高风险。本插件在所有 SQL 执行前检查：
 * 凡涉及「带 user_id 的业务表」，SQL 中必须出现 user_id 条件，否则直接拦截并抛异常，
 * 把「口头约定」变成「机制强制」。</p>
 *
 * <p>当前为<b>检查式</b>防护（漏写即拦截，零误放行）；
 * 后续可升级为<b>注入式</b>（自动往 WHERE 里补 user_id），届时需引入 SQL 解析器。</p>
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                        CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class UserIsolationInterceptor implements Interceptor {

    private static final long serialVersionUID = 1L;

    /** 需要按 user_id 隔离的业务表 */
    private static final Set<String> USER_TABLES = new HashSet<>(Arrays.asList(
            "note", "note_category",
            "ledger_record", "ledger_category",
            "clothing_item", "outfit_record",
            "todo", "async_task"
    ));

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Long userId = UserContext.getUserId();
        // 未登录场景（如登录接口）不做隔离校验
        if (userId == null) {
            return invocation.proceed();
        }

        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        BoundSql boundSql = ms.getBoundSql(args[1]);
        String sql = boundSql.getSql();

        if (sql != null) {
            String lower = sql.toLowerCase();
            if (touchesUserTable(lower) && !lower.contains("user_id")) {
                throw new BizException("数据隔离校验失败：涉及用户数据的 SQL 必须包含 user_id 条件");
            }
        }
        return invocation.proceed();
    }

    /** 判断 SQL 是否触及需要隔离的表（用词边界避免 note 命中 note_category 这类子串） */
    private boolean touchesUserTable(String lowerSql) {
        for (String t : USER_TABLES) {
            if (lowerSql.contains("`" + t + "`")) {
                return true;
            }
            if (lowerSql.matches("(?s).*(^|[^a-z0-9_])" + t + "([^a-z0-9_]|$).*")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 无需外部属性
    }
}
