/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.CodecUtils;
import com.rebuild.core.Application;
import com.rebuild.core.helper.ConfigurationItem;
import com.rebuild.core.helper.QiniuCloud;
import com.rebuild.core.helper.RebuildConfiguration;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 文件共享
 *
 * @author ZHAO
 * @since 2019/9/26
 */
@Controller
public class FileShare extends BaseController {

    // URL of public
    @RequestMapping("/filex/make-url")
    public void makeUrl(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String fileUrl = getParameterNotNull(request, "url");
        String publicUrl = genPublicUrl(fileUrl);
        writeSuccess(response, JSONUtils.toJSONObject("publicUrl", publicUrl));
    }

    // URL of share
    @RequestMapping("/filex/make-share")
    public void makeShareUrl(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Assert.isTrue(RebuildConfiguration.getBool(ConfigurationItem.FileSharable), "不允许分享文件");

        String fileUrl = getParameterNotNull(request, "url");
        int minte = getIntParameter(request, "time", 5);

        String shareKey = CodecUtils.randomCode(40);
        Application.getCommonsCache().put(shareKey, fileUrl, minte * 60);

        String shareUrl = RebuildConfiguration.getHomeUrl("s/" + shareKey);
        writeSuccess(response, JSONUtils.toJSONObject("shareUrl", shareUrl));
    }

    @RequestMapping("/s/{shareKey}")
    public ModelAndView makeShareUrl(@PathVariable String shareKey,
                                     HttpServletResponse response) throws IOException {
        String fileUrl;
        if (!RebuildConfiguration.getBool(ConfigurationItem.FileSharable)
                || (fileUrl = Application.getCommonsCache().get(shareKey)) == null) {
            response.sendError(403, "分享的文件已过期");
            return null;
        }

        String publicUrl = genPublicUrl(fileUrl);
        ModelAndView mv = createModelAndView("/commons/shared-file.jsp");
        mv.getModelMap().put("publicUrl", publicUrl);
        return mv;
    }

    /**
     * @param fileUrl
     * @return
     * @see FileDownloader#download(HttpServletRequest, HttpServletResponse)
     */
    private String genPublicUrl(String fileUrl) {
        String publicUrl;
        if (QiniuCloud.instance().available()) {
            publicUrl = QiniuCloud.instance().url(fileUrl, 60);
        } else {
            // @see FileDownloader#download
            String e = CodecUtils.randomCode(40);
            Application.getCommonsCache().put(e, "rb", 60);

            publicUrl = "filex/access/" + fileUrl + "?e=" + e;
            publicUrl = RebuildConfiguration.getHomeUrl(publicUrl);
        }
        return publicUrl;
    }
}
