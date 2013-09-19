# 关于soube

这是一个基于dropbox的个人博客引擎，可以轻松创建一个简洁的博客。

* [下载war文件](http://pan.baidu.com/share/link?shareid=2749663359&uk=2366555814 "soube war")
* 默认样式可以参照这个博客:[kurrunk](http://blog.kurrunk.com "kurrunk")

## 运行环境

* 程序运行在java servlet环境，推荐使用tomcat、jetty。
* 需要一个数据库，目前仅支持mysql/mariadb
* 需要您有dropbox帐号，并有开发者的key

## 部署步骤

1. 部署war文件到环境中
2. 设置环境变量 (参见下方)
3. 启动jetty/tomcat后，通过浏览器访问http://yourhostname/admin(会要求登录dropbox);   
   登录后会在`dropbox/apps(中文帐号是"应用")/`下创建soube文件夹。
4. 创建和域名相同的目录在soube目录下，只有这个目录里存放的\*.md文件会同步。
5. 启动mysql服务
6. 点击"工具->初始化数据库"按钮，这个会在您的db中建一个table;
7. 点击同步按钮，这样会把dropbox的\*.md文件同步到db中，站点部署就完成了

## 配置

可以通过设置系统环境变量或JVM环境变量两种方式来配置您的站点

|| *系统环境变量*		||	*JVM环境变量*		||	*说明*														||  
|| DATABASE_URL			||	database.url		|| 	db连接，如:mysql://user:password@127.0.0.1:3306/soube	||  
|| DROPBOX_KEY			||	dropbox.key			||	dropbox key	||  
|| DROPBOX_SECRET		||	dropbox.secret	||	dropbox.secret	||  
|| DROPBOX_UID			||	dropbox.uid			||	dropbox帐号白名单，用","分隔多个uid	||  
|| SITE_NAME				||	site.name				||	blog主题，将会显示在网页的title上	||  
|| SITE_DESCIPTION	||	site.desciption	||	blog简介	||

### vps主机配置

推荐使用jetty。下载jetty并解压后把以上的内容加入到/bin/jetty.sh，示例：

	JAVA_OPTIONS="-Xmx400m -Xms400m -Ddatabase.url=mysql://user:pw@localhost:3306/soube?characterEncoding=UTF-8 -Ddropbox.key=* -Ddropbox.secret=* -Ddropbox.uid=uid1:uname1,uid2:uname2"

把soube.war放入webapps目录里，并命名为`root.war`

	sh bin/jetty.sh start

启动后就能访问到blog了。

### 常见appengine平台的配置方法

#### jelastic

1. 创建environment,选择jetty,数据库用mariadb
2. 上传war文件
3. jetty->config:   
   打开server/variables.conf，内容如下格式:   
	-Ddropbox.key=   
	-Ddropbox.secret=   
	-Ddropbox.uid= #dropbox的uid,这里是同步文章用的   ,多个id请用英文逗号分隔
	\#你的数据库 
	-Ddatabase.url=mysql://user:password@mariadb-*.jelastic.servint.net/*?characterEncoding=UTF-8  
	\#你的站点信息  
	-Dsite.name=myblog  
	-Dsite.desciption=这是我的博客简介  

#### appfog

1. 创建app并绑定一个mysql数据库
2. 上传war文件
3. 配置环境变量(Env Variables):  
   * DROPBOX_KEY   
   * DROPBOX_SECRET   
   * DROPBOX_UID   
   * SITE_NAME   
   * SITE_DESCIPTION   

## 高级使用

soube支持更灵活的皮肤，也支持多博客(需要您有不同的域名)。但这样需要您动一下手，在本地编辑源文件来实现。

您需要 [Leiningen][1] 1.7.0 或者更高版本。

[1]: https://github.com/technomancy/leiningen

编辑`src/soube/config.clj`可以实现多个blog。

在`resources/yourhostname/`可以自定义站点样式。

### 本地运行

在本地启动你的博客，运行:

    lein ring server

### 生成war

	lein ring uberwar soube.war


Copyright © 2013 FIXME
