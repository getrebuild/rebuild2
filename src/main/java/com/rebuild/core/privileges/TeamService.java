/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.RebuildApplication;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.BaseServiceImpl;

import java.util.Collection;

/**
 * 团队
 *
 * @author devezhao
 * @since 2019/11/13
 */
public class TeamService extends BaseServiceImpl implements AdminGuard {

    protected TeamService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.Team;
    }

    @Override
    public Record create(Record record) {
        record = super.create(record);
        RebuildApplication.getUserStore().refreshTeam(record.getPrimary());
        return record;
    }

    @Override
    public Record update(Record record) {
        record = super.update(record);
        RebuildApplication.getUserStore().refreshTeam(record.getPrimary());
        return record;
    }

    @Override
    public int delete(ID teamId) {
        int del = super.delete(teamId);
        RebuildApplication.getUserStore().removeTeam(teamId);
        return del;
    }

    /**
     * 添加成员
     *
     * @param teamId
     * @param members
     * @return
     */
    public int createMembers(ID teamId, Collection<ID> members) {
        int added = 0;
        Team team = RebuildApplication.getUserStore().getTeam(teamId);
        for (ID user : members) {
            if (team.isMember(user)) {
                continue;
            }
            Record record = EntityHelper.forNew(EntityHelper.TeamMember, RebuildApplication.getCurrentUser());
            record.setID("teamId", teamId);
            record.setID("userId", user);
            super.create(record);
            added++;
        }

        if (added > 0) {
            RebuildApplication.getUserStore().refreshTeam(teamId);
        }
        return added;
    }

    /**
     * 移除成员
     *
     * @param teamId
     * @param members
     * @return
     */
    public int deleteMembers(ID teamId, Collection<ID> members) {
        int deleted = 0;
        for (ID m : members) {
            Object[] exists = RebuildApplication.createQueryNoFilter(
                    "select memberId from TeamMember where teamId = ? and userId = ?")
                    .setParameter(1, teamId)
                    .setParameter(2, m)
                    .unique();
            if (exists != null) {
                super.delete((ID) exists[0]);
                deleted++;
            }
        }

        if (deleted > 0) {
            RebuildApplication.getUserStore().refreshTeam(teamId);
        }
        return deleted;
    }
}
