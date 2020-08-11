/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 基础服务类
 * 
 * @author zhaofang123@gmail.com
 * @since 05/21/2017
 */
public abstract class BaseService implements ServiceSpec {

	protected final Logger LOG = LoggerFactory.getLogger(getClass());

	private final PersistManagerFactory factory;

	@Autowired
	protected BaseService(PersistManagerFactory factory) {
		this.factory = factory;
	}
	
	@Override
	public Record create(Record record) {
		return getPersistManagerFactory().createPersistManager().save(record);
	}

	@Override
	public Record update(Record record) {
		return getPersistManagerFactory().createPersistManager().update(record);
	}

	@Override
	public int delete(ID recordId) {
		return getPersistManagerFactory().createPersistManager().delete(recordId);
	}

	@Override
	public Record find(ID recordId, String... returnFields) {
		return null;
	}

	/**
	 * @return
	 */
	protected PersistManagerFactory getPersistManagerFactory() {
		return factory;
	}
	
	@Override
	public String toString() {
		if (getEntityCode() > 0) {
			return "service."
					+ getPersistManagerFactory().getMetadataFactory().getEntity(getEntityCode()).getName()
					+ "@"
					+ Integer.toHexString(hashCode());
		} else {
			return super.toString();
		}
	}
}
