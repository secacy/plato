import grpc from 'k6/net/grpc';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { randomString, uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// --- 配置 ---
const PROTO_PATH = './storage-proto/src/main/proto/storage_search.proto';
const ADDRESS = '127.0.0.1:9091';

// 1. 定义我们最关心的指标：P99
const writeLatency = new Trend('grpc_req_duration_ms');

const client = new grpc.Client();
client.load(['.'], PROTO_PATH);

export const options = {
    // 2. 场景：模拟持续的稳定并发，而不是爆发
    scenarios: {
        constant_load: {
            executor: 'constant-vus',
            vus: 50,              // 保持 50 个并发用户一直在发
            duration: '30s',      // 持续 30 秒
        },
    },
    // 3. 阈值：这是你的核心验收标准
    thresholds: {
        // 只有当 P99 < 100ms 时，测试才算"PASS"
        'grpc_req_duration_ms': ['p(99) < 100'],
    },
};

export default function () {
    if (__ITER == 0) {
        client.connect(ADDRESS, { plaintext: true, reflect: true });
    }

    // 4. 关键：完全随机化 Session ID
    // 范围设大一点 (1~100w)，模拟全网不同的私聊/群聊
    // 这样几乎不会触发 TiDB 的行锁竞争，纯测系统管道速度
    const randomSessionId = randomIntBetween(1, 1000000);

    const payload = {
        session_id: randomSessionId,
        sender_id: randomIntBetween(1000, 9999),
        client_message_id: uuidv4(),
        msg_type: 1,
        content: randomString(30), // 模拟短文本
        extra: { "test": "p99_latency" }
    };

    const response = client.invoke('com.plato.gateway.StorageMessageService/SaveMessage', payload);

    check(response, {
        'status is OK': (r) => r && r.status === grpc.StatusOK,
    });

    // k6 会自动记录 grpc_req_duration，但如果你想看自定义指标，也可以手动加
    // writeLatency.add(response.timings.duration);
}