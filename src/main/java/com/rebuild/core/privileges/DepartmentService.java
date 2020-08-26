/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.service.BaseServiceImpl;
import com.rebuild.core.service.DataSpecificationException;
import org.springframework.stereotype.Service;

/**
 * for Department
 *
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
@Service
public class DepartmentService extends BaseServiceImpl {

    /**
     * 根级部门
     */
    public static final ID ROOT_DEPT = ID.valueOf("002-0000000000000001");

    protected DepartmentService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.Department;
    }

    @Override
    public Record create(Record record) {
        checkAdminGuard(BizzPermission.CREATE, null);

        record = super.create(record);
        RebuildApplication.getUserStore().refreshDepartment(record.getPrimary());
        return record;
    }

    @Override
    public Record update(Record record) {
        checkAdminGuard(BizzPermission.UPDATE, record.getPrimary());

        // 检查父子循环依赖
        if (record.hasValue("parentDept", false)) {
            ID parentDept = record.getID("parentDept");
            if (parentDept.equals(record.getPrimary())) {
                throw new DataSpecificationException("父级部门不能选择自己");
            }

            Department parent = RebuildApplication.getUserStore().getDepartment(parentDept);
            Department that = RebuildApplication.getUserStore().getDepartment(record.getPrimary());

            if (that.isChildren(parent, true)) {
                throw new DataSpecificationException("子级部门不能同时作为父级部门");
            }
        }

        record = super.update(record);
        RebuildApplication.getUserStore().refreshDepartment(record.getPrimary());
        return record;
    }

    @Override
    public int delete(ID recordId) {
        deleteAndTransfer(recordId, null);
        return 1;
    }

    /**
     * TODO 删除后转移成员到其他部门
     *
     * @param deptId
     * @param transferTo
     */
    public void deleteAndTransfer(ID deptId, ID transferTo) {
        checkAdminGuard(BizzPermission.DELETE, null);

        Department dept = RebuildApplication.getUserStore().getDepartment(deptId);
        if (!dept.getChildren().isEmpty()) {
            throw new DataSpecificationException("Has child department");
        }

        super.delete(deptId);
        RebuildApplication.getUserStore().removeDepartment(deptId, transferTo);
    }

    /**
     * @param action
     * @param dept
     * @see AdminGuard
     */
    private void checkAdminGuard(Permission action, ID dept) {
        ID currentUser = RebuildApplication.getCurrentUser();
        if (UserHelper.isAdmin(currentUser)) return;

        if (action == BizzPermission.CREATE || action == BizzPermission.DELETE) {
            throw new PrivilegesException("无操作权限 (E2)");
        }

        // 用户可自己改自己的部门
        ID currentDeptOfUser = (ID) RebuildApplication.getUserStore().getUser(currentUser).getOwningDept().getIdentity();
        if (action == BizzPermission.UPDATE && dept.equals(currentDeptOfUser)) {
            return;
        }
        throw new PrivilegesException("无操作权限");
    }
}
