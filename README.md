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
- MyBatisX：由MyBatis-Plus开发，可以从mapper方法快速定位到XML文件。
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

## 配置与测试微服务基本CRUD功能

整合MyBatis-Plus参考[快速开始](https://baomidou.com/pages/226c21/#%E9%85%8D%E7%BD%AE)。

注意[驱动版本与数据库版本要兼容](https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-versions.html)。

# 分布式组件

本项目，结合[Spring Cloud](https://spring.io/projects/spring-cloud)与[SpringCloud Alibaba](https://github.com/alibaba/spring-cloud-alibaba)（中文文档：[Spring Cloud Alibaba](https://github.com/alibaba/spring-cloud-alibaba/blob/master/README-zh.md)），微服务的技术搭配方案如下：

- SpringCloud Alibaba - Nacos：注册中心（服务发现/注册）。
- SpringCloud Alibaba - Nacos：配置中心（动态配置管理）。
- SpringCloud - Ribbon：负载均衡。
- SpringCloud - Feign：声明式HTTP客户端（调用远程服务）。
- SpringCloud Alibaba - Sentinel：服务容错（限流、降级、熔断）。
- SpringCloud - Gateway：API网关（webflux 编程模式）。
- SpringCloud - Sleuth：调用链监控。
- SpringCloud Alibaba - Seata：原Fescar，即分布式事务解决方案。

使用Spring Cloud Alibaba的原因在于，SpringCloud的几大痛点：

- SpringCloud部分组件停止维护和更新，给开发带来不便。

- SpringCloud部分环境搭建复杂，没有完善的可视化界面，我们需要大量的二次开发和定制。

- SpringCloud配置复杂，难以上手，部分配置差别难以区分和合理应用。

SpringCloud Alibaba的优势：

- 阿里使用过的组件经历了考验，性能强悍，设计合理，现在开源出来大家用。
- 成套的产品搭配完善的可视化界面给开发运维带来极大的便利。
- 搭建简单，学习曲线低。

SpringCloud Alibaba的[全线产品](https://spring.io/projects/spring-cloud-alibaba)也在SpringCloud官方网站中介绍。

## Nacos注册中心

参考[Nacos Discovery Example](https://github.com/alibaba/spring-cloud-alibaba/blob/master/spring-cloud-alibaba-examples/nacos-example/nacos-discovery-example/readme-zh.md)。

`spring-cloud-starter-alibaba-nacos-discovery`使用`2.1.0.RELEASE`版本的，如果不指定版本可能导致MyBatis-Plus报错。

Nacos Server使用2.1.1版本，运行在Windows环境下。

进入Nacos Server的bin目录，使用`startup.cmd -m standalone`命令启动Nacos Server（直接双击startup.cmd文件可能出错）。

访问`http://localhost:8848/nacos`，登录注册中心。用户名与密码默认都是`nacos`。

[^纠错]: 引入`spring-cloud-alibaba-dependencies`依赖。

## OpenFeign测试远程调用

Feign是一个声明式的HTTP客户端，它的目的就是让远程调用更加简单。Feign提供了HTTP请求的模板，通过编写简单的接口和插入注解，就可以定义好HTTP请求的参数、格式、地址等信息。

Feign整合了Ribbon（负载均衡）和Hystrix（服务熔断），可以让我们不再需要显式地使用这两个组件。

SpringCloudFeign在NetflixFeign的基础上扩展了对SpringMVC注解的支持，在其实现下，我们只需创建一个接口并用注解的方式来配置它，即可完成对服务提供方的接口绑定。简化了SpringCloudRibbon自行封装服务调用客户端的开发量。

要想实现远程调用，首先在调用远程服务的模块中引入`spring-cloud-starter-openfeign`与`spring-cloud-starter-loadbalancer`依赖。

然后编写一个接口，通过`@FeignClient("[远程服务名]")`注解告诉SpringCloud这个接口需要调用远程服务。接口的每一个方法用于调用远程服务的特定请求，通过方法的完整签名（完整的`@RequestMapping`路径）指定要调用哪个请求。

最后，在调用远程服务的模块的启动类上指定`@EnableFeignClients(basePackages="[远程调用接口所在的全包名]")`注解，开启远程调用功能。

然后就可以调用该接口获取所需请求。

如果被调用服务未上线（例如直接关闭它），则服务注册中心中它的健康实例数将为0，且访问该服务出现异常。重新上线，则恢复正常。

## Nacos配置中心

参考[Nacos Config Example](https://github.com/alibaba/spring-cloud-alibaba/blob/master/spring-cloud-alibaba-examples/nacos-example/nacos-config-example/readme-zh.md)。注意引入`spring-cloud-starter-bootstrap`依赖。

服务启动默认会读取`[服务名].properties`配置文件。可以给配置中心默认添加一个数据集（Data Id），命名规则为：`[服务名].properties`，并在其中添加任何配置。

要动态获取并刷新配置，则需要两个注解：

- 在对应的`Controller`上添加`org.springframework.cloud.context.config.annotation.RefreshScope`注解，用于动态获取并刷新配置。

- 使用`@org.springframework.beans.factory.annotation.Value;("${[配置项名称]}")`注解获取配置。

如果配置中心和当前应用的配置文件中都配置了相同的项，优先使用配置中心的配置。

### 命名空间与配置分组

#### 命名空间

命名空间用于进行租户粒度的配置隔离。不同的命名空间下，可以存在相同的 Group 或 Data ID的配置。Namespace 的常用场景之一是不同环境的配置的区分隔离，例如开发、测试与生产环境的资源（如配置、服务）隔离等。

默认新增的所有配置都在`public`空间。在配置中心中可以管理命名空间以及相应配置。

应用默认使用`public`空间中的配置，要想切换命名空间，在`bootstrap.properties`中指定：

```properties
spring.cloud.nacos.config.namespace=[命名空间的UUID]
```

命名空间的作用/使用：

- 开发、测试、生产：利用命名空间来做环境隔离。
- 每一个微服务之间互相隔离配置，每一个微服务都创建自己的命名空间，只加载自己命名空间下的所有配置。

#### 配置集

一组相关或者不相关的配置项的集合称为配置集。在系统中，一个配置文件通常就是一个配置集，包含了系统各个方面的配置。例如，一个配置集可能包含了数据源、线程池、日志级别等配置项。

#### 配置集ID

Nacos中的某个配置集的ID。配置集ID（即Data ID）是组织划分配置的维度之一。Data ID通常用于组织划分系统的配置集。一个系统或者应用可以包含多个配置集，每个配置集都可以被一个有意义的名称标识。Data ID通常采用类Java包（如`com.taobao.tc.refund.log.level`）的命名规则保证全局唯一性。此命名规则非强制。

#### 配置分组

Nacos中的一组配置集，是组织配置的维度之一。通过一个有意义的字符串（如 `Buy`或`Trade`）对配置集进行分组，从而区分Data ID相同的配置集。当在Nacos上创建一个配置时，如果未填写配置分组的名称，则配置分组的名称默认采用`DEFAULT_GROUP `。配置分组的常见场景：不同的应用或组件使用了相同的配置类型，如`database_url`配置和`MQ_topic`配置。

要想切换使用的组，在`bootstrap.properties`中指定：

```properties
spring.cloud.nacos.config.group=[配置分组名称]
```

在本项目中，将会为每个微服务创建自己的命名空间，使用配置分组区分环境，如dev、test、prod环境。配置集ID采用微服务应用名。

### 加载多配置集

要加载多配置集（例如将数据源配置、框架配置与微服务其他配置拆分开来形成多配置集），在`bootstrap.properties`中指定：

```properties
spring.application.name=[应用名]
spring.cloud.nacos.config.server-addr=[配置中心地址]
spring.cloud.nacos.config.namespace=[命名空间的UUID]

# 加载的默认配置文件所属的分组
spring.cloud.nacos.config.group=[配置分组名称]

# 加载第一个配置集
spring.cloud.nacos.config.ext-config[0].data-id=[配置集ID 1]
spring.cloud.nacos.config.ext-config[0].group-id=[配置分组名称 1]
# 是否动态刷新，默认为false，此时应用不会获取到配置中心的最新修改
spring.cloud.nacos.config.ext-config[0].refresh=true

# 加载第二个配置集
spring.cloud.nacos.config.ext-config[1].data-id=[配置集ID 2]
spring.cloud.nacos.config.ext-config[1].group-id=[配置分组名称 2]
spring.cloud.nacos.config.ext-config[1].refresh=true

# 加载第三个配置集
spring.cloud.nacos.config.ext-config[2].data-id=[配置集ID 3]
spring.cloud.nacos.config.ext-config[2].group-id=[配置分组名称 3]
spring.cloud.nacos.config.ext-config[2].refresh=true
```

注意：如果配置中心中找不到相应配置选项，则使用本地配置文件中的配置；微服务启动会读取`[服务名].properties`配置文件，如果没有为其指定分组，则默认其位于`public`组。

微服务任何配置信息、任何配置文件都可以放在配置中心中，只需要在`bootstrap.properties`中说明加载配置中心中哪些配置文件即可。以前SpringBoot任何方法从配置文件中获取值的方法（例如`@Value`、`@ConfigurationProperties`），都能使用。配置中心有的优先使用配置中心中的。