# TranInfoFromCheci
从网上抓取列车各个站点停靠信息
1、	准备工作：
1.1	需要先建立三张数据库表，用于存储数据，mysql的数据库：
1.2	tbl_tran_info3存放结果数据、tbl_tran_list存放列车列表、tbl_tran_list_unknow存放未查到的数据。建表使用的ddl见 script文件夹下
2、	使用程序：
2.1 程序分为两部分，列车列表收集和列车详情收集
2.2 列车列表收集需要运行的主函数在service包的TranListService。可以直接运行，等待程序收集完毕，存放到tbl_tran_list里面
2.3 列车详情收集在main包下Main函数，可以直接运行，它根据数据库里面的列车列表，然后在网上收集列车的停靠信息
3、	程序说明：
3.1 列车列表收集实现方法如下：
根据12306的js文件：https://kyfw.12306.cn/otn/resources/js/query/train_list.js?scriptVersion=1.0，获取到它列车的列表、始发站，和终点站信息。将数据存入到数据库
3.2列车详细停靠信息收集方法如下：
根据数据库的列表，
从https://www.huoche.net/checi/ 后面跟上车的名字 比如
https://www.huoche.net/checi/K1234 来查询车次K1234的停靠信息 。对方返回html页面，解析html页面的数据。对方没有查询到数据，则去http://checi.114piaowu.com/ 网查询信息，和上面一样。http://checi.114piaowu.com/K1234  来查询列车为K1234的信息，然后解析返回的html页面。 如果还是没有查到数据，则去12306上获取数据：https://kyfw.12306.cn/otn/czxx/queryByTrainNo?train_no=" + tranNo +
                "&from_station_telecode=" + startStation +
                "&to_station_telecode=" + endStation +
                "&depart_date=" + queryData; 这样则可以获取到数据。最终将查询到的信息汇总到数据库结果表中，如果实在查不到，则将数据插入到失败的结果表中。

其中两个网站是https要关闭ssl验证。
网络不太好，使用了如果连接超时则再次连接。。
把可能出错的地方程序都catch到，根据不同原因，做不同的处理
