/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.signup;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author zhaofang123@gmail.com
 * @since 08/26/2020
 */
@Controller
public class Index {

    @GetMapping("/")
    public String index() {
        return "redirect:/user/login";
    }
}
