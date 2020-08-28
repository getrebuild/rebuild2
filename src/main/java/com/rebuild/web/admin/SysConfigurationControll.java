/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.RegexUtils;
import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.JSONObject;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.util.Auth;
import com.rebuild.core.Application;
import com.rebuild.core.helper.*;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;

/**
 * 系统配置
 *
 * @author zhaofang123@gmail.com
 * @see RebuildConfiguration
 * @see ConfigurationItem
 * @since 09/20/2018
 */
@Controller
@RequestMapping("/admin/")
public class SysConfigurationControll extends BaseController {

    @RequestMapping("systems")
    public ModelAndView pageSystems() {
        ModelAndView mv = createModelAndView("/admin/system-general.jsp");
        for (ConfigurationItem item : ConfigurationItem.values()) {
            mv.getModel().put(item.name(), RebuildConfiguration.get(item));
        }

        JSONObject authority = License.queryAuthority();
        mv.getModel().put("LicenseType",
                authority.getString("authType") + " (" + authority.getString("authObject") + ")");

        return mv;
    }

    @RequestMapping(value = "systems", method = RequestMethod.POST)
    public void postSystems(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);

        String dHomeURL = defaultIfBlank(data, ConfigurationItem.HomeURL);
        if (!RegexUtils.isUrl(dHomeURL)) {
            writeFailure(response, "无效主页地址/域名");
            return;
        }

        // 验证数字参数
        ConfigurationItem[] validNumbers = new ConfigurationItem[]{
                ConfigurationItem.RecycleBinKeepingDays,
                ConfigurationItem.RevisionHistoryKeepingDays,
                ConfigurationItem.DBBackupsKeepingDays
        };
        for (ConfigurationItem item : validNumbers) {
            String number = defaultIfBlank(data, item);
            if (!NumberUtils.isNumber(number)) {
                data.put(item.name(), item.getDefaultValue());
            }
        }

        setValues(data);

        writeSuccess(response);
    }

    @RequestMapping("integration/storage")
    public ModelAndView pageIntegrationStorage() {
        ModelAndView mv = createModelAndView("/admin/integration/storage-qiniu.jsp");
        mv.getModel().put("storageAccount",
                starsAccount(RebuildConfiguration.getStorageAccount(), 0, 1));
        mv.getModel().put("storageStatus", QiniuCloud.instance().available());

        // 存储大小
        long size = QiniuCloud.storageSize();
        mv.getModel().put("_StorageSize", FileUtils.byteCountToDisplaySize(size));

        return mv;
    }

    @RequestMapping(value = "integration/storage", method = RequestMethod.POST)
    public void postIntegrationStorage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);

        String dStorageURL = defaultIfBlank(data, ConfigurationItem.StorageURL);
        String dStorageBucket = defaultIfBlank(data, ConfigurationItem.StorageBucket);
        String dStorageApiKey = defaultIfBlank(data, ConfigurationItem.StorageApiKey);
        String dStorageApiSecret = defaultIfBlank(data, ConfigurationItem.StorageApiSecret);

        if (dStorageURL.startsWith("//")) {
            dStorageURL = "https:" + dStorageURL;
        }
        if (!RegexUtils.isUrl(dStorageURL)) {
            writeFailure(response, "无效访问域名");
            return;
        }

        try {
            // Test
            Auth auth = Auth.create(dStorageApiKey, dStorageApiSecret);
            BucketManager bucketManager = new BucketManager(auth, QiniuCloud.CONFIGURATION);
            bucketManager.getBucketInfo(dStorageBucket);

            setValues(data);
            writeSuccess(response);

        } catch (QiniuException ex) {
            writeFailure(response, "无效配置参数 : " + ex.response.error);
        } catch (Exception ex) {
            writeFailure(response, ThrowableUtils.getRootCause(ex).getLocalizedMessage());
        }
    }

    @RequestMapping("integration/submail")
    public ModelAndView pageIntegrationSubmail() {
        ModelAndView mv = createModelAndView("/admin/integration/submail.jsp");
        mv.getModel().put("smsAccount",
                starsAccount(RebuildConfiguration.getSmsAccount(), 1));
        mv.getModel().put("mailAccount",
                starsAccount(RebuildConfiguration.getMailAccount(), 1));
        return mv;
    }

    @RequestMapping(value = "integration/submail", method = RequestMethod.POST)
    public void postIntegrationSubmail(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);

        String dMailAddr = defaultIfBlank(data, ConfigurationItem.MailAddr);
        if (!RegexUtils.isEMail(dMailAddr)) {
            writeFailure(response, "无效发件人地址");
            return;
        }

        setValues(data);
        writeSuccess(response);
    }

    @RequestMapping(value = "integration/submail/test", method = RequestMethod.POST)
    public void testSubmail(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject data = (JSONObject) ServletUtils.getRequestJson(request);
        String type = getParameterNotNull(request, "type");
        String receiver = getParameterNotNull(request, "receiver");

        String sent = null;
        if ("SMS".equalsIgnoreCase(type)) {
            if (!RegexUtils.isCNMobile(receiver)) {
                writeFailure(response, "无效接收手机");
                return;
            }

            String[] specAccount = new String[]{
                    data.getString("SmsUser"), data.getString("SmsPassword"),
                    data.getString("SmsSign")
            };
            if (specAccount[1].contains("**********")) {
                specAccount[1] = RebuildConfiguration.get(ConfigurationItem.SmsPassword);
            }

            sent = SMSender.sendSMS(receiver, "收到此消息说明你的短信服务配置正确", specAccount);
        } else if ("EMAIL".equalsIgnoreCase(type)) {
            if (!RegexUtils.isEMail(receiver)) {
                writeFailure(response, "无效接收邮箱");
                return;
            }

            String[] specAccount = new String[]{
                    data.getString("MailUser"), data.getString("MailPassword"),
                    data.getString("MailAddr"), data.getString("MailName")
            };
            if (specAccount[1].contains("**********")) {
                specAccount[1] = RebuildConfiguration.get(ConfigurationItem.MailPassword);
            }

            sent = SMSender.sendMail(receiver, "测试邮件", "收到此消息说明你的邮件服务配置正确", true, specAccount);
        }

        if (sent != null) {
            writeSuccess(response, sent);
        } else {
            writeFailure(response, "测试发送失败，请检查你的配置");
        }
    }

    @RequestMapping(value = "integration/submail/stats")
    public void statsSubmail(HttpServletResponse response) throws IOException {
        final Date xday = CalendarUtils.clearTime(CalendarUtils.addDay(-90));
        final String sql = "select date_format(sendTime,'%Y-%m-%d'),count(sendId) from SmsendLog" +
                " where type = ? and sendTime > ? group by date_format(sendTime,'%Y-%m-%d')";

        Object[][] sms = Application.createQueryNoFilter(sql)
                .setParameter(1, 1)
                .setParameter(2, xday)
                .array();
        Arrays.sort(sms, Comparator.comparing(o -> o[0].toString()));

        Object[] smsCount = Application.createQueryNoFilter(
                "select count(sendId) from SmsendLog where type = ?")
                .setParameter(1, 1)
                .unique();


        Object[][] email = Application.createQueryNoFilter(sql)
                .setParameter(1, 2)
                .setParameter(2, xday)
                .array();
        Arrays.sort(email, Comparator.comparing(o -> o[0].toString()));

        Object[] emailCount = Application.createQueryNoFilter(
                "select count(sendId) from SmsendLog where type = ?")
                .setParameter(1, 2)
                .unique();

        JSONObject data = JSONUtils.toJSONObject(
                new String[]{"sms", "email", "smsCount", "emailCount"},
                new Object[]{sms, email, smsCount, emailCount});
        writeSuccess(response, data);
    }

    private String[] starsAccount(String[] account, int... index) {
        if (account == null) {
            return null;
        }
        for (int i : index) {
            account[i] = CommonsUtils.stars(account[i]);
        }
        return account;
    }

    private String defaultIfBlank(JSONObject data, ConfigurationItem item) {
        return StringUtils.defaultIfBlank(data.getString(item.name()), RebuildConfiguration.get(item));
    }

    private void setValues(JSONObject data) {
        for (Map.Entry<String, Object> e : data.entrySet()) {
            try {
                ConfigurationItem item = ConfigurationItem.valueOf(e.getKey());
                RebuildConfiguration.set(item, e.getValue());
            } catch (Exception ex) {
                LOG.error("Invalid item : " + e.getKey() + " = " + e.getValue());
            }
        }
    }
}
