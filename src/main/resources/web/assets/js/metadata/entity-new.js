/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(function () {
  const $btn = $('.btn-primary').click(function () {
    const entityLabel = $val('#entityLabel'),
      comments = $val('#comments')
    if (!entityLabel) {
      RbHighbar.create($lang('PlsInputSome,EntityName'))
      return
    }

    const data = {
      label: entityLabel,
      comments: comments,
    }
    if ($val('#isSlave')) {
      data.masterEntity = $val('#masterEntity')
      if (!data.masterEntity) {
        RbHighbar.create($lang('PlsSelectSome,MasterEntity'))
        return
      }
    }

    $btn.button('loading')
    $.post('/admin/entity/entity-new?nameField=' + $val('#nameField'), JSON.stringify(data), function (res) {
      if (res.error_code === 0) parent.location.href = `${rb.baseUrl}/admin/entity/${res.data}/base`
      else RbHighbar.error(res.error_msg)
    })
  })

  let entityLoaded = false
  $('#isSlave').click(function () {
    $('.J_masterEntity').toggleClass('hide')
    parent.RbModal.resize()
    if (entityLoaded === false) {
      entityLoaded = true
      $.get('/admin/entity/entity-list', function (res) {
        $(res.data).each(function () {
          if (!this.slaveEntity) $(`<option value="${this.entityName}">${this.entityLabel}</option>`).appendTo('#masterEntity')
        })
      })
    }
  })

  $('.nav-tabs a').click(() => parent.RbModal.resize())

  let indexLoaded = false
  $('.J_imports').click(() => {
    if (indexLoaded) return
    renderRbcomp(<MetaschemaList />, 'metaschemas')
    indexLoaded = true
  })
})

class MetaschemaList extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    return (
      <div>
        {this.state.indexes ? (
          <div className="rbs-indexes ">
            {this.state.indexes.map((item) => {
              return (
                <div key={'data-' + item.file}>
                  <div className="float-left">
                    <h5>{item.name}</h5>
                    <div className="text-muted">
                      {$lang('DataSource')}{' '}
                      <a target="_blank" className="link" rel="noopener noreferrer" href={item.source}>
                        {item.author || item.source}
                      </a>
                      {item.updated && ' · ' + item.updated}
                    </div>
                  </div>
                  <div className="float-right">
                    {item.exists ? (
                      <button disabled className="btn btn-sm btn-primary">
                        {$lang('Exists')}
                      </button>
                    ) : (
                      <button disabled={this.state.inProgress === true} className="btn btn-sm btn-primary" onClick={() => this.imports(item)}>
                        {$lang('Import')}
                      </button>
                    )}
                  </div>
                  <div className="clearfix"></div>
                </div>
              )
            })}
          </div>
        ) : (
          <RbSpinner fully={true} />
        )}
      </div>
    )
  }

  componentDidMount() {
    $.get('/admin/rbstore/load-metaschemas', (res) => {
      if (res.error_code === 0)
        this.setState({ indexes: res.data }, () => {
          parent.RbModal.resize()
        })
      else RbHighbar.error(res.error_msg)
    })
  }

  imports(item) {
    const tips = [`<strong>${$lang('Import')} [ ${item.name} ]</strong>`]
    if ((item.refs || []).length > 0) {
      const refNames = []
      this.state.indexes.forEach((bar) => {
        if (item.refs.includes(bar.key) && !bar.exists) refNames.push(bar.name)
      })
      if (refNames.length > 0) tips.push($lang('ImportEntityTips1').replace('%s', refNames.join('、')))
    }
    tips.push($lang('ImportEntityTips2'))

    const that = this
    const $mp2 = parent && parent.$mp ? parent.$mp : $mp
    parent.RbAlert.create(tips.join('<br>'), {
      html: true,
      confirm: function () {
        this.hide()
        that.setState({ inProgress: true })

        $mp2.start()
        $.post(`/admin/metadata/imports?key=${item.key}`, (res) => {
          $mp2.end()
          that.setState({ inProgress: false })
          if (res.error_code === 0) {
            RbHighbar.success($lang('SomeSuccess,Import'))
            setTimeout(() => (parent.location.href = `${rb.baseUrl}/admin/entity/${res.data}/base`), 1500)
          } else {
            RbHighbar.error(res.error_msg)
          }
        })
      },
    })
  }
}
