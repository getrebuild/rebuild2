/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.support;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.metadata.impl.ConfigurationMetadataFactory;
import cn.devezhao.persist4j.util.support.Table;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.EasyMeta;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 根据 METADATA 生成语言
 * 
 * @author Zhao Fangfang
 * @since 0.2, 2020-09-10
 */
public class LanguageGenerator {

	private static PersistManagerFactory PMF;

	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(Application.class);
		PMF = ctx.getBean(PersistManagerFactory.class);

		generate();

		System.exit(0);
	}

	static void generate() {
		for (Entity entity : PMF.getMetadataFactory().getEntities()) {
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
