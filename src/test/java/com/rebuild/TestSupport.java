/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.Entity2Schema;
import com.rebuild.core.metadata.impl.Field2Schema;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.rbstore.MetaschemaImporter;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.BlackList;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ResourceUtils;

/**
 * JUnit4 测试基类
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class TestSupport {

    protected static final Logger LOG = LoggerFactory.getLogger(TestSupport.class);

    private static boolean RebuildStarted = false;

    @BeforeClass
    public static void setUp() {
        if (RebuildStarted) return;
        LOG.warn("TESTING Setup ...");

        Application.debug();
        RebuildStarted = true;

        try {
            addTestEntities(false);
        } catch (Exception ex) {
            LOG.error("Add entities of test error!", ex);
            System.exit(-1);
        }
    }

    @AfterClass
    public static void setDown() {
        LOG.warn("TESTING Setdown ...");

        Application.getSessionStore().clean();
    }

    @Test
    public void contextLoads() {
    }

    // -- 测试实体

    // 全部字段类型
    protected static final String TEST_ENTITY = "TestAllFields";

    // 业务实体
    protected static final String Account = "Account999";

    // 主-明细实体
    protected static final String SalesOrder = "SalesOrder999";
    protected static final String SalesOrderItem = "SalesOrderItem999";

    // -- 测试用户

    // 示例用户
    protected static final ID SIMPLE_USER = ID.valueOf("001-9000000000000001");
    // 示例部门
    protected static final ID SIMPLE_DEPT = ID.valueOf("002-9000000000000001");
    // 示例角色（无任何权限）
    protected static final ID SIMPLE_ROLE = ID.valueOf("003-9000000000000001");
    // 示例团队
    protected static final ID SIMPLE_TEAM = ID.valueOf("006-9000000000000001");

    /**
     * 添加测试用实体
     *
     * @param dropExists
     * @throws Exception
     */
    protected static void addTestEntities(boolean dropExists) throws Exception {
        if (dropExists) {
            if (MetadataHelper.containsEntity(TEST_ENTITY)) {
                LOG.warn("Dropping test entity : " + TEST_ENTITY);
                new Entity2Schema(UserService.ADMIN_USER).dropEntity(MetadataHelper.getEntity(TEST_ENTITY), true);
            }

            if (MetadataHelper.containsEntity(SalesOrder)) {
                LOG.warn("Dropping test entity : " + SalesOrder);
                new Entity2Schema(UserService.ADMIN_USER).dropEntity(MetadataHelper.getEntity(SalesOrder), true);
            }

            if (MetadataHelper.containsEntity(Account)) {
                LOG.warn("Dropping test entity : " + Account);
                new Entity2Schema(UserService.ADMIN_USER).dropEntity(MetadataHelper.getEntity(Account), true);
            }
        }

        if (!MetadataHelper.containsEntity(TEST_ENTITY)) {
            Entity2Schema entity2Schema = new Entity2Schema(UserService.ADMIN_USER);
            String entityName = entity2Schema.createEntity(TEST_ENTITY.toUpperCase(), null, null, true);
            Entity testEntity = MetadataHelper.getEntity(entityName);

            for (DisplayType dt : DisplayType.values()) {
                if (dt == DisplayType.ID || dt == DisplayType.LOCATION || dt == DisplayType.ANYREFERENCE) continue;

                String fieldName = dt.name().toUpperCase();
                if (BlackList.isBlack(fieldName)) fieldName += "1";

                if (dt == DisplayType.REFERENCE) {
                    new Field2Schema(UserService.ADMIN_USER)
                            .createField(testEntity, fieldName, dt, null, entityName, null);
                } else if (dt == DisplayType.CLASSIFICATION) {
                    JSON area = JSON.parseObject("{classification:'018-0000000000000001'}");
                    new Field2Schema(UserService.ADMIN_USER)
                            .createField(testEntity, fieldName, dt, null, entityName, area);
                } else if (dt == DisplayType.STATE) {
                    JSON area = JSON.parseObject("{stateClass:'com.rebuild.core.support.state.HowtoState'}");
                    new Field2Schema(UserService.ADMIN_USER)
                            .createField(testEntity, fieldName, dt, null, entityName, area);
                } else {
                    new Field2Schema(UserService.ADMIN_USER)
                            .createField(testEntity, fieldName, dt, null, null, null);
                }
            }
        }

        if (!MetadataHelper.containsEntity(Account)) {
            String metaschema = FileUtils.readFileToString(
                    ResourceUtils.getFile("classpath:metaschema.Account.json"));
            MetaschemaImporter importer = new MetaschemaImporter(JSON.parseObject(metaschema));
            TaskExecutors.run(importer.setUser(UserService.ADMIN_USER));
        }

        if (!MetadataHelper.containsEntity(SalesOrder)) {
            String metaschema = FileUtils.readFileToString(
                    ResourceUtils.getFile("classpath:metaschema.SalesOrder.json"));
            MetaschemaImporter importer = new MetaschemaImporter(JSON.parseObject(metaschema));
            TaskExecutors.run(importer.setUser(UserService.ADMIN_USER));
        }
    }
}
