/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(() => {
  $('.J_delete-role').click(() => deleteRole(window.__PageConfig.recordId))
})

// 删除角色
const deleteRole = function (id) {
  const alertExt = {
    type: 'danger',
    confirmText: $lang('Delete'),
    confirm: function () {
      this.disabled(true)

      $.post(`/admin/bizuser/role-delete?transfer=&id=${id}`, (res) => {
        if (res.error_code === 0) location.replace(rb.baseUrl + '/admin/bizuser/role-privileges')
        else RbHighbar.error(res.error_msg)
      })
    },
  }

  $.get(`/admin/bizuser/delete-checks?id=${id}`, function (res) {
    if (res.data.hasMember === 0) {
      RbAlert.create($lang('DeleteRoleSafeConfirm'), $lang('DeleteSome,e.Role'), { ...alertExt, icon: 'alert-circle-o' })
    } else {
      RbAlert.create($lang('DeleteRoleUnSafeConfirm').replace('%d', res.data.hasMember), $lang('DeleteSome,e.Role'), { ...alertExt, html: true })
    }
  })
}
