module.exports = {
  root: true,
  env: {
    browser: true,
    jquery: true,
    es6: true,
  },
  parserOptions: {
    ecmaVersion: 2016,
    sourceType: 'script',
  },
  parser: 'babel-eslint',
  plugins: ['react'],
  extends: ['eslint:recommended', 'plugin:react/recommended'],
  settings: {
    react: {
      pragma: 'React',
      version: '16.10.2',
    },
  },

  rules: {
    'react/jsx-no-target-blank': 0,
    'react/prop-types': 0,
    strict: 0,
    'no-redeclare': 0,
    indent: [2, 2],
    'linebreak-style': [0, 'unix'],
    quotes: [2, 'single'],
    semi: [2, 'never'],
    eqeqeq: [2, 'always'],
  },

  globals: {
    module: true,
    define: true,
    require: true,
    Mprogress: true,
    gridster: true,
    echarts: true,
    qiniu: true,
    React: true,
    ReactDOM: true,
    PropTypes: true,
    rb: true,
    $setTimeout: true,
    $random: true,
    $regex: true,
    $storage: true,
    $val: true,
    $urlp: true,
    $encode: true,
    $decode: true,
    $fileCutName: true,
    $fileExtName: true,
    $gotoSection: true,
    $createUploader: true,
    $cleanMenu: true,
    $cleanMap: true,
    $pages: true,
    $same: true,
    $unmount: true,
    $initReferenceSelect2: true,
    $keepModalOpen: true,
    renderRbcomp: true,
    RbSpinner: true,
    RbAlertBox: true,
    RbModal: true,
    RbModalHandler: true,
    RbFrom: true,
    RbFormHandler: true,
    RbFormElement: true,
    RbList: true,
    RbListPage: true,
    CellRenders: true,
    AdvFilter: true,
    UserSelector: true,
    UserShow: true,
    DateShow: true,
    DeleteConfirm: true,
    ApprovalProcessor: true,
    RbFormModal: true,
    DlgAssign: true,
    DlgShare: true,
    DlgUnshare: true,
    DlgShareManager: true,
    RbAlert: true,
    RbHighbar: true,
    ApprovalSubmitForm: true,
    ConfigList: true,
    ConfigFormDlg: true,
    RbPreview: true,
    $countdownButton: true,
    ChartSelect: true,
    Share2: true,
    $stopEvent: true,
    $addResizeHandler: true,
    $empty: true,
    $mp: true,
    $converEmoji: true,
    $throttle: true,
    $timechunk: true,
    moment: true,
    $moment: true,
    $fromNow: true,
    $expired: true,
    $lang: true,
  },
}
