/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// eslint-disable-next-line no-unused-vars
class OnlineUserViewer extends RbModalHandler {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <RbModal ref={(c) => (this._dlg = c)} title={$lang('ViewOnlineUsers')} disposeOnHide={true}>
        <table className="table table-striped table-hover table-sm dialog-table">
          <thead>
            <tr>
              <th style={{ minWidth: 150 }}>{$lang('e.User')}</th>
              <th style={{ minWidth: 150 }}>{$lang('LastActive')}</th>
              <th width="90"></th>
            </tr>
          </thead>
          <tbody>
            {(this.state.users || []).map((item) => {
              return (
                <tr key={`user-${item.user}`}>
                  <td className="user-avatar cell-detail user-info">
                    <img src={`${rb.baseUrl}/account/user-avatar/${item.user}`} />
                    <span className="pt-1">{item.fullName}</span>
                  </td>
                  <td className="cell-detail">
                    <code className="text-break">{item.activeUrl || $lang('None')}</code>
                    <span className="cell-detail-description">{item.activeTime}</span>
                  </td>
                  <td className="actions text-right">
                    <button className="btn btn-danger btn-sm bordered" type="button" onClick={() => this._killSession(item.user)}>
                      {$lang('KillSession')}
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </RbModal>
    )
  }

  componentDidMount() {
    this._load()
  }

  _load() {
    $.get('/admin/bizuser/online-users', (res) => {
      if (res.error_code === 0) this.setState({ users: res.data })
      else RbHighbar.error(res.error_msg)
    })
  }

  _killSession(user) {
    const that = this
    RbAlert.create($lang('KillSessionConfirm'), {
      confirm: function () {
        $.post(`/admin/bizuser/kill-session?user=${user}`, () => {
          this.hide()
          that._load()
        })
      },
    })
  }
}
