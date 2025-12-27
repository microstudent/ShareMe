# ShareMe
<img src="/pic/logo.png" width="80" height="80" />

ShareMe 是一个基于 Wi-Fi Direct 的面对面音乐同步播放应用。两台手机通过 Wi-Fi Direct 组网，群主设备提供 RTSP 音频流，所有设备通过 WebSocket 同步播放控制与进度，实现近场多人同步播放与双声道控制。

<img src="/pic/Screenshot.png" width="50%" height="50%" />

## Features
- Wi-Fi Direct 近场发现与配对
- RTSP 音频流传输与播放
- WebSocket 同步播放控制、列表同步与时序校准
- 双声道独立音量控制

## Architecture
1. 设备发现与连接：Wi-Fi Direct DNS-SD 广播本机服务，交换密码与时间戳，自动建立 P2P 连接。  
2. 角色分配：群主设备作为服务端，其它设备作为客户端。  
3. 音频传输：服务端通过 RTSP 输出音频，客户端接收 PCM 并播放。  
4. 同步控制：WebSocket 传递播放事件与同步信号，客户端基于单调时钟与抖动缓冲对齐播放。  

## Tech Stack
- Android (minSdk 21, targetSdk 28)
- Kotlin + Java
- Wi-Fi Direct (P2P)
- RTSP / RTP (libstreaming)
- WebSocket (AndroidAsync)
- RxJava / Realm / Glide / ExoMedia

## Module Structure
- `app/` 主应用与 UI
- `libstreaming/` RTSP 流媒体实现
- `AndroidAsync/` WebSocket/HTTP 异步网络库
- `bouncyfastscroller/` 列表滚动 UI 组件

## Getting Started
### Prerequisites
- Android Studio 3.4+
- Android SDK 28

### Build
```bash
./gradlew assembleDebug
```

### Run
1. 两台设备开启 Wi-Fi Direct。
2. 在设备上安装应用并启动。
3. 一台设备进入连接页面并完成配对，群主会自动成为服务端。
4. 选择歌曲后会同步到其他设备播放。

## Sync Strategy
- 服务端定期发送 `[elapsedRealtime, playTime]`。
- 客户端估算服务端时间基准，计算播放差值并执行：
  - 进度领先：短暂 sleep
  - 进度落后：跳过部分 PCM 帧
- 通过抖动缓冲维持目标缓冲时长，降低网络波动影响。

## Limitations
- 依赖 Wi-Fi Direct，部分机型兼容性和稳定性存在差异。
- WebSocket 与 RTSP 分离传输，网络抖动会影响同步精度。
- 同步精度受设备时钟漂移和音频硬件缓冲影响。

## Roadmap
- 自适应同步与更精细的时钟校准
- 多客户端状态可视化
- 网络状况与同步延迟诊断

## Acknowledgements
- libstreaming
- AndroidAsync
- ExoMedia

---

## English Summary
ShareMe is a face-to-face music sharing app built on Wi-Fi Direct. The group owner acts as an RTSP server and all clients receive audio while synchronizing playback via WebSocket control and timing signals. It supports synchronized playback, playlist sync, and dual-channel volume control.
