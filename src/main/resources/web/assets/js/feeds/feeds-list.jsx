/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global converEmoji, FeedsEditor */

const FeedsSorts = {newer: '最近发布', older: '最早发布', modified: '最近修改'}
const FeedsTypes = {1: '动态', 2: '跟进', 3: '公告', 4: '日程'}

// ~ 动态列表
// eslint-disable-next-line no-unused-vars
class FeedsList extends React.Component {

    constructor(props) {
        super(props)
        this.state = {...props, tabType: 0, pageNo: 1, sort: $storage.get('Feeds-sort')}
        this.__lastFilter = {entity: 'Feeds', items: []}
    }

    render() {
        return <div>
            <div className="types-bar">
                <ul className="nav nav-tabs">
                    <li className="nav-item"><a onClick={() => this._switchTab(0)}
                                                className={`nav-link ${this.state.tabType === 0 && 'active'}`}>全部</a>
                    </li>
                    <li className="nav-item"><a onClick={() => this._switchTab(1)}
                                                className={`nav-link ${this.state.tabType === 1 && 'active'}`}>@我的</a>
                    </li>
                    <li className="nav-item"><a onClick={() => this._switchTab(10)}
                                                className={`nav-link ${this.state.tabType === 10 && 'active'}`}>我发布的</a>
                    </li>
                    <li className="nav-item"><a onClick={() => this._switchTab(2)}
                                                className={`nav-link ${this.state.tabType === 2 && 'active'}`}>我评论的</a>
                    </li>
                    <li className="nav-item"><a onClick={() => this._switchTab(3)}
                                                className={`nav-link ${this.state.tabType === 3 && 'active'}`}>我点赞的</a>
                    </li>
                    <li className="nav-item"><a onClick={() => this._switchTab(11)}
                                                className={`nav-link ${this.state.tabType === 11 && 'active'}`}>私密</a>
                    </li>
                    <span className="float-right">
            <div className="btn-group">
              <button type="button" className="btn btn-link pr-0 text-right"
                      data-toggle="dropdown">{FeedsSorts[this.state.sort] || '默认排序'} <i
                  className="icon zmdi zmdi-chevron-down up-1"></i></button>
              <div className="dropdown-menu dropdown-menu-right">
                <a className="dropdown-item" data-sort="newer" onClick={this._sortFeeds}>最近发布</a>
                <a className="dropdown-item" data-sort="modified" onClick={this._sortFeeds}>最近修改</a>
                <a className="dropdown-item" data-sort="older" onClick={this._sortFeeds}>最早发布</a>
              </div>
            </div>
          </span>
                </ul>
            </div>
            <div className="feeds-list">
                {(this.state.data && this.state.data.length === 0) && <div className="list-nodata pt-8 pb-8">
                    <i className="zmdi zmdi-chart-donut"></i>
                    <p>暂无相关动态{this.state.tabType === 11 && <React.Fragment><br/>即使管理员也不能查看他人的私密动态</React.Fragment>}</p>
                </div>
                }
                {(this.state.data || []).map((item) => {
                    return this.renderItem(item)
                })}
            </div>
            <Pagination ref={(c) => this._pagination = c} call={this.gotoPage} pageSize={40}/>
        </div>
    }

    renderItem(item) {
        if (item.deleted) return null
        const id = `feeds-${item.id}`
        return (
            <div key={id} id={id} className={`${item.id === this.state.focusFeed ? 'focus' : ''}`}>
                <div className="feeds">
                    <div className="user">
                        <a className="user-show">
                            <div className="avatar"><img alt="Avatar"
                                                         src={`${rb.baseUrl}/account/user-avatar/${item.createdBy[0]}`}/>
                            </div>
                        </a>
                    </div>
                    <div className="content">
                        <div className="meta">
                            <span className="float-right badge">{FeedsTypes[item.type] || '动态'}</span>
                            <a>{item.createdBy[1]}</a>
                            <p className="text-muted fs-12 m-0">
                                <DateShow date={item.createdOn}/>
                                {item.createdOn !== item.modifedOn &&
                                <span className="text-danger" title={`编辑于 ${item.modifedOn}`}> (已编辑)</span>}
                                &nbsp;&nbsp;·&nbsp;&nbsp;
                                {typeof item.scope === 'string' ? item.scope : <span>{item.scope[1]} <i title="团队成员可见"
                                                                                                        className="zmdi zmdi-accounts fs-14 down-1"></i></span>}
                            </p>
                        </div>
                        {__renderRichContent(item)}
                    </div>
                </div>
                <div className="actions">
                    <ul className="list-unstyled m-0">
                        {item.self && <li className="list-inline-item mr-2">
                            <a data-toggle="dropdown" href="#mores" className="fixed-icon" title="更多"><i
                                className="zmdi zmdi-more"></i>&nbsp;</a>
                            <div className="dropdown-menu dropdown-menu-right">
                                {this._renderMoreMenu(item)}
                                <a className="dropdown-item" onClick={() => this._handleEdit(item)}><i
                                    className="icon zmdi zmdi-edit"/> 编辑</a>
                                <a className="dropdown-item" onClick={() => this._handleDelete(item.id)}><i
                                    className="icon zmdi zmdi-delete"/>删除</a>
                            </div>
                        </li>
                        }
                        <li className="list-inline-item mr-3">
                            <a href="#thumbup" onClick={() => this._handleLike(item.id)}
                               className={`fixed-icon ${item.myLike && 'text-primary'}`}>
                                <i className="zmdi zmdi-thumb-up"></i>赞 {item.numLike > 0 &&
                            <span>({item.numLike})</span>}
                            </a>
                        </li>
                        <li className="list-inline-item">
                            <a href="#comments" onClick={() => this._toggleComment(item.id)}
                               className={`fixed-icon ${item.shownComments && 'text-primary'}`}>
                                <i className="zmdi zmdi-comment-outline"></i>评论 {item.numComments > 0 &&
                            <span>({item.numComments})</span>}
                            </a>
                        </li>
                    </ul>
                </div>
                <span className={`${!item.shownComments && 'hide'}`}>{item.shownCommentsReal &&
                <FeedsComments feeds={item.id}/>}</span>
            </div>
        )
    }

    componentDidMount = () => this.props.fetchNow && this.fetchFeeds()

    /**
     * 加载数据
     * @param {*} filter AdvFilter
     */
    fetchFeeds(filter) {
        this.__lastFilter = filter = filter || this.__lastFilter
        const s = this.state
        const firstFetch = !s.data
        // s.focusFeed 首次加载有效

        $.post(`/feeds/feeds-list?pageNo=${s.pageNo}&sort=${s.sort || ''}&type=${s.tabType}&foucs=${firstFetch ? s.focusFeed : ''}`, JSON.stringify(filter), (res) => {
            const _data = res.data || {data: [], total: 0}
            this.state.pageNo === 1 && this._pagination.setState({rowsTotal: _data.total, pageNo: 1})
            this.setState({data: _data.data, focusFeed: firstFetch ? s.focusFeed : null})
        })
    }

    _switchTab(t) {
        this.setState({tabType: t, pageNo: 1}, () => this.fetchFeeds())
    }

    _sortFeeds = (e) => {
        const s = e.target.dataset.sort
        $storage.set('Feeds-sort', s)
        this.setState({sort: s, pageNo: 1}, () => this.fetchFeeds())
    }

    _toggleComment(feeds) {
        event.preventDefault()
        const _data = this.state.data
        _data.forEach((item) => {
            if (feeds === item.id) {
                item.shownComments = !item.shownComments
                item.shownCommentsReal = true
            }
        })
        this.setState({data: _data})
    }

    // eslint-disable-next-line react/jsx-no-undef
    _handleEdit = (item) => renderRbcomp(<FeedsEditDlg {...item} call={() => this.fetchFeeds()}/>)
    _handleLike = (id) => _handleLike(id, this)

    _handleDelete(id) {
        event.preventDefault()
        const that = this
        RbAlert.create('确认删除该动态？', {
            type: 'danger',
            confirmText: '删除',
            confirm: function () {
                this.disabled(true)
                $.post(`/feeds/post/delete?id=${id}`, () => {
                    this.hide()
                    $(`#feeds-${id}`).animate({opacity: 0}, 600, () => {
                        const _data = that.state.data
                        _data.forEach((item) => {
                            if (id === item.id) item.deleted = true
                        })
                        that.setState({data: _data})
                    })
                })
            }
        })
    }

    // 渲染菜单
    _renderMoreMenu(item) {
        if (item.type === 4 && item.contentMore && !item.contentMore.finishTime) {
            return <React.Fragment>
                <a className="dropdown-item" onClick={() => this._handleFinish(item.id)}><i
                    className="icon zmdi zmdi-check"/> 完成</a>
                <div className="dropdown-divider"></div>
            </React.Fragment>
        }
        return null
    }

    _handleFinish(id) {
        const that = this
        RbAlert.create('确认完成该日程？', {
            confirm: function () {
                this.disabled(true)
                $.post(`/feeds/post/finish-schedule?id=${id}`, (res) => {
                    if (res.error_code === 0) {
                        this.hide()
                        RbHighbar.success('日程已完成')
                        that.fetchFeeds()
                    } else RbHighbar.error(res.error_msg)
                })
            }
        })
    }

    gotoPage = (pageNo) => {
        this.setState({pageNo: pageNo}, () => this.fetchFeeds())
    }
}

// ~ 评论
class FeedsComments extends React.Component {
    state = {...this.props, pageNo: 1}

    render() {
        return <div className="comments">
            <div className="comment-reply">
                <div onClick={() => this._commentState(true)}
                     className={`reply-mask ${this.state.openComment && 'hide'}`}>添加评论
                </div>
                <span className={`${!this.state.openComment && 'hide'}`}>
          <FeedsEditor placeholder="添加评论" ref={(c) => this._editor = c}/>
          <div className="mt-2 text-right">
            <button onClick={() => this._commentState(false)} className="btn btn-sm btn-link">取消</button>
            <button className="btn btn-sm btn-primary" ref={(c) => this._btn = c}
                    onClick={() => this._post()}>评论</button>
          </div>
        </span>
            </div>
            <div className="feeds-list comment-list">
                {(this.state.data || []).map((item) => {
                    if (item.deleted) return null
                    const id = `comment-${item.id}`
                    return <div key={id} id={id}>
                        <div className="feeds">
                            <div className="user">
                                <a className="user-show">
                                    <div className="avatar"><img alt="Avatar"
                                                                 src={`${rb.baseUrl}/account/user-avatar/${item.createdBy[0]}`}/>
                                    </div>
                                </a>
                            </div>
                            <div className="content">
                                <div className="meta">
                                    <a>{item.createdBy[1]}</a>
                                </div>
                                {__renderRichContent(item)}
                                <div className="actions">
                                    <div className="float-left text-muted fs-12 time">
                                        <DateShow date={item.createdOn}/>
                                    </div>
                                    <ul className="list-unstyled m-0">
                                        {item.self && <li className="list-inline-item mr-2">
                                            <a data-toggle="dropdown" href="#mores" className="fixed-icon" title="更多"><i
                                                className="zmdi zmdi-more"></i>&nbsp;</a>
                                            <div className="dropdown-menu dropdown-menu-right">
                                                <a className="dropdown-item"
                                                   onClick={() => this._handleDelete(item.id)}><i
                                                    className="icon zmdi zmdi-delete"/>删除</a>
                                            </div>
                                        </li>
                                        }
                                        <li className="list-inline-item mr-3">
                                            <a href="#thumbup" onClick={() => this._handleLike(item.id)}
                                               className={`fixed-icon ${item.myLike && 'text-primary'}`}>
                                                <i className="zmdi zmdi-thumb-up"></i>赞 {item.numLike > 0 &&
                                            <span>({item.numLike})</span>}
                                            </a>
                                        </li>
                                        <li className="list-inline-item">
                                            <a href="#reply" onClick={() => this._toggleReply(item.id)}
                                               className={`fixed-icon ${item.shownReply && 'text-primary'}`}>
                                                <i className="zmdi zmdi-mail-reply"></i>回复
                                            </a>
                                        </li>
                                    </ul>
                                </div>
                                <div className={`comment-reply ${!item.shownReply && 'hide'}`}>
                                    {item.shownReplyReal &&
                                    <FeedsEditor placeholder="添加回复" initValue={`回复 @${item.createdBy[1]} : `}
                                                 ref={(c) => item._editor = c}/>}
                                    <div className="mt-2 text-right">
                                        <button onClick={() => this._toggleReply(item.id, false)}
                                                className="btn btn-sm btn-link">取消
                                        </button>
                                        <button className="btn btn-sm btn-primary" ref={(c) => this._btn = c}
                                                onClick={() => this._post(item._editor)}>回复
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                })}
            </div>
            <Pagination ref={(c) => this._pagination = c} call={this.gotoPage} pageSize={20} comment={true}/>
        </div>
    }

    componentDidMount = () => this._fetchComments()

    _fetchComments() {
        $.get(`/feeds/comments-list?feeds=${this.props.feeds}&pageNo=${this.state.pageNo}`, (res) => {
            const _data = res.data || {}
            this.state.pageNo === 1 && this._pagination.setState({rowsTotal: _data.total, pageNo: 1})
            this.setState({data: _data.data})
        })
    }

    _post = (whichEditor) => {
        if (!whichEditor) whichEditor = this._editor
        const _data = whichEditor.vals()
        if (!_data.content) {
            RbHighbar.create('请输入评论内容');
            return
        }
        _data.feedsId = this.props.feeds
        _data.metadata = {entity: 'FeedsComment'}

        const btn = $(this._btn).button('loading')
        $.post('/feeds/post/publish', JSON.stringify(_data), (res) => {
            btn.button('reset')
            if (res.error_msg > 0) {
                RbHighbar.error(res.error_msg);
                return
            }
            this._editor.reset()
            this._commentState(false)
            this._fetchComments()
        })
    }

    _commentState = (state) => {
        this.setState({openComment: state}, () => this.state.openComment && this._editor.focus())
    }

    _toggleReply = (id, state) => {
        event.preventDefault()
        const _data = this.state.data
        _data.forEach((item) => {
            if (id === item.id) {
                if (state !== undefined) item.shownReply = state
                else item.shownReply = !item.shownReply
                item.shownReplyReal = true
                if (item.shownReply) setTimeout(() => item._editor.focus(), 200)
            }
        })
        this.setState({data: _data})
    }

    _handleLike = (id) => _handleLike(id, this)
    _handleDelete = (id) => {
        event.preventDefault()
        const that = this
        RbAlert.create('确认删除该评论？', {
            type: 'danger',
            confirmText: '删除',
            confirm: function () {
                this.disabled(true)
                $.post(`/feeds/post/delete?id=${id}`, () => {
                    this.hide()
                    $(`#comment-${id}`).animate({opacity: 0}, 600, () => {
                        const _data = that.state.data
                        _data.forEach((item) => {
                            if (id === item.id) item.deleted = true
                        })
                        that.setState({data: _data})
                    })
                })
            }
        })
    }

    gotoPage = (pageNo) => {
        this.setState({pageNo: pageNo}, () => this._fetchComments())
    }
}

// ~ 分页
class Pagination extends React.Component {
    state = {
        ...this.props,
        pageSize: this.props.pageSize || 5,
        pageNo: this.props.pageNo || 1
    }

    render() {
        if (!this.state.rowsTotal) return null

        this.__pageTotal = Math.ceil(this.state.rowsTotal / this.state.pageSize)
        if (this.__pageTotal <= 0) this.__pageTotal = 1
        const pages = this.__pageTotal <= 1 ? [1] : $pages(this.__pageTotal, this.state.pageNo)

        return <div className="feeds-pages">
            <div className="float-left">
                <p className="text-muted">共 {this.state.rowsTotal} 条动态</p>
            </div>
            <div className="float-right">
                <ul className={`pagination ${this.props.comment && 'pagination-sm'}`}>
                    {this.state.pageNo > 1
                    && <li className="paginate_button page-item"><a className="page-link" onClick={this._prev}><span
                        className="icon zmdi zmdi-chevron-left"></span></a></li>}
                    {pages.map((item, idx) => {
                        if (item === '.') return <li key={`pnx-${idx}`} className="paginate_button page-item disabled">
                            <a className="page-link">...</a></li>
                        else return <li key={`pn-${item}`}
                                        className={'paginate_button page-item ' + (this.state.pageNo === item && 'active')}>
                            <a className="page-link" onClick={() => this._goto(item)}>{item}</a></li>
                    })}
                    {this.state.pageNo !== this.__pageTotal
                    && <li className="paginate_button page-item"><a className="page-link" onClick={this._next}><span
                        className="icon zmdi zmdi-chevron-right"></span></a></li>}
                </ul>
            </div>
            <div className="clearfix"></div>
        </div>
    }

    _prev = () => {
        if (this.state.pageNo === 1) return
        this._goto(this.state.pageNo - 1)
    }
    _next = () => {
        if (this.state.pageNo === this.__pageTotal) return
        this._goto(this.state.pageNo + 1)
    }
    _goto = (pageNo) => {
        this.setState({pageNo: pageNo}, () => {
            typeof this.props.call === 'function' && this.props.call(pageNo)
        })
    }
}

// 渲染动态内容
function __renderRichContent(e) {
    // 表情和换行不在后台转换，因为不同客户端所需的格式不同
    const contentHtml = converEmoji(e.content.replace(/\n/g, '<br />'))
    const contentMore = e.contentMore || {}
    return <div className="rich-content">
        <div className="texts text-break"
             dangerouslySetInnerHTML={{__html: contentHtml}}
        />
        <div className="appends">
            {e.type === 4 && <div>
                <div><span>日程时间 : </span> {contentMore.scheduleTime}</div>
                {contentMore.finishTime && <div>
                    <span>完成时间 : </span> {contentMore.finishTime.substr(0, 16)}</div>}
                {contentMore.scheduleRemind > 0 && <div>
                    <span>发送提醒 : </span> {__findMaskTexts(contentMore.scheduleRemind, REM_OPTIONS).join('、')}</div>}
            </div>
            }
            {e.relatedRecord && <div>
                <span><i className={`icon zmdi zmdi-${e.relatedRecord.icon}`}/> {e.relatedRecord.entityLabel} : </span>
                <a href={`${rb.baseUrl}/app/list-and-view?id=${e.relatedRecord.id}`}
                   title="查看相关记录">{e.relatedRecord.text}</a>
            </div>
            }
            {e.type === 3 && <div>
                {contentMore.showWhere > 0 &&
                <div><span>公示位置 : </span> {__findMaskTexts(contentMore.showWhere, ANN_OPTIONS).join('、')}</div>}
                {(contentMore.timeStart || contentMore.timeEnd) &&
                <div><span>公示时间 : </span> {contentMore.timeStart || ''} 至 {contentMore.timeEnd}</div>}
            </div>
            }
        </div>
        {(e.images || []).length > 0 && <div className="img-field">
            {e.images.map((item, idx) => {
                return (<span key={'img-' + item}>
          <a title={$fileCutName(item)} onClick={() => RbPreview.create(e.images, idx)}
             className="img-thumbnail img-upload zoom-in">
            <img src={`${rb.baseUrl}/filex/img/${item}?imageView2/2/w/100/interlace/1/q/100`}/>
          </a>
        </span>)
            })}
        </div>
        }
        {(e.attachments || []).length > 0 && <div className="file-field">
            {e.attachments.map((item) => {
                const fileName = $fileCutName(item)
                return <a key={'file-' + item} title={fileName} onClick={() => RbPreview.create(item)}
                          className="img-thumbnail">
                    <i className="file-icon" data-type={$fileExtName(fileName)}/><span>{fileName}</span>
                </a>
            })}
        </div>
        }
    </div>
}

const ANN_OPTIONS = [[1, '动态页'], [2, '首页'], [4, '登录页']]
const REM_OPTIONS = [[1, '消息通知'], [2, '邮件'], [4, '短信']]

function __findMaskTexts(mask, options) {
    const texts = []
    options.forEach((item) => {
        if ((item[0] & mask) !== 0) texts.push(item[1])
    })
    return texts
}

// 点赞
function _handleLike(id, comp) {
    event.preventDefault()
    $.post(`/feeds/post/like?id=${id}`, (res) => {
        const _data = comp.state.data
        _data.forEach((item) => {
            if (id === item.id) {
                item.numLike += (res.data ? 1 : -1)
                item.myLike = res.data
            }
        })
        comp.setState({data: _data})
    })
}