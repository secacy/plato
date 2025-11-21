import grpc from 'k6/net/grpc';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { randomString, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// 1. 加载你的 Protobuf 定义
const client = new grpc.Client();
client.load(['.'], 'im_service.proto'); // 假设你的 proto 文件在当前目录

// 2. 自定义指标：记录成功写入的耗时
const writeLatency = new Trend('write_duration');

export const options = {
    // 压测策略：阶梯式施压
    stages: [
        { duration: '30s', target: 100 }, // 30秒内爬坡到 100 并发
        { duration: '1m', target: 500 },  // 维持 500 并发 1分钟
        { duration: '30s', target: 1000 },// 冲刺 1000 并发
        { duration: '10s', target: 0 },   // 结束
    ],
    // 阈值告警：如果 P99 > 100ms，则测试视为不达标（根据你的预期价值设定）
    thresholds: {
        'grpc_req_duration': ['p(99)<100'],
    },
};

export default function () {
    // 建立连接
    if (__ITER == 0) {
        client.connect('127.0.0.1:9000', {
            plaintext: true // 开发环境通常是非 TLS
        });
    }

    // --- 场景配置 ---
    // true = 模拟李佳琦直播间（单会话高并发，测试 RingBuffer）
    // false = 模拟普通全网流量（多会话，测试 TiDB 整体写入能力）
    const isHotspotTest = true;

    let sessionId;
    if (isHotspotTest) {
        sessionId = "session_hotspot_8888";
    } else {
        sessionId = `session_${randomIntBetween(1, 10000)}`;
    }

    // 构造 Payload
    const data = {
        session_id: sessionId,
        sender_id: `user_${randomIntBetween(1, 5000)}`,
        payload: randomString(50), // 模拟 50 字节的消息
        // ... 其他字段
    };

    // 发起 gRPC 请求
    const start = Date.now();
    const response = client.invoke('im.StorageService/SaveMessage', data);
    const duration = Date.now() - start;

    // 检查结果
    check(response, {
        'status is OK': (r) => r && r.status === grpc.StatusOK,
        // 验证服务端是否返回了生成的 msg_id 和 seq_id
        'has msg_id': (r) => r.message && r.message.msg_id,
    });

    // 记录数据
    if (response.status === grpc.StatusOK) {
        writeLatency.add(duration);
    } else {
        console.error(`Error: ${response.error}`);
    }

    // 稍微休眠一下，避免客户端把本机 CPU 跑满，模拟真实用户间隔
    // 如果要测极限 TPS，可以注释掉 sleep
    // sleep(0.01);
}