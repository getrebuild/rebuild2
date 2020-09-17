package com.rebuild.core;

import com.rebuild.core.support.task.TaskExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

/**
 * 系统停止清理资源
 *
 * @author zhaofang123@gmail.com
 * @since 2020/09/17
 */
public class CloseListener implements ApplicationListener<ContextClosedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(CloseListener.class);

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        TaskExecutors.shutdown();

        LOG.warn("Rebuild Shutting down");
    }
}
