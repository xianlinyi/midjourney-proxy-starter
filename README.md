# midjourney-proxy-starter

> 修改自项目：[midjourney-proxy](https://github.com/novicezk/midjourney-proxy)

[![GitHub release](https://img.shields.io/static/v1?label=release&message=v2.3.5&color=blue)](https://www.github.com/novicezk/midjourney-proxy)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

### 项目介绍
适用于 springboot 框架的，可依赖引入项目的 MidJourney discord频道代理，实现api形式调用AI绘图。

### 使用方法
1. 引入依赖
```xml
<dependency>
    <groupId>com.github.novicezk</groupId>
    <artifactId>midjourney-proxy-starter</artifactId>
    <version>2.3.5</version>
</dependency>
```
如果你的项目是spring-boot-web项目，需要排除spring-boot-starter-webflux依赖
```xml
<dependency>
    <groupId>com.prechatting</groupId>
    <artifactId>midjourney-proxy-starter</artifactId>
    <version>2.3.5</version>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```
2. 配置参数
```yaml
mj:
  discord:
    - guild-id: xxx # discord服务器ID
      channel-id: xxx # discord频道ID
      user-token: xxx # discord用户Token
      session-id: bf90fb2e67fa1c795f470be84dbc2f99 # discord用户的sessionId，不设置时使用默认的，建议从interactions请求中复制替换掉
      user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36
      user-wss: true
    - guild-id: xxx
      channel-id: xxx
      user-token: xxx
      session-id: bf90fb2e67fa1c795f470be84dbc2f99
      user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36
      user-wss: true
  task-store:
    type: in_memory
    timeout: 30d
  translate-way: null
  queue:
    timeout-minutes: 5
    core-size: 3
    queue-size: 10
  proxy:
    host: 127.0.0.1 # 代理host
    port: 1090 # 代理端口
```
3. 调用接口(去掉`/mj`路由，其余与midjourney-proxy一致)
```
//例如：
POST http://IP:端口/submit/imagine
Content-Type: application/json
Body:        
        {
        "prompt":"a cat",
        "base64":"data:image/png;base64,i
        }
```
4. 代码调用
```java
//例如：
@Autowired
private MJService mjService;

@PostMapping(value = "/image")
public ResultEntity image(@RequestBody SubmitImagineDTO submit) {
        SubmitResultVO imagine = mjService.imagine(submit);
        return new ResultEntity().ok().data(imagine);
        }

@GetMapping(value = "/task/{id}")
public ResultEntity task(@ApiParam(value = "任务ID") @PathVariable String id) {
        Task fetch = mjService.fetch(id);
        return new ResultEntity().ok().data(fetch);
        }
```
5. 自定义账号选择策略
```java
// 实现DiscordConfigService接口，重写getDiscordConfig方法，注入容器即可
import com.prechatting.ProxyProperties;
import com.prechatting.service.DiscordConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonalDiscordConfigService implements DiscordConfigService {
    @Override
    public ProxyProperties.DiscordConfig getDiscordConfig(List<ProxyProperties.DiscordConfig> discordConfigs) {
        return discordConfigs.get(0);
    }
}
```
6. 启动项目
```log
2023-07-27T01:17:24.370+08:00  INFO 62737 --- [           main] com.prechatting.Application              : Started Application in 2.673 seconds (process running for 3.031)
# 启动日志：项目启动成功后出现以下日志，即为该 DiscordConfig 连接成功，如果配置了多个 DiscordConfig ，则会依次出现多个链接成功日志
2023-07-27T01:17:32.040+08:00  INFO 62737 --- [  ReadingThread] c.p.wss.user.UserWebSocketStarter        : [gateway] Connected to websocket. userToken:xxx, guildId:xxx, channelId:xxx 
```
## 新增功能
- [x] 支持配置多个discord账号
- [x] 支持自定义账号选择策略

## 原版功能（与[midjourney-proxy](https://github.com/novicezk/midjourney-proxy)-2.3.5 相同）
- [x] 支持 Imagine 指令和相关U、V操作
- [x] Imagine 时支持添加图片base64，作为垫图
- [x] 支持 Blend(图片混合) 指令和相关U、V操作
- [x] 支持 Describe 指令，根据图片生成 prompt
- [x] 支持 Imagine、V、Blend 图片生成进度
- [x] 支持中文 prompt 翻译，需配置百度翻译或 gpt
- [x] prompt 敏感词判断，支持覆盖调整
- [x] 任务队列，默认队列10，并发3。可参考 [MidJourney订阅级别](https://docs.midjourney.com/docs/plans) 调整mj.queue
- [x] user-token 连接 wss，可以获取错误信息和完整功能
- [x] 支持 discord域名(server、cdn、wss)反代，配置 mj.ng-discord

## 使用前提
1. 注册 MidJourney，创建自己的频道，参考 https://docs.midjourney.com/docs/quick-start
2. 获取用户Token、服务器ID、频道ID：[获取方式](./docs/discord-params.md)

## 风险须知
1. 作图频繁等行为，可能会触发midjourney账号警告，请谨慎使用
2. 为减少风险，请设置`mj.discord.user-agent` 和 `mj.discord.session-id`
3. 默认使用user-wss方式，可以获取midjourney的错误信息、图片变换进度等，但可能会增加账号风险
4. 支持设置mj.discord.user-wss为false，使用bot-token连接wss，需添加自定义机器人：[流程说明](./docs/discord-bot.md)

## 配置项
- mj.discord.guild-id：discord服务器ID
- mj.discord.channel-id：discord频道ID
- mj.discord.user-token：discord用户Token
- mj.discord.session-id：discord用户的sessionId，不设置时使用默认的，建议从interactions请求中复制替换掉
- mj.discord.user-agent：调用discord接口、连接wss时的user-agent，默认使用作者的，建议从浏览器network复制替换掉
- mj.discord.user-wss：是否使用user-token连接wss，默认true
- mj.discord.bot-token：自定义机器人Token，user-wss=false时必填
- 更多配置查看 [Wiki / 配置项](https://github.com/novicezk/midjourney-proxy/wiki/%E9%85%8D%E7%BD%AE%E9%A1%B9)

## 本地开发
- 依赖java17和maven

## 其它
- 多用户依然采用 midjourney-proxy 启动时创建ws链接的方式，因此需要在启动前添加多个账号，由于账号成本太贵，未尝试过作图请求时进行ws链接，推测可能会有封号风险，各位可以自行改动尝试，如果没被封请告知一下；
- 如果觉得这个项目对你有所帮助，请帮忙点个star；
