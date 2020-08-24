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

package com.rebuild.core.service.trigger;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/24
 */
public class ActionFactory {

    /**
     * @return
     */
    public static ActionType[] getAvailableActions() {
        return ActionType.values();
    }

    /**
     * @param type
     * @return
     */
    public static TriggerAction createAction(String type) {
        return createAction(type, null);
    }

    /**
     * @param type
     * @param context
     * @return
     */
    public static TriggerAction createAction(String type, ActionContext context) {
        return createAction(ActionType.valueOf(type), context);
    }

    /**
     * @param type
     * @param context
     * @return
     */
    public static TriggerAction createAction(ActionType type, ActionContext context) {
        try {
            return type.newInstance(context);
        } catch (NoSuchMethodException ex) {
            throw new TriggerException("无效操作类型: " + type, ex);
        }
    }
}
