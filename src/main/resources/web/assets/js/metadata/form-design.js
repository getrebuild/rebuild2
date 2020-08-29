/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig
const DIVIDER_LINE = '$DIVIDER$'

$(document).ready(function () {
  $.get(`../list-field?entity=${wpc.entityName}`, function (res) {
    const validFields = {},
      configFields = []
    $(wpc.formConfig.elements).each(function () {
      configFields.push(this.field)
    })
    $(res.data).each(function () {
      validFields[this.fieldName] = this
      if (configFields.includes(this.fieldName) === false) render_unset(this, '.field-list')
    })

    $(wpc.formConfig.elements).each(function () {
      const field = validFields[this.field]
      if (this.field === DIVIDER_LINE) {
        render_item({ fieldName: this.field, fieldLabel: this.label || '', isFull: true }, '.form-preview')
      } else if (!field) {
        const $item = $(`<div class="dd-item"><div class="dd-handle J_field J_missed"><span class="text-danger">[${this.field.toUpperCase()}] 字段已被删除</span></div></div>`).appendTo(
          '.form-preview'
        )
        const $action = $('<div class="dd-action"><a><i class="zmdi zmdi-close"></i></a></div>').appendTo($item.find('.dd-handle'))
        $action.find('a').click(function () {
          $item.remove()
          check_empty()
        })
      } else {
        render_item({ ...field, isFull: this.isFull || false, tip: this.tip || null }, '.form-preview')
      }
    })

    check_empty()
    $('.form-preview')
      .sortable({
        cursor: 'move',
        placeholder: 'dd-placeholder',
        cancel: '.nodata',
        stop: check_empty,
      })
      .disableSelection()
  })

  $('.J_add-divider').click(function () {
    render_item({ fieldName: DIVIDER_LINE, fieldLabel: '', isFull: true }, '.form-preview')
  })

  const _handleSave = function (elements) {
    const data = { belongEntity: wpc.entityName, applyType: 'FORM', config: JSON.stringify(elements) }
    data.metadata = { entity: 'LayoutConfig', id: wpc.formConfig.id || null }

    $('.J_save').button('loading')
    $.post('form-update', JSON.stringify(data), function (res) {
      if (res.error_code === 0) location.reload()
      else RbHighbar.error(res.error_msg)
    })
  }

  $('.J_save').click(function () {
    const formElements = []
    $('.form-preview .J_field').each(function () {
      const $this = $(this)
      if (!$this.data('field')) return
      const item = { field: $this.data('field') }
      if (item.field === DIVIDER_LINE) {
        item.isFull = true
        item.label = $this.find('span').text()
      } else {
        item.isFull = $this.parent().hasClass('w-100')
        const tip = $this.find('.J_tip').attr('title')
        if (tip) item.tip = tip
        item.__newLabel = $this.find('span').text()
        if (item.__newLabel === $this.data('label')) delete item.__newLabel
      }
      formElements.push(item)
    })
    if (formElements.length === 0) {
      RbHighbar.create('请至少布局 1 个字段')
      return
    }

    if ($('.field-list .not-nullable').length > 0) {
      RbAlert.create('有必填字段未被布局，这可能导致新建记录失败。是否仍要保存？', {
        type: 'warning',
        confirmText: '保存',
        confirm: function () {
          this.hide()
          _handleSave(formElements)
        },
      })
    } else {
      _handleSave(formElements)
    }
  })

  $addResizeHandler(() => {
    $setTimeout(() => $('.field-aside .rb-scroller').height($(window).height() - 123), 200, 'FeildAslide-resize')
  })()

  $('.J_new-field').click(() => {
    if (wpc.isSuperAdmin) {
      RbModal.create(`${rb.baseUrl}/p/admin/metadata/field-new?entity=${wpc.entityName}&ref=form-design`, '添加字段')
    } else {
      RbHighbar.error('仅超级管理员可添加字段')
    }
  })
})

const render_item = function (data) {
  const item = $('<div class="dd-item"></div>').appendTo('.form-preview')
  if (data.isFull === true) item.addClass('w-100')

  const handle = $(`<div class="dd-handle J_field" data-field="${data.fieldName}" data-label="${data.fieldLabel}"><span>${data.fieldLabel}</span></div>`).appendTo(item)
  if (data.creatable === false) handle.addClass('readonly')
  else if (data.nullable === false) handle.addClass('not-nullable')
  // 填写提示
  if (data.tip) $('<i class="J_tip zmdi zmdi-info-outline"></i>').appendTo(handle.find('span')).attr('title', data.tip)

  const action = $('<div class="dd-action"></div>').appendTo(handle)
  if (data.displayType) {
    $('<span class="ft">' + data.displayType + '</span>').appendTo(item)
    $('<a class="rowspan mr-1" title="单列/双列"><i class="zmdi zmdi-unfold-more"></i></a>')
      .appendTo(action)
      .click(function () {
        item.toggleClass('w-100')
      })
    $('<a title="修改属性"><i class="zmdi zmdi-edit"></i></a>')
      .appendTo(action)
      .click(function () {
        let call = function (nv) {
          // 字段名
          if (nv.fieldLabel) item.find('.dd-handle>span').text(nv.fieldLabel)
          else item.find('.dd-handle>span').text(item.find('.dd-handle').data('label'))
          // 填写提示
          let tip = item.find('.dd-handle>span>i')
          if (!nv.fieldTips) tip.remove()
          else {
            if (tip.length === 0) tip = $('<i class="J_tip zmdi zmdi-info-outline"></i>').appendTo(item.find('.dd-handle span'))
            tip.attr('title', nv.fieldTips)
          }
        }
        const ov = {
          fieldTips: item.find('.dd-handle>span>i').attr('title'),
          fieldLabel: item.find('.dd-handle>span').text(),
        }
        ov.fieldLabelOld = item.find('.dd-handle').data('label')
        if (ov.fieldLabelOld === ov.fieldLabel) ov.fieldLabel = null
        renderRbcomp(<DlgEditField call={call} {...ov} />)
      })

    $('<a><i class="zmdi zmdi-close"></i></a>')
      .appendTo(action)
      .click(function () {
        render_unset(data)
        item.remove()
        check_empty()
      })
  }

  if (data.fieldName === DIVIDER_LINE) {
    item.addClass('divider')
    $('<a title="修改属性"><i class="zmdi zmdi-edit"></i></a>')
      .appendTo(action)
      .click(function () {
        const call = function (nv) {
          item.find('.dd-handle span').text(nv.dividerName || '')
        }
        const ov = item.find('.dd-handle span').text()
        renderRbcomp(<DlgEditDivider call={call} dividerName={ov || ''} />)
      })

    $('<a><i class="zmdi zmdi-close"></i></a>')
      .appendTo(action)
      .click(function () {
        item.remove()
        check_empty()
      })
  }
}

const render_unset = function (data) {
  const item = $('<li class="dd-item"><div class="dd-handle">' + data.fieldLabel + '</div></li>').appendTo('.field-list')
  $('<span class="ft">' + data.displayType + '</span>').appendTo(item)
  if (data.creatable === false) item.find('.dd-handle').addClass('readonly')
  else if (data.nullable === false) item.find('.dd-handle').addClass('not-nullable')
  item.click(function () {
    render_item(data)
    item.remove()
    check_empty()
  })
  return item
}

const check_empty = function () {
  if ($('.field-list .dd-item').length === 0) $('.field-list .nodata').show()
  else $('.field-list .nodata').hide()
  if ($('.form-preview .dd-item').length === 0) $('.form-preview .nodata').show()
  else $('.form-preview .nodata').hide()
}

// 字段属性
class DlgEditField extends RbAlert {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  renderContent() {
    return (
      <form className="field-attr">
        <div className="form-group">
          <label>填写提示</label>
          <input type="text" className="form-control form-control-sm" name="fieldTips" value={this.state.fieldTips || ''} onChange={this.handleChange} placeholder="输入填写提示" />
        </div>
        <div className="form-group">
          <label>
            字段名称 <span>(部分内建字段不能修改)</span>
          </label>
          <input
            type="text"
            className="form-control form-control-sm"
            name="fieldLabel"
            value={this.state.fieldLabel || ''}
            onChange={this.handleChange}
            placeholder={this.props.fieldLabelOld || '修改字段名称'}
          />
        </div>
        <div className="form-group mb-1">
          <button type="button" className="btn btn-space btn-primary" onClick={this.confirm}>
            确定
          </button>
        </div>
      </form>
    )
  }

  handleChange = (e) => {
    let target = e.target
    let s = {}
    s[target.name] = target.type === 'checkbox' ? target.checked : target.value
    this.setState(s)
  }

  confirm = () => {
    typeof this.props.call === 'function' && this.props.call(this.state || {})
    this.hide()
  }
}

// 分栏属性
class DlgEditDivider extends DlgEditField {
  constructor(props) {
    super(props)
  }

  renderContent() {
    return (
      <form className="field-attr">
        <div className="form-group">
          <label>分栏名称</label>
          <input type="text" className="form-control form-control-sm" name="dividerName" value={this.state.dividerName || ''} onChange={this.handleChange} placeholder="输入分栏名称" />
        </div>
        <div className="form-group mb-1">
          <button type="button" className="btn btn-space btn-primary" onClick={this.confirm}>
            确定
          </button>
        </div>
      </form>
    )
  }
}

// 追加到布局
// eslint-disable-next-line no-unused-vars
const add2Layout = function (add, fieldName) {
  $.get(`../list-field?entity=${wpc.entityName}`, function (res) {
    $(res.data).each(function () {
      if (this.fieldName === fieldName) {
        if (add) render_item({ ...this, isFull: this.isFull || false, tip: this.tip || null }, '.form-preview')
        else render_unset(this, '.field-list')
        return false
      }
    })
  })

  RbModal.hide()
}
