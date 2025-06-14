[English](README.md) | [中文](README_zh.md)

#### 意图

为了便捷的在局域网内将文件分享出去，该项目由此诞生了

* 便捷：一键将本地文件通过http协议分享！
* 安全：无需通过ftp或者微信服务器中转文件。
* 协同：支持二维码下载文件。
* 命令行：支持cmd分享/取消分享/列出文件，兼容无图形界面系统。

#### 对比

与nginx和python的http.server相比，首先中最大的区别就是前者只能分享指定目录下的文件，HttpFileShare可以分享任意目录下的文件而无需修改配置和重启，其次是可以通过界面进行分享和取消分享文件。

#### 参数说明

| 参数                             | 描述                                |
|--------------------------------|-----------------------------------|
| -DhttpFileShare.port=11111     | 指定http服务端口号，如未指定则系统随机获取一个未被占用的端口号 |
| -DhttpFileShare.ip=172.16.1.37 | 指定http服务端ip，一般针对多网卡电脑主机           |

#### 演示视频

[Bilibili Video](https://www.bilibili.com/video/BV1XHTezDEDC/)

#### Q&A

* 1.windows修改启动ip和端口

![HttpFileShare](help1.png)

* 2.mac修改启动ip和端口

找到HttpFileShare.app 编辑Contents/app/HttpFileShare.cfg 文件（如果有），或者创建它。 在其中添加 JVM 参数：
```
[JavaOptions]
java-options=-Djpackage.app-version=1.0.0
java-options=-DhttpFileShare.port=11111
java-options=-DhttpFileShare.ip=172.16.1.37
```

#### 截图
![HttpFileShare](main_frame.png)
![HttpFileShare](main_frame2.png)
![HttpFileShare](main_frame3.png)
![HttpFileShare](main_frame4.png)
![HttpFileShare](main_frame5.png)
