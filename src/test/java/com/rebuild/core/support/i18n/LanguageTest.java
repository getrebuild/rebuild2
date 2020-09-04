package com.rebuild.core.support.i18n;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import com.rebuild.core.TestSupport;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.EasyMeta;
import org.junit.jupiter.api.Test;

class LanguageTest extends TestSupport {

    @Test
    void genLangOfMetadata() {
        setUp();
        for (Entity entity : MetadataHelper.getMetadataFactory().getEntities()) {
            if (!entity.isQueryable()) continue;
            if (EasyMeta.valueOf(entity).getMetaId() != null) continue;

            System.out.printf("  \"e.%s\": \"%s\",\n", entity.getName(), entity.getDescription());

            for (Field field : entity.getFields()) {
                if (field.getType() == FieldType.PRIMARY || !field.isQueryable()) continue;
                if (MetadataHelper.isCommonsField(field)) continue;

                System.out.printf("  \"f.%s.%s\": \"%s\",\n", entity.getName(), field.getName(), field.getDescription());
            }
        }
    }
}