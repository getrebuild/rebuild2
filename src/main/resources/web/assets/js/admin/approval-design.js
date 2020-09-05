/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

var wpc = window.__PageConfig
let fieldsCache
let activeNode
let donotCloseSidebar

$(document).ready(() => {
  if (!wpc || !wpc.configId) return
  // eslint-disable-next-line no-console
  if (rb.env === 'dev') console.log(wpc.flowDefinition)

  // 备用
  $.get(`/commons/metadata/fields?entity=${wpc.applyEntity}`, (res) => (fieldsCache = res.data))

  renderRbcomp(<RbFlowCanvas />, 'rbflow')

  $(document.body).click(function (e) {
    if (donotCloseSidebar) return
    const $target = $(e.target)
    if (e.target && ($target.hasClass('rb-right-sidebar') || $target.parents('div.rb-right-sidebar').length > 0)) return

    $(this).removeClass('open-right-sidebar')
    if (activeNode) {
      activeNode.setState({ active: false })
      activeNode = null
    }
  })

  $addResizeHandler(() => $('#rbflow').css('min-height', $(window).height() - 217))()
})

// 画布准备完毕
let isCanvasMounted = false
// 节点类型
const NTs = {
  start: ['start', $lang('NodeStart'), $lang('NodeUserOwns')],
  approver: ['approver', $lang('NodeApprover'), $lang('SelfSelectSome,NodeApprover')],
  cc: ['cc', $lang('NodeCc'), $lang('SelfSelectSome,NodeCc')],
}
const UTs = {
  ALL: $lang('NodeUserAll'),
  OWNS: $lang('NodeUserOwns'),
  SELF: $lang('NodeUserSelf'),
  SPEC: $lang('NodeUserSpec'),
}
// 添加节点按钮
const AddNodeButton = function (props) {
  let c = function () {
    showDlgAddNode(props.addNodeCall)
  }
  return (
    <div className="add-node-btn-box">
      <div className="add-node-btn">
        <button type="button" onClick={c}>
          <i className="zmdi zmdi-plus" />
        </button>
      </div>
    </div>
  )
}

// 节点规范
class NodeSpec extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  componentDidMount() {
    this.props.$$$parent.onRef(this)
  }

  addNodeQuick = (type) => {
    this.props.$$$parent.addNode(type, this.props.nodeId)
  }

  removeNodeQuick = () => {
    this.props.$$$parent.removeNode(this.props.nodeId)
    this.props.$$$parent.onRef(this, true)
  }

  openConfig = () => {
    if (wpc.preview) return
    let that = this
    let call = function (d) {
      that.setState({ data: d, active: false })
    }
    let props = { ...(this.state.data || {}), call: call, key: 'kns-' + this.props.nodeId }
    if (this.nodeType === 'start') renderRbcomp(<StartNodeConfig {...props} />, 'config-side')
    else if (this.nodeType === 'approver') renderRbcomp(<ApproverNodeConfig {...props} />, 'config-side')
    else if (this.nodeType === 'cc') renderRbcomp(<CCNodeConfig {...props} />, 'config-side')

    $(document.body).addClass('open-right-sidebar')
    this.setState({ active: true })
    activeNode = this
  }

  serialize() {
    // 检查节点是否有必填设置
    if (this.nodeType === 'approver' || this.nodeType === 'cc') {
      const users = this.state.data ? this.state.data.users : ['']
      if (users[0] === 'SPEC') {
        RbHighbar.create(NTs[this.nodeType][2])
        this.setState({ hasError: true })
        return false
      } else this.setState({ hasError: false })
    }
    return { type: this.props.type, nodeId: this.props.nodeId, data: this.state.data }
  }
}

