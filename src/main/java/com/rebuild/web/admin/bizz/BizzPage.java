/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.bizz;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.web.EntityController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * URL-Rewrite
 *
 * @author devezhao
 * @since 11/01/2018
 */
@Controller
@RequestMapping("/app/")
public class BizzPage extends EntityController {

    @RequestMapping("User/view/{id}")
    public ModelAndView userView(@PathVariable String id, HttpServletRequest request) throws IOException {
        ID record = ID.valueOf(id);
        ModelAndView mv = createModelAndView("/admin/bizuser/user-view.jsp", "User", getRequestUser(request));
        mv.getModel().put("id", record);
        return mv;
    }

    @RequestMapping("Department/view/{id}")
    public ModelAndView deptView(@PathVariable String id, HttpServletRequest request) throws IOException {
        ID record = ID.valueOf(id);
        ModelAndView mv = createModelAndView("/admin/bizuser/dept-view.jsp", "Department", getRequestUser(request));
        mv.getModel().put("id", record);
        return mv;
    }

    @RequestMapping("Role/view/{id}")
    public ModelAndView roleView(@PathVariable String id, HttpServletRequest request) throws IOException {
        ID record = ID.valueOf(id);
        ModelAndView mv = createModelAndView("/admin/bizuser/role-view.jsp", "Role", getRequestUser(request));
        mv.getModel().put("id", record);
        return mv;
    }

    @RequestMapping("Team/view/{id}")
    public ModelAndView teamView(@PathVariable String id, HttpServletRequest request) throws IOException {
        ID record = ID.valueOf(id);
        ModelAndView mv = createModelAndView("/admin/bizuser/team-view.jsp", "Team", getRequestUser(request));
        mv.getModel().put("id", record);
        return mv;
    }
}
