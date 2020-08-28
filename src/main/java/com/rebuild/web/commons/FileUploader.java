/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.web.ServletUtils;
import com.rebuild.api.ResultBody;
import com.rebuild.core.helper.QiniuCloud;
import com.rebuild.core.helper.RebuildConfiguration;
import com.rebuild.core.service.files.FilesHelper;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 文件上传
 *
 * @author zhaofang123@gmail.com
 * @since 11/06/2017
 */
@RequestMapping("/filex/")
@Controller
public class FileUploader {

    private static final Log LOG = LogFactory.getLog(FileUploader.class);

    @RequestMapping(value = "upload", method = RequestMethod.POST)
    public void upload(HttpServletRequest request, HttpServletResponse response) {
        String uploadName = null;
        try {
            List<FileItem> fileItems = parseFileItem(request);
            for (FileItem item : fileItems) {
                uploadName = item.getName();
                if (uploadName == null) {
                    continue;
                }

                uploadName = QiniuCloud.formatFileKey(uploadName);
                File file;
                // 上传临时文件
                if (BooleanUtils.toBoolean(request.getParameter("temp"))) {
                    uploadName = uploadName.split("/")[2];
                    file = RebuildConfiguration.getFileOfTemp(uploadName);
                } else {
                    file = RebuildConfiguration.getFileOfData(uploadName);
                    FileUtils.forceMkdir(file.getParentFile());
                }

                item.write(file);
                if (!file.exists()) {
                    ServletUtils.writeJson(response, ResultBody.error("上传失败", 1000).toString());
                    return;
                }

                break;
            }

        } catch (Exception e) {
            LOG.error(null, e);
            uploadName = null;
        }

        if (uploadName != null) {
            ServletUtils.writeJson(response, ResultBody.ok(uploadName).toString());
        } else {
            ServletUtils.writeJson(response, ResultBody.error("上传失败", 1000).toString());
        }
    }

    /**
     * @see FilesHelper#storeFileSize(String, int)
     */
    @RequestMapping("store-filesize")
    public void storeFilesize(HttpServletRequest request) {
        int fileSize = ObjectUtils.toInt(request.getParameter("fs"));
        if (fileSize < 1) {
            return;
        }
        String filePath = request.getParameter("fp");
        if (StringUtils.isNotBlank(filePath)) {
            FilesHelper.storeFileSize(filePath, fileSize);
        }
    }

    // ----

    private static FileItemFactory fileItemFactory;

    static {
        File track = RebuildConfiguration.getFileOfTemp("track");
        try {
            FileUtils.forceMkdir(track);
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
        fileItemFactory = new DiskFileItemFactory(
                DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD * 5 /*50MB*/, track);
    }

    /**
     * 读取上传的文件列表
     *
     * @param request
     * @return
     * @throws Exception
     */
    private static List<FileItem> parseFileItem(HttpServletRequest request) throws Exception {
        if (!ServletFileUpload.isMultipartContent(request)) {
            return Collections.emptyList();
        }

        ServletFileUpload upload = new ServletFileUpload(fileItemFactory);
        List<FileItem> files;
        try {
            files = upload.parseRequest(request);
        } catch (Exception ex) {
            if (ex.getCause() instanceof IOException) {
                LOG.warn("传输意外中断", ex);
                return Collections.emptyList();
            }
            throw ex;
        }

        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        return files;
    }
}
