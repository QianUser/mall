# 开发环境

- 操作系统（开发）：Windows 10
- 操作系统（数据库等）：[CentOS 7](https://mirrors.aliyun.com/centos/7/isos/x86_64/CentOS-7-x86_64-Everything-2009.iso?spm=a2c6h.25603864.0.0.74092d1cfur2hR)
- 操作系统静态IP：192.168.227.131
- Java 8
- Maven 3.6.3
- IDE：Intellij IDEA 2020.3.2
- Git：2.37.3
- Spring Boot 2.7.3

## [安装Docker](https://docs.docker.com/engine/install/centos/)

卸载旧版本Docker：

```sh
sudo yum remove docker \
                  docker-client \
                  docker-client-latest \
                  docker-common \
                  docker-latest \
                  docker-latest-logrotate \
                  docker-logrotate \
                  docker-engine
```

设置仓库

```sh
sudo yum install -y yum-utils

sudo yum-config-manager \
    --add-repo \
    https://download.docker.com/linux/centos/docker-ce.repo
```

安装Docker引擎：

```sh
sudo yum install docker-ce docker-ce-cli containerd.io docker-compose-plugin
```

启动Docker：

```sh
sudo systemctl start docker
```

设置Docker开机自启：

```sh
sudo systemctl enable docker
```

查看：

```sh
docker -v  # Docker版本
sudo docker images  # 查看下载的镜像
```

下面可以测试[Docker常用命令](https://docs.docker.com/engine/reference/commandline/docker/)。

### 配置docker阿里云镜像加速

这里使用阿里云容器镜像加速（针对Docker客户端版本大于1.10.0的用户）。登录[阿里云](https://www.aliyun.com/)，进入控制台，选择容器镜像服务，选择镜像加速器，选择CentOS，然后得到并执行以下命令：

```sh
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": ["https://y403z5bw.mirror.aliyuncs.com"]
}
EOF
sudo systemctl daemon-reload
sudo systemctl restart docker
```

## Docker安装MySQL

下载[镜像文件](https://hub.docker.com/_/mysql/tags)：

```sh
docker pull mysql:5.7  # 不指定版本号会下载最新版本
```

创建实例并启动：

```sh
docker run -p 3306:3306 --name mysql \  # 将容器的3306端口映射到主机的3306端口
-v /mydata/mysql/log:/var/log/mysql \  # 将配置文件夹挂载到主机
-v /mydata/mysql/data:/var/lib/mysql \  # 将日志文件夹挂载到主机
-v /mydata/mysql/conf:/etc/mysql \  # 将配置文件夹挂载到主机
-e MYSQL_ROOT_PASSWORD=root \  # 初始化root用户的密码
-d mysql:5.7  # 以后台方式运行；使用mysql:5.7镜像启动容器
```

查看Docker运行中的容器：

```sh
docker ps
```

可以看到容器启动了，然后就可以使用客户端连接该MySQL了。

进入容器内部：

```sh
docker exec -it 14f /bin/bash  # 14f为容器id前缀（只要能保证唯一即可），也可以写容器名字：mysql
ls /  # 目录结构是完整的Linux目录结构，说明MySQL容器是一个完整的运行环境
whereis mysql  # 查看MySQL在Docker中的安装位置
```

下面退出容器（执行`exit`命令）并配置MySQL（主要修改字符编码）。打开MySQL配置文件：

```sh
vi /mydata/mysql/conf/my.cnf
```

然后输入以下内容：

```mysql
[client]
default-character-set=utf8

[mysql]
default-character-set=utf8

[mysqld]
init_connect='SET collation_connection = utf8_unicode_ci'
init_connect='SET NAMES utf8'
character-set-server=utf8
collation-server=utf8_unicode_ci
skip-character-set-client-handshake
skip-name-resolve  # 跳过域名解析，用于解决MySQL连接慢的问题
```

重启MySQL：

```sh
docker restart mysql
```

验证：

```sh
docker exec -it 14f /bin/bash
cd /etc/mysql
ls  # 确实产生了一个my.cnf文件
cat my.cnf  # 内容也确实与配置的一样
```

## Docker安装Redis

下载[镜像文件](https://hub.docker.com/_/redis/tags)：

```sh
docker pull redis:6.2.7
```

创建实例并启动：

```sh
mkdir -p /mydata/redis/conf
touch /mydata/redis/conf/redis.conf  # 创建文件，防止docker run的过程中将该文件当成目录

docker run -p 6379:6379 --name redis -v /mydata/redis/data:/data \
-v /mydata/redis/conf/redis.conf:/etc/redis/redis.conf \
-d redis:6.2.7 redis-server /etc/redis/redis.conf
```

进入Redis客户端：

```sh
docker exec -it redis redis-cli 
```

较老的Redis默认情况下可能没有持久化，因此重启Reids（`docker restart redis`）后数据会丢失。此时可以配置持久化：

```sh
vi redis.conf
```

然后输入以下内容，启用AOF持久化方式：

```ini
appendonly yes
```

Redis的所有配置见[Redis configuration](https://redis.io/docs/manual/config/)。

设置Docker启动后容器自动启动：

```sh
sudo docker ps -a  # 查看所有容器
sudo docker update mysql -- restart=always
sudo docker update redis -- restart=always
```

## 安装插件

后端插件：

- Lombok：简化Java Bean的开发。
- MyBatisX：由MyBatisPlus开发，可以从mapper方法快速定位到XML文件。
- Gitee：用于向码云提交代码。

# 项目初始化

## 项目结构创建

在项目中添加微服务模块（都是SpringBoot项目）。

- 商品模块（Group：`com.example.mall`，Artifact：`mall-product`，Description：商城-商品服务，Package：`com.example.mall.product`）。
- 仓储模块（Group：`com.example.mall`，Artifact：`mall-ware`，Description：商城-仓储服务，Package：`com.example.mall.ware`）。
- 订单模块（Group：`com.example.mall`，Artifact：`mall-order`，Description：商城-订单服务，Package：`com.example.mall.order`）。
- 优惠券模块（Group：`com.example.mall`，Artifact：`mall-coupon`，Description：商城-优惠券服务，Package：`com.example.mall.coupon`）。
- 用户模块（Group：`com.example.mall`，Artifact：`mall-member`，Description：商城-用户服务，Package：`com.example.mall.member`）。

所有模块都导入Spring Web与Openfeign（用于微服务间的互相调用）依赖。

最后在项目（的`pom.xml`文件）中聚合这些模块，并将项目添加到Maven中。

## 数据库初始化

表设计的最大特点是表之间不管关系多复杂，不会建立外键，因为电商系统中数据量超大，外键关联非常耗费数据库性能（插入或删除数据要检查外键，来保证数据的一致性与完整性）。

导入以下SQL到与文件名（不包括后缀名）同名的数据库中：

- [mall_oms.sql](resources/db/mall_oms.sql)（订单系统，对应mall-order模块）
- [mall_pms.sql](resources/db/mall_pms.sql)（商品系统，对应mall-product模块）
- [mall_sms.sql](resources/db/mall_sms.sql)（营销系统，对应mall-coupon模块）
- [mall_ums.sql](resources/db/mall_ums.sql)（用户系统，对应mall-member模块）
- [mall_wms.sql](resources/db/mall_wms.sql)（库存系统，对应mall-ware模块）。

编码统一选择`utf8mb4`（兼容UTF8并解决一些字符乱码问题）。

# 快速开发

## 人人开源搭建后台管理系统

`clone` [renren-fast](https://gitee.com/renrenio/renren-fast)项目，删除.git文件夹，放到工程目录下，加入到项目模块中。

将[db/mysql.sql](renren-fast/db/mysql.sql)导入数据库mall_admin，设置编码为`utf8mb4`。

启动renren-fast模块。这样就可以访问`localhost:8080/renren-fast/`了，但是它还需要前端项目来互相发送请求。

`clone` [renren-fast-vue](https://gitee.com/renrenio/renren-fast-vue)项目，删除.git文件夹，放到工程目录下。前端开发，少不了 Node.js。Node.js 是一个基于 Chrome V8 引擎的 JavaScript 运行环境。下载并安装[Node.js 16.17.0](https://nodejs.org/dist/v16.17.0/node-v16.17.0-x64.msi)。

API参考[Node.js v16.17.0 documentation](https://nodejs.org/dist/latest-v16.x/docs/api/)。这里关注Node.js的npm功能，它是随同 NodeJS 一起安装的包管理工具。功能类似于Maven，能够帮助JavaScript自动下载前端的相关依赖。

检查Node.js的版本：

```sh
node -v
```

配置npm使用淘宝镜像：

```sh
npm config set registry http://registry.npm.taobao.org/
```

进入renren-fast-vue目录下，输入以下命令，下载前端依赖的所有组件：

```sh
npm install
```

这些依赖是通过`package.json`文件指定的，下载完成后所有依赖信息存在于`node_modules`文件夹中。

运行项目：

```sh
npm run dev
```

这样就可以访问前端项目了：`http://localhost:8001`。所有请求都会发送到renren-fast后端。登录账号与密码默认都是`admin`。

## 逆向工程搭建与使用

`clone` [renren-generator](https://gitee.com/renrenio/renren-generator)项目，删除.git文件夹，放到工程目录下，加入到项目模块中。

配置完成后，启动项目。访问`http://localhost`，选中所有表，生成代码。将生成的main文件夹粘贴到gulimall-product模块的src文件夹下。目前用不到前端代码（resources目录下的src文件夹），可以删除它。

创建名为`mall-common`的Maven模块，提供微服务公共需要的功能。在mall-common模块中导入相关依赖，或拷贝renren-fast模块下的相应类，可以使得生成的代码编译通过。