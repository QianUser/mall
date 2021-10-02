# 开发环境

- 操作系统（开发）：Windows 10

- 操作系统（数据库等）：[CentOS 7](https://mirrors.aliyun.com/centos/7/isos/x86_64/CentOS-7-x86_64-Everything-2009.iso?spm=a2c6h.25603864.0.0.74092d1cfur2hR)
- 操作系统静态IP：192.168.227.131
- Java 8
- Maven 3.6.3
- IDE：Intellij IDEA 2020.3.2
- Git：2.37.3

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
