/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.i18n;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import com.rebuild.core.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.EasyMeta;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

/**
 * @author devezhao
 * @since 2020/8/26
 */
public class LanguageTest extends TestSupport {

    @Test
    public void genLanguageViaMetadata() {
        for (Entity entity : MetadataHelper.getMetadataFactory().getEntities()) {
            if (EasyMeta.valueOf(entity).getMetaId() != null) continue;
            if (StringUtils.isBlank(entity.getDescription())) continue;

            System.out.printf("  \"e.%s\": \"%s\",\n", entity.getName(), entity.getDescription().split("\\(")[0].trim());

            for (Field field : entity.getFields()) {
                if (field.getType() == FieldType.PRIMARY || MetadataHelper.isCommonsField(field)) continue;
                if (StringUtils.isBlank(field.getDescription())) continue;

                System.out.printf("  \"f.%s.%s\": \"%s\",\n", entity.getName(), field.getName(), field.getDescription().split("\\(")[0].trim());
            }
        }
    }
}