/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const _INSTALL_STATES = {
  10: ['zmdi-settings zmdi-hc-spin', $lang('Installing')],
  11: ['zmdi-check text-success', $lang('InstallSucceed')],
  12: ['zmdi-close-circle-o text-danger', $lang('InstallFailed')],
}

class Setup extends React.Component {
  state = { ...this.props, stepNo: 0, installState: 10 }

  render() {
    let state = _INSTALL_STATES[this.state.installState]
    return (
      <div>
        {!this.state.stepNo && <RbWelcome $$$parent={this} />}
        {this.state.stepNo === 2 && <DatabaseConf {...this.state.databaseProps} $$$parent={this} />}
        {this.state.stepNo === 3 && <CacheConf {...this.state.cacheProps} $$$parent={this} />}
        {this.state.stepNo === 4 && <AdminConf {...this.state.adminProps} $$$parent={this} />}
        {this.state.stepNo === 10 && (
          <div>
            <div className="rb-finish text-center">
              <div>
                <i className={`zmdi icon ${state[0]}`}></i>
              </div>
              <h2 className="mb-0">{state[1]}</h2>
              {this.state.installState === 11 && (
                <a className="btn btn-secondary mt-3" href="../user/login">
                  {$lang('LoginNow')}
                </a>
              )}
              {this.state.installState === 12 && (
                <a className="btn btn-secondary mt-3" href="install">
                  {$lang('Retry')}
                </a>
              )}
              {this.state.installState === 12 && this.state.installError && (
                <div className="alert alert-danger alert-icon alert-icon-border alert-sm mt-5 mb-0 text-left">
                  <div className="icon">
                    <span className="zmdi zmdi-close-circle-o"></span>
                  </div>
                  <div className="message">{this.state.installError}</div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    )
  }

  install = () => {
    const data = {
      installType: this.state.installType || 1,
      databaseProps: this.state.databaseProps || {},
      cacheProps: this.state.cacheProps || {},
      adminProps: this.state.adminProps || {},
    }
    this.setState({ installState: 10 })
    $.post('/setup/install-rebuild', JSON.stringify(data), (res) => {
      this.setState({ installState: res.error_code === 0 ? 11 : 12, installError: res.error_msg })
    })
  }
}

// ~
class RbWelcome extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="rb-welcome pb-1">
        <h3>{$lang('SelectSome,InstallMode')}</h3>
        <ul className="list-unstyled">
          <li>
            <a onClick={() => this._start(1)}>
              <h5 className="m-0 text-bold">{$lang('InstallMySql')}</h5>
              <p className="m-0 mt-1 text-muted">{$lang('InstallMySqlTips')}</p>
            </a>
          </li>
          <li>
            <a onClick={() => this._start(99)}>
              <h5 className="m-0 text-bold">{$lang('InstallH2')}</h5>
              <p className="m-0 mt-1 text-muted">{$lang('InstallH2Tips')}</p>
            </a>
          </li>
        </ul>
      </div>
    )
  }

  // 开始安装
  _start(type) {
    const that = this
    RbAlert.create(`<div class="text-left link">${$('.license').html()}<p class="text-bold">${$lang('CommercialTips')}</p></div>`, {
      html: true,
      type: 'warning',
      cancelText: $lang('Disagree'),
      confirmText: $lang('Agree'),
      confirm: function () {
        this.hide()
        that.props.$$$parent.setState({ installType: type, stepNo: type === 1 ? 2 : 4 })
      },
    })
  }
}

// ~
class DatabaseConf extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="rb-database">
        <h3>{$lang('SetSome,Database')}</h3>
        <form>
          <div className="form-group row pt-0">
            <div className="col-sm-3 col-form-label text-sm-right">{$lang('DbType')}</div>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" name="dbType">
                <option value="mysql">MySQL</option>
              </select>
              <div className="form-text">{$lang('DbMySqlTips')}</div>
            </div>
          </div>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$lang('Host')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="dbHost" value={this.state.dbHost || ''} onChange={this.handleValue} placeholder="127.0.0.1" />
            </div>
          </div>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$lang('Port')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="dbPort" value={this.state.dbPort || ''} onChange={this.handleValue} placeholder="3306" />
            </div>
          </div>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$lang('DbName')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="dbName" value={this.state.dbName || ''} onChange={this.handleValue} placeholder="rebuild10" />
              <div className="form-text">{$lang('DbNameTips')}</div>
            </div>
          </div>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$lang('DbUser')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="dbUser" value={this.state.dbUser || ''} onChange={this.handleValue} placeholder="rebuild" />
              <div className="form-text">{$lang('DbUserTips')}</div>
            </div>
          </div>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$lang('Passwd')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="dbPassword" value={this.state.dbPassword || ''} onChange={this.handleValue} placeholder="rebuild" />
            </div>
          </div>
        </form>
        <div className="progress">
          <div className="progress-bar" style={{ width: '25%' }}></div>
        </div>
        <div className="splash-footer">
          {this.state.testMessage && (
            <div className={`alert ${this.state.testState ? 'alert-success' : 'alert-danger'} alert-icon alert-icon-border text-left alert-sm`}>
              <div className="icon">
                <span className={`zmdi ${this.state.testState ? 'zmdi-check' : 'zmdi-close-circle-o'}`}></span>
              </div>
              <div className="message">{this.state.testMessage}</div>
            </div>
          )}
          <button className="btn btn-link float-left text-left pl-0" onClick={this._prev}>
            <i className="zmdi zmdi-chevron-left icon" />
            {$lang('SelectSome,InstallMode')}
          </button>
          <div className="float-right">
            <button className="btn btn-link text-right mr-2" disabled={this.state.inTest} onClick={this._testConnection}>
              {this.state.inTest && <i className="zmdi icon zmdi-refresh zmdi-hc-spin" />}
              {$lang('TestConnection')}
            </button>
            <button className="btn btn-secondary" onClick={this._next}>
              {$lang('NextStep')}
            </button>
          </div>
          <div className="clearfix"></div>
        </div>
      </div>
    )
  }

  handleValue = (e) => {
    const name = e.target.name
    const value = $(e.target).attr('type') === 'checkbox' ? $(e.target).prop('checked') : e.target.value
    this.setState({ [name]: value })
  }

  _buildProps(check) {
    const ps = {
      dbType: 'mysql',
      dbHost: this.state.dbHost || '127.0.0.1',
      dbPort: this.state.dbPort || 3306,
      dbName: this.state.dbName || 'rebuild10',
      dbUser: this.state.dbUser || 'rebuild',
      dbPassword: this.state.dbPassword || 'rebuild',
    }
    if (check && isNaN(ps.dbPort)) {
      RbHighbar.create($lang('SomeInvalid,Port'))
      return
    }
    return ps
  }

  _testConnection = (call) => {
    if (this.state.inTest) return
    const ps = this._buildProps(true)
    if (!ps) return

    this.setState({ inTest: true })
    $.post('/setup/test-connection', JSON.stringify(ps), (res) => {
      this.setState({ inTest: false, testState: res.error_code === 0, testMessage: res.data || res.error_msg }, () => typeof call === 'function' && call(ps, res))
    })
  }

  _prev = () => this.props.$$$parent.setState({ stepNo: 0, databaseProps: this._buildProps() })
  _next = () => {
    this._testConnection((ps, res) => {
      if (res.error_code === 0) this.props.$$$parent.setState({ stepNo: 3, databaseProps: ps })
    })
  }
}

// ~
class CacheConf extends DatabaseConf {
  state = { ...this.props }

  render() {
    return (
      <div className="rb-systems">
        <h3>{$lang('SetSome,CacheSrv')}</h3>
        <form>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$lang('CacheType')}</div>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" name="cacheType" onChange={this.handleValue} defaultValue={this.props.cacheType}>
                <option value="ehcache">EHCACHE ({$lang('BuiltIn')})</option>
                <option value="redis">REDIS</option>
              </select>
              {this.state.cacheType === 'redis' && <div className="form-text">{$lang('CacheRedisTips')}</div>}
            </div>
          </div>
          {this.state.cacheType === 'redis' && (
            <React.Fragment>
              <div className="form-group row">
                <div className="col-sm-3 col-form-label text-sm-right">{$lang('Host')}</div>
                <div className="col-sm-7">
                  <input type="text" className="form-control form-control-sm" name="CacheHost" value={this.state.CacheHost || ''} onChange={this.handleValue} placeholder="127.0.0.1" />
                </div>
              </div>
              <div className="form-group row">
                <div className="col-sm-3 col-form-label text-sm-right">{$lang('Port')}</div>
                <div className="col-sm-7">
                  <input type="text" className="form-control form-control-sm" name="CachePort" value={this.state.CachePort || ''} onChange={this.handleValue} placeholder="6379" />
                </div>
              </div>
              <div className="form-group row">
                <div className="col-sm-3 col-form-label text-sm-right">{$lang('Passwd')}</div>
                <div className="col-sm-7">
                  <input
                    type="text"
                    className="form-control form-control-sm"
                    name="CachePassword"
                    value={this.state.CachePassword || ''}
                    onChange={this.handleValue}
                    placeholder={$lang('CacheNoPasswdTips')}
                  />
                </div>
              </div>
            </React.Fragment>
          )}
        </form>
        <div className="progress">
          <div className="progress-bar" style={{ width: '50%' }}></div>
        </div>
        <div className="splash-footer">
          {this.state.testMessage && (
            <div className={`alert ${this.state.testState ? 'alert-success' : 'alert-danger'} alert-icon alert-icon-border text-left alert-sm`}>
              <div className="icon">
                <span className={`zmdi ${this.state.testState ? 'zmdi-check' : 'zmdi-close-circle-o'}`}></span>
              </div>
              <div className="message">{this.state.testMessage}</div>
            </div>
          )}
          <button className="btn btn-link float-left text-left pl-0" onClick={this._prev}>
            <i className="zmdi zmdi-chevron-left icon" />
            {$lang('SetSome,Database')}
          </button>
          <div className="float-right">
            {this.state.cacheType === 'redis' && (
              <button className="btn btn-link text-right mr-2" disabled={this.state.inTest} onClick={this._testConnection}>
                {this.state.inTest && <i className="zmdi icon zmdi-refresh zmdi-hc-spin" />}
                {$lang('TestConnection')}
              </button>
            )}
            <button className="btn btn-secondary" onClick={this._next}>
              {$lang('NextStep')}
            </button>
          </div>
          <div className="clearfix"></div>
        </div>
      </div>
    )
  }

  _buildProps(check) {
    if (this.state.cacheType !== 'redis') return {}
    const ps = {
      cacheType: 'redis',
      CacheHost: this.state.CacheHost || '127.0.0.1',
      CachePort: this.state.CachePort || 6379,
      CachePassword: this.state.CachePassword || '',
    }
    if (check && isNaN(ps.CachePort)) {
      RbHighbar.create($lang('SomeInvalid,Port'))
      return
    }
    return ps
  }

  _testConnection = (call) => {
    if (this.state.inTest) return
    const ps = this._buildProps(true)
    if (!ps) return

    this.setState({ inTest: true })
    $.post('/setup/test-cache', JSON.stringify(ps), (res) => {
      this.setState({ inTest: false, testState: res.error_code === 0, testMessage: res.data || res.error_msg }, () => typeof call === 'function' && call(ps, res))
    })
  }

  _prev = () => this.props.$$$parent.setState({ stepNo: 2, cacheProps: this._buildProps() })
  _next = () => {
    if (this.state.cacheType === 'redis') {
      this._testConnection((ps, res) => {
        if (res.error_code === 0) this.props.$$$parent.setState({ stepNo: 4, cacheProps: ps })
      })
    } else {
      this.props.$$$parent.setState({ stepNo: 4, cacheProps: {} })
    }
  }
}

// ~
class AdminConf extends DatabaseConf {
  state = { ...this.props }

  render() {
    return (
      <div className="rb-systems">
        <h3>{$lang('SetSome,SuperAdmin')}</h3>
        <form>
          <div className="form-group row pt-0">
            <div className="col-sm-3 col-form-label text-sm-right">{$lang('f.User.password')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="adminPasswd" value={this.state.adminPasswd || ''} onChange={this.handleValue} placeholder="admin" />
              <div className="form-text">
                {$lang('DefaultPasswd')} <code className="text-danger">admin</code>
              </div>
            </div>
          </div>
          <div className="form-group row">
            <div className="col-sm-3 col-form-label text-sm-right">{$lang('AdminEmail')}</div>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="adminMail" value={this.state.adminMail || ''} onChange={this.handleValue} placeholder="(选填)" />
              <div className="form-text">{$lang('AdminEmailTips')}</div>
            </div>
          </div>
        </form>
        <div className="progress">
          <div className="progress-bar" style={{ width: '75%' }}></div>
        </div>
        <div className="splash-footer">
          {this.props.$$$parent.state.installType === 1 && (
            <button className="btn btn-link float-left text-left pl-0" onClick={() => this._prev(3)}>
              <i className="zmdi zmdi-chevron-left icon" />
              {$lang('SetSome,CacheSrv')}
            </button>
          )}
          {this.props.$$$parent.state.installType === 99 && (
            <button className="btn btn-link float-left text-left pl-0" onClick={() => this._prev(0)}>
              <i className="zmdi zmdi-chevron-left icon" />
              {$lang('SelectSome,InstallMode')}
            </button>
          )}
          <div className="float-right">
            <button className="btn btn-primary" onClick={this._next}>
              {$lang('FinishInstall')}
            </button>
          </div>
          <div className="clearfix"></div>
        </div>
      </div>
    )
  }

  _buildProps(check) {
    const ps = {
      adminPasswd: this.state.adminPasswd,
      adminMail: this.state.adminMail,
    }
    if (check && ps.adminMail && !$regex.isMail(ps.adminMail)) {
      RbHighbar.create($lang('SomeInvalid,AdminEmail'))
      return
    }
    return ps
  }

  _prev = (stepNo) => this.props.$$$parent.setState({ stepNo: stepNo || 0, adminProps: this._buildProps() })
  _next = () => {
    const ps = this._buildProps(true)
    if (!ps) return
    this.props.$$$parent.setState({ stepNo: 10, adminProps: ps }, () => this.props.$$$parent.install())
  }
}

$(document).ready(() => renderRbcomp(<Setup />, $('.card-body')))
