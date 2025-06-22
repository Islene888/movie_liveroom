// config.js
window.APP_CONFIG = {
    WS_API_BASE_URL: "ws://localhost:8080",  // 本地开发时
    HTTP_API_BASE_URL: "http://localhost:8080" // 如果你还有 fetch 请求
};

// 上线时只要把这里的地址改成云服务器的地址，比如 ws://18.216.138.180:8080
// window.APP_CONFIG = {
//     WS_API_BASE_URL: "ws://18.216.138.180:8080",  // 本地开发时
//     HTTP_API_BASE_URL: "http://18.216.138.180:8080" // 如果你还有 fetch 请求
// };