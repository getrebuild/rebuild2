/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// eslint-disable-next-line no-undef
RbForm.postAfter = function (data) {
    location.href = rb.baseUrl + '/admin/bizuser/role/' + data.id
}

const roleId = window.__PageConfig.recordId
$(document).ready(function () {
    $('.J_new-role').click(() => RbFormModal.create({title: '新建角色', entity: 'Role', icon: 'lock'}))

    if (roleId) {
        $('.J_save').attr('disabled', false).click(updatePrivileges)
        loadPrivileges()
    }
    loadRoles()

    // ENTITY

    // 单个操作
    $('#priv-entity tbody .priv').click(function () {
        const $this = $(this)
        clickPriv($this, $this.data('action'))
    })
    // 批量操作
    $('#priv-entity thead th>a').click(function () {
        const action = $(this).data('action')
        const $privCol = $('#priv-entity tbody .priv[data-action="' + action + '"]')
        clickPriv($privCol, action)
    })
    // 批量操作
    $('#priv-entity tbody .name>a').click(function () {
        const $privRow = $(this).parent().parent().find('.priv')
        const clz = ['R0']
        if ($privRow.eq(0).hasClass('R0')) clz.push('R4')
        $privRow.removeClass('R0 R1 R2 R3 R4').addClass(clz.join(' '))
    })

    // ZERO

    $('#priv-zero tbody .priv').click(function () {
        clickPriv($(this), 'Z')
    })
    $('#priv-zero thead th>a').click(function () {
        const $privZero = $('#priv-zero tbody .priv[data-action="Z"]')
        clickPriv($privZero, 'Z')
    })
    $('#priv-zero tbody .name>a').click(function () {
        const $el = $(this).parent().next().find('i.priv')
        clickPriv($el, 'Z')
    })
})

const clickPriv = function (elements, action) {
    if (action === 'C' || action === 'Z') {
        if (elements.first().hasClass('R0')) elements.removeClass('R0').addClass('R4')
        else elements.removeClass('R4').addClass('R0')
    } else {
        let clz = 'R0'
        if (elements.hasClass('R0')) clz = 'R1'
        else if (elements.hasClass('R1')) clz = 'R2'
        else if (elements.hasClass('R2')) clz = 'R3'
        else if (elements.hasClass('R3')) clz = 'R4'
        elements.removeClass('R0 R1 R2 R3 R4').addClass(clz)
    }
}

const loadRoles = function () {
    $.get('/admin/bizuser/role-list', function (res) {
        $('.aside-tree .ph-item').remove()
        $('.aside-tree ul').empty()
        $(res.data).each(function () {
            const _id = this.id
            const $item = $('<li><a class="text-truncate' + (this.disabled ? ' text-disabled' : '') + '" href="' + rb.baseUrl + '/admin/bizuser/role/' + _id + '">' + this.name + (this.disabled ? '<small></small>' : '') + '</a></li>').appendTo('.aside-tree ul')
            const $action = $('<div class="action"><a class="J_edit"><i class="zmdi zmdi-edit"></i></a><a class="J_del"><i class="zmdi zmdi-delete"></i></a></div>').appendTo($item)
            if (roleId === _id) $item.addClass('active')
            if (_id === '003-0000000000000001') $action.remove()

            $action.find('a.J_edit').click(() => RbFormModal.create({
                title: '编辑角色',
                entity: 'Role',
                icon: 'lock',
                id: _id
            }))

            $action.find('a.J_del').click(function () {
                const alertExt = {
                    type: 'danger',
                    confirmText: '删除',
                    confirm: function () {
                        deleteRole(_id, this)
                    }
                }

                $.get(`/admin/bizuser/delete-checks?id=${_id}`, function (res) {
                    if (res.data.hasMember === 0) {
                        RbAlert.create('此角色可以被安全的删除', '删除角色', {...alertExt, icon: 'alert-circle-o'})
                    } else {
                        const msg = '有 <b>' + res.data.hasMember + '</b> 个用户使用了此角色<br>删除将导致这些用户被禁用，直到你为他们指定了新的角色'
                        RbAlert.create(msg, '删除角色', {...alertExt, html: true})
                    }
                })
            })
        })
    })
}

const loadPrivileges = function () {
    $.get(`/admin/bizuser/privileges-list?role=${roleId}`, function (res) {
        if (res.error_code === 0) {
            $(res.data).each(function () {
                let $tr = $('.table-priv tbody td.name>a[data-name="' + this.name + '"]')
                $tr = $tr.parent().parent()
                let defi = {}
                try {
                    defi = JSON.parse(this.definition)
                } catch (ignored) {
                    // NOOP
                }
                for (let k in defi) {
                    $tr.find('.priv[data-action="' + k + '"]').removeClass('R0 R1 R2 R3 R4').addClass('R' + defi[k])
                }
            })
        } else {
            $('.J_save').attr('disabled', true)
            $('.J_tips').removeClass('hide').find('.message p').text(res.error_msg)
        }
    })
}

const updatePrivileges = function () {
    const privEntity = {}
    $('#priv-entity tbody>tr').each(function () {
        const $tr = $(this)
        const name = $tr.find('td.name a').data('name')

        const definition = {}
        $tr.find('i.priv').each(function () {
            const $this = $(this)
            const action = $this.data('action')
            let deep = 0
            if ($this.hasClass('R1')) deep = 1
            else if ($this.hasClass('R2')) deep = 2
            else if ($this.hasClass('R3')) deep = 3
            else if ($this.hasClass('R4')) deep = 4
            definition[action] = deep
        })
        privEntity[name] = definition
    })

    const privZero = {}
    $('#priv-zero tbody>tr').each(function () {
        const etr = $(this)
        const name = etr.find('td.name a').data('name')
        const definition = etr.find('i.priv').hasClass('R0') ? {Z: 0} : {Z: 4}
        privZero[name] = definition
    })

    const _data = {entity: privEntity, zero: privZero}
    $.post(`/admin/bizuser/privileges-update?role=${roleId}`, JSON.stringify(_data), (res) => {
        if (res.error_code === 0) location.reload()
        else RbHighbar.error(res.error_msg)
    })
}

const deleteRole = function (id, dlg) {
    dlg.disabled(true)
    $.post(`/admin/bizuser/role-delete?transfer=&id=${id}`, (res) => {
        if (res.error_code === 0) location.replace(rb.baseUrl + '/admin/bizuser/role-privileges')
        else RbHighbar.error(res.error_msg)
    })
}