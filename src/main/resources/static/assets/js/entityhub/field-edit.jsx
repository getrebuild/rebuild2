/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig
const __gExtConfig = {}

const SHOW_REPEATABLE = ['TEXT', 'DATE', 'DATETIME', 'EMAIL', 'URL', 'PHONE', 'REFERENCE', 'CLASSIFICATION']

$(document).ready(function () {
    const dt = wpc.fieldType
    const extConfig = wpc.extConfig

    const $btn = $('.J_save').click(function () {
        if (!wpc.metaId) return
        let _data = {
            fieldLabel: $val('#fieldLabel'),
            comments: $val('#comments'),
            nullable: $val('#fieldNullable'),
            creatable: $val('#fieldCreatable'),
            updatable: $val('#fieldUpdatable'),
            repeatable: $val('#fieldRepeatable')
        }

        if (_data.fieldLabel === '') return RbHighbar.create('请输入字段名称')

        const dv = $val('#defaultValue')
        if (dv) {
            if (checkDefaultValue(dv, dt) === false) return
            else _data.defaultValue = dv
        } else if (dv === '') _data.defaultValue = dv

        const extConfigNew = {...__gExtConfig}
        $(`.J_for-${dt} .form-control, .J_for-${dt} .custom-control-input`).each(function () {
            const k = $(this).attr('id')
            if (k && 'defaultValue' !== k) extConfigNew[k] = $val(this)
        })
        // 单选
        $(`.J_for-${dt} .custom-radio .custom-control-input:checked`).each(function () {
            const k = $(this).attr('name')
            extConfigNew[k] = $val(this)
        })

        if (!$same(extConfigNew, extConfig)) {
            _data['extConfig'] = JSON.stringify(extConfigNew)
            if (Object.keys(extConfigNew).length === 0) _data['extConfig'] = ''
        }

        _data = $cleanMap(_data)
        if (Object.keys(_data).length === 0) {
            location.href = '../fields'
            return
        }

        const save = function () {
            _data.metadata = {entity: 'MetaField', id: wpc.metaId}
            $btn.button('loading')
            $.post('/admin/entity/field-update', JSON.stringify(_data), function (res) {
                if (res.error_code === 0) {
                    location.href = '../fields'
                } else {
                    $btn.button('reset')
                    RbHighbar.error(res.error_msg)
                }
            })
        }

        if ($('#fieldNullable').prop('checked') === false && $('#fieldCreatable').prop('checked') === false) {
            RbAlert.create('同时设置不允许为空和不允许创建可能导致新建记录失败。是否仍要保存？', {
                confirm: function () {
                    this.disabled(true)
                    save()
                }
            })
        } else {
            save()
        }
    })

    $('#fieldNullable').attr('checked', $('#fieldNullable').data('o') === true)
    $('#fieldCreatable').attr('checked', $('#fieldCreatable').data('o') === true)
    $('#fieldUpdatable').attr('checked', $('#fieldUpdatable').data('o') === true)
    $('#fieldRepeatable').attr('checked', $('#fieldRepeatable').data('o') === true)

    $(`.J_for-${dt}`).removeClass('hide')

    // 设置扩展值
    for (let k in extConfig) {
        const $extControl = $(`#${k}`)
        if ($extControl.length === 1) {
            if ($extControl.attr('type') === 'checkbox') $extControl.attr('checked', extConfig[k] === 'true' || extConfig[k] === true)
            else if ($extControl.prop('tagName') === 'DIV') $extControl.text(extConfig[k])
            else $extControl.val(extConfig[k])

        } else {
            $(`.custom-control-input[name="${k}"][value="${extConfig[k]}"]`).attr('checked', true)
        }
    }

    // 特殊字段-审批
    if (wpc.fieldName === 'approvalState' || wpc.fieldName === 'approvalId') {
        $('.J_for-STATE, .J_for-REFERENCE').remove()
    }
    // 列表 & 多选
    else if (dt === 'PICKLIST' || dt === 'MULTISELECT') {
        $.get(`/admin/field/picklist-gets?entity=${wpc.entityName}&field=${wpc.fieldName}&isAll=false`, function (res) {
            if (res.data.length === 0) {
                $('#picklist-items li').text('请添加选项');
                return
            }
            $('#picklist-items').empty()
            $(res.data).each(function () {
                picklistItemRender(this)
            })
            if (res.data.length > 5) $('#picklist-items').parent().removeClass('autoh')
        })
        $('.J_picklist-edit').click(() =>
            RbModal.create(`${rb.baseUrl}/admin/p/entityhub/picklist-editor?entity=${wpc.entityName}&field=${wpc.fieldName}&multi=${dt === 'MULTISELECT'}`, '配置选项'))
    }
    // 自动编号
    else if (dt === 'SERIES') {
        $('#defaultValue').parents('.form-group').remove()
        $('.J_options input').attr('disabled', true)

        $('.J_series-reindex').click(() => {
            RbAlert.create('此操作将为空字段补充编号（空字段过多耗时会较长）。是否继续？', {
                confirm: function () {
                    this.disabled(true)
                    $.post(`/admin/field/series-reindex?entity=${wpc.entityName}&field=${wpc.fieldName}`, () => {
                        this.hide()
                        RbHighbar.success('编号已补充')
                    })
                }
            })
        })
    }
    // 日期时间
    else if (dt === 'DATE' || dt === 'DATETIME') {
        $('#defaultValue').datetimepicker({
            format: dt === 'DATE' ? 'yyyy-mm-dd' : 'yyyy-mm-dd hh:ii:ss',
            minView: dt === 'DATE' ? 2 : 0,
        })
        $('#defaultValue').next().removeClass('hide').find('button').click(() => renderRbcomp(<AdvDateDefaultValue
            type={dt}/>))
    }
    // 文件 & 图片
    else if (dt === 'FILE' || dt === 'IMAGE') {
        let uploadNumber = [0, 9]
        if (extConfig['uploadNumber']) {
            uploadNumber = extConfig['uploadNumber'].split(',')
            uploadNumber[0] = ~~uploadNumber[0]
            uploadNumber[1] = ~~uploadNumber[1]
            $('.J_minmax b').eq(0).text(uploadNumber[0])
            $('.J_minmax b').eq(1).text(uploadNumber[1])
        }

        $('input.bslider').slider({value: uploadNumber}).on('change', function (e) {
            const v = e.value.newValue
            $setTimeout(() => {
                $('.J_minmax b').eq(0).text(v[0])
                $('.J_minmax b').eq(1).text(v[1])
                $('#fieldNullable').attr('checked', v[0] <= 0)
            }, 200, 'bslider-change')
        })
        $('#fieldNullable').attr('disabled', true)
    }
    // 分类
    else if (dt === 'CLASSIFICATION') {
        $.get(`/admin/entityhub/classification/info?id=${extConfig.classification}`, function (res) {
            $('#useClassification a').attr({href: `${rb.baseUrl}/admin/entityhub/classification/${extConfig.classification}`}).text(res.data.name)
        })
    }
    // 引用
    else if (dt === 'REFERENCE') {
        _handleReference()
    }
    // 条形码
    else if (dt === 'BARCODE') {
        $('.J_options input').attr('disabled', true)
    }

    // 显示重复值选项
    if (SHOW_REPEATABLE.includes(dt) && wpc.fieldName !== 'approvalId') {
        $('#fieldRepeatable').parents('.custom-control').removeClass('hide')
    }

    // 内建字段
    if (wpc.fieldBuildin) {
        $('.J_options input, .J_del').attr('disabled', true)
        if (wpc.isSlaveToMasterField) {
            $('.J_action').removeClass('hide')
        } else {
            $('.footer .alert').removeClass('hide')
        }
    } else {
        $('.J_action').removeClass('hide')
    }

    $('.J_del').click(function () {
        if (!wpc.isSuperAdmin) {
            RbHighbar.error('仅超级管理员可删除字段');
            return
        }

        const alertExt = {type: 'danger', confirmText: '删除'}
        alertExt.confirm = function () {
            this.disabled(true)
            $.post(`/admin/entity/field-drop?id=${wpc.metaId}`, (res) => {
                if (res.error_code === 0) {
                    this.hide()
                    RbHighbar.success('字段已删除')
                    setTimeout(function () {
                        location.replace('../fields')
                    }, 1500)
                } else RbHighbar.error(res.error_msg)
            })
        }
        alertExt.call = function () {
            $countdownButton($(this._dlg).find('.btn-danger'))
        }
        RbAlert.create('字段删除后将无法恢复，请务必谨慎操作。确认删除吗？', '删除字段', alertExt)
    })
})

// Render item to PickList box
const picklistItemRender = function (data) {
    const $item = $(`<li class="dd-item" data-key="${data.id}"><div class="dd-handle">${data.text}</div></li>`).appendTo('#picklist-items')
    if (data['default'] === true) $item.addClass('default')
}

// Check incorrect?
// Also see RbFormElement#checkHasError in rb-forms.jsx
const checkDefaultValue = function (v, t) {
    let valid = true
    if (t === 'NUMBER' || t === 'DECIMAL') {
        valid = !isNaN(v)
    } else if (t === 'URL') {
        valid = $regex.isUrl(v)
    } else if (t === 'EMAIL') {
        valid = $regex.isMail(v)
    } else if (t === 'PHONE') {
        valid = $regex.isTel(v)
    }
    if (valid === false) RbHighbar.create('默认值设置有误')
    return valid
}

// ~~ 日期高级表达式
class AdvDateDefaultValue extends RbAlert {

    constructor(props) {
        super(props)
        this._refs = []
        this.state.uncalc = true
    }

    renderContent() {
        return (
            <form className="ml-6 mr-6">
                <div className="form-group">
                    <label className="text-bold">设置日期公式</label>
                    <div className="input-group">
                        <select className="form-control form-control-sm" ref={(c) => this._refs[0] = c}>
                            <option value="NOW">当前日期</option>
                        </select>
                        <select className="form-control form-control-sm ml-1" ref={(c) => this._refs[1] = c}
                                onChange={(e) => this.setState({uncalc: !e.target.value})}>
                            <option value="">不计算</option>
                            <option value="+">加上</option>
                            <option value="-">减去</option>
                        </select>
                        <input type="number" min="1" max="999999" className="form-control form-control-sm ml-1"
                               defaultValue="1" disabled={this.state.uncalc} ref={(c) => this._refs[2] = c}/>
                        <select className="form-control form-control-sm ml-1" disabled={this.state.uncalc}
                                ref={(c) => this._refs[3] = c}>
                            <option value="D">天</option>
                            <option value="M">月</option>
                            <option value="Y">年</option>
                            {this.props.type === 'DATETIME' &&
                            <React.Fragment>
                                <option value="H">小时</option>
                                <option value="I">分钟</option>
                            </React.Fragment>
                            }
                        </select>
                    </div>
                </div>
                <div className="form-group mb-1">
                    <button type="button" className="btn btn-space btn-primary" onClick={this.confirm}>确定</button>
                </div>
            </form>
        )
    }

    confirm = () => {
        let expr = 'NOW'
        const op = $(this._refs[1]).val()
        const num = $(this._refs[2]).val() || 1
        if (op) {
            if (isNaN(num)) {
                RbHighbar.create('请输入数字')
                return
            }
            expr += ` ${op} ${num}${$(this._refs[3]).val()}`
        }
        $('#defaultValue').val('{' + expr + '}')
        this.hide()
    }
}

// 引用
const _handleReference = function () {
    const referenceEntity = $('.J_referenceEntity').data('refentity')

    let dataFilter = (wpc.extConfig || {}).referenceDataFilter
    const saveFilter = function (res) {
        if (res && res.items && res.items.length > 0) {
            $('#referenceDataFilter').text(`已设置条件 (${res.items.length})`)
            dataFilter = res
        } else {
            $('#referenceDataFilter').text('点击设置')
            dataFilter = null
        }
        __gExtConfig.referenceDataFilter = dataFilter
    }
    dataFilter && saveFilter(dataFilter)

    let advFilter
    $('#referenceDataFilter').click(() => {
        if (advFilter) advFilter.show()
        else renderRbcomp(<AdvFilter title="附加过滤条件" inModal={true} canNoFilters={true}
                                     entity={referenceEntity}
                                     filter={dataFilter}
                                     confirm={saveFilter}/>, null, function () {
            advFilter = this
        })
    })
}
