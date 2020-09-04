/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.member.User;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.support.BlackList;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.SMSender;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.BaseServiceImpl;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.notification.Message;
import com.rebuild.core.service.notification.MessageBuilder;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import org.springframework.stereotype.Service;

/**
 * for User
 *
 * @author zhaofang123@gmail.com
 * @since 07/25/2018
 */
@Service
public class UserService extends BaseServiceImpl {

    // 系统用户
    public static final ID SYSTEM_USER = ID.valueOf("001-0000000000000000");
    // 管理员
    public static final ID ADMIN_USER = ID.valueOf("001-0000000000000001");

//    // 暂未用：全部用户（注意这是一个虚拟用户 ID，并不真实存在）
//    public static final ID _ALL_USER = ID.valueOf("001-9999999999999999");

    protected UserService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.User;
    }

    @Override
    public Record create(Record record) {
        return create(record, true);
    }

    /**
     * @param record
     * @param notifyUser
     * @return
     */
    private Record create(Record record, boolean notifyUser) {
        checkAdminGuard(BizzPermission.CREATE, null);

        final String passwd = record.getString("password");
        saveBefore(record);
        record = super.create(record);
        Application.getUserStore().refreshUser(record.getPrimary());

        if (notifyUser) {
            notifyNewUser(record, passwd);
        }
        return record;
    }

    @Override
    public Record update(Record record) {
        checkAdminGuard(BizzPermission.UPDATE, record.getPrimary());

        saveBefore(record);
        Record r = super.update(record);
        Application.getUserStore().refreshUser(record.getPrimary());
        return r;
    }

    @Override
    public int delete(ID record) {
        checkAdminGuard(BizzPermission.DELETE, null);

        super.delete(record);
        Application.getUserStore().removeUser(record);
        return 1;
    }

    /**
     * @param record
     */
    protected void saveBefore(Record record) {
        if (record.hasValue("loginName")) {
            checkLoginName(record.getString("loginName"));
        }

        if (record.hasValue("password")) {
            String password = record.getString("password");
            checkPassword(password);
            record.setString("password", EncryptUtils.toSHA256Hex(password));
        }

        if (record.hasValue("email") && Application.getUserStore().existsUser(record.getString("email"))) {
            throw new DataSpecificationException(Language.getLang("SomeDuplicate", "Email"));
        }

        if (record.getPrimary() == null && !record.hasValue("fullName")) {
            record.setString("fullName", record.getString("loginName").toUpperCase());
        }

        if (record.hasValue("fullName")) {
            try {
                UserHelper.generateAvatar(record.getString("fullName"), true);
            } catch (Exception ex) {
                LOG.error(null, ex);
            }
        }
    }

    /**
     * @param loginName
     * @throws DataSpecificationException
     */
    private void checkLoginName(String loginName) throws DataSpecificationException {
        if (Application.getUserStore().existsUser(loginName)) {
            throw new DataSpecificationException(Language.getLang("SomeDuplicate", "Username"));
        }

        if (!CommonsUtils.isPlainText(loginName) || BlackList.isBlacked(loginName)) {
            throw new DataSpecificationException(Language.getLang("SomeInvalid", "Username"));
        }
    }

    /**
     * @param action
     * @param user
     * @see AdminGuard
     */
    private void checkAdminGuard(Permission action, ID user) {
        ID currentUser = Application.getCurrentUser();
        if (UserHelper.isAdmin(currentUser)) return;

        if (action == BizzPermission.CREATE || action == BizzPermission.DELETE) {
            throw new PrivilegesException(Language.getLang("NoPrivileges"));
        }

        // 用户可自己改自己
        if (action == BizzPermission.UPDATE && currentUser.equals(user)) {
            return;
        }
        throw new PrivilegesException(Language.getLang("NoPrivileges"));
    }

    /**
     * 检查密码是否符合安全策略
     *
     * @param password
     * @throws DataSpecificationException
     */
    protected void checkPassword(String password) throws DataSpecificationException {
        if (password.length() < 6) {
            throw new DataSpecificationException(Language.getLang("PasswordLevel1"));
        }

        int policy = RebuildConfiguration.getInt(ConfigurationItem.PasswordPolicy);
        if (policy <= 1) {
            return;
        }

        int countUpper = 0;
        int countLower = 0;
        int countDigit = 0;
        int countSpecial = 0;
        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                countUpper++;
            } else if (Character.isLowerCase(ch)) {
                countLower++;
            } else if (Character.isDigit(ch)) {
                countDigit++;
            } else if (CommonsUtils.isSpecialChar(ch)) {
                countSpecial++;
            }
        }

        if (countUpper == 0 || countLower == 0 || countDigit == 0) {
            throw new DataSpecificationException(Language.getLang("PasswordLevel2"));
        }
        if (policy >= 3 && (countSpecial == 0 || password.length() < 8)) {
            throw new DataSpecificationException(Language.getLang("PasswordLevel3"));
        }
    }

    /**
     * @param newUser
     * @param passwd
     * @return
     */
    private boolean notifyNewUser(Record newUser, String passwd) {
        if (RebuildConfiguration.getMailAccount() == null || !newUser.hasValue("email")) {
            return false;
        }

        String appName = RebuildConfiguration.get(ConfigurationItem.AppName);
        String homeUrl = RebuildConfiguration.getHomeUrl();

        String subject = Language.getLang("YourAccountReady");
        String content = String.format(
                Language.getLang("NewUserAddedNotify"),
                appName, newUser.getString("loginName"), passwd, homeUrl, homeUrl);

        SMSender.sendMail(newUser.getString("email"), subject, content);
        return true;
    }

    /**
     * xxxNew 值为 null 表示不做修改
     *
     * @param user
     * @param deptNew
     * @param roleNew
     * @param enableNew
     */
    public void updateEnableUser(ID user, ID deptNew, ID roleNew, Boolean enableNew) {
        User u = Application.getUserStore().getUser(user);
        ID deptOld = null;
        // 检查是否需要更新部门
        if (deptNew != null) {
            deptOld = u.getOwningBizUnit() == null ? null : (ID) u.getOwningBizUnit().getIdentity();
            if (deptNew.equals(deptOld)) {
                deptNew = null;
                deptOld = null;
            }
        }

        // 检查是否需要更新角色
        if (u.getOwningRole() != null && u.getOwningRole().getIdentity().equals(roleNew)) {
            roleNew = null;
        }

        Record record = EntityHelper.forUpdate(user, Application.getCurrentUser());
        if (deptNew != null) {
            record.setID("deptId", deptNew);
        }
        if (roleNew != null) {
            record.setID("roleId", roleNew);
        }
        if (enableNew != null) {
            record.setBoolean("isDisabled", !enableNew);
        }
        super.update(record);
        Application.getUserStore().refreshUser(user);

        // 改变记录的所属部门
        if (deptOld != null) {
            TaskExecutors.submit(new ChangeOwningDeptTask(user, deptNew), Application.getCurrentUser());
        }
    }

    /**
     * 新用户注册
     *
     * @param record
     * @return
     * @throws DataSpecificationException
     */
    public ID txSignUp(Record record) throws DataSpecificationException {
        record = this.create(record, false);

        ID newUserId = record.getPrimary();
        String viewUrl = AppUtils.getContextPath() + "/app/list-and-view?id=" + newUserId;
        String content = String.format(Language.getLang("NewUserSignupNotify"), newUserId, viewUrl);

        Message message = MessageBuilder.createMessage(ADMIN_USER, content, newUserId);
        Application.getNotifications().send(message);
        return newUserId;
    }
}
