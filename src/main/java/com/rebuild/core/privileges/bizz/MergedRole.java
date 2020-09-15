/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges.bizz;

import cn.devezhao.bizz.privileges.Privileges;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.EntityPrivileges;
import cn.devezhao.bizz.security.member.Role;
import cn.devezhao.persist4j.engine.ID;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.security.Principal;
import java.util.*;

/**
 * 角色（权限合并）
 *
 * @author zhaofang123@gmail.com
 * @since 09/16/2018
 */
public class MergedRole extends Role {

    /**
     * 无角色
     */
    public static final MergedRole NULL = new MergedRole();

    private MergedRole() {
        super(null, "MERGEDROLE#NULL", true);
    }

    /**
     * 权限掩码
     */
    private static final Integer[] PERMISSION_MASKS = new Integer[]{
            BizzPermission.CREATE.getMask(),
            BizzPermission.DELETE.getMask(),
            BizzPermission.UPDATE.getMask(),
            BizzPermission.READ.getMask(),
            BizzPermission.ASSIGN.getMask(),
            BizzPermission.SHARE.getMask()
    };

    private Role main;
    private Set<Role> roleAppends = Collections.emptySet();

    /**
     * @param user
     * @param appends
     */
    public MergedRole(User user, Set<Role> appends) {
        super(user.getOwningRole().getIdentity(), user.getOwningRole().getName(), user.getOwningRole().isDisabled());
        this.main = user.getOwningRole();

        this.roleAppends = appends;
        this.mergePrivileges();
        user.mergedRole = this;
    }

    @Override
    public boolean addMember(Principal user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeMember(Principal user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMember(Principal user) {
        return main != null && main.isMember(user);
    }

    @Override
    public boolean isMember(Serializable identity) {
        return main != null && main.isMember(identity);
    }

    @Override
    public Set<Principal> getMembers() {
        return main == null ? Collections.emptySet() : main.getMembers();
    }

    @SuppressWarnings("deprecation")
    @Override
    public Enumeration<? extends Principal> members() {
        return main == null ? Collections.emptyEnumeration() : main.members();
    }

    @Override
    public boolean isDisabled() {
        return main == null || super.isDisabled();
    }

    /**
     * @param roleId
     * @return
     */
    public boolean containsRole(ID roleId) {
        for (Role r : roleAppends) {
            if (r.getIdentity().equals(roleId)) return true;
        }

        if (main != null) return main.getIdentity().equals(roleId);
        else return false;
    }

    /**
     * @return
     */
    public ID[] getRoleAppends() {
        Set<ID> set = new HashSet<>();
        for (Role r : roleAppends) set.add((ID) r.getIdentity());
        return set.toArray(new ID[0]);
    }

    /**
     * 合并权限
     */
    protected void mergePrivileges() {
        for (Privileges priv : main.getAllPrivileges()) {
            addPrivileges(priv);
        }

        for (Role role : roleAppends) {
            for (Privileges priv : role.getAllPrivileges()) {
                addPrivileges(mergePrivileges(priv, getPrivileges(priv.getIdentity())));
            }
        }
    }

    private Privileges mergePrivileges(Privileges a, Privileges b) {
        if (b == null) return a;

        // Only one key
        if (a instanceof ZeroPrivileges) {
            Map<String, Integer> aDefMap = parseDefinitionMasks(((ZeroPrivileges) a).getDefinition());
            if (aDefMap.get(ZeroPrivileges.ZERO_FLAG) == ZeroPrivileges.ZERO_MASK) {
                return a;
            } else {
                return b;
            }
        }

        Map<String, Integer> aDefMap = parseDefinitionMasks(((EntityPrivileges) a).getDefinition());
        Map<String, Integer> bDefMap = parseDefinitionMasks(((EntityPrivileges) b).getDefinition());

        Map<String, Integer> defMap = new HashMap<>();

        for (String key : aDefMap.keySet()) {
            Integer aMask = aDefMap.remove(key);
            Integer bMask = bDefMap.remove(key);
            defMap.put(key, mergeMaskValue(aMask, bMask));
        }

        for (Map.Entry<String, Integer> e : bDefMap.entrySet()) {
            defMap.put(e.getKey(), e.getValue());
        }

        Set<String> defs = new HashSet<>();
        for (Map.Entry<String, Integer> e : defMap.entrySet()) {
            defs.add(e.getKey() + ":" + e.getValue());
        }

        String definition = StringUtils.join(defs.iterator(), ",");
        return new EntityPrivileges(((EntityPrivileges) a).getEntity(), definition);
    }

    private Map<String, Integer> parseDefinitionMasks(String d) {
        Map<String, Integer> map = new HashMap<>();
        for (String s : d.split(",")) {
            String[] ss = s.split(":");
            map.put(ss[0], Integer.valueOf(ss[1]));
        }
        return map;
    }

    private int mergeMaskValue(Integer a, Integer b) {
        if (a == null || a == 0) return b;
        if (b == null || b == 0) return a;

        Set<Integer> masks = new HashSet<>();
        for (Integer mask : PERMISSION_MASKS) {
            if ((a & mask) != 0) masks.add(mask);
        }
        for (Integer mask : PERMISSION_MASKS) {
            if ((b & mask) != 0) masks.add(mask);
        }

        int maskValue = 0;
        for (Integer mask : masks) {
            maskValue += mask;
        }
        return maskValue;
    }
}
