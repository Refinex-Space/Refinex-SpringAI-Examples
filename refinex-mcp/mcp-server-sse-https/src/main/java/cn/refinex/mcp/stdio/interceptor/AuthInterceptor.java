package cn.refinex.mcp.stdio.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器
 *
 * @author refinex
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /**
     * 暂时固定一个 TOKEN 用于测试，实际生产环境需要从请求头中获取
     */
    private static final String FIXED_TOKEN = "abc123456789";

    /**
     * 认证请求头名称，实际生产环境可以通过 YAML 配置
     */
    private static final String AUTH_HEADER = "Authorization";

    /**
     * 认证请求头的前缀，实际生产环境可以通过 YAML 配置
     */
    private static final String PREFIX = "Bearer ";

    /**
     * 拦截器前置处理
     *
     * @param request  请求
     * @param response 响应
     * @param handler  处理器
     * @return 是否继续处理请求
     * @throws Exception 可能的异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String header = request.getHeader(AUTH_HEADER);

        // 判断用于认证的请求头是否存在，不存在直接拒绝
        if (header == null || !header.startsWith(PREFIX)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing Authorization header");
            return false;
        }

        // 提取 Token, 需要去掉前缀
        String token = header.substring(PREFIX.length());

        // 校验 Token
        if (!FIXED_TOKEN.equals(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid token");
            return false;
        }

        return true;
    }
}
