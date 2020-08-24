/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.core.privileges.bizz;

/**
 * 扩权限项
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/13
 */
public enum ZeroEntry {

    /**
     * 允许登录
     */
    AllowLogin(true),
    /**
     * 允许批量修改
     */
    AllowBatchUpdate(false),
    /**
     * 允许导入
     */
    AllowDataImport(false),
    /**
     * 允许导出
     */
    AllowDataExport(false),
    /**
     * 允许自定义导航菜单
     */
    AllowCustomNav(true),
    /**
     * 允许自定义列表显示列
     */
    AllowCustomDataList(true),

    ;

    private boolean defaultVal;

    ZeroEntry(boolean defaultVal) {
        this.defaultVal = defaultVal;
    }

    /**
     * 默认值
     *
     * @return
     */
    public boolean getDefaultVal() {
        return defaultVal;
    }
}
