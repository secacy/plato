/**
 * @since 2025/11/18 11:15
 * @className IdGeneratorAlgorithm
 * @author hc
 */
import java.time.Duration;
import java.time.Instant;
import java.util.Random;

/**
 * Java "类雪花" ID 生成器
 *
 * ID 结构 (64位):
 * 1位 (保留) | 32位 (秒级时间戳) | 8位 (实例ID) | 6位 (业务ID) | 17位 (自增序列)
 * 0 | TTT...T | III...I | BBB...B | NNN...N
 */

/*
这段代码是实现一个用于生成64位整型id的类，使用的是Java语言。
它的设计理念来源于Twitter的雪花算法，可以在分布式环境中生成全局唯一ID。
在ID中包含了时间戳、机器实例ID、业务id和自增数，通过这些字段的混合，保证了id的全局唯一性。
代码的大部分都是在这些字段的处理，包括它们在整个64位ID中的布局和位操作。
*/

public class IdGeneratorAlgorithm {

    // --- 1. 定义位掩码和常量 ---

    // 17位自增序列的最大值 (2^17 - 1)
    private static final long MAX_NUMBER = (1L << 17) - 1;
    // 16位随机数，用于重置序列号 (2^16)
    private static final int RAND_MAX = 1 << 16;

    // 32位时间戳掩码 (2^32 - 1)
    private static final long TIME_MASK = (1L << 32) - 1;
    // 8位实例ID掩码 (2^8 - 1)
    private static final long INST_MASK = (1L << 8) - 1;
    // 6位业务ID掩码 (2^6 - 1)
    private static final long BID_MASK = (1L << 6) - 1;
    // 17位自增序列掩码 (2^17 - 1)
    private static final long NUM_MASK = (1L << 17) - 1;


    // --- 2. 内部状态---

    /**
     * 在这里用作内部可变状态
     */
    private static class InternalState {
        long time;
        long instanceId;
        long bid;
        long num;
    }

    private final InternalState tempState;
    private final Random random;
    private final Object lock = new Object();


    /**
     * 构造函数
     * @param instanceId 实例ID (0-255).
     */
    public IdGeneratorAlgorithm(byte instanceId) {
        this.tempState = new InternalState();
        this.tempState.instanceId = (long)instanceId & INST_MASK;
        this.random = new Random();
    }


    // --- 3. 核心生成逻辑---

    /**
     * 生成一个新的唯一ID
     * @param bid 业务ID (0-63)
     * @return 64位唯一ID
     */
    public long gen(int bid) {
        // 使用synchronized来保证对 tempState 的访问是线程安全的
        synchronized (lock) {
            // 1. 序列号递增
            this.tempState.num++;

            // 2. 检查序列号是否溢出
            if (this.tempState.num > MAX_NUMBER) {
                // 等待下一秒
                waitNextSecond();
                // 等待后，时间会改变，下面的逻辑会重置序列号
            }

            // 3. 检查时间是否变化
            long t = Instant.now().getEpochSecond();
            if (this.tempState.time != t) {
                this.tempState.time = t;
                // 重置为一个随机数
                this.tempState.num = (long) this.random.nextInt(RAND_MAX);
            }

            // 4. 设置业务ID
            this.tempState.bid = (long)bid & BID_MASK;

            // 5. 编码并返回
            try {
                return encode(this.tempState);
            } catch (Exception e) {
                // 理论上，在Gen方法内部，encode不会失败
                throw new RuntimeException("ID generation failed", e);
            }
        }
    }

    // --- 4. 辅助方法---

    /**
     * 阻塞并等待，直到下一秒开始
     */
    private void waitNextSecond() {
        // 计算下一秒的开始时间
        Instant nextSecond = Instant.ofEpochSecond(this.tempState.time + 1);

        // 计算需要等待的时间
        Duration duration = Duration.between(Instant.now(), nextSecond);

        if (duration.isNegative() || duration.isZero()) {
            return;
        }

        try {
            // 阻塞当前线程
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            // 捕获到中断时，恢复中断状态并抛出异常
            Thread.currentThread().interrupt();
            throw new RuntimeException("ID generator was interrupted while waiting for next second", e);
        }
    }


    // --- 5. 编码与解码---

    /**
     * 'ID' 结构体，用于'decode'方法的返回。
     * 这是一个不可变的数据传输对象 (DTO)。
     */
    public static class DecodedID {
        public final long time;
        public final long instanceId;
        public final long bid;
        public final long num;

        public DecodedID(long time, long instanceId, long bid, long num) {
            this.time = time;
            this.instanceId = instanceId;
            this.bid = bid;
            this.num = num;
        }

        @Override
        public String toString() {
            return "DecodedID{" +
                    "time=" + time + " (Instant: " + Instant.ofEpochSecond(time) + ")" +
                    ", instanceId=" + instanceId +
                    ", bid=" + bid +
                    ", num=" + num +
                    '}';
        }
    }

    /**
     * 编码
     * (InternalState 是可变的，DecodedID 是不可变的, 但它们结构相同)
     */
    private static long encode(InternalState i) throws IllegalArgumentException {
        // 错误检查
        if (i.num > MAX_NUMBER) {
            throw new IllegalArgumentException("Sequence number (num) is too big");
        }
        if (i.instanceId > INST_MASK) { // > 255
            throw new IllegalArgumentException("Invalid instance ID");
        }

        // 使用位移和"或"运算来组装ID
        // 关键：(i.time << 31) 确保最高位(符号位)为0
        return (i.time << 31) |
                (i.instanceId << 23) |
                (i.bid << 17) |
                i.num;
    }

    /**
     * 解码
     * @param id 64位ID
     * @return 解码后的 DecodedID 对象
     */
    public static DecodedID decode(long id) {
        // 使用位移和掩码来提取
        // (id >>> 31) 使用无符号右移来获取32位的时间戳
        long time = (id >>> 31) & TIME_MASK;
        long instanceId = (id >>> 23) & INST_MASK;
        long bid = (id >>> 17) & BID_MASK;
        long num = id & NUM_MASK;

        return new DecodedID(time, instanceId, bid, num);
    }

    /**
     * 从ID中获取时间戳
     * @param id 64位ID
     * @return 秒级时间戳
     */
    public static long getTimeFromId(long id) {
        // 优化：我们不需要解码所有内容，只需提取时间
        return (id >>> 31) & TIME_MASK;
    }

    // --- 示例用法 ---
    public static void main(String[] args) {
        // 1. 初始化一个生成器，实例ID为 10 (必须是 0-255)
        IdGeneratorAlgorithm generator = new IdGeneratorAlgorithm((byte) 10);

        // 2. 生成一个ID，业务ID为 5 (必须是 0-63)
        long id1 = generator.gen(5);
        long id2 = generator.gen(5);

        System.out.println("Generated ID 1: " + id1);
        System.out.println("Generated ID 2: " + id2);

        // 3. 解码一个ID
        IdGeneratorAlgorithm.DecodedID decoded = IdGeneratorAlgorithm.decode(id1);
        System.out.println("Decoded ID 1: " + decoded);

        // 4. 单独获取时间
        long timestamp = IdGeneratorAlgorithm.getTimeFromId(id1);
        System.out.println("Timestamp from ID 1: " + timestamp);
    }
}