/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(function () {
  $('.J_add').click(() => renderRbcomp(<DlgEdit />))
  renderRbcomp(<GridList />, 'list')
})

class GridList extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    return (
      <div className="card-list row">
        {(this.state.list || []).map((item) => {
          return (
            <div key={'item-' + item[0]} className="col-xl-2 col-lg-3 col-md-4 col-sm-6">
              <div className="card">
                <div className="card-body">
                  <a className="text-truncate" href={'classification/' + item[0]}>
                    {item[1]}
                  </a>
                  <p className="text-muted text-truncate">
                    {item[3]}
                    {$lang('XLevelClass')}
                  </p>
                </div>
                <div className="card-footer card-footer-contrast">
                  <div className="float-left">
                    <a onClick={() => this._handleEdit(item)}>
                      <i className="zmdi zmdi-edit"></i>
                    </a>
                    <a onClick={() => this._handleDelete(item[0])} className="danger">
                      <i className="zmdi zmdi-delete"></i>
                    </a>
                  </div>
                  {item[2] && <div className="badge badge-warning">{$lang('Disabled')}</div>}
                  <div className="clearfix"></div>
                </div>
              </div>
            </div>
          )
        })}
        {(!this.state.list || this.state.list.length === 0) && <div className="text-muted">{$lang('NoData')}</div>}
      </div>
    )
  }

  componentDidMount() {
    $.get('/admin/metadata/classification/list', (res) => this.setState({ list: res.data }))
  }

  _handleEdit(item) {
    renderRbcomp(<DlgEdit id={item[0]} name={item[1]} isDisabled={item[2]} />)
  }

  _handleDelete(dataId) {
    RbAlert.create($lang('DeleteClassDataConfirm'), {
      type: 'danger',
      confirmText: $lang('Delete'),
      confirm: function () {
        this.disabled(true)
        $.post(`/app/entity/record-delete?id=${dataId}`, (res) => {
          if (res.error_code === 0) {
            RbHighbar.success($lang('SomeDeleted,ClassData'))
            setTimeout(() => location.reload(), 500)
          } else {
            this.disabled(false)
            RbHighbar.error(res.error_msg)
          }
        })
      },
    })
  }
}

class DlgEdit extends RbFormHandler {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    return (
      <RbModal title={$lang((this.props.id ? 'ModifySome' : 'AddSome') + ',ClassData')} ref={(c) => (this._dlg = c)} disposeOnHide={true}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$lang('ClassDataName')}</label>
            <div className="col-sm-7">
              <input className="form-control form-control-sm" value={this.state.name || ''} data-id="name" onChange={this.handleChange} maxLength="40" />
            </div>
          </div>
          {this.props.id && (
            <div className="form-group row">
              <div className="col-sm-7 offset-sm-3">
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                  <input className="custom-control-input" type="checkbox" checked={this.state.isDisabled === true} data-id="isDisabled" onChange={this.handleChange} />
                  <span className="custom-control-label">{$lang('IsDisableTips')}</span>
                </label>
              </div>
            </div>
          )}
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary" type="button" onClick={this.save}>
                {$lang('Confirm')}
              </button>
              <a className="btn btn-link" onClick={this.hide}>
                {$lang('Cancel')}
              </a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  save = (e) => {
    e.preventDefault()
    if (!this.state.name) return RbHighbar.create($lang('PlsInputSome,ClassDataName'))

    const data = {
      name: this.state.name,
      isDisabled: this.state.isDisabled === true,
      metadata: {
        entity: 'Classification',
        id: this.props.id || null,
      },
    }

    this.disabled(true)
    $.post('/app/entity/record-save', JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        if (this.props.id) location.reload()
        else location.href = 'classification/' + res.data.id
      } else {
        RbHighbar.error(res.error_msg)
        this.disabled(false)
      }
    })
  }
}
