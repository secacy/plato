import grpc from 'k6/net/grpc';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const PROTO_PATH = './storage_gateway.proto';
const ADDRESS = '127.0.0.1:9000';

const readLatency = new Trend('read_latency_ms');

const client = new grpc.Client();
client.load(['.'], PROTO_PATH);

export const options = {
    stages: [
        { duration: '30s', target: 100 }, // 100 并发读
    ],
    thresholds: {
        'read_latency_ms': ['p(99)<50'], // 读请求通常要求更快，比如 < 50ms
    },
};

export default function () {
    if (__ITER == 0) {
        client.connect(ADDRESS, { plaintext: true });
    }

    // 场景 1: 模拟用户打开会话，拉取"最新"的 20 条
    // 对应 Proto: anchor_seq_id = 0, direction = BACKWARD (0)
    const reqLatest = {
        session_id: 10086, // 确保这个 session 里有数据（先跑写脚本）
        anchor_seq_id: 0,  // 0 代表从最新开始
        direction: 0,      // BACKWARD (查看历史)
        limit: 20
    };

    const start = Date.now();
    const res = client.invoke('com.plato.gateway.StorageMessageService/GetMessages', reqLatest);
    const duration = Date.now() - start;

    check(res, {
        'status is OK': (r) => r.status === grpc.StatusOK,
        // 确保拉到了数据 (前提是你先跑了 bench_save.js 灌入了数据)
        'has messages': (r) => r.message && r.message.messages && r.message.messages.length > 0,
    });

    if (res.status === grpc.StatusOK) {
        readLatency.add(duration);
    } else {
        console.log(JSON.stringify(res.error));
    }

    sleep(0.1);
}