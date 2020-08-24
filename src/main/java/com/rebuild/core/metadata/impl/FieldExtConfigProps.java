/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

/**
 * 字段扩展属性常量
 *
 * @author ZHAO
 * @since 2019/12/3
 */
public class FieldExtConfigProps {

    /**
     * 是否允许负数
     */
    public static final String NUMBER_NOTNEGATIVE = "notNegative";
    /**
     * 格式
     */
    public static final String NUMBER_FORMAT = "numberFormat";

    /**
     * 是否允许负数
     */
    public static final String DECIMAL_NOTNEGATIVE = NUMBER_NOTNEGATIVE;
    /**
     * 格式
     */
    public static final String DECIMAL_FORMAT = "decimalFormat";

    /**
     * 日期格式
     */
    public static final String DATE_DATEFORMAT = "dateFormat";

    /**
     * 日期格式
     */
    public static final String DATETIME_DATEFORMAT = "datetimeFormat";

    /**
     * 允许上传数量
     */
    public static final String FILE_UPLOADNUMBER = "uploadNumber";

    /**
     * 允许上传数量
     */
    public static final String IMAGE_UPLOADNUMBER = FILE_UPLOADNUMBER;

    /**
     * 自动编号规则
     */
    public static final String SERIES_SERIESFORMAT = "seriesFormat";
    /**
     * 自动编号归零方式
     */
    public static final String SERIES_SERIESZERO = "seriesZero";

    /**
     * 使用哪个分类数据
     */
    public static final String CLASSIFICATION_USE = "classification";

    /**
     * 使用哪个状态类
     */
    public static final String STATE_STATECLASS = "stateClass";
}
