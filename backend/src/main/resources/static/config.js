// config.js
// window.APP_CONFIG = {
// WS_API_BASE_URL: "ws://localhost:8080/ws",   // ✅ 必须加 /ws
//     HTTP_API_BASE_URL: "http://localhost:8080" //  fetch 请求
// };

// 上线时把地址改成云服务器的地址
window.APP_CONFIG = {
    WS_API_BASE_URL: "ws://34.134.39.193",
    HTTP_API_BASE_URL: "http://34.134.39.193" // fetch 请求
};