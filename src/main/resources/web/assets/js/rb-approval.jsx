/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */

// 审批流程
class ApprovalProcessor extends React.Component {

    constructor(props) {
        super(props)
        this.state = {...props}
    }

    render() {
        return <div className="approval-pane">
            {this.state.state === 1 && this.renderStateDraft()}
            {this.state.state === 2 && this.renderStateProcessing()}
            {this.state.state === 10 && this.renderStateApproved()}
            {this.state.state === 11 && this.renderStateRejected()}
            {this.state.state === 12 && this.renderStateCanceled()}
            {this.state.state === 13 && this.renderStateRevoked()}
        </div>
    }

    renderStateDraft() {
        return <div className="alert alert-warning shadow-sm">
            <button className="close btn btn-secondary" onClick={this.submit}>提交</button>
            <div className="icon"><span className="zmdi zmdi-info-outline"></span></div>
            <div className="message">当前记录尚未提交审批，请在信息完善后尽快提交</div>
        </div>
    }

    renderStateProcessing() {
        window.RbViewPage && window.RbViewPage.setReadonly(true)
        let aMsg = '当前记录正在审批中'
        if (this.state.imApprover) {
            if (this.state.imApproveSatate === 1) aMsg = '当前记录正在等待你审批'
            else if (this.state.imApproveSatate === 10) aMsg = '你已审批同意，正在等待他人审批'
            else if (this.state.imApproveSatate === 11) aMsg = '你已驳回审批'
        }

        return <div className="alert alert-warning shadow-sm">
            <button className="close btn btn-secondary" onClick={this.viewSteps}>详情</button>
            {this.state.canCancel && <button className="close btn btn-secondary" onClick={this.cancel}>撤回</button>}
            {(this.state.imApprover && this.state.imApproveSatate === 1) &&
            <button className="close btn btn-secondary" onClick={this.approve}>审批</button>}
            <div className="icon"><span className="zmdi zmdi-hourglass-alt"></span></div>
            <div className="message">{aMsg}</div>
        </div>
    }

    renderStateApproved() {
        window.RbViewPage && window.RbViewPage.setReadonly(true)

        return <div className="alert alert-success shadow-sm">
            <button className="close btn btn-secondary" onClick={this.viewSteps}>详情</button>
            {rb.isAdminUser && <button className="close btn btn-secondary" onClick={this.revoke}>撤销</button>}
            <div className="icon"><span className="zmdi zmdi-check"></span></div>
            <div className="message">当前记录已审批完成</div>
        </div>
    }

    renderStateRejected() {
        return <div className="alert alert-danger shadow-sm">
            <button className="close btn btn-secondary" onClick={this.viewSteps}>详情</button>
            <button className="close btn btn-secondary" onClick={this.submit}>再次提交</button>
            <div className="icon"><span className="zmdi zmdi-close-circle-o"></span></div>
            <div className="message">审批被驳回，可在信息完善后再次提交</div>
        </div>
    }

    renderStateCanceled() {
        return <div className="alert alert-warning shadow-sm">
            <button className="close btn btn-secondary" onClick={this.viewSteps}>详情</button>
            <button className="close btn btn-secondary" onClick={this.submit}>再次提交</button>
            <div className="icon"><span className="zmdi zmdi-rotate-left"></span></div>
            <div className="message">审批已撤回，请在信息完善后再次提交</div>
        </div>
    }

    renderStateRevoked() {
        return <div className="alert alert-warning shadow-sm">
            <button className="close btn btn-secondary" onClick={this.viewSteps}>详情</button>
            <button className="close btn btn-secondary" onClick={this.submit}>再次提交</button>
            <div className="icon"><span className="zmdi zmdi-rotate-left"></span></div>
            <div className="message">审批已撤销，请在信息完善后再次提交</div>
        </div>
    }

    componentDidMount() {
        $.get(`/app/entity/approval/state?record=${this.props.id}`, (res) => this.setState(res.data))
    }

    submit = () => {
        const that = this
        if (this._SubmitForm) this._SubmitForm.show(null, () => that._SubmitForm.reload())
        else renderRbcomp(<ApprovalSubmitForm id={this.props.id}/>, null, function () {
            that._SubmitForm = this
        })
    }

    approve = () => {
        const that = this
        if (this._ApproveForm) this._ApproveForm.show()
        else renderRbcomp(<ApprovalApproveForm id={this.props.id} approval={this.state.approvalId}
                                               entity={this.props.entity}/>, null, function () {
            that._ApproveForm = this
        })
    }

    cancel = () => {
        const that = this
        RbAlert.create('确认撤回当前审批？', {
            confirm: function () {
                this.disabled(true)
                $.post(`/app/entity/approval/cancel?record=${that.props.id}`, (res) => {
                    if (res.error_code > 0) RbHighbar.error(res.error_msg)
                    else _reload(this, '审批已撤回')
                    this.disabled()
                })
            }
        })
    }

    revoke = () => {
        const that = this
        RbAlert.create('将要撤销已通过审批，每条记录仅有 3 次撤销机会。确认吗？', {
            type: 'warning',
            confirm: function () {
                this.disabled(true)
                $.post(`/app/entity/approval/revoke?record=${that.props.id}`, (res) => {
                    if (res.error_code > 0) RbHighbar.error(res.error_msg)
                    else _reload(this, '审批已撤销')
                    this.disabled()
                })
            }
        })
    }

    viewSteps = () => {
        const that = this
        if (this._stepViewer) this._stepViewer.show()
        else renderRbcomp(<ApprovalStepViewer id={this.props.id} approval={this.state.approvalId}/>, null, function () {
            that._stepViewer = this
        })
    }
}

// 审批人/抄送人选择
class ApprovalUsersForm extends RbFormHandler {

    constructor(props) {
        super(props)
    }

    renderUsers() {
        const approverHas = (this.state.nextApprovers || []).length > 0 || this.state.approverSelfSelecting
        const ccHas = (this.state.nextCcs || []).length > 0 || this.state.ccSelfSelecting

        return <div>
            {approverHas && <div className="form-group">
                <label><i
                    className="zmdi zmdi-account zicon"/> {`${this._approverLabel || '审批人'} (${this.state.signMode === 'AND' ? '会签' : '或签'})`}
                </label>
                <div>
                    {(this.state.nextApprovers || []).map((item) => {
                        return <UserShow key={'AU' + item[0]} id={item[0]} name={item[1]} showName={true}/>
                    })}
                </div>
                {this.state.approverSelfSelecting && <div>
                    <UserSelector ref={(c) => this._approverSelector = c}/>
                </div>}
            </div>}
            {ccHas && <div className="form-group">
                <label><i className="zmdi zmdi-mail-send zicon"/> 本次审批结果将抄送给</label>
                <div>
                    {(this.state.nextCcs || []).map((item) => {
                        return <UserShow key={'CU' + item[0]} id={item[0]} name={item[1]} showName={true}/>
                    })}
                </div>
                {this.state.ccSelfSelecting && <div>
                    <UserSelector ref={(c) => this._ccSelector = c}/>
                </div>}
            </div>}
        </div>
    }

    getSelectUsers() {
        const selectUsers = {
            selectApprovers: this.state.approverSelfSelecting ? this._approverSelector.getSelected() : [],
            selectCcs: this.state.ccSelfSelecting ? this._ccSelector.getSelected() : []
        }

        if (!this.state.isLastStep) {
            if ((this.state.nextApprovers || []).length === 0 && selectUsers.selectApprovers.length === 0) {
                RbHighbar.create('请选择审批人')
                return false
            }
        }
        return selectUsers
    }

    getNextStep(approval) {
        $.get(`/app/entity/approval/fetch-nextstep?record=${this.props.id}&approval=${approval || this.props.approval}`, (res) => {
            this.setState(res.data)
        })
    }
}

// 审核提交
class ApprovalSubmitForm extends ApprovalUsersForm {

    constructor(props) {
        super(props)
        this.state.approvals = []
    }

    render() {
        return <RbModal ref={(c) => this._dlg = c} title="提交审批" width="600"
                        disposeOnHide={this.props.disposeOnHide === true}>
            <div className="form approval-form">
                <div className="form-group">
                    <label>选择审批流程</label>
                    <div className="approval-list">
                        {!this.state.approvals && <p className="text-muted">无适用流程 {rb.isAdminUser &&
                        <a className="icon-link ml-1" target="_blank" href={`${rb.baseUrl}/admin/robot/approvals`}><i
                            className="zmdi zmdi-settings"></i> 点击配置</a>}</p>}
                        {(this.state.approvals || []).map((item) => {
                            return (<div key={'A' + item.id}>
                                <label className="custom-control custom-control-sm custom-radio mb-0">
                                    <input className="custom-control-input" type="radio" name="useApproval"
                                           value={item.id} onChange={this.handleChange}
                                           checked={this.state.useApproval === item.id}/>
                                    <span className="custom-control-label">{item.name}</span>
                                </label>
                                <a href={`${rb.baseUrl}/app/RobotApprovalConfig/view/${item.id}`} target="_blank"><i
                                    className="zmdi zmdi-usb zmdi-hc-rotate-180"></i> 流程图</a>
                            </div>)
                        })}
                    </div>
                </div>
                {this.renderUsers()}
                <div className="dialog-footer" ref={(c) => this._btns = c}>
                    <button type="button" className="btn btn-primary btn-space" onClick={() => this.post()}>提交</button>
                    <button type="button" className="btn btn-secondary btn-space" onClick={this.hide}>取消</button>
                </div>
            </div>
        </RbModal>
    }

    componentDidMount = () => this.reload()

    reload() {
        $.get(`/app/entity/approval/workable?record=${this.props.id}`, (res) => {
            if (res.data && res.data.length > 0) {
                this.setState({approvals: res.data, useApproval: res.data[0].id}, () => {
                    this.getNextStep(res.data[0].id)
                })
            } else {
                this.setState({approvals: null, useApproval: null})
            }
        })
    }

    handleChangeAfter(id, val) {
        if (id === 'useApproval') this.getNextStep(val)
    }

    post() {
        if (!this.state.useApproval) return RbHighbar.create('请选择审批流程')
        const selectUsers = this.getSelectUsers()
        if (!selectUsers) return

        this.disabled(true)
        $.post(`/app/entity/approval/submit?record=${this.props.id}&approval=${this.state.useApproval}`, JSON.stringify(selectUsers), (res) => {
            if (res.error_code > 0) RbHighbar.error(res.error_msg)
            else _reload(this, '审批已提交')
            this.disabled()
        })
    }
}

// 审批
class ApprovalApproveForm extends ApprovalUsersForm {

    constructor(props) {
        super(props)
        this._approverLabel = '下一审批人'
    }

    render() {
        return <RbModal ref={(c) => this._dlg = c} title="审批" width="600">
            <div className="form approval-form">
                {this.state.bizMessage &&
                <div className="form-group">
                    <RbAlertBox message={this.state.bizMessage} onClose={() => this.setState({bizMessage: null})}/>
                </div>
                }
                {this.state.aform && this._renderEditableForm()}
                <div className="form-group">
                    <label>批注</label>
                    <textarea className="form-control form-control-sm row2x" name="remark" placeholder="输入批注 (可选)"
                              value={this.state.remark || ''} onChange={this.handleChange} maxLength="600"/>
                </div>
                {this.renderUsers()}
                <div className="dialog-footer" ref={(c) => this._btns = c}>
                    <button type="button" className="btn btn-primary btn-space" onClick={() => this.post(10)}>同意
                    </button>
                    <button type="button" className="btn btn-danger bordered btn-space"
                            onClick={() => this.post(11)}>驳回
                    </button>
                </div>
            </div>
        </RbModal>
    }

    _renderEditableForm() {
        const fake = {
            state: {id: this.props.id, __formModel: {}}
        }
        return <div className="form-group">
            <label>信息完善 (驳回时无需填写)</label>
            <EditableForm $$$parent={fake} entity={this.props.entity} ref={(c) => this._rbform = c}>
                {this.state.aform.map((item) => {
                    // eslint-disable-next-line no-undef
                    return detectElement(item)
                })}
            </EditableForm>
        </div>
    }

    componentDidMount = () => this.getNextStep()

    post(state) {
        const aformData = {}
        if (this.state.aform && state === 10) {
            const fd = this._rbform.__FormData
            for (let k in fd) {
                let err = fd[k].error
                if (err) {
                    RbHighbar.create(err);
                    return
                } else aformData[k] = fd[k].value
            }
            aformData.metadata = {id: this.props.id}
        }

        let selectUsers
        if (state === 10) {
            selectUsers = this.getSelectUsers()
            if (!selectUsers) return
        }

        const data = {
            remark: this.state.remark || '',
            selectUsers: selectUsers,
            aformData: aformData,
            useGroup: this.state.useGroup
        }

        this.disabled(true)
        $.post(`/app/entity/approval/approve?record=${this.props.id}&state=${state}`, JSON.stringify(data), (res) => {
            if (res.error_code === 499) {
                this.setState({bizMessage: res.error_msg})
                this.getNextStep()

            } else if (res.error_code > 0) {
                RbHighbar.error(res.error_msg)
            } else {
                _reload(this, '审批已' + (state === 10 ? '同意' : '驳回'))
                typeof this.props.call === 'function' && this.props.call()
            }
            this.disabled()
        })
    }
}

// @see rb-forms.jsx
// eslint-disable-next-line no-undef
class EditableForm extends RbForm {
    constructor(props) {
        super(props)
    }

    renderFormAction() {
        return null
    }
}

const STATE_NAMES = {10: '审批同意', 11: '驳回审批', 12: '撤回审批', 13: '撤销审批 (管理员)'}

// 已审批步骤查看
class ApprovalStepViewer extends React.Component {

    constructor(props) {
        super(props)
        this.state = {...props}
    }

    render() {
        const stateLast = this.state.steps ? this.state.steps[0].approvalState : 0
        return <div className="modal" ref={(c) => this._dlg = c} tabIndex="-1">
            <div className="modal-dialog modal-dialog-centered">
                <div className="modal-content">
                    <div className="modal-header pb-0">
                        <button className="close" type="button" onClick={this.hide}><span className="zmdi zmdi-close"/>
                        </button>
                    </div>
                    <div className="modal-body approved-steps-body">
                        {!this.state.steps && <RbSpinner fully={true}/>}
                        <ul className="timeline approved-steps">
                            {(this.state.steps || []).map((item, idx) => {
                                return idx === 0 ? this.renderSubmitter(item, idx) : this.renderApprovers(item, idx, stateLast)
                            })}
                            {stateLast >= 10 &&
                            <li className="timeline-item last"><span>{stateLast === 13 ? '重审' : '结束'}</span></li>}
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    }

    renderSubmitter(s, idx) {
        return <li className="timeline-item state0" key={`step-${idx}`}>
            {this.__formatTime(s.createdOn)}
            <div className="timeline-content">
                <div className="timeline-avatar"><img src={`${rb.baseUrl}/account/user-avatar/${s.submitter}`}/></div>
                <div className="timeline-header">
                    <p className="timeline-activity">由 {s.submitter === rb.currentUser ? '你' : s.submitterName} 提交审批</p>
                    {s.approvalName && <blockquote className="blockquote timeline-blockquote mb-0">
                        <p><a target="_blank" href={`${rb.baseUrl}/app/RobotApprovalConfig/view/${s.approvalId}`}><i
                            className="zmdi zmdi-usb zmdi-hc-rotate-180"></i> {s.approvalName}</a></p>
                    </blockquote>}
                </div>
            </div>
        </li>
    }

    renderApprovers(s, idx, lastState) {
        const kp = 'step-' + idx + '-'
        const sss = []
        let nodeState = 0
        if (s[0].signMode === 'OR') {
            s.forEach(item => {
                if (item.state >= 10) nodeState = item.state
            })
        }

        s.forEach(item => {
            const approverName = item.approver === rb.currentUser ? '你' : item.approverName
            let aMsg = `等待 ${approverName} 审批`
            if (item.state >= 10) aMsg = `由 ${approverName} ${STATE_NAMES[item.state]}`
            if ((nodeState >= 10 || lastState >= 10) && item.state < 10) aMsg = `${approverName} 未进行审批`

            sss.push(<li className={'timeline-item state' + item.state} key={kp + sss.length}>
                {this.__formatTime(item.approvedTime || item.createdOn)}
                <div className="timeline-content">
                    <div className="timeline-avatar"><img src={`${rb.baseUrl}/account/user-avatar/${item.approver}`}/>
                    </div>
                    <div className="timeline-header">
                        <p className="timeline-activity">{aMsg}</p>
                        {item.remark && <blockquote className="blockquote timeline-blockquote mb-0"><p
                            className="text-wrap">{item.remark}</p></blockquote>}
                    </div>
                </div>
            </li>)
        })
        if (sss.length < 2) return sss

        let clazz = 'joint0'
        if (s[0].signMode === 'OR') clazz = 'joint or'
        else if (s[0].signMode === 'AND') clazz = 'joint'
        return <div key={kp} className={clazz}>{sss}</div>
    }

    __formatTime(time) {
        time = time.split(' ')
        return <div className="timeline-date">{time[1]}<span>{time[0]}</span></div>
    }

    componentDidMount() {
        this.show()
        $.get(`/app/entity/approval/fetch-workedsteps?record=${this.props.id}`, (res) => {
            if (!res.data || res.data.length === 0) {
                RbHighbar.create('未查询到流程详情')
                this.hide()
                this.__noStepFound = true
            } else this.setState({steps: res.data})
        })
    }

    hide = () => $(this._dlg).modal('hide')
    show = () => {
        if (this.__noStepFound === true) {
            RbHighbar.create('未查询到流程详情')
            this.hide()
        } else $(this._dlg).modal({show: true, keyboard: true})
    }
}

// 刷新页面
const _reload = function (dlg, msg) {
    dlg && dlg.hide()
    msg && RbHighbar.success(msg)

    setTimeout(() => {
        if (window.RbViewPage) window.RbViewPage.reload()
        if (window.RbListPage) window.RbListPage.reload()
        else if (parent.RbListPage) parent.RbListPage.reload()
    }, 1000)
}