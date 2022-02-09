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

## Docker安装[MySQL](https://www.mysql.com/)

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

## Docker安装[Redis](https://redis.io/)

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

## Docker安装[ElasticSearch](https://www.elastic.co/)

下载[ElasticSearch](https://hub.docker.com/_/elasticsearch/tags)与[Kibana](https://hub.docker.com/_/kibana/tags)镜像文件：

```sh
docker pull elasticsearch:7.5.2  # 存储与检索数据
docker pull kibana:7.5.2  # 可视化检索数据
```

创建ElasticSearch实例：

```sh
mkdir -p /mydata/elasticsearch/config
mkdir -p /mydata/elasticsearch/data
echo "http.host: 0.0.0.0" >> /mydata/elasticsearch/config/elasticsearch.yml

chmod -R 777 /mydata/elasticsearch/ # 保证权限
docker run --name elasticsearch -p 9200:9200 -p 9300:9300 \  # 9200端口用于接收HTTP请求，9300端口为ElasticSearch在分布式集群状态下节点之间的通信端口
-e "discovery.type=single-node" \
-e ES_JAVA_OPTS="-Xms256m -Xmx1024m" \  # 测试环境下，设置ElasticSearch的初始内存与最大内存，否则内存占用过大
-v /mydata/elasticsearch/config/elasticsearch.yml:/usr/share/elasticsearch/config/elasticsearch.yml \
-v /mydata/elasticsearch/data:/usr/share/elasticsearch/data \
-v /mydata/elasticsearch/plugins:/usr/share/elasticsearch/plugins \  # 以后在容器外面装好插件重启即可
-d --privileged=true elasticsearch:7.5.2
```

访问`9200`端口，可以看到容器已经启动。

创建Kibana实例：

```sh
docker run --name kibana -e ELASTICSEARCH_HOSTS=http://192.168.227.131:9200 -p 5601:5601 \
-d kibana:7.5.2
```

访问`5601`端口，进入可视化界面。

### 安装IK分词器

安装IK分词器：

```sh
cd /mydata/elasticsearch/plugins
wget https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.5.2/elasticsearch-analysis-ik-7.5.2.zip
unzip elasticsearch-analysis-ik-7.5.2.zip -d ik
rm -rf *.zip

# 确认是否安装好了分词器
docker exec -it 55b /bin/bash  # 进入elasticsearch容器内部
cd bin
elasticsearch-plugin list  # 列出系统的分词器

exit
docker restart elasticsearch
```

IK分词器包含两种常用分词器：`ik_smart`与`ik_max_word`。

### 自定义词库

IK分词器的词库不够强大，需要指定远程词库。这里用Nginx保存远程词库：

```sh
cd /mydata
mkdir nginx
docker run -p 80:80 --name nginx -d nginx:1.22.0  # 随便启动一个Nginx实例，只是为了复制出配置
docker container cp nginx:/etc/nginx .  # 将容器内的配置文件拷贝到当前目录

docker stop nginx
docker rm nginx

mv nginx conf
mkdir nginx
mv conf/ nginx/

docker run -p 80:80 --name nginx \  # 创建新的Nginx
-v /mydata/nginx/html:/usr/share/nginx/html \
-v /mydata/nginx/logs:/var/log/nginx \
-v /mydata/nginx/conf:/etc/nginx \
-d nginx:1.22.0
```

```sh
cd html
mkdir es
cd es
vi fenci.txt  # 保存单词
```

修改`/mydata/elasticsearch/plugins/ik/config/IKAnalyzer.cfg.xml`文件：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
        <comment>IK Analyzer 扩展配置</comment>
        <!--用户可以在这里配置自己的扩展字典 -->
        <entry key="ext_dict"></entry>
         <!--用户可以在这里配置自己的扩展停止词字典-->
        <entry key="ext_stopwords"></entry>
        <!--用户可以在这里配置远程扩展字典 -->
        <entry key="remote_ext_dict">http://192.168.227.131/es/fenci.txt</entry>
        <!--用户可以在这里配置远程扩展停止词字典-->
        <!-- <entry key="remote_ext_stopwords">words_location</entry> -->
</properties>
```

重启ElasticSearch：

```sh
docker restart elasticsearch
```

设置Docker启动后容器自动启动：

```sh
sudo docker ps -a  # 查看所有容器
sudo docker update mysql --restart=always
sudo docker update redis --restart=always
sudo docker update elasticsearch --restart=always
sudo docker update kibana --restart=always
sudo docker update nginx --restart=always
```

## 安装插件

后端插件：

- Lombok：简化Java Bean的开发。
- MyBatisX：由MyBatis-Plus开发，可以从mapper方法快速定位到XML文件。
- Gitee：用于向码云提交代码。

前端插件：

- Vue.js。

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

将[mysql.sql](renren-fast/db/mysql.sql)导入数据库mall_admin，设置编码为`utf8mb4`。

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

配置完成后，启动项目。访问`http://localhost`，选中所有表，生成代码。将生成的main文件夹粘贴到mall-product模块的src文件夹下。目前用不到前端代码（resources目录下的src文件夹），可以删除它。

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

## 网关

网关作为流量的入口，常用功能包括路由转发、权限校验、限流控制等。而 [SpringCloud Gateway](https://spring.io/projects/spring-cloud-gateway)作为SpringCloud官方推出的第二代网关框架，取代了Zuul。官方文档见[Spring Cloud Gateway](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)。

### 为什么使用API网关

API网关出现的原因是微服务架构的出现，不同的微服务一般会有不同的网络地址，而外部 客户端可能需要调用多个服务的接口才能完成一个业务需求，如果让客户端直接与各个微服务通信，会有以下的问题：

- 客户端会多次请求不同的微服务，增加了客户端的复杂性。 
- 存在跨域请求，在一定场景下处理相对复杂。 
- 认证复杂，每个服务都需要独立认证。 
- 难以重构，随着项目的迭代，可能需要重新划分微服务。例如，可能将多个服务合 并成一个或者将一个服务拆分成多个。如果客户端直接与微服务通信，那么重构将 会很难实施。 
- 某些微服务可能使用了防火墙或浏览器不友好的协议，直接访问会有一定的困难。

以上这些问题可以借助API网关解决。API网关是介于客户端和服务器端之间的中间层， 所有的外部请求都会先经过API网关这一层。也就是说，API 的实现方面更多的考虑业务 逻辑，而安全、性能、监控可以交由 API 网关来做，这样既提高业务灵活性又不缺安全性。 使用API网关后的优点如下：

- 易于监控。可以在网关收集监控数据并将其推送到外部系统进行分析。 

- 易于认证。可以在网关上进行认证，然后再将请求转发到后端的微服务，而无须在 每个微服务中进行认证。
- 减少了客户端与各个微服务之间的交互次数。

### [核心概念](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#glossary)

- 路由（Route）。路由是网关最基础的部分，路由信息有一个ID、一个目的URL、一组断言和一组Filter组成。如果断言路由为真，则说明请求的URL和配置匹配。
- [断言（Predicate）](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#gateway-request-predicates-factories)。Java8中的断言函数。Spring Cloud Gateway中的断言函数输入类型是Spring5.0框 架中的ServerWebExchange。Spring Cloud Gateway中的断言函数允许开发者去定义匹配来自于HTTP request中的任何信息，比如请求头和参数等。
- [过滤器（Filter）](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#gatewayfilter-factories)。一个标准的 Spring webFilter。Spring Cloud Gateway中的过滤器分为两种类型，分别是Gateway Filter和Global Filter。过滤器将会对请求和响应进行修改处理。

[工作原理](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#gateway-how-it-works)是：客户端发送请求给网关，Gateway Handler Mapping判断该请求是否满足某个路由，满足就发给网关的Gateway Web Handler。这个Gateway Web Handler将请求交给一个过滤器链，请求到达目标服务之前，会执行所有过滤器的`pre`方法。请求到达目标服务处理之后再依次执行所有过滤器的`post`方法。

![网关工作原理](resources\images\网关工作原理.png)

### 创建并测试API网关

创建SpringBoot网关模块（Group：`com.example.mall`，Artifact：`mall-gateway`，Description：API网关，Package：`com.example.mall.gateway`），设置相关配置。

修改启动类的注解：`@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})`，排除数据库有关自动配置（或者在引入依赖时排除MyBatisPlus相关配置），否则启动异常（引入MyBatisPlus相关操作会产生数据源相关的自动配置，而该模块暂时没有用到数据源）。

根据[官方文档过滤器配置](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-addrequestparameter-gatewayfilter-factory)与[官方文档断言配置](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-query-route-predicate-factory)，实现功能：如果请求参数中包含参数`url=baidu`，则转到`www.baidu.com`，如果请求参数中包含`url=qq`，则转到`www.qq.com`。

注意，转发请求时会携带URI。例如访问`localhost:88/hello?url=qq`，会转到`www.qq.com/hello`，这可能会出现404页面。

# 商品服务

## 三级分类

### 递归树形结构数据获取

将[pms_catelog](db/pms_catelog.sql)导入数据库mall_pms。

### 配置网关路由与路径重写

启动`renren-fast`模块；启动前端项目`renren-fast-vue`。

[^备注]: 如果编译报错`You aren‘t using a compiler supported by lombok, so lombok will not work and has been disabled.`，则进入`File`$\rightarrow$`Settings`$\rightarrow$`Build, Execution Deployment`$\rightarrow$`compiler`$，设置`Shared build process VM options`为`-Djps.track.ap.dependencies=false`。

在前端的菜单管理中添加一个一级目录（上级菜单为一级菜单）：商品系统，并在其中添加一个菜单：分类维护，路由为`product/category`（这些信息会记录到`mall_admin`数据库的`sys_menu`表中）。路由的`/`在URL中被替换为`-`，而相应的Vue文件则保存在`src\views\modules`文件夹中。例如以上菜单的URL为`product-category`，Vue文件路径为`src\views\modules\product\category.vue`。

前端项目的请求会发送给网关，网关再将请求转给特定的后端服务。

全部配置完成后，重新启动`renren-fast`模块，并启动`mall-gateway`模块。访问`localhost:8001/#/login`，可以正常进入登录界面（正确生成验证码）。但是无法登录，因为跨域请求被拒绝（403）。

### 网关统一配置跨域

跨域指的是浏览器不能执行其他网站的脚本。它是由浏览器的同源策略造成的，是浏览器对JavaScript施加的安全限制。

同源策略：是指协议，域名，端口都要相同，其中有一个不同都会产生跨域。注意，即使两个请求分别使用了域名与域名对应的IP，它们也属于不同源的请求。

以上访问产生跨域请求为非简单请求（因为`Content-Type`为`application/json`），会先发送预检请求（Request Method：`OPTIONS`）。发送[非简单请求](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/CORS)时，成功跨域的流程如下：

- 浏览器首先发送预检请求（OPTIONS）。
- 服务器响应允许跨域。
- 浏览器发送真实请求。
- 服务器响应数据。

要解决跨域，一种方式是使用Nginx将请求的网站与目标网站部署为同一域。即将前端项目与网关部署为同一域。浏览器通过访问Nginx地址访问前端项目，然后Nginx将静态请求代理到前端项目，将动态请求（带`/api`前缀）代理到网关，网关转到其他服务。

另一种方法是配置当次请求允许跨域，添加响应头：

- `Access-Control-Allow-Origin`：支持哪些来源的请求跨域 。
- `Access-Control-Allow-Methods`：支持哪些方法跨域。
- `Access-Control-Allow-Credentials`：跨域请求默认不包含cookie，设置为`true`可以包含cookie。
- `Access-Control-Expose-Headers`：跨域请求暴露的字段。
  - 跨源资源共享（CORS）请求时，`XMLHttpRequest`对象的`getResponseHeader`方法只能拿到6个基本字段： `Cache-Control`、`Content-Language`、`Content-Type`、`Expires`、`Last-Modified`与`Pragma`。如 果想拿到其他字段，就必须在`Access-Control-Expose-Headers`里面指定。
- `Access-Control-Max-Age`：表明该响应的有效时间为多少秒。在有效时间内，浏览器无 须为同一请求再次发起预检请求。请注意，浏览器自身维护了一个最大有效时间，如果该首部字段的值超过了最大有效时间，将不会生效。

为了便于统一管理，可以在过滤器中配置这些响应头。过滤器放行请求，响应返回给浏览器前添加以上字段。且过滤器只应该在网关中设置。

重启`mall-gateway`与`renren-fast`模块。登录`localhost:8001/#/login`。可以看到发送两个`login`请求。第一个请求是预检请求，响应头中有相应字段（而不仅仅是`content-length: 0`）。

### 功能实现

三级分类的功能包括：

- 树形展示三级分类数据：前端开发使用[ElementUI](https://element.eleme.io/#/zh-CN)组件库：[Tree 树形控件](https://element.eleme.io/#/zh-CN/component/tree)。注意网关的路由配置注意配置顺序，越靠前的配置优先级越高。

- 删除：可以使用ElementUI的[render-content或scoped slot](https://element.eleme.cn/#/zh-CN/component/tree#zi-ding-yi-jie-dian-nei-rong)实现删除效果，这里使用scoped slot。注意：只有一级或二级分类才可以追加节点，只有叶节点才可以被删除。没有子节点的一、二级分类既可以追加节点也可以被删除。
  - 这里采用逻辑删除。所谓逻辑删除，就是通过某个字段表明该条记录是否相当于被删除，而不是直接将该条记录删除。要实现逻辑删除，参考[逻辑删除](https://baomidou.com/pages/6b03c5/)。
  - 使用ElementUI的[Button 按钮](https://element.eleme.io/#/zh-CN/component/button)，实现批量删除效果。

- 新增：使用ElementUI的[Dialog 对话框](https://element.eleme.io/#/zh-CN/component/dialog)实现新增效果。

- 修改：使用同样的对话框实现修改效果，并通过`dialogType`属性确认对话框用于新增还是删除。

- 拖拽效果：要求节点拖拽后其层级不能大于3。
  - 这里使用ElementUI的[Switch 开关](https://element.eleme.io/#/zh-CN/component/switch)与[Button 按钮](https://element.eleme.io/#/zh-CN/component/button)，实现批量拖拽效果。

后面的前端功能同样会使用到ElementUI的组件，不再复述。

## 品牌管理

品牌表见数据库mall_pms的表pms_brand。

在前端的菜单管理中，创建一个菜单：品牌管理，上级菜单为商品系统，路由为`product/brand`。

这里基于之前`mall-product`模块生成的逆向工程前端代码作为模板实现品牌管理前端功能（`mall-product/src/main/resources/src/views/modules/product`目录下的`brand.vue`与`brand-add-or-update.vue`文件）。

### 云存储

在单体系统中，文件上传后可以直接当前放在当前项目中。在分布式系统中，文件上传到一个服务器，一旦获取文件的请求负载均衡到其他服务器，则无法获取到该文件。此时上传文件应统一存储到文件存储服务器中。该文件存储服务器可以自建，例如使用FastDFS、vsftpd，缺点是搭建负载，维护成本高，前期费用高；也可以使用云存储，例如阿里云对象存储、七牛云存储，特点是即开即用，无需维护，按量收费。这里使用阿里云对象存储。

登录[阿里云](https://www.aliyun.com/)，开通`对象存储 OSS`服务。创建存储空间`mall-qian`，

向存储空间中上传文件有两种方式：

- 普通上传方式：用户将文件上传到应用服务器（例如网关$\rightarrow$商品服务），应用服务器将图片传给OSS。账号密码保存在应用服务器，不会暴露给用户。这可能导致应用服务器负担过重，成为瓶颈。
- 服务端签名后上传：用户向应用服务器请求上传的Policy，应用服务器利用阿里云账号与密码生成防伪签名（包含访问阿里云的授权令牌以及上传地址等信息）并返回它，用户将防伪签名以及数据上传给阿里云，由阿里云验证防伪签名并决定是否接受上传请求。这里采用第二种方式。

项目使用的是第二种方式，基本使用方式参考[Java](https://help.aliyun.com/document_detail/32007.html)或[Alibaba Cloud OSS Example](https://github.com/alibaba/aliyun-spring-boot/tree/master/aliyun-spring-boot-samples/aliyun-oss-spring-boot-sample)。这里将其放在SpringBoot第三方模块`mall-third-party`（Group：`com.example.mall`，Artifact：`mall-third-party`，Description：第三方服务，Package：`com.example.mall.thirdparty`，导入Spring Web与Openfeign依赖）中。·

创建子用户 AccessKey，登录名称与显示名称都为`mall`，访问方式选中`编程访问`，并为其添加权限：`AliyunOSSFullAccess`。

服务端签名后直传参考[服务端签名后直传](https://help.aliyun.com/document_detail/31926.html)。

### 文件上传

OSS前后联调测试上传时，注意在阿里云中，开启OSS的跨域访问。跨域规则为：

- 来源：`*`。
- 允许的方法：POST。
- 允许的头：`*`。

### 前端校验

前端的表单校验参考[表单验证](https://element.eleme.io/#/zh-CN/component/form#biao-dan-yan-zheng)与[自定义校验规则](https://element.eleme.io/#/zh-CN/component/form#zi-ding-yi-xiao-yan-gui-ze)。

### 后端校验

后端校验使用JSR303（其规定了数据校验的相关标准），方法如下：

- 给要校验的Bean添加校验注解，并定义自己的`message`提示。这些校验注解位于`javax.validation.constraints`包下（当然还有额外实现）。如果不指定`message`，则默认会取出`ValidationMessages.properties`（针对中文为：`ValidationMessages_zh_CN.properties`）中定义的内容。
- 在需要校验的Bean上添加`javax.validation.Valid`注解，否则校验注解不起作用。还可以在校验的Bean紧跟一个`org.springframework.validation.BindingResult`注解，获取到校验的结果。

如果校验不通过，会有默认的响应，返回的状态码为400。

项目的后端校验逻辑如下：

- 使用`com.example.mall.product.exception.MallExceptionControllerAdvice`统一处理异常（注意取消需要校验的Bean上的`BindingResult`注解，让异常抛出）。
- 使用JSR303分组校验：给校验注解添加属性`groups`，指定什么情况下才需要进行校验，并使用`import org.springframework.validation.annotation.Validated`注解代替`Valid`注解。注意：默认情况下，没有指定分组的校验注解，在分组校验情况下不生效，只会在不分组（`@Validated`不指定`value`）的情况下生效。
- 对于复杂的校验功能，使用JSR303自定义校验注解。方法是：编写自定义校验注解（在JSR303规范中，校验注解必须拥有三个属性：`message`、`groups`与`payload`）与自定义校验器（实现`javax.validation.ConstraintValidator`接口），并关联自定义的校验器与自定义的校验注解（通过在自定义校验注解上指定`@javax.validation.Constraint(validatedBy = [自定义校验器])`）。

## SPU与SKU

标准化产品单元（Standard Product Unit，SPU）是商品信息聚合的最小单位，是一组可复用、易检索的标准化信息的集合，该集合描述了一个产品的特性。

库存量单位（Stock Keeping Unit，SKU） 即库存进出计量的基本单元，可以是以件、盒、托盘等为单位。SKU是大型连锁超市 DC（配送中心）物流管理的一个必要的方法。现在已经被引申为产品统一编号的简称，每种产品均对应有唯一的SKU号。

每个分类下的商品共享规格参数与销售属性，只是有些商品不一定要用这个分类下全部的属性。

- 属性是以三级分类组织起来的。
- 规格参数中有些是可以提供检索的。
- 规格参数也是基本属性，他们具有自己的分组。
- 属性的分组也是以三级分类组织起来的。
- 属性名确定的，但是值是每一个商品不同来决定的。

在mall_pms数据库中：

- pms_attr：保存属性信息。
- pms_attr_group：保存属性分组信息。
- pms_attr_attrgroup_relation：保存属性与属性分组的关联关系。
- pms_product_attr_value：保存商品属性值。
- pms_spu_info：保存SPU的信息。
- pms_sku_info：保存SKU的信息。
- pms_sku_images：保存SKU的图片。
- pms_sku_sale_attr_value：保存SKU的属性值。

将[sys_menus.sql](resources/db/sys_menus.sql)导入数据库mall_admin，完成前端项目的多个菜单的创建。

## 品牌管理

实现品牌管理的分页功能，需要配置分页插件[分页插件](https://baomidou.com/pages/97710a/#paginationinnerinterceptor)。

每个品牌可以关联多个分类，关联信息通过pms_category_brand_relation维护。在该表中，`brand_name`与`catelog_name`冗余存储，因此在修改品牌时，要解决一致性问题。否则会看到，前端修改了品牌名或分类名，查看关联分类时，该品牌名或分类名没有更新。

## 平台属性

### 属性分组

点击三级分类分类，动态展示该分类下的分组属性，它们的关系由pms_attr_group表维护。这里涉及父子组件交互：

- 子组件给父组件节点传递数据，使用事件机制。子组件通过`this.$emit("[事件名]",[携带的数据]...)`给父组件发送数据，父组件通过`@[事件名]='[回调方法]'`接收子组件发送过来的数据并触发自己的回调方法。

支持分组新增、分组修改操作，并使用[Cascader 级联选择器](https://element.eleme.io/#/zh-CN/component/cascader)展示属性分组的三级分类。

每个属性分组关联多个规格参数（非销售属性），可以查询、添加或删除分组关联的属性。分组与属性的关联关系由pms_attr_attrgroup_relation表维护。注意，每个分组只能关联本分类下的未被其他分组关联的属性，这些信息可以通过pms_attr_group、pms_attr_attrgroup_relation、pms_attr表得到。

### 规格参数

新增规格参数（由pms_attr表维护）时，注意绑定它的属性分组，这是通过pms_attr_attrgroup_relation表维护的。同样，显示与修改规格参数列表需要获取规格参数所属的分类与分组（修改规则参数列表需要查询分类的完整路径）。

这里通过VO接受前端传递过来的对象：

- 持久对象（Persistant Object，PO）：对应数据库中某个表中的一条记录。 PO 中应该不包含任何对数据库的操作。
- 领域对象（Domain Object，DO）：从现实世界中抽象出来的有形或无形的业务实体。
- 数据传输对象（Transfer Object，TO）：不同的应用程序之间传输的对象。
- 数据传输对象（Data Transfer Object，DTO）：这个概念来源于 J2EE的设计模式，原来的目的是为了EJB的分布式应用提供粗粒度的 数据实体，以减少分布式调用的次数，从而提高分布式调用的性能和降低网络负载，但在这里，泛指用于展示层与服务层之间的数据传输对象。
- 值对象（Value Object，VO）：通常用于业务层之间的数据传递，和PO一样也是仅仅包含数据而已，但应是抽象出的业务对象，可以和表对应，也可以不对应。在这里可以称VO为视图对象（View Object），它接受页面传递来的数据封装其为对象，同时将业务处理完成的对象封装成页面要用的数据。
- 业务对象（Business Object，BO）：主要作用是把业务逻辑封装为一个对象。这个对象可以包括一个或多个其它的对象。 比如一个简 历，有教育经历、工作经历、社会关系等等。 可以把教育经历对应一个PO ，工作经历对应一个PO ，社会关系对应一个PO 。 建立一个对应简历的BO对象处理简历，每 个BO包含这些PO 。 这样处理业务逻辑时，我们就可以针对BO去处理。
- 简单的Java对象（Plain Ordinary Java Object，POJO）：传统意义的Java对象。POJO 是DO、DTO、BO、VO的统称。
- 数据访问对象（Data Access Object，DAO）：负责持久层的操作，为业务层提供接口。

### 销售属性

销售属性与规格参数类似。这些属性都由pms_attr表维护，其中`attr_type`字段决定了该字段表示销售属性（`0`）还是规格参数（`1`）。不考虑某个字段既是销售属性又是规格参数的情况。另外，销售属性没有分组信息。

[^纠错]: 导入所有前端项目后，执行命令：`npm install -g cnpm --registry=https://registry.npm.taobao.org`与`cnpm install --save pubsub-js`。

## 商品维护

### 发布商品

进入发布商品页面，首先会获取会员等级，由`mall-member`模块处理请求。

填写基本信息，选择分类时，会要求获取分类关联的品牌。基本参数填写完成，进入下一步，会请求查出当前分类下的所有属性分组与每个属性分组下的所有属性。

[^备注]: 这里采用如下设计原则：Controller：处理请求，接收与校验数据；2、Service接收Controller传来的数据，进行业务处理；3、Controller接收Service处理完的数据，封装页面指定的vo。

录入商品的基本信息、规格参数与销售属性后，前端会将这些信息全部发送给后端，后端负责保存所有信息。一些信息需要调用其他服务的控制器完成保存，这些信息保存为TO，通过JSON格式传递给其他服务。当`A`服务的`a`方法需要给远程服务`B`发送请求，执行如下操作：

- 找到`B`服务，给相应的路径发送请求，此时会将标注`@RequestBody`的对象转换为JSON，放在请求体位置。
- `B`服务收到请求，通过`@RequestBody`将请求体的JSON转换为对象。

只要JSON数据模型是兼容的。双方服务无需使用同一个TO。

使用OpenFeign发送请求时，可以让所有请求过网关（带上`/api`前缀），也可以直接让后台指定服务处理，项目采用后一种方法。

[^备注]: 复制前端要提交的所有数据，然后可以通过[BEJSON](https://www.bejson.com/)等网站或工具自动将这些JSON数据转换为VO，然后作适当调整。另外可能需要后端校验这些数据。
[^备注]: 在IDEA中，打开`Run/Debug Configurations`，添加`Compound`配置，可以管理多个服务。

### SPU管理与商品管理

这里注意要设置前端页面的时间展示格式。另外，如果前端未提交分类、品牌等信息，则不要求条件查询。

# 库存系统

[^纠错]: 之前包复制到了错误的模块。

库存系统包括：

- 仓库维护：可以对仓库执行增删改查操作。
- 商品库存：可以对仓库库存信息执行增删改查操作。
- 采购单维护：库存一般通过采购单新增，而不是直接从商品库存中增删改查。要想生成采购单，首先需要填写采购需求。采购流程如下：
  - 人工创建采购需求，或库存预警创建采购需求。
  - 人工合并，或系统定时合并采购需求到（新建或已分配的）采购单。
  - 将采购单分配给采购人员，通知供应商供货，或者自主采购。
  - 采购单入库。
  - 为成功采购的项目添加库存。

[^备注]: 如果在`商品维护`的`spu管理`界面对每个SPU执行`规格`操作报错：找不到页面，则在mall_admin数据库下执行：`INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES (76, 37, '规格维护', 'product/attrupdate', '', 1, 'log', 0);`。

# 商城业务

创建SpringBoot模块（Group：`com.example.mall`，Artifact：`mall-search`，Description：ElasticSearch检索服务，Package：`com.example.mall.search`），导入Spring Web依赖。

项目使用[Java REST Client](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/7.5/index.html)客户端与ElasticSearch通信，且使用其[Java High Level REST Client](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/7.5/java-rest-high.html)。

从[加载示例数据](https://www.elastic.co/guide/cn/kibana/current/tutorial-load-dataset.html)处导入[accounts.zip](https://download.elastic.co/demos/kibana/gettingstarted/accounts.zip)并测试：

```sh
POST /bank/_bulk
# 复制批量数据到这里
```

## 商品上架

上架的商品才可以在网站展示。

上架的商品需要可以被检索（即保存到ElasticSearch）。

为了节省内存，ElasticSearch中只保存商品有用的信息，不保存图片等信息。这些信息的保存格式类似：

```json
{
    "skuId": 1,
    "spuId": 11,
    "skuTitle": "苹果",
    "price": 4998,
    "saleCount": 99,
    "attrs": {
        "尺寸": "5寸",
        "CPU": "A14",
        "分辨率": "全高清"
    }
}
```

这种格式会浪费一点内存，但方便检索。

或者：

```json
{
    "skuId": 1,
    "spuId": 11,
    "skuTitle": "苹果",
    "price": 4998,
    "saleCount": 99,
}

{  // 将attrs字段单独保存，通过spuId检索
    "spuId": 11,
    "attrs": {
        "尺寸": "5寸",
        "CPU": "A14",
        "分辨率": "全高清"
    }
}
```

这种格式保存的信息不冗余，同时方便检索。但是效率不如第一种，考虑如下情况：用户检索`小米`，检索出10000个商品，涉及4000个SPU；查出4000个SPU对应的所有属性，这样一个请求就会发送$32KB(8B \times 4000)$数据，并发情况下性能会很差。

这里采用方案1，发送给ElasticSearch检索（`PUT product`）的数据如下：

```json
{
  "mappings": {
    "properties": {
      "skuId": {
        "type": "long"
      },
      "spuId": {
        "type": "keyword"
      },
      "skuTitle": {
        "type": "text",
        "analyzer": "ik_smart"
      },
      "skuPrice": {
        "type": "keyword"
      },
      "skuImg": {
        "type": "keyword",
        "index": false,
        "doc_values": false
      },
      "saleCount": {
        "type": "long"
      },
      "hasStock": {
        "type": "boolean"  // 有无库存，这里不记录库存数，因为维护库存数代价大（需要频繁更新）
      },
      "hotScore": {
        "type": "long"
      },
      "brandId": {
        "type": "long"
      },
      "catalogId": {
        "type": "long"
      },
      "brandName": {
        "type": "keyword",
        "index": false,
        "doc_values": false
      },
      "brandImg": {
        "type": "keyword",
        "index": false,
        "doc_values": false
      },
      "catalogName": {
        "type": "keyword",
        "index": false,
        "doc_values": false
      },
      "attrs": {
        "type": "nested",
        "properties": {
          "attrId": {
            "type": "long"
          },
          "attrName": {
            "type": "keyword",
            "index": false,
            "doc_values": false
          },
          "attrValue": {
            "type": "keyword"
          }
        }
      }
    }
  }
}
```

说明：

- `index`：默认为`true`。如果为`false`，表示该字段不会被索引，但是检索结果里面有，但字段本身不能当做检索条件
- `doc_values`：默认`true`，设置为`false`，表示不可以做排序、聚合以及脚本操作，这样更节省磁盘空间；可以通过设定`doc_values`为`true`，`index`为`false`来让字段不能被搜索但可以用于排序、聚合以及脚本操作。
- `"type": "nested"`：[防止数组扁平化](https://www.elastic.co/guide/en/elasticsearch/reference/7.5/nested.html)。

在`商品系统`$\rightarrow$`商品维护`$\rightarrow$`spu管理`中，每个商品可以上架。

[^0]: 将商品服务端口改为10001。

## 首页

### 整合Thymeleaf渲染首页

将控制器拆分为两个部分：

- 和Web页面有关的放在`web`包下。
- REST接口放在`app`包下。

整合[Thymeleaf](https://www.thymeleaf.org/)渲染首页时：

- 关闭Thymeleaf缓存，开发期间能看到实时效果。
- 静态资源都放在`src/main/resources/static`文件夹下，按照路径直接访问。
- 页面放在`src/main/resources/templates`文件夹下。直接访问`http://localhost:10001/`，即可看到首页。SpringBoot访问项目默认会找`index.html`（见`org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration`配合类）。

### 渲染分类数据

Thymeleaf使用参考[Tutorial: Using Thymeleaf](https://www.thymeleaf.org/documentation.html)，或者直接下载[PDF](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.pdf)。

使用Thymeleaf后，如果页面修改，则必须要重启服务才能看到效果。要想不重启，则引入Devtools工具，然后重新编译项目（快捷键：`Ctrl+F9`）或当前资源（快捷键：`Ctrl+Shift+F9`），前提是要关闭Thymeleaf缓存。如果类或配置被改，则推荐重启服务。

## 反向代理配置

为主机（IP：`192.168.227.131`）设置域名：`mall.com`（可以通过修改hosts文件实现）。

将所有来自`mall.com`的所有请求都通过Nginx反向代理到网关：

```sh
cd /mydata/nginx/conf/conf.d
cp default.conf mall.conf
vi mall.conf
```

修改如下配置项：

```nginx
server_name mall.com;

location / {
    proxy_set_header Host $host;  # Nginx代理给网关会丢失请求的host信息，这里需要主动添上
    proxy_pass http://mall;
}
```

```sh
vi ../nginx.conf
```

在`http`中添加如下配置项：

```nginx
upstream mall {
    server 192.168.227.1:88;  # 本机（Windows 10）IP地址
}
```

重启Nginx：

```sh
docker restart nginx
```

配置网关路由规则（注意配置在最后）。

访问`mall.com`，可以看到首页。

# 性能压测

性能压测工具：

- [Apache JMeter](https://jmeter.apache.org/)
- JDK工具：JConsole与JVisualVM

优化方面：

- 中间件越多，性能损失越大，大多都损失在网络交互了。

- MySQL优化：例如给mall_pms数据库的`pms_category`表的`parent_cid`字段加普通索引；`com.example.mall.product.service.impl.CategoryServiceImpl#getCatalogJson`方法一次查询出所有需要的数据然后过滤，而不是多次查询数据库。

- 模板的渲染速度（缓存）：服务上线后，开启Thymeleaf缓存。

- 动静分离。将项目的静态资源放在Ngnx中：

  ```sh
  cd /mydata/nginx/html
  mkdir static
  ```

  将项目的`mall-product/src/main/resources/static/index`文件夹移动到虚拟机以上的`static`目录中。然后修改`mall-product/src/main/resources/templates/index.html`中的超链接路径。

  ```sh
  cd /mydata/nginx/conf/conf.d
  vi mall.conf
  ```

  在`server`块中，添加：

  ```nginx
  location /static/ {
      root /usr/share/nginx/html;
  }
  ```

  ```sh
  docker restart nginx
  ```

  然后访问`mall.com`可以看到首页，但是访问`localhost:10001`则无法访问静态资源。

- 调高日志级别，不要设置为`DEBUG`级别。

- 优化JVM：设置合理的`-Xmx`、`-Xms`与`-Xmn`虚拟机选项。如果这些值设置过小，可能导致压力测试时内存崩溃。

# 缓存

为了系统性能的提升，我们一般都会将部分数据放入缓存中，加速访问。而数据库承担数据落盘工作。

以下数据适合放入缓存：

- 即时性、数据一致性要求不高的数据。
- 访问量大且更新频率不高的数据（读多写少）。

在使用缓存的情况下，查询时优先读取缓存中数据，如果命中则返回结果，否则查询数据库，将数据放入缓存，再返回结果。

分布式系统要使用分布式缓存，不适合使用本地缓存（读无法共享缓存，写无法保证多个本地缓存的一致性）。这里整合Redis作为缓存。

注意，在开发中，凡是放入缓存中的数据我们都应该指定过期时间，使其可以在系统即使没 有主动更新数据也能自动触发数据加载进缓存的流程。避免业务崩溃导致的数据永久不一致问题。

## 缓存击穿

在高并发情况下，缓存会出现缓存穿透、缓存雪崩、缓存击穿等问题。这里通过加锁解决缓存击穿问题。

在分布式情况下，（本地）锁只对当前实例有效，多个实例会有多个锁。在本例中，这个问题不大，只是造成数据库压力大了一点。

## 分布式锁

采用Redis的`SETNX`命令模拟分布式锁操作。注意，要设置锁的过期时间，且它与`SETNX`尝试获取锁的操作组成原子操作，防止锁未释放导致死锁等情况。

为了防止锁过期后，其他线程趁虚而入删除不属于自己的锁，需要在占锁时存入自己的标识（例如UUID），每个线程只能删除自己的锁。此时，删除锁时需要获取锁的值，判断锁是不是自己的，如果是自己的，则删除该锁，这些步骤也必须是原子操作。这里使用Lua脚本实现原子操作。

### Redisson

这里使用[Redisson](https://github.com/redisson/redisson)实现分布式锁。Redisson实现的锁的部分特点如下：

- 分布式锁。

- 锁可重入。
- 实现锁的自动续期机制。如果业务超长，运行期间自动锁上新的30s。不用担心业务时间长，锁自动过期被删掉。
- 加锁的业务只要运行完成，就不会给当前锁续期，即使不手动解锁（例如业务宕机），锁默认会在30s内自动过期，不会产生死锁问题。注意，如果给锁指定了时间，则不会自动续期，因此此时要确保解锁时间大于业务执行时间。

Redisson支持的锁参考[分布式锁（Lock）和同步器（Synchronizer）](https://github.com/redisson/redisson/wiki/8.-分布式锁和同步器)。

### 缓存一致性

为了确保缓存一致性，可以采用两种模式：

- 双写模式：每次更新数据库都更新缓存。如果线程1先于线程2写数据库，后于线程2写缓存，则发生不一致问题。
- 失效模式：更新数据库时让缓存失效，查询时再更新缓存。同样也会发生不一致问题。

这两种模式下，都会发生不一致问题。可以加锁解决不一致问题，或者考虑是否容忍不一致，只要缓存过期，总能保证最终一致性：

- 如果是用户纬度数据（订单数据、用户数据），这种并发几率非常小，不用考虑这个问题，缓存数据加上过期时间，每隔一段时间触发读的主动更新即可。
- 如果是菜单、商品介绍等基础数据，也可以使用Canal订阅binlog的方式更新缓存。
- 缓存数据$+$过期时间也足够解决大部分业务对于缓存的要求。
- 通过加锁保证并发读写，写写的时候按顺序排好队，读读无所谓。所以适合使用读写锁。（业务不关心脏数据，允许临时脏数据则可忽略锁）。

如果数据经常修改，则无需使用缓存，直接读数据库。

总之，我们能放入缓存的数据本就不应该是实时性、一致性要求超高的。所以缓存数据的时候加上过期时间，保证每天拿到当前最新数据即可。我们不应该过度设计，增加系统的复杂性。不应该过度设计，增加系统的复杂性。

后面实现的系统的一致性解决方案：

- 缓存的所有数据都有过期时间，数据过期下一次查询触发主动更新。
- 读写数据的时候，加上分布式的读写锁。 只要写少，对性能影响不大。

## [Spring Cache](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)

Spring从3.1开始定义了`org.springframework.cache.Cache`与`org.springframework.cache.CacheManager`接口来统一不同的缓存技术； 并支持使用JCache（JSR-107）注解简化开发：

`Cache`接口为缓存的组件规范定义，包含缓存的各种操作集合。Cache接口下Spring提供了各种`xxxCache`的实现，如`RedisCache`、`EhCacheCache`、`ConcurrentMapCache`等。

每次调用需要缓存功能的方法时，Spring会检查检查指定参数的指定的目标方法是否已经被调用过。如果是就直接从缓存中获取方法调用后的结果（不会调用该方法），如果没有就调用方法并缓存结果后返回给用户。要想缓存方法返回结果，只需要在方法上添加`org.springframework.cache.annotation.Cacheable`注解即可。默认情况下，缓存键自动生成（`[缓存名]::SimpleKey []`），采用JDK序列化机制将序列化后的值存到数据库，默认TTL为-1。

这里使用Redis作为缓存：存储统一类型的数据，指定为同一分区（即`@Cacheable`指定相同的`value`），从而实现批量删除；分区名默认就是缓存的前缀。

要想让Spring Cache解决缓存击穿问题，则在`@Cacheable`中设置`sync=true`（只是加了本地锁），对于其他注解，则不会加锁。因此，常规数据（读多写少，即时性与一致性要求不高的数据）可以使用Spring Cache，只要缓存的数据有过期时间即可；特殊数据则特殊设计。

# 商城业务

## 检索服务

将`mall-search/src/main/resources/static/search`目录下的所有资源放到虚拟机的`/mydata/nginx/html/static/search`目录下。

为主机（IP：`192.168.227.131`）设置域名：`search.mall.com`。

修改`/mydata/nginx/conf/conf.d/mall.conf`的`server.server_name`配置：

```nginx
server_name mall.com *.mall.com;
```

然后修改网关配置。

商品检索有三个入口：

- 主页选择分类进入商品检索。
- 主页输入检索关键字展示检索页。
- 进入搜索页后，选择筛选条件。

检索条件与筛选条件包括：

- 全文检索：`skuTitle`。
- 排序：`saleCount`、`hotScore`、`skuPrice`。
- 过滤：`hasStock`、`skuPrice`区间、`brandId`、`catalogId`、`attrs`。
- 聚合：`attrs`。

完整的参数示例：

```
keyword=小米&sort=saleCount_desc/asc&hasStock=0/1&skuPrice=400_1900&brandId=1
&catalogId=1&attrs=1_3G:4G:5G&attrs=2_骁龙 845&attrs=4_高清屏
```



# 参考

[^1]: [Java项目《谷粒商城》Java架构师 | 微服务 | 大型电商项目](https://www.bilibili.com/video/BV1np4y1C7Yf)
[^1]: 资料：[谷粒商城](https://pan.baidu.com/s/18FuF760AYt3kILGWCmXVEA#list/path=%2F)，提取码：yyds