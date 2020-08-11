/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service;

import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.MetadataHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * 普通的 CRUD 服务
 * <br>- 此类有事物
 * <br>- 此类不经过用户权限验证
 *
 * @author zhaofang123@gmail.com
 * @since 11/06/2017
 */
@Service
public class CommonsService extends BaseService {

	@Autowired
	protected CommonsService(PersistManagerFactory factory) {
		super(factory);
	}

	@Override
	final public int getEntityCode() {
		return 0;
	}

	@Override
	public Record create(Record record) {
		return create(record, true);
	}

	@Override
	public int delete(ID recordId) {
		return delete(recordId, true);
	}

	@Override
	public Record update(Record record) {
		return update(record, true);
	}

	/**
	 * @param record
	 * @param strictMode
	 * @return
	 */
	public Record create(Record record, boolean strictMode) {
		if (strictMode) {
			tryIfWithPrivileges(record);
		}
		return super.create(record);
	}

	/**
	 * @param record
	 * @param strictMode
	 * @return
	 */
	public Record update(Record record, boolean strictMode) {
		if (strictMode) {
			tryIfWithPrivileges(record);
		}
		return super.update(record);
	}

	/**
	 * @param recordId
	 * @param strictMode
	 * @return
	 */
	public int delete(ID recordId, boolean strictMode) {
		if (strictMode) {
			tryIfWithPrivileges(recordId);
		}
		return super.delete(recordId);
	}

	/**
	 * 批量新建/更新
	 *
	 * @param records
	 * @param strictMode
	 */
	public void createOrUpdate(Record[] records, boolean strictMode) {
		Assert.notNull(records, "[records] cannot be null");
		for (Record r : records) {
			if (r.getPrimary() == null) {
				create(r, strictMode);
			} else {
				update(r, strictMode);
			}
		}
	}

	/**
	 * 批量新建/更新、删除
	 *
	 * @param records
	 * @param deletes
	 * @param strictMode
	 */
	public void createOrUpdateAndDelete(Record[] records, ID[] deletes, boolean strictMode) {
		if (records != null) {
			createOrUpdate(records, strictMode);
		}

		if (deletes != null) {
			for (ID d : deletes) {
				delete(d, strictMode);
			}
		}
	}

	/**
	 * 业务实体禁止调用此类
	 *
	 * @param idOrRecord
	 * @throws PrivilegesException
	 */
	protected void tryIfWithPrivileges(Object idOrRecord) throws PrivilegesException {
		Entity useEntity;

		if (idOrRecord instanceof ID) {
			useEntity = MetadataHelper.getEntity(((ID) idOrRecord).getEntityCode());
		} else if (idOrRecord instanceof Record) {
			useEntity = ((Record) idOrRecord).getEntity();
		} else {
			throw new RebuildException("Invalid argument [idOrRecord] : " + idOrRecord);
		}

		// 验证主实体
		if (MetadataHelper.isSlaveEntity(useEntity.getEntityCode())) {
			useEntity = useEntity.getMasterEntity();
		}

		if (MetadataHelper.hasPrivilegesField(useEntity)) {
			throw new PrivilegesException("Privileges entity cannot use this class (methods) : " + useEntity.getName());
		}
	}
}
