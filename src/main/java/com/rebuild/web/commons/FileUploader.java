/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.ObjectUtils;
import com.rebuild.core.helper.QiniuCloud;
import com.rebuild.core.helper.RebuildConfiguration;
import com.rebuild.core.service.files.FilesHelper;
import com.rebuild.web.BaseController;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * 文件上传
 *
 * @author zhaofang123@gmail.com
 * @since 11/06/2017
 */
@Controller
@RequestMapping("/filex/")
public class FileUploader extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(FileUploader.class);

    @PostMapping("upload")
    public void upload(HttpServletRequest request, HttpServletResponse response) {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver(request.getServletContext());

        MultipartFile file = null;
        MultipartHttpServletRequest mp = resolver.resolveMultipart(request);
        for (MultipartFile t : mp.getFileMap().values()) {
            file = t;
            break;
        }

        if (file == null || file.isEmpty()) {
            writeFailure(response, "上传失败，请选择文件");
            return;
        }

        String uploadName;
        try {
            uploadName = QiniuCloud.formatFileKey(file.getOriginalFilename());

            File dest;
            // 上传临时文件
            if (BooleanUtils.toBoolean(request.getParameter("temp"))) {
                uploadName = uploadName.split("/")[2];
                dest = RebuildConfiguration.getFileOfTemp(uploadName);
            } else {
                dest = RebuildConfiguration.getFileOfData(uploadName);
                FileUtils.forceMkdir(dest.getParentFile());
            }

            file.transferTo(dest);
            if (!dest.exists()) {
                writeFailure(response, "上传失败，请稍后重试");
                return;
            }

        } catch (Exception ex) {
            LOG.error(null, ex);
            uploadName = null;
        }

        if (uploadName != null) {
            writeSuccess(response, uploadName);
        } else {
            writeFailure(response, "上传失败，请稍后重试");
        }
    }

    /**
     * @see FilesHelper#storeFileSize(String, int)
     */
    @RequestMapping("store-filesize")
    public void storeFilesize(HttpServletRequest request, HttpServletResponse response) {
        int fileSize = ObjectUtils.toInt(request.getParameter("fs"));
        if (fileSize < 1) {
            return;
        }

        String filePath = request.getParameter("fp");
        if (StringUtils.isNotBlank(filePath)) {
            FilesHelper.storeFileSize(filePath, fileSize);
        }
        writeSuccess(response);
    }
}
