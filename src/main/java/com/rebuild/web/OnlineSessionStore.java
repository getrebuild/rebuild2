/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.commons.web.WebUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.helper.ConfigurationItem;
import com.rebuild.core.helper.RebuildConfiguration;
import com.rebuild.core.privileges.CurrentCaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NamedThreadLocal;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 在线会话/用户
 *
 * @author devezhao
 * @since 09/27/2018
 */
@Component
public class OnlineSessionStore extends CurrentCaller implements HttpSessionListener {

    private static final Logger LOG = LoggerFactory.getLogger(OnlineSessionStore.class);

    private static final Set<HttpSession> ONLINE_SESSIONS = new CopyOnWriteArraySet<>();

    private static final Map<ID, HttpSession> ONLINE_USERS = new ConcurrentHashMap<>();

    private static final ThreadLocal<String> LOCALE = new NamedThreadLocal<>("Current session locale");

    /**
     * 最近访问 [时间, 路径]
     * @see #storeLastActive(HttpServletRequest)
     */
    public static final String SK_LASTACTIVE = WebUtils.KEY_PREFIX + "Session-LastActive";

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        if (LOG.isDebugEnabled()) {
            LOG.info("Created session - " + event);
        }
        ONLINE_SESSIONS.add(event.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        if (LOG.isDebugEnabled()) {
            LOG.info("Destroyed session - " + event);
        }

        HttpSession s = event.getSession();
        if (ONLINE_SESSIONS.contains(s)) {
            ONLINE_SESSIONS.remove(s);
        } else {
            for (Map.Entry<ID, HttpSession> e : ONLINE_USERS.entrySet()) {
                if (s.equals(e.getValue())) {
                    ONLINE_USERS.remove(e.getKey());
                    break;
                }
            }
        }
    }

    /**
     * 所有会话
     *
     * @return
     */
    public Set<HttpSession> getAllSession() {
        Set<HttpSession> all = new HashSet<>();
        all.addAll(ONLINE_SESSIONS);
        all.addAll(ONLINE_USERS.values());
        return Collections.unmodifiableSet(all);
    }

    /**
     * 用户会话
     *
     * @param user
     * @return
     */
    public HttpSession getSession(ID user) {
        return ONLINE_USERS.get(user);
    }

    /**
     * @param request
     */
    public void storeLastActive(HttpServletRequest request) {
        HttpSession s = request.getSession();
        s.setAttribute(SK_LASTACTIVE, new Object[]{System.currentTimeMillis(), request.getRequestURI()});
    }

    /**
     * @param request
     */
    public void storeLoginSuccessed(HttpServletRequest request) {
        HttpSession s = request.getSession();
        Object loginUser = s.getAttribute(WebUtils.CURRENT_USER);
        Assert.notNull(loginUser, "No login user found in session!");

        if (!RebuildConfiguration.getBool(ConfigurationItem.MultipleSessions)) {
            HttpSession previous = getSession((ID) loginUser);
            if (previous != null) {
                LOG.warn("Kill previous session : " + loginUser + " < " + previous.getId());
                try {
                    previous.invalidate();
                } catch (Exception ignored) {
                }
            }
        }

        ONLINE_SESSIONS.remove(s);
        ONLINE_USERS.put((ID) loginUser, s);
    }

    /**
     * @param locale
     */
    public void setLocale(String locale) {
        LOCALE.set(locale);
    }

    /**
     * @return Returns default if unset
     * @see com.rebuild.core.helper.i18n.Language
     */
    public String getLocale() {
        String locale = LOCALE.get();
        if (locale == null) {
            locale = RebuildConfiguration.get(ConfigurationItem.DefaultLanguage);
        }
        return locale;
    }

    @Override
    public void clean() {
        super.clean();
        LOCALE.remove();
    }
}
