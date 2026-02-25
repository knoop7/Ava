# Ava 用户脚本开发指南

轻量级用户脚本管理器，基于 Android-WebMonkey 项目开发。

## 支持的 Greasemonkey API

### 数据存储
```javascript
// 存储数据
GM_setValue('key', 'value');
GM_setValue('count', 123);
GM_setValue('config', {a: 1, b: 2});

// 读取数据
var value = GM_getValue('key', 'default');

// 删除数据
GM_deleteValue('key');

// 列出所有键
var keys = GM_listValues();
```

### 页面样式
```javascript
// 添加 CSS 样式
GM_addStyle('body { background: #000 !important; }');
GM_addStyle('.ad { display: none !important; }');

// 添加 DOM 元素
GM_addElement('script', { src: 'https://example.com/lib.js' });
GM_addElement('link', { rel: 'stylesheet', href: 'https://example.com/style.css' });
```

### 网络请求
```javascript
// 发送 HTTP 请求
GM_xmlhttpRequest({
    method: 'GET',
    url: 'https://api.example.com/data',
    headers: { 'Content-Type': 'application/json' },
    onload: function(response) {
        console.log(response.responseText);
        var data = JSON.parse(response.responseText);
    },
    onerror: function(error) {
        console.error('请求失败', error);
    }
});

// POST 请求
GM_xmlhttpRequest({
    method: 'POST',
    url: 'https://api.example.com/submit',
    data: JSON.stringify({ name: 'test' }),
    headers: { 'Content-Type': 'application/json' },
    onload: function(response) {
        console.log('提交成功');
    }
});

// 使用 fetch 风格 API
GM_fetch('https://api.example.com/data')
    .then(response => response.json())
    .then(data => console.log(data));
```

### Cookie 管理
```javascript
// 获取 Cookie 列表
GM_cookie.list({}).then(cookies => {
    console.log(cookies);
});

// 设置 Cookie
GM_cookie.set({
    name: 'token',
    value: 'abc123',
    domain: 'example.com',
    path: '/'
});

// 删除 Cookie
GM_cookie.delete({ name: 'token' });

// 删除所有 Cookie
GM_removeAllCookies();
```

### 通知和日志
```javascript
// 显示 Toast 通知
GM_notification('操作成功');
GM_toastShort('短提示');
GM_toastLong('长提示');

// 输出日志
GM_log('调试信息');
```

### 页面控制
```javascript
// 获取当前 URL
var url = GM_getUrl();

// 加载新页面
GM_loadUrl('https://example.com');
GM_loadUrl('https://example.com', { 'Referer': 'https://google.com' });

// 加载 iframe
GM_loadFrame('https://example.com/frame.html', 'https://example.com');

// 解析相对 URL
var fullUrl = GM_resolveUrl('/path/to/page', 'https://example.com');

// 退出/关闭
GM_exit();
```

### User-Agent
```javascript
// 获取当前 UA
var ua = GM_getUserAgent();

// 设置自定义 UA
GM_setUserAgent('Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/100.0.0.0');
```

### 其他功能
```javascript
// 在新标签页打开
GM_openInTab('https://example.com');

// 复制到剪贴板
GM_setClipboard('要复制的文本');

// 启动 Android Intent
GM_startIntent('android.intent.action.VIEW', 'https://example.com', null, null);

// 注册菜单命令（仅日志记录）
GM_registerMenuCommand('设置', function() {
    console.log('点击了设置');
});
```

### GM 4 Promise 风格 API
所有 API 都支持 Promise 风格调用：
```javascript
await GM.getValue('key');
await GM.setValue('key', 'value');
await GM.addStyle('body { color: red; }');
await GM.cookie.list({});
```

## 脚本模板

```javascript
// ==UserScript==
// @name         我的脚本
// @namespace    https://example.com
// @version      1.0
// @description  脚本描述
// @match        https://example.com/*
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_addStyle
// @grant        GM_xmlhttpRequest
// ==/UserScript==

(function() {
    'use strict';
    
    // 你的代码写在这里
    console.log('脚本已加载');
    
    // 示例：隐藏广告
    GM_addStyle('.ad, .advertisement { display: none !important; }');
    
    // 示例：修改页面内容
    document.querySelectorAll('h1').forEach(el => {
        el.style.color = 'red';
    });
})();
```

## 不支持的功能

- `GM_download` - 文件下载
- `GM_webRequest` - 请求拦截
- `unsafeWindow` - 直接访问页面 window 对象
- `GM_getTab` / `GM_saveTab` - 标签页数据
- `@require` - 外部脚本依赖
- `@resource` - 外部资源（部分支持）

## 注意事项

1. 脚本在页面加载完成后执行
2. 跨域请求受浏览器安全策略限制
3. 部分网站可能检测并阻止脚本运行
4. 建议使用 try-catch 包裹代码防止错误

## 项目地址

https://github.com/warren-bank/Android-WebMonkey
