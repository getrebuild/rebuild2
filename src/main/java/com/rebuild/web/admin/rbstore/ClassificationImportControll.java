/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.rbstore;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.helper.RebuildConfiguration;
import com.rebuild.core.helper.task.TaskExecutors;
import com.rebuild.core.rbstore.ClassificationFileImporter;
import com.rebuild.core.rbstore.ClassificationImporter;
import com.rebuild.web.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * 分类数据导入
 *
 * @author devezhao zhaofang123@gmail.com
 * @see ClassificationImporter
 * @see ClassificationFileImporter
 * @since 2019/04/08
 */
@Controller
public class ClassificationImportControll extends BaseController {

    @RequestMapping("/admin/entityhub/classification/imports/start")
    public void starts(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        ID dest = getIdParameterNotNull(request, "dest");
        String fileUrl = getParameterNotNull(request, "file");

        ClassificationImporter importer = new ClassificationImporter(dest, fileUrl);
        String taskid = TaskExecutors.submit(importer, user);
        writeSuccess(response, taskid);
    }

    @RequestMapping("/admin/entityhub/classification/imports/file")
    public void startsFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        ID dest = getIdParameterNotNull(request, "dest");
        String filePath = getParameterNotNull(request, "file");

        File file = RebuildConfiguration.getFileOfTemp(filePath);
        ClassificationFileImporter importer = new ClassificationFileImporter(dest, file);
        String taskid = TaskExecutors.submit(importer, user);
        writeSuccess(response, taskid);
    }
}
