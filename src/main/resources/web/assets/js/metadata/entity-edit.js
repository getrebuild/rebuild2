/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// eslint-disable-next-line no-undef, no-unused-vars
window.clickIcon = function (icon) {
  $('#entityIcon')
    .attr('value', icon)
    .find('i')
    .attr('class', 'icon zmdi zmdi-' + icon)
  RbModal.hide()
}

const wpc = window.__PageConfig

$(document).ready(function () {
  if (!wpc.metaId) $('.footer .alert').removeClass('hide')
  else $('.footer .J_action').removeClass('hide')

  $('.J_tab-' + wpc.entity + ' a').addClass('active')

  const $btn = $('.J_save').click(function () {
    if (!wpc.metaId) return
    let data = {
      entityLabel: $val('#entityLabel'),
      comments: $val('#comments'),
      nameField: $val('#nameField'),
    }
    if (data.label === '') return RbHighbar.create($lang('PlsInoutSome,EntityName'))

    const icon = $val('#entityIcon')
    if (icon) data.icon = icon

    data = $cleanMap(data)
    if (Object.keys(data).length === 0) {
      location.reload()
      return
    }

    data.metadata = {
      entity: 'MetaEntity',
      id: wpc.metaId,
    }

    $btn.button('loading')
    $.post('../entity-update', JSON.stringify(data), function (res) {
      if (res.error_code === 0) location.reload()
      else RbHighbar.error(res.error_msg)
    })
  })

  $('#entityIcon').click(function () {
    RbModal.create('/p/commons/search-icon', $lang('SelectSome,Icon'))
  })

  $.get(`/commons/metadata/fields?entity=${wpc.entity}`, function (d) {
    const rs = d.data.map((item) => {
      const canName =
        item.type === 'NUMBER' ||
        item.type === 'DECIMAL' ||
        item.type === 'TEXT' ||
        item.type === 'EMAIL' ||
        item.type === 'URL' ||
        item.type === 'PHONE' ||
        item.type === 'SERIES' ||
        item.type === 'PICKLIST' ||
        item.type === 'CLASSIFICATION' ||
        item.type === 'DATE' ||
        item.type === 'DATETIME'
      return {
        id: item.name,
        text: item.label,
        disabled: canName === false,
        title: canName === false ? $lang('NotBeNameFieldTips') : item.label,
      }
    })

    const rsSort = []
    rs.forEach((item) => {
      if (item.disabled === false) rsSort.push(item)
    })
    rs.forEach((item) => {
      if (item.disabled === true) rsSort.push(item)
    })

    $('#nameField')
      .select2({
        placeholder: $lang('SelectSome,Field'),
        allowClear: false,
        data: rsSort,
      })
      .val(wpc.nameField)
      .trigger('change')
  })
})
