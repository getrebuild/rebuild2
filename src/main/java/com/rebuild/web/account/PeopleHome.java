/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.account;

import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * TODO People's home
 *
 * @author devezhao-mac zhaofang123@gmail.com
 * @since 2019/01/24
 */
@Controller
public class PeopleHome extends BaseController {

    @RequestMapping("/account/{user}/home")
    public ModelAndView peopleView(@PathVariable String user,
                                   HttpServletRequest request, HttpServletResponse response) throws IOException {
        return null;
    }
}
