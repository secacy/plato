package idgen

import (
    "errors"
    "math/rand"
    "sync"
    "time"
)

/***********************************
 *Genter a int64
 *reserved 1bit|timestamp 32bit|instanceid 8bit|bid 6bit|increment 17bit/
 ***********************************/

/*
这段代码是实现一个用于生成64位整型id的包，使用的是Go语言。
它的设计理念来源于Twitter的雪花算法，可以在分布式环境中生成全局唯一ID。
在ID中包含了时间戳、机器实例ID、业务id和自增数，通过这些字段的混合，
保证了id的全局唯一性。代码的大部分都是在这些字段的处理，包括它们在整个64位ID中的布局和位操作。
*/

// 定义了一些在位运算中需要使用的常量。
const (
    VERSION      = 1
    MAX_NUMBER   = (1 << 17) - 1
    RAND_MAX     = 1 << 16
    TIME_MASK    = ((1 << 32) - 1)
    INST_MASK    = (1 << 8) - 1
    BID_MASK     = (1 << 6) - 1
    NUM_MASK     = (1 << 17) - 1
)

// 定义了一个IdGen结构体，使用sync.RWMutex控制并发访问，temp为储存临时ID值。
type IdGen struct {
    mutex sync.RWMutex
    temp  *ID
}

// 接下来是对id生成和处理的一系列方法。
type ID struct {
    Time        int64
    Instanceid  int64
    Bid         int64
    Num         int64
}

// 定义的第一个方法是用于初始化 IdGen 的方法，传入一个字节作为初始的实例ID。
func NewIdGen(i byte) *IdGen {
    temp := &ID{
        Instanceid: int64(i) & INST_MASK,
    }

    return &IdGen{
        temp: temp,
    }
}

// 接下来是等待下一秒钟的方法 waitNextSecond，该方法用于在达到每秒生成数的最大限制时等待到下一秒。
func (self *IdGen) waitNextSecond() {
    nextSecond := time.Unix(self.temp.Time+1, 0)
    duration := nextSecond.Sub(time.Now())
    if duration <= 0 {
        return
    }
    time.Sleep(duration)
}

func (self *IdGen) Gen(bid int) (id int64) {
    self.mutex.Lock()

    defer self.mutex.Unlock()
    self.temp.Num += 1
    if self.temp.Num > MAX_NUMBER {
        self.waitNextSecond()
    }

    t := time.Now().Unix()
    if self.temp.Time != t {
        self.temp.Time = t
        self.temp.Num = rand.Int63n(RAND_MAX)
    }
    self.temp.Bid = int64(bid) & BID_MASK
    id, _ = Encode(self.temp)
    return
}

// 从ID中获取时间戳的方法，GetTimeFromId将会时间戳解码后返回
func GetTimeFromId(id int64) (int64, error) {
    d, err := Decode(id)
    if err != nil {
        return 0, err
    }
    return d.Time, nil
}

// 对于id的编码和解码方法：
func Encode(i *ID) (int64, error) {
    if i.Num > MAX_NUMBER {
        return 0, errors.New("num is too big")
    }
    if i.Instanceid > 255 {
        return 0, errors.New("invalid instanceid")
    }
    id := (i.Time << 31) |
        (i.Instanceid << 23) | (i.Bid << 17) | i.Num
    return id, nil
}
func Decode(id int64) (*ID, error) {
    d := new(ID)
    d.Time = (id >> 31) & TIME_MASK
    d.Instanceid = (id >> 23) & INST_MASK
    d.Bid = (id >> 17) & BID_MASK
    d.Num = id & NUM_MASK
    return d, nil
}