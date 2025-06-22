// config.js
// window.APP_CONFIG = {
//     WS_API_BASE_URL: "ws://localhost:8080",  // 本地开发时
//     HTTP_API_BASE_URL: "http://localhost:8080" //  fetch 请求
// };

// 上线时把地址改成云服务器的地址
window.APP_CONFIG = {
    WS_API_BASE_URL: "ws://3.15.170.2:8080",  // 本地开发时
    HTTP_API_BASE_URL: "http://3.15.170.2:8080" // fetch 请求
};