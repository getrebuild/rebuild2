/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.core.service.dashboard.charts;

import com.rebuild.core.RebuildException;

/**
 * @author devezhao
 * @since 12/20/2018
 */
public class ChartsException extends RebuildException {
    private static final long serialVersionUID = 1552569207578832059L;

    public ChartsException(String message) {
        super(message);
    }
}
