/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges.bizz;

import cn.devezhao.bizz.security.member.Role;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.metadata.EntityHelper;

import java.security.Principal;

/**
 * TODO 多角色
 *
 * @author zhaofang123@gmail.com
 * @since 09/16/2018
 */
public class RoleGroup extends Role {

    protected RoleGroup() {
        super(ID.newId(EntityHelper.RoleMember), "RoleGroup", false);
    }

    @Override
    public boolean addMember(Principal user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeMember(Principal user) {
        throw new UnsupportedOperationException();
    }

    

}