// 节点组规范
class NodeGroupSpec extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, nodes: props.nodes || [] }
    this.__nodeRefs = {}
  }

  renderNodes() {
    const nodes = (this.state.nodes || []).map((item) => {
      const props = { ...item, key: 'kn-' + item.nodeId, $$$parent: this }
      if (item.type === 'condition') return <ConditionNode {...props} />
      else return <SimpleNode {...props} />
    })
    return nodes
  }

  onRef = (nodeRef, remove) => {
    const nodeId = nodeRef.props.nodeId
    this.__nodeRefs[nodeId] = remove ? null : nodeRef
  }

  addNode = (type, depsNodeId, call) => {
    const n = { type: type, nodeId: $random(type === 'condition' ? 'COND' : 'NODE') }
    const nodes = []
    if (depsNodeId) {
      if (depsNodeId === 'ROOT' || depsNodeId === 'COND') nodes.push(n)
      this.state.nodes.forEach((item) => {
        nodes.push(item)
        if (depsNodeId === item.nodeId) nodes.push(n)
      })
    } else {
      nodes.push(n)
      this.state.nodes.forEach((item) => {
        nodes.push(item)
      })
    }
    this.setState({ nodes: nodes }, () => {
      typeof call === 'function' && call(n.nodeId)
      hideDlgAddNode()
      if (isCanvasMounted && type !== 'condition') {
        setTimeout(() => {
          if (this.__nodeRefs[n.nodeId]) this.__nodeRefs[n.nodeId].openConfig()
        }, 200)
      }
    })
  }

  removeNode = (nodeId) => {
    const nodes = []
    this.state.nodes.forEach((item) => {
      if (nodeId !== item.nodeId) nodes.push(item)
    })
    this.setState({ nodes: nodes })
  }

  serialize() {
    const ns = []
    for (let i = 0; i < this.state.nodes.length; i++) {
      const nodeRef = this.__nodeRefs[this.state.nodes[i].nodeId]
      const s = nodeRef.serialize()
      if (!s) return false
      ns.push(s)
    }
    return { nodes: ns, nodeId: this.state.nodeId }
  }
}

// 画布:节点 1:N

// 一般节点
class SimpleNode extends NodeSpec {
  constructor(props) {
    super(props)
    this.nodeType = props.type || 'approver'
  }

  render() {
    const NT = NTs[this.nodeType]
    const data = this.state.data || {}

    const descs = data.users && data.users.length > 0 ? [UTs[data.users[0]] || `${$lang('SpecUser')}(${data.users.length})`] : [NT[2]]

    if (data.selfSelecting) descs.push($lang('AllowSelfSelect'))
    if (data.ccAutoShare) descs.push($lang('AutoShare'))
    if (this.nodeType === 'approver') descs.push($lang(data.signMode === 'AND' ? 'SignAnd' : data.signMode === 'ALL' ? 'SignAll' : 'SignOr'))

    return (
      <div className="node-wrap">
        <div className={`node-wrap-box animated fadeIn ${NT[0]}-node ${this.state.hasError ? 'error' : ''} ${this.state.active ? 'active' : ''}`} title={rb.env === 'dev' ? this.props.nodeId : null}>
          <div className="title">
            <span>{data.nodeName || NT[1]}</span>
            {this.props.nodeId !== 'ROOT' && <i className="zmdi zmdi-close aclose" title={$lang('Remove')} onClick={this.removeNodeQuick} />}
          </div>
          <div className="content" onClick={this.openConfig}>
            <div className="text">{descs.join(' / ')}</div>
            <i className="zmdi zmdi-chevron-right arrow"></i>
          </div>
        </div>
        <AddNodeButton addNodeCall={this.addNodeQuick} />
      </div>
    )
  }
}

// 条件节点
class ConditionNode extends NodeSpec {
  constructor(props) {
    super(props)
    this.state.branches = props.branches || [{ nodeId: $random('COND') }, { nodeId: $random('COND') }]
    this.__branchRefs = []
  }

  render() {
    const bLen = this.state.branches.length - 1
    return (
      bLen >= 0 && (
        <div className="branch-wrap">
          <div className="branch-box-wrap">
            <div className="branch-box">
              <button className="add-branch" onClick={this.addBranch}>
                {$lang('AddBranch')}
              </button>
              {this.state.branches.map((item, idx) => {
                return <ConditionBranch key={'kcb-' + item.nodeId} priority={idx + 1} isFirst={idx === 0} isLast={idx === bLen} $$$parent={this} {...item} />
              })}
            </div>
            <AddNodeButton addNodeCall={this.addNodeQuick} />
          </div>
        </div>
      )
    )
  }

  onRef = (branchRef, remove) => {
    const nodeId = branchRef.props.nodeId
    this.__branchRefs[nodeId] = remove ? null : branchRef
  }

  addBranch = () => {
    const bs = this.state.branches
    bs.push({ nodeId: $random('COND') })
    this.setState({ branches: bs })
  }

  removeBranch = (nodeId, e) => {
    if (e) {
      e.stopPropagation()
      e.nativeEvent.stopImmediatePropagation()
    }
    const bs = []
    this.state.branches.forEach((item) => {
      if (nodeId !== item.nodeId) bs.push(item)
    })
    this.setState({ branches: bs }, () => {
      if (bs.length === 0) {
        this.props.$$$parent.removeNode(this.props.nodeId)
      }
    })
  }

  serialize() {
    let holdANode = null
    const bs = []
    for (let i = 0; i < this.state.branches.length; i++) {
      let branchRef = this.__branchRefs[this.state.branches[i].nodeId]
      if (!holdANode) holdANode = branchRef
      let s = branchRef.serialize()
      if (!s) return false
      bs.push(s)
    }

    // 至少两个分支
    if (bs.length < 2) {
      RbHighbar.create($lang('Add2BranchLeast'))
      if (holdANode) holdANode.setState({ hasError: true })
      return false
    } else if (holdANode) holdANode.setState({ hasError: false })
    return { branches: bs, type: 'condition', nodeId: this.props.nodeId }
  }
}

// 条件节点序列
class ConditionBranch extends NodeGroupSpec {
  constructor(props) {
    super(props)
  }

  render() {
    const data = this.state.data || {}
    const filters = data.filter ? data.filter.items.length : 0

    return (
      <div className="col-box">
        {this.state.isFirst && <div className="top-left-cover-line"></div>}
        {this.state.isFirst && <div className="bottom-left-cover-line"></div>}
        <div className="condition-node">
          <div className="condition-node-box animated fadeIn" title={rb.env === 'dev' ? this.props.nodeId : null}>
            <div className={`auto-judge ${this.state.hasError ? 'error' : ''} ${this.state.active ? 'active' : ''}`} onClick={this.openConfig}>
              <div className="title-wrapper">
                <span className="editable-title float-left">{data.nodeName || $lang('BranchCond')}</span>
                <span className="priority-title float-right">{$lang('DefaultPiro')}</span>
                <i className="zmdi zmdi-close aclose" title={$lang('Remove')} onClick={(e) => this.props.$$$parent.removeBranch(this.props.nodeId, e)} />
                <div className="clearfix"></div>
              </div>
              <div className="content">
                <div className="text">{this.state.isLast ? $lang('OtherFilter') : filters > 0 ? `${$lang('AdvFiletrSeted')} (${filters})` : $lang('PlsSetFilter')}</div>
                <i className="zmdi zmdi-chevron-right arrow"></i>
              </div>
            </div>
            <AddNodeButton addNodeCall={this.addNode} />
          </div>
        </div>
        {this.renderNodes()}
        {this.state.isLast && <div className="top-right-cover-line"></div>}
        {this.state.isLast && <div className="bottom-right-cover-line"></div>}
      </div>
    )
  }

  componentDidMount() {
    this.props.$$$parent.onRef(this)
  }

  UNSAFE_componentWillReceiveProps(props) {
    this.setState({ ...props, nodes: this.state.nodes })
  }

  addNode(type, depsNodeId) {
    super.addNode(type, depsNodeId || 'COND')
  }

  removeNode(nodeId) {
    super.removeNode(nodeId)
    this.props.$$$parent.onRef(this, true)
  }

  openConfig = () => {
    if (wpc.preview) return
    const that = this
    const call = function (d) {
      that.setState({ data: d, active: false })
    }
    const props = { ...(this.state.data || {}), entity: wpc.applyEntity, call: call }
    renderRbcomp(<ConditionBranchConfig key={'kcbc-' + this.props.nodeId} {...props} isLast={this.state.isLast} />, 'config-side')

    $(document.body).addClass('open-right-sidebar')
    this.setState({ active: true })
    activeNode = this
  }

  serialize() {
    const s = super.serialize()
    if (!s || s.nodes.length === 0) {
      this.setState({ hasError: true })
      if (s !== false) RbHighbar.create($lang('PlsSetApproverOrCc'))
      return false
    } else this.setState({ hasError: false })

    s.priority = this.props.priority
    if (this.state.data) s.data = this.state.data
    return s
  }
}

// 添加节点
class DlgAddNode extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      call:
        props.call ||
        function (t) {
          alert(t)
        },
    }
  }

  render() {
    return (
      <div className="modal add-node" tabIndex="-1" ref={(c) => (this._dlg = c)}>
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <div className="row">
                <div className="col-4">
                  <a className="approver" onClick={() => this.state.call('approver')}>
                    <div>
                      <i className="zmdi zmdi-account"></i>
                    </div>
                    <p>{$lang('NodeApprover')}</p>
                  </a>
                </div>
                <div className="col-4">
                  <a className="cc" onClick={() => this.state.call('cc')}>
                    <div>
                      <i className="zmdi zmdi-mail-send"></i>
                    </div>
                    <p>{$lang('NodeCc')}</p>
                  </a>
                </div>
                <div className="col-4">
                  <a className="condition" onClick={() => this.state.call('condition')}>
                    <div>
                      <i className="zmdi zmdi-usb zmdi-hc-rotate-180"></i>
                    </div>
                    <p>{$lang('NodeBranch')}</p>
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    this.show()
  }

  hide() {
    $(this._dlg).modal('hide')
  }

  show(call) {
    $(this._dlg).modal({ show: true, keyboard: true })
    if (call) this.setState({ call: call })
  }
}

let __DlgAddNode
const showDlgAddNode = function (call) {
  if (__DlgAddNode) __DlgAddNode.show(call)
  else
    renderRbcomp(<DlgAddNode call={call} />, null, function () {
      __DlgAddNode = this
    })
}
const hideDlgAddNode = function () {
  if (__DlgAddNode) __DlgAddNode.hide()
}

// 发起人
class StartNodeConfig extends RbFormHandler {
  constructor(props) {
    super(props)
    this.state.users = (props.users || ['OWNS'])[0]
    if (!UTs[this.state.users]) this.state.users = 'SPEC'

    if (props.selfSelecting === false) this.state.selfSelecting = false
    else this.state.selfSelecting = true
  }

  render() {
    return (
      <div>
        <div className="header">
          <h5>{$lang('NodeStart')}</h5>
        </div>
        <div className="form">
          <div className="form-group mb-0">
            <label className="text-bold">{$lang('WhoCanStart')}</label>
            <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" type="radio" name="users" value="ALL" onChange={this.handleChange} checked={this.state.users === 'ALL'} />
              <span className="custom-control-label">{$lang('NodeUserAll')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" type="radio" name="users" value="OWNS" onChange={this.handleChange} checked={this.state.users === 'OWNS'} />
              <span className="custom-control-label">{$lang('NodeUserOwns')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" type="radio" name="users" value="SPEC" onChange={this.handleChange} checked={this.state.users === 'SPEC'} />
              <span className="custom-control-label">{$lang('NodeUserSpec')}</span>
            </label>
          </div>
          {this.state.users === 'SPEC' && (
            <div className="form-group">
              <UserSelector selected={this.state.selectedUsers} ref={(c) => (this._UserSelector = c)} />
            </div>
          )}
        </div>
        {this.renderButton()}
      </div>
    )
  }

  renderButton() {
    return (
      <div className="footer">
        <button type="button" className="btn btn-primary" onClick={this.save}>
          {$lang('Confirm')}
        </button>
        <button type="button" className="btn btn-secondary" onClick={this.cancel}>
          {$lang('Cancel')}
        </button>
      </div>
    )
  }

  componentDidMount() {
    if (this.state.users === 'SPEC' && this.props.users) {
      $.post('/commons/search/user-selector?entity=', JSON.stringify(this.props.users), (res) => {
        if (res.data.length > 0) this.setState({ selectedUsers: res.data })
      })
    }
  }

  save = () => {
    const d = {
      nodeName: this.state.nodeName,
      users: this.state.users === 'SPEC' ? this._UserSelector.getSelected() : [this.state.users],
    }
    if (this.state.users === 'SPEC' && d.users.length === 0) {
      RbHighbar.create($lang('PlsSelectSome,e.User'))
      return
    }
    typeof this.props.call && this.props.call(d)
    this.cancel()
  }

  cancel = () => {
    $(document.body).removeClass('open-right-sidebar')
  }
}

// 审批人
class ApproverNodeConfig extends StartNodeConfig {
  constructor(props) {
    super(props)
    this.state.signMode = props.signMode || 'OR'
    this.state.users = (props.users || ['SPEC'])[0]
    if (!this.state.users || this.state.users.length === 20) this.state.users = 'SPEC'
  }

  render() {
    return (
      <div>
        <div className="header">
          <input type="text" placeholder={$lang('NodeApprover')} data-id="nodeName" value={this.state.nodeName || ''} onChange={this.handleChange} maxLength="20" />
        </div>
        <div className="form rb-scroller">
          <div className="form-group mb-0">
            <label className="text-bold">{$lang('WhoCanApproval')}</label>
            <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" type="radio" name="users" value="SELF" onChange={this.handleChange} checked={this.state.users === 'SELF'} />
              <span className="custom-control-label">{$lang('StartApproverSelf')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" type="radio" name="users" value="SPEC" onChange={this.handleChange} checked={this.state.users === 'SPEC'} />
              <span className="custom-control-label">{$lang('StartApproverSpec')}</span>
            </label>
          </div>
          {this.state.users === 'SPEC' && (
            <div className="form-group mb-3">
              <UserSelector selected={this.state.selectedUsers} ref={(c) => (this._UserSelector = c)} />
            </div>
          )}
          <div className="form-group mb-0">
            <label className="custom-control custom-control-sm custom-checkbox">
              <input className="custom-control-input" type="checkbox" name="selfSelecting" checked={this.state.selfSelecting} onChange={this.handleChange} />
              <span className="custom-control-label">{$lang('AllowSelfSelectYet')}</span>
            </label>
          </div>
          <div className="form-group mt-4">
            <label className="text-bold">{$lang('WhenNUsersApproval')}</label>
            <label className="custom-control custom-control-sm custom-radio mb-2 hide">
              <input className="custom-control-input" type="radio" name="signMode" value="ALL" onChange={this.handleChange} checked={this.state.signMode === 'ALL'} />
              <span className="custom-control-label">{$lang('SignAll')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" type="radio" name="signMode" value="AND" onChange={this.handleChange} checked={this.state.signMode === 'AND'} />
              <span className="custom-control-label">{$lang('SignAndTips')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" type="radio" name="signMode" value="OR" onChange={this.handleChange} checked={this.state.signMode === 'OR'} />
              <span className="custom-control-label">{$lang('SignOrTips')}</span>
            </label>
          </div>
          <div className="form-group mt-4">
            <label className="text-bold">{$lang('UpdatableFields')}</label>
            <div style={{ position: 'relative' }}>
              <table className={`table table-sm fields-table ${(this.state.editableFields || []).length === 0 && 'hide'}`}>
                <tbody ref={(c) => (this._editableFields = c)}>
                  {(this.state.editableFields || []).map((item) => {
                    return (
                      <tr key={`field-${item.field}`}>
                        <td>{this.__fieldLabel(item.field)}</td>
                        <td width="100">
                          <label className="custom-control custom-control-sm custom-checkbox">
                            <input className="custom-control-input" type="checkbox" name="notNull" defaultChecked={item.notNull} data-field={item.field} />
                            <span className="custom-control-label">{$lang('Required')}</span>
                          </label>
                        </td>
                        <td width="40">
                          <a className="close" title={$lang('Remove')} onClick={() => this.removeEditableField(item.field)}>
                            &times;
                          </a>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
              <div className="pb-4">
                <button className="btn btn-secondary btn-sm" onClick={() => renderRbcomp(<DlgFields selected={this.state.editableFields} call={(fs) => this.setEditableFields(fs)} />)}>
                  + {$lang('SelectSome,Field')}
                </button>
              </div>
            </div>
          </div>
        </div>
        {this.renderButton()}
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount()

    const h = $('#config-side').height() - 120
    $('#config-side .form.rb-scroller').height(h).perfectScrollbar()

    $(this._editableFields)
      .sortable({
        cursor: 'move',
        axis: 'y',
      })
      .disableSelection()
  }

  save = () => {
    const editableFields = []
    $(this._editableFields)
      .find('input')
      .each(function () {
        let $this = $(this)
        editableFields.push({ field: $this.data('field'), notNull: $this.prop('checked') })
      })

    const d = {
      nodeName: this.state.nodeName,
      users: this.state.users === 'SPEC' ? this._UserSelector.getSelected() : [this.state.users],
      signMode: this.state.signMode,
      selfSelecting: this.state.selfSelecting,
      editableFields: editableFields,
    }
    if (d.users.length === 0 && !d.selfSelecting) {
      RbHighbar.create($lang('PlsSelectApprover'))
      return
    }

    typeof this.props.call && this.props.call(d)
    this.cancel()
  }

  setEditableFields(fs) {
    fs = fs.map((item) => {
      return { field: item, notNull: false }
    })
    this.setState({ editableFields: fs })
  }

  removeEditableField(field) {
    const fs = []
    this.state.editableFields.forEach((item) => {
      if (item.field !== field) fs.push(item)
    })
    this.setState({ editableFields: fs })
  }

  __fieldLabel(name) {
    const field = fieldsCache.find((x) => x.name === name)
    return field ? field.label : `[${name.toUpperCase()}]`
  }
}

// 抄送人
class CCNodeConfig extends StartNodeConfig {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <div>
        <div className="header">
          <input type="text" placeholder={$lang('NodeCc')} data-id="nodeName" value={this.state.nodeName || ''} onChange={this.handleChange} maxLength="20" />
        </div>
        <div className="form">
          <div className="form-group mb-3">
            <label className="text-bold">{$lang('ApprovalCcToWho')}</label>
            <UserSelector selected={this.state.selectedUsers} ref={(c) => (this._UserSelector = c)} />
          </div>
          <div className="form-group mb-0">
            <label className="custom-control custom-control-sm custom-checkbox mb-2">
              <input className="custom-control-input" type="checkbox" name="selfSelecting" checked={this.state.selfSelecting} onChange={(e) => this.handleChange(e)} />
              <span className="custom-control-label">{$lang('AllowSelfSelectYet')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-checkbox">
              <input className="custom-control-input" type="checkbox" name="ccAutoShare" checked={this.state.ccAutoShare} onChange={(e) => this.handleChange(e)} />
              <span className="custom-control-label">{$lang('ApprovalCcAutoShare')}</span>
            </label>
          </div>
        </div>
        {this.renderButton()}
      </div>
    )
  }

  save = () => {
    const d = {
      nodeName: this.state.nodeName,
      users: this._UserSelector.getSelected(),
      selfSelecting: this.state.selfSelecting,
      ccAutoShare: this.state.ccAutoShare,
    }
    if (d.users.length === 0 && !d.selfSelecting) {
      RbHighbar.create($lang('PlsSelectCc'))
      return
    }
    typeof this.props.call && this.props.call(d)
    this.cancel()
  }
}

// 条件
class ConditionBranchConfig extends StartNodeConfig {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <div>
        <div className="header">
          <input type="text" placeholder={$lang('BranchCond')} data-id="nodeName" value={this.state.nodeName || ''} onChange={this.handleChange} maxLength="20" />
        </div>
        {this.state.isLast && <div className="alert alert-warning">{$lang('BranchCondDefaultTips')}</div>}
        <AdvFilter filter={this.state.filter} entity={this.props.entity} confirm={this.save} cancel={this.cancel} canNoFilters={true} />
      </div>
    )
  }

  save = (filter) => {
    const d = { nodeName: this.state.nodeName, filter: filter }
    typeof this.props.call && this.props.call(d)
    this.cancel()
  }
}

// 画布
class RbFlowCanvas extends NodeGroupSpec {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <div>
        <div className="zoom">
          <a className="zoom-in" onClick={() => this.zoom(10)}>
            <i className="zmdi zmdi-plus" />
          </a>
          {this.state.zoomValue || 100}%
          <a className="zoom-out" onClick={() => this.zoom(-10)}>
            <i className="zmdi zmdi-minus" />
          </a>
        </div>
        <div className={'box-scale' + (wpc.preview ? ' preview' : '')} style={this.state.zoomStyle}>
          <SimpleNode type="start" $$$parent={this} nodeId="ROOT" ref={(c) => (this._root = c)} />
          {this.renderNodes()}
          <div className="end-node">
            <div className="end-node-circle"></div>
            <div className="end-node-text">{$lang('ApprovalEnd')}</div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    if (wpc.flowDefinition) {
      let flowNodes = wpc.flowDefinition.nodes
      this._root.setState({ data: flowNodes[0].data })
      flowNodes.remove(flowNodes[0])
      this.setState({ nodes: flowNodes }, () => {
        isCanvasMounted = true
      })
    } else {
      isCanvasMounted = true
    }

    $('.box-scale').draggable({ cursor: 'move', axis: 'x', scroll: false })
    $('#rbflow').removeClass('rb-loading-active')

    const $btns = $('.J_save').click(() => {
      const s = this.serialize()
      if (!s) return

      let data = {
        flowDefinition: s,
        metadata: { id: wpc.configId },
      }
      data = JSON.stringify(data)
      const noApproverNode = !data.includes('"approver"')

      $btns.button('loading')
      $.post('/app/entity/record-save', data, (res) => {
        if (res.error_code === 0) {
          RbAlert.create($lang('SaveAndPublishSuccess') + (noApproverNode ? $lang('NoAnyNodesTips') : ''), {
            cancelText: $lang('ReturnList'),
            cancel: () => location.replace('../approvals'),
            confirmText: $lang('ReturnEdit'),
            confirm: () => location.reload(),
          })
        } else {
          RbHighbar.error(res.error_msg)
        }
        $btns.button('reset')
      })
    })

    $('.J_copy').click(() => renderRbcomp(<DlgCopy father={wpc.configId} name={wpc.name + '(2)'} isDisabled={true} />))
  }

  zoom(v) {
    let zv = (this.state.zoomValue || 100) + v
    if (zv < 20) zv = 20
    else if (zv > 200) zv = 200
    this.setState({ zoomValue: zv, zoomStyle: { transform: `scale(${zv / 100})` } })
  }

  serialize() {
    const ns = super.serialize()
    if (!ns) return false
    ns.nodes.insert(0, this._root.serialize())
    return ns
  }
}

// ~ 流程中可编辑字段
class DlgFields extends RbModalHandler {
  constructor(props) {
    super(props)
    donotCloseSidebar = true
    this._selected = (props.selected || []).map((item) => {
      return item.field
    })
  }

  render() {
    return (
      <RbModal title={$lang('SelectSome,UpdatableFields')} ref={(c) => (this._dlg = c)} disposeOnHide={true} onHide={() => (donotCloseSidebar = false)}>
        <div className="row p-1" ref={(c) => (this._fields = c)}>
          {fieldsCache.map((item) => {
            if (item.type === 'BARCODE') return null
            return (
              <div className="col-3" key={`field-${item.name}`}>
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-1">
                  <input className="custom-control-input" type="checkbox" disabled={!item.updatable} value={item.name} defaultChecked={item.updatable && this._selected.includes(item.name)} />
                  <span className="custom-control-label">{item.label}</span>
                </label>
              </div>
            )
          })}
        </div>
        <div className="dialog-footer">
          <button className="btn btn-primary" type="button" onClick={this.confirm}>
            {$lang('Confirm')}
          </button>
          <button className="btn btn-secondary" type="button" onClick={this.hide}>
            {$lang('Cancel')}
          </button>
        </div>
      </RbModal>
    )
  }

  confirm = () => {
    const selected = []
    $(this._fields)
      .find('input:checked')
      .each(function () {
        selected.push(this.value)
      })

    typeof this.props.call === 'function' && this.props.call(selected)
    this.hide()
  }
}

// ~~ 另存为
class DlgCopy extends ConfigFormDlg {
  constructor(props) {
    super(props)
    this.title = $lang('SaveAs')
  }

  renderFrom() {
    return (
      <React.Fragment>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$lang('NewName')}</label>
          <div className="col-sm-7">
            <input type="text" className="form-control form-control-sm" data-id="name" onChange={this.handleChange} value={this.state.name || ''} />
          </div>
        </div>
        <div className="form-group row">
          <div className="col-sm-7 offset-sm-3">
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
              <input className="custom-control-input" type="checkbox" checked={this.state.isDisabled === true} data-id="isDisabled" onChange={this.handleChange} />
              <span className="custom-control-label">{$lang('NewAfterDisabled')}</span>
            </label>
          </div>
        </div>
      </React.Fragment>
    )
  }

  confirm = () => {
    const approvalName = this.state.name
    if (!approvalName) {
      RbHighbar.create($lang('PlsInputSome,NewName'))
      return
    }

    this.disabled(true)
    $.post(`/admin/robot/approval/copy?father=${this.props.father}&disabled=${this.state.isDisabled}&name=${$encode(approvalName)}`, (res) => {
      if (res.error_code === 0) {
        RbHighbar.success($lang('SomeSuccess,SaveAs'))
        setTimeout(() => location.replace('./' + res.data.approvalId), 500)
      } else {
        RbHighbar.error(res.error_msg)
      }
      this.disabled()
    })
  }
}
