package com.example.mall.cart.interceptor;

import com.example.common.vo.MemberResponseVo;
import com.example.mall.cart.to.UserInfoTo;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

import static com.example.common.constant.AuthServerConstant.LOGIN_USER;
import static com.example.common.constant.CartConstant.TEMP_USER_COOKIE_NAME;
import static com.example.common.constant.CartConstant.TEMP_USER_COOKIE_TIMEOUT;

public class CartInterceptor implements HandlerInterceptor {

    public static ThreadLocal<UserInfoTo> toThreadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserInfoTo userInfoTo = new UserInfoTo();
        HttpSession session = request.getSession();
        // 获得当前登录用户的信息
        MemberResponseVo memberResponseVo = (MemberResponseVo) session.getAttribute(LOGIN_USER);
        if (memberResponseVo != null) {
            userInfoTo.setUserId(memberResponseVo.getId());
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                String name = cookie.getName();
                if (name.equals(TEMP_USER_COOKIE_NAME)) {
                    userInfoTo.setUserKey(cookie.getValue());
                    // 标记为已是临时用户
                    userInfoTo.setTempUser(true);
                }
            }
        }
        // 如果没有临时用户一定分配一个临时用户
        if (StringUtils.isEmpty(userInfoTo.getUserKey())) {
            String uuid = UUID.randomUUID().toString();
            userInfoTo.setUserKey(uuid);
        }
        // 目标方法执行之前
        toThreadLocal.set(userInfoTo);
        return true;
    }

    /**
     * 业务执行之后，分配临时用户给浏览器保存
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // 获取当前用户的值
        UserInfoTo userInfoTo = toThreadLocal.get();
        // 如果没有临时用户一定保存一个临时用户
        if (!userInfoTo.getTempUser()) {
            // 创建一个cookie
            Cookie cookie = new Cookie(TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            // 扩大作用域
            cookie.setDomain("mall.com");
            // 设置过期时间
            cookie.setMaxAge(TEMP_USER_COOKIE_TIMEOUT);
            response.addCookie(cookie);
        }
    }

}
