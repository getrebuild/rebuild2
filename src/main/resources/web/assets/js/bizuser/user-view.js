/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const userId = window.__PageConfig.recordId

$(document).ready(function () {
  $('.J_delete')
    .off('click')
    .click(function () {
      $.get(`/admin/bizuser/delete-checks?id=${userId}`, (res) => {
        if (res.data.hasMember === 0) {
          RbAlert.create($lang('DeleteUserSafeConfirm'), $lang('DeleteSome,e.User'), {
            icon: 'alert-circle-o',
            type: 'danger',
            confirmText: $lang('Delete'),
            confirm: function () {
              deleteUser(userId, this)
            },
          })
        } else {
          RbAlert.create($lang('DeleteUserUnSafeConfirm'), $lang('NotDelete'), {
            icon: 'alert-circle-o',
            type: 'danger',
            confirmText: $lang('NotDelete'),
            confirm: function () {
              toggleDisabled(true, this)
            },
          })
        }
      })
    })

  $('.J_disable').click(() => {
    RbAlert.create($lang('DisableUserConfirm'), {
      confirmText: $lang('Disable'),
      confirm: function () {
        toggleDisabled(true, this)
      },
    })
  })
  $('.J_enable').click(() => toggleDisabled(false))

  $('.J_changeRole').click(() => renderRbcomp(<DlgEnableUser user={userId} role={true} />))
  $('.J_changeDept').click(() => renderRbcomp(<DlgEnableUser user={userId} dept={true} />))

  $('.J_resetpwd').click(() => {
    const newpwd = $random(null, true, 8) + '!8'
    RbAlert.create($lang('ResetPasswdConfirm').replace('%s', newpwd), {
      html: true,
      confirm: function () {
        this.disabled(true)
        $.post(`/admin/bizuser/user-resetpwd?id=${userId}&newp=${$decode(newpwd)}`, (res) => {
          this.disabled()
          if (res.error_code === 0) {
            RbHighbar.success($lang('SomeSuccess,ResetPassword'))
            this.hide()
          } else RbHighbar.error(res.error_code)
        })
      },
    })
  })

  if (rb.isAdminVerified) {
    $.get(`/admin/bizuser/check-user-status?id=${userId}`, (res) => {
      if (res.error_code > 0) return
      if (res.data.system === true && rb.isAdminVerified === true) {
        $('.view-action').remove()
        $('.J_tips').removeClass('hide').find('.message p').text($lang('NotModifyAdminUser'))
        return
      }

      if (res.data.disabled === true) {
        $('.J_disable').remove()

        if (!res.data.role || !res.data.dept) {
          $('.J_enable')
            .off('click')
            .click(() => renderRbcomp(<DlgEnableUser enable={true} user={userId} dept={!res.data.dept} role={!res.data.role} />))
        }
      } else {
        $('.J_enable').remove()
      }

      if (res.data.active === true) return

      const reasons = []
      if (!res.data.role) reasons.push($lang('NotSpecRole'))
      else if (res.data.roleDisabled) reasons.push($lang('OwningRoleDisabled'))
      if (!res.data.dept) reasons.push($lang('NotSpecDept'))
      else if (res.data.deptDisabled) reasons.push($lang('OwningDeptDisabled'))
      if (res.data.disabled === true) reasons.push($lang('Disabled'))
      $('.J_tips')
        .removeClass('hide')
        .find('.message p')
        .text($lang('UserUnactiveReason').replace('%s', reasons.join(' / ')))
    })
  }
})

// 启用/禁用
const toggleDisabled = function (disabled, alert) {
  alert && alert.disabled(true)

  const data = {
    user: userId,
    enable: !disabled,
  }
  $.post('/admin/bizuser/enable-user', JSON.stringify(data), (res) => {
    if (res.error_code === 0) {
      RbHighbar.success($lang('e.User') + ' ' + $lang(disabled ? 'Disabled' : 'Enabled'))
      _reload(200)
    } else RbHighbar.error(res.error_msg)
  })
}

// 删除用户
const deleteUser = function (id, alert) {
  alert && alert.disabled(true)

  $.post(`/admin/bizuser/user-delete?id=${id}`, (res) => {
    if (res.error_code === 0) {
      parent.location.hash = '!/View/'
      parent.location.reload()
    } else RbHighbar.error(res.error_msg)
  })
}

// 激活用户/变更部门/角色
class DlgEnableUser extends RbModalHandler {
  constructor(props) {
    super(props)

    this.__title = $lang('ActiveUser')
    if (!props.enable) this.__title = $lang('ModifySome,' + (props.dept === true ? 'e.Department' : 'e.Role'))
  }

  render() {
    return (
      <RbModal title={this.__title} ref={(c) => (this._dlg = c)} disposeOnHide={true}>
        <div className="form">
          {this.props.dept === true && (
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$lang('SelectSome,f.User.deptId')}</label>
              <div className="col-sm-7">
                <UserSelector hideUser={true} hideRole={true} hideTeam={true} multiple={false} ref={(c) => (this._deptNew = c)} />
              </div>
            </div>
          )}
          {this.props.role === true && (
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$lang('SelectSome,f.User.roleId')}</label>
              <div className="col-sm-7">
                <UserSelector hideUser={true} hideDepartment={true} hideTeam={true} multiple={false} ref={(c) => (this._roleNew = c)} />
              </div>
            </div>
          )}
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary btn-space" type="button" onClick={() => this.post()}>
                {$lang('Confirm')}
              </button>
              <a className="btn btn-link btn-space" onClick={() => this.hide()}>
                {$lang('Cancel')}
              </a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  post() {
    const data = { user: this.props.user }

    if (this.props.enable === true) {
      data.enable = true
    }
    if (this._deptNew) {
      const v = this._deptNew.val()
      if (v.length === 0) return RbHighbar.create($lang('PlsSelectSome,e.Department'))
      data.dept = v[0]
    }
    if (this._roleNew) {
      const v = this._roleNew.val()
      if (v.length === 0) return RbHighbar.create($lang('PlsSelectSome,e.Role'))
      data.role = v[0]
    }

    const $btns = $(this._btns).find('.btn').button('loading')
    $.post('/admin/bizuser/enable-user', JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        if (data.enable === true) RbHighbar.success($lang('SomeEnabled,e.User'))
        _reload(data.enable ? 200 : 0)
      } else {
        RbHighbar.error(res.error_msg)
      }
      $btns.button('reset')
    })
  }
}

const _reload = function (timeout) {
  setTimeout(() => location.reload(), timeout || 1)
  parent && parent.RbListPage && parent.RbListPage.reload()
}
