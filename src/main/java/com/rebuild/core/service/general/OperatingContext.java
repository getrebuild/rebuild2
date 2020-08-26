/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import org.springframework.util.Assert;

/**
 * 记录操作上下文
 *
 * @author devezhao
 * @since 10/31/2018
 */
public class OperatingContext {

    final private ID operator;
    final private Permission action;

    final private Record beforeRecord;
    final private Record afterRecord;

    final private ID[] affected;

    /**
     * @param operator
     * @param action
     * @param beforeRecord
     * @param afterRecord
     * @param affected
     */
    private OperatingContext(ID operator, Permission action, Record beforeRecord, Record afterRecord, ID[] affected) {
        this.operator = operator;
        this.action = action;

        Assert.isTrue(beforeRecord != null || afterRecord != null, "'beforeRecord' 或 'afterRecord' 至少有一个不为空");
        this.beforeRecord = beforeRecord;
        this.afterRecord = afterRecord;
        this.affected = affected == null ? new ID[]{getAnyRecord().getPrimary()} : affected;
    }

    /**
     * @return
     */
    public ID getOperator() {
        return operator;
    }

    /**
     * @return
     */
    public Permission getAction() {
        return action;
    }

    /**
     * @return
     */
    public Record getBeforeRecord() {
        return beforeRecord;
    }

    /**
     * @return
     */
    public Record getAfterRecord() {
        return afterRecord;
    }

    /**
     * @return
     */
    public Record getAnyRecord() {
        return getAfterRecord() != null ? getAfterRecord() : getBeforeRecord();
    }

    /**
     * 一次操作可能涉及多条记录
     *
     * @return
     */
    public ID[] getAffected() {
        return affected;
    }

    @Override
    public String toString() {
        String clearTxt = "{ Operator: %s, Action: %s, Record(s): %s(%d) }";
        return String.format(clearTxt,
                getOperator(), getAction().getName(), getAnyRecord().getPrimary(), getAffected().length);
    }

    /**
     * @param operator
     * @param action
     * @param before
     * @param after
     * @return
     * @see #create(ID, Permission, Record, Record, ID[])
     */
    public static OperatingContext create(ID operator, Permission action, Record before, Record after) {
        return create(operator, action, before, after, null);
    }

    /**
     * @param operator 操作人
     * @param action   动作
     * @param before   操作前的记录
     * @param after    操作后的记录
     * @param affected 影响的记录
     * @return
     */
    public static OperatingContext create(ID operator, Permission action, Record before, Record after, ID[] affected) {
        return new OperatingContext(operator, action, before, after, affected);
    }
}
