/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.trigger;

import com.rebuild.core.helper.SMSender;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/25
 */
@Controller
@RequestMapping("/admin/robot/trigger/")
public class SendNotificationControll extends BaseController {

    @RequestMapping("sendnotification-atypes")
    public void availableTypes(HttpServletResponse response) throws IOException {
        Map<String, Boolean> ta = new HashMap<>();
        ta.put("serviceMail", SMSender.availableMail());
        ta.put("serviceSms", SMSender.availableSMS());
        writeSuccess(response, ta);
    }
}
