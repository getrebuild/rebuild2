/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global RbForm */

let RbForm_postAfter = RbForm.postAfter
RbForm.postAfter = function () {
    RbForm_postAfter()
    if (parent && parent.loadDeptTree) parent.loadDeptTree()
}

const deleteDept = function (alert) {
    alert && alert.disabled(true)
    $.post(`/admin/bizuser/dept-delete?transfer=&id=${dept_id}`, (res) => {
        if (res.error_code === 0) {
            parent.location.hash = '!/View/'
            parent.location.reload()
        } else RbHighbar.error(res.error_msg)
    })
}

const dept_id = window.__PageConfig.recordId
$(document).ready(function () {
    $('.J_delete').off('click').click(() => {
        $.get(`/admin/bizuser/delete-checks?id=${dept_id}`, (res) => {
            if (res.data.hasMember === 0 && res.data.hasChild === 0) {
                RbAlert.create('此部门可以被安全的删除', '删除部门', {
                    icon: 'alert-circle-o',
                    type: 'danger',
                    confirmText: '删除',
                    confirm: function () {
                        deleteDept(this)
                    }
                })
            } else {
                let msg = '此部门下有 '
                if (res.data.hasMember > 0) msg += '<b>' + res.data.hasMember + '</b> 个用户' + (res.data.hasChild > 0 ? '和 ' : ' ')
                if (res.data.hasChild > 0) msg += '<b>' + res.data.hasChild + '</b> 个子部门'
                msg += '<br>需要先将他们转移至其他部门，然后才能安全删除'
                RbAlert.create(msg, '无法删除', {
                    type: 'danger',
                    html: true
                })
            }
        })
    })
})
