# cpush

基于apns2协议推送终端库

解决以下问题：
#### 1：DNS自解析，解决默认DNS解析指向统一IP，IP断开后，一段时间无法连接。
#### 2：基于令牌桶算法实现限流，解决可能的流速限制问题导致的重连。
#### 3：解决无效token导致连接断开的问题。
#### 4：包含一个mocke服务程序，方便测试。
