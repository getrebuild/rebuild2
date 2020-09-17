/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(function () {
  $('.J_add').click(() => renderRbcomp(<ApprovalEdit />))
  renderRbcomp(<ApprovalList />, 'dataList')
})

class ApprovalList extends ConfigList {
  constructor(props) {
    super(props)
    this.requestUrl = '/admin/robot/approval/list'
  }

  render() {
    return (
      <React.Fragment>
        {(this.state.data || []).map((item) => {
          return (
            <tr key={'k-' + item[0]}>
              <td>
                <a href={`approval/${item[0]}`}>{item[3]}</a>
              </td>
              <td>{item[2] || item[1]}</td>
              <td>{item[4] ? <span className="badge badge-warning font-weight-light">{$lang('False')}</span> : <span className="badge badge-success font-weight-light">{$lang('True')}</span>}</td>
              <td>{item[5]}</td>
              <td className="actions">
                <a className="icon" title={$lang('Modify')} onClick={() => this.handleEdit(item)}>
                  <i className="zmdi zmdi-edit" />
                </a>
                <a className="icon danger-hover" title={$lang('Delete')} onClick={() => this.handleDelete(item[0])}>
                  <i className="zmdi zmdi-delete" />
                </a>
              </td>
            </tr>
          )
        })}
      </React.Fragment>
    )
  }

  handleEdit(item) {
    renderRbcomp(<ApprovalEdit id={item[0]} name={item[3]} isDisabled={item[4]} />)
  }

  handleDelete(id) {
    const handle = super.handleDelete
    RbAlert.create($lang('DeleteApprovalConfirm'), {
      html: true,
      type: 'danger',
      confirmText: $lang('Delete'),
      confirm: function () {
        this.disabled(true)
        handle(id)
      },
    })
  }
}

class ApprovalEdit extends ConfigFormDlg {
  constructor(props) {
    super(props)
    this.subtitle = $lang('e.RobotApprovalConfig')
  }

  renderFrom() {
    return (
      <React.Fragment>
        {!this.props.id && (
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$lang('SelectSome,ApplyEntity')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" ref={(c) => (this._entity = c)}>
                {(this.state.entities || []).map((item) => {
                  return (
                    <option key={'e-' + item.name} value={item.name}>
                      {item.label}
                    </option>
                  )
                })}
              </select>
            </div>
          </div>
        )}
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$lang('f.RobotApprovalConfig.name')}</label>
          <div className="col-sm-7">
            <input type="text" className="form-control form-control-sm" data-id="name" onChange={this.handleChange} value={this.state.name || ''} />
          </div>
        </div>
        {this.props.id && (
          <div className="form-group row">
            <div className="col-sm-7 offset-sm-3">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" checked={this.state.isDisabled === true} data-id="isDisabled" onChange={this.handleChange} />
                <span className="custom-control-label">
                  {$lang('IsDisable')}
                  <i ref={(c) => (this._tooltip = c)} className="zmdi zmdi-help zicon" title={$lang('DisableApprovalTips')}></i>
                </span>
              </label>
            </div>
          </div>
        )}
      </React.Fragment>
    )
  }

  componentDidMount() {
    super.componentDidMount()
    if (this.props.id) $(this._tooltip).tooltip()
  }

  confirm = () => {
    const post = { name: this.state['name'] }
    if (!post.name) {
      RbHighbar.create($lang('PlsInputSome,f.RobotApprovalConfig.name'))
      return
    }
    if (!this.props.id) {
      post.belongEntity = this.__select2.val()
      if (!post.belongEntity) {
        RbHighbar.create($lang('PlsSelectSome,ApplyEntity'))
        return
      }
    } else {
      post.isDisabled = this.state.isDisabled === true
    }
    post.metadata = {
      entity: 'RobotApprovalConfig',
      id: this.props.id || null,
    }

    this.disabled(true)
    $.post('/app/entity/record-save', JSON.stringify(post), (res) => {
      if (res.error_code === 0) {
        if (this.props.id) location.reload()
        else location.href = 'approval/' + res.data.id
      } else {
        RbHighbar.error(res.error_msg)
      }
      this.disabled()
    })
  }
}
