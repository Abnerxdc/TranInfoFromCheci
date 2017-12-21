package service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import core.ConfigManager;
import dao.DbManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import util.DbQueryUtil;
import util.HttpClientUtils;

import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Created by Administrator on 2017/9/5.
 */
public class TranInfoService {
    Logger logger = Logger.getLogger(TranInfoService.class);
    private final static Long sleepTime = Long.parseLong(ConfigManager.getInstance().getSleepTime());
    private final static Long forbiddenSleepTime = Long.parseLong(ConfigManager.getInstance().getForbiddenSleepTime());


    /**
     * 从数据库中获取所有列车列表，查询信息，得到结果并插入到详情数据库
     *
     */
    public void getDateFromDb(){
        String sql = "select * from tbl_tran_list order by id desc";
        try (Connection con = DbManager.getConnection()){
            JSONArray arr = DbQueryUtil.getInstance().selectJSONArrayMap(con,sql);
            for (int i=0;i<arr.size();i++){
                JSONObject obj = arr.getJSONObject(i);
                String tran_name = obj.getString("tran_name");
                String starting_station = obj.getString("starting_station");
                String ending_station = obj.getString("ending_station");
                String train_no = obj.getString("train_no");
                totalDate(tran_name,starting_station,ending_station,train_no);
            }
        }catch (Exception e){
            logger.error("从数据库里面获取数据失败",e);
        }
    }


    /**
     * 将数据汇总,被插入方法调用
     * @param tran  火车的名字  k123之类
     * @param starting_station  始发站  特用于12306查询
     * @param ending_station 终点站   特用于12306查询
     * @param train_no  列车的编号  特用于12306查询
     */
    public void totalDate(String tran,String starting_station,String ending_station,String train_no){
        JSONArray array = getTranInfoFromStr(tran);
        //获取当前时间
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime nowDateTime = LocalDateTime.now();
        String trigger_time = dateTimeFormatter.format(nowDateTime);
        if(!array.isEmpty()){
            insertDateV2(tran,trigger_time,array);
        }else {
            logger.warn("根据该tran 从https://www.huoche.net/checi/网上未查询到信息，正在尝试从http://checi.114piaowu.com/获取数据 标记: "+tran);
            String url = "http://checi.114piaowu.com/"+tran;
            array = getTranTimeByUrl(url);
            if(!array.isEmpty()){
                insertDateV2(tran,trigger_time,array);
            }else{
                logger.warn("根据该tran 从http://checi.114piaowu.com/网上也未查询到信息，正在尝试从1206获取数据  标记一下 :"+tran);
                //从12306获取需要传入时间参数，就传入未来几天的吧，拿取其中有参数的那一天为准
                DateTimeFormatter dateTimeFormatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate nowDate = LocalDate.now();
                LocalDate tomorrow = nowDate.plus(1, ChronoUnit.DAYS);
                String tomorrow_date= dateTimeFormatter2.format(tomorrow);
                array = getEveryTranInfo(train_no,starting_station,ending_station,tomorrow_date);
                if(array.isEmpty()){
                    LocalDate after = tomorrow.plus(1, ChronoUnit.DAYS);
                    String after_date= dateTimeFormatter2.format(after);
                    array = getEveryTranInfo(train_no,starting_station,ending_station,after_date);
                    if(array.isEmpty()){
                        LocalDate after2 = after.plus(1, ChronoUnit.DAYS);
                        String after2_date= dateTimeFormatter2.format(after2);
                        array = getEveryTranInfo(train_no,starting_station,ending_station,after2_date);
                        if(array.isEmpty()){
                            logger.warn("从12306也未查到该车次的信息，作为遗失数据 。特此标记-----------------"+tran);
                            String sql = "insert into tbl_tran_list_unknow (tran_name,starting_station,ending_station,tran_no,trigger_time,status)" +
                                    "values('"+tran+"','"+starting_station+"','"+ending_station+"','"+train_no+"','"+trigger_time+"','unprocessed')" +
                                    "on DUPLICATE KEY UPDATE tran_name = '"+tran+"', starting_station = '"+starting_station+"',ending_station = '"+ending_station+"'," +
                                    "trigger_time = '"+trigger_time+"',status='unprocessed'";
                            try(Connection con = DbManager.getConnection()){
                                DbQueryUtil.getInstance().insertRow(con,sql,true);
                                logger.info("插入一条数据------- tran_name = "+tran);
                                logger.debug("插入一条数据------- tran_name = "+tran);
                            }catch (Exception e){
                                logger.error("获取连接失败/插入数据库失败,尝试重新插入");
                                try(Connection con = DbManager.getConnection()){
                                    DbQueryUtil.getInstance().insertRow(con,sql,true);
                                }catch (Exception ex){
                                    logger.error("获取连接失败/插入数据库再次失败,再次尝试"+tran);
                                    try(Connection con = DbManager.getConnection()){
                                        DbQueryUtil.getInstance().insertRow(con,sql,true);
                                    }catch (Exception el){
                                        logger.error("获取连接失败/插入数据库失败,放弃数据"+tran);
                                    }
                                }
                            }
                        }else {
                            insertDateV2(tran,trigger_time,array);
                        }
                    }else {
                        insertDateV2(tran,trigger_time,array);
                    }
                }else {
                    insertDateV2(tran,trigger_time,array);
                }
            }
        }
    }

    /**
     * 数据统计，插入数据库
     */
    public void insertDateV1(String tran,String trigger_time,JSONArray array ){
        String sql = "insert into tbl_tran_info3(tran_name,trigger_time,detail_info)" +
                "values('" + tran + "','" + trigger_time + "','" + array.toString() + "')" +
                " ON DUPLICATE KEY UPDATE trigger_time = '" + trigger_time + "',detail_info = '" + array.toString() + "'";
        try(Connection con = DbManager.getConnection()){
            DbQueryUtil.getInstance().insertRow(con,sql,true);
            logger.info("插入一条数据------- tran_name = "+tran);
            logger.debug("插入一条数据------- tran_name = "+tran);
        }catch (Exception e){
            logger.error("获取连接失败/插入数据库失败,尝试重新插入");
            try(Connection con = DbManager.getConnection()){
                DbQueryUtil.getInstance().insertRow(con,sql,true);
            }catch (Exception ex){
                logger.error("获取连接失败/插入数据库再次失败,再次尝试"+tran);
                try(Connection con = DbManager.getConnection()){
                    DbQueryUtil.getInstance().insertRow(con,sql,true);
                }catch (Exception el){
                    logger.error("获取连接失败/插入数据库失败,放弃数据"+tran);
                }
            }
        }
    }

    /**
     * 数据统计，插入数据库
     */
    public void insertDateV2(String tran,String trigger_time,JSONArray array ){
        for(int i=0;i<array.size();i++){
            JSONObject obj = array.getJSONObject(i);
            String station = obj.getString("station");
            String arrive_time = obj.getString("arrive_time");
            String stop_time = obj.getString("stop_time");
            String start_time = obj.getString("start_time");
            String sql = "insert into tbl_tran_info4(tran_name,trigger_time,start_time,stop_time,arrive_time,station,station_num)" +
                    "values('" + tran + "','" + trigger_time + "','" + start_time +"','" + stop_time +"','" + arrive_time +"','" + station +"','"+i+"')" +
                    " ON DUPLICATE KEY UPDATE trigger_time = '" + trigger_time + "',start_time = '" + start_time + "',stop_time = '" + stop_time + "'," +
                    "arrive_time = '" + arrive_time + "',station = '" + station + "'";
            try(Connection con = DbManager.getConnection()){
                DbQueryUtil.getInstance().insertRow(con,sql,true);
                logger.info("插入一条数据------- tran_name = "+tran);
                logger.debug("插入一条数据------- tran_name = "+tran);
            }catch (Exception e){
                logger.error("获取连接失败/插入数据库失败,尝试重新插入");
                try(Connection con = DbManager.getConnection()){
                    DbQueryUtil.getInstance().insertRow(con,sql,true);
                }catch (Exception ex){
                    logger.error("获取连接失败/插入数据库再次失败,再次尝试"+tran);
                    try(Connection con = DbManager.getConnection()){
                        DbQueryUtil.getInstance().insertRow(con,sql,true);
                    }catch (Exception el){
                        logger.error("获取连接失败/插入数据库失败,放弃数据"+tran);
                    }
                }
            }
        }
    }
    /**
     * 从12306获取数据
     * 获取一辆列车的具体停靠信息
     * @param tranNo 列车的no
     * @param startStation 数据来源的开始站点
     * @param endStation 结束站点
     */
    public JSONArray getEveryTranInfo(String tranNo,String startStation,String endStation,String queryData){
        String url = "https://kyfw.12306.cn/otn/czxx/queryByTrainNo?train_no=" + tranNo +
                "&from_station_telecode=" + startStation +
                "&to_station_telecode=" + endStation +
                "&depart_date=" + queryData;
        String everyTranInfo;
        RestTemplate restTemplate = null;
        //关闭ssl验证
        try{
            Thread.sleep(sleepTime);
            logger.info("url:"+url);
            CloseableHttpClient httpClient = HttpClientUtils.acceptsUntrustedCertsHttpClient();
            HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
            restTemplate = new RestTemplate(clientHttpRequestFactory);
            clientHttpRequestFactory.setConnectTimeout(10000);
            clientHttpRequestFactory.setReadTimeout(10000);
//            RestTemplate restTemplate = new RestTemplate();
            everyTranInfo = restTemplate.getForObject(url, String.class);
            logger.info("从webService获取单个列车信息 数据成功，数据为："+everyTranInfo);
        }catch (Exception e){
            logger.error("从webService获取数据失败",e);
            everyTranInfo = "";
            logger.error("程序可能被禁止访问，程序进入休眠状态，需要重新获取数据，等待禁止访问结束，休眠30000ms");
            try{
                Thread.sleep(forbiddenSleepTime);
                everyTranInfo = restTemplate.getForObject(url, String.class);
            }catch (Exception ex){
                logger.error("程序再次被禁止访问，程序放弃休眠，放弃数据，执行后面数据error error error error error error error");
            }
        }
        if(!everyTranInfo.isEmpty()){
            try{
                JSONObject obj = JSON.parseObject(everyTranInfo);
                JSONObject dataObj = obj.getJSONObject("data");
                JSONArray resultArr = dataObj.getJSONArray("data");
                return resultArr;
            }catch (Exception e){
                logger.error("12306返回了一个错误的页面，放弃数据："+tranNo);
            }
        }
        return new JSONArray();
    }


    /**
     * 从页面转成的str中分析想要的信息
     * @param tranNo 火车编号
     */
    public JSONArray getTranInfoFromStr(String tranNo){
        String pageStr = getTranInfoFromNet(tranNo);
        if(pageStr!=null){
            Document doc = Jsoup.parse(pageStr);
            Elements tables = doc.getElementsByTag("table");
            JSONArray resultArray = new JSONArray();
            for (Element table : tables) {
//            logger.info("table 内容： "+table);
                Elements trs = table.getElementsByTag("tr");
                //去掉第一行无关数据
                for (int i=1;i<trs.size();i++) {
                    Element tr = trs.get(i);
                    logger.info("每个tr 内容： "+tr.text());
                    String infoStr = tr.text();
                    String repStr = infoStr.replace(" ", ",");
                    String[] sa = repStr.split(",");
                    //第一行为编号无关，第二行为当前站点，第三行为到站时间，第四行为出发时间，第五行为停留时间，第六行为公里数
                    JSONObject obj = new JSONObject();
                    obj.put("station",sa[1]);
                    obj.put("arrive_time",sa[2]);
                    obj.put("start_time",sa[3]);
                    obj.put("stop_time",sa[4]);
                    obj.put("mileage",sa[5]);
                    resultArray.add(obj);
                }
            }
            return resultArray;
        }
        return new JSONArray();
    }
    /**
     * 根据url获取一辆列车的时刻表
     * @param url
     * @return
     */
    public JSONArray getTranTimeByUrl(String url){
        JSONArray resultArr = new JSONArray();
        try{
            Thread.sleep(300);
        }catch (Exception e){
            e.printStackTrace();
        }
        String str;
        Document doc = null;
        try{
//             str = restTemplate.getForObject(url,String.class);
            //使用jsoup的方法获取html
            doc = Jsoup.connect(url).timeout(10000).get();
        }catch (Exception e){
            logger.error("从网上获取数据失败，尝试重新获取");
            try{
//                str = restTemplate.getForObject(url,String.class);
                doc = Jsoup.connect(url).timeout(10000).get();
            }catch (Exception ex){
                logger.error("从网上获取数据失败，尝试重新获取",ex);
//                str = restTemplate.getForObject(url,String.class);
                try{
                    doc = Jsoup.connect(url).timeout(10000).get();
                }catch (Exception el){
                    logger.error("从网上获取数据失败，放弃数据",el);
                }
            }
        }

//        Document doc = Jsoup.parse(str);
        if(doc!=null){
            Elements lists = doc.getElementsByClass("list");
//        logger.info("lists : "+lists.text());
            for (Element list : lists) {
//            logger.info("list : "+list);
                Elements trs = list.getElementsByTag("tr");
                //去头去尾 无关紧要的信息
                for(int i=1;i<trs.size()-1;i++){
                    JSONObject obj = new JSONObject();
                    Element tr = trs.get(i);
                    String trText = tr.text();
                    logger.info(trText);
                    String repStr = trText.replace(" ", ",");
                    String[] sa = repStr.split(",");
                    //第一行无关紧要去掉
                    obj.put("station",sa[1]);
                    obj.put("arrive_time",sa[2]);
                    obj.put("stop_time",getDisparityTime(sa[2],sa[3]).toString()+"min");
                    obj.put("start_time",sa[3]);
                    obj.put("mileage",sa[4]);
                    resultArr.add(obj);
                }
            }
            return resultArr;
        }
        return new JSONArray();
    }



    /**
     * 从网上获取数据
     * @param tranNo 火车的编号
     * @return 页面数据
     */
    public String getTranInfoFromNet(String tranNo) {
        String url = "https://www.huoche.net/checi/"+tranNo;
        String pageStr = null;
        RestTemplate restTemplate = null;
        //关闭ssl验证
        try {
            Thread.sleep(sleepTime);
            logger.debug("url : " + url);
            CloseableHttpClient httpClient = HttpClientUtils.acceptsUntrustedCertsHttpClient();
            HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
            clientHttpRequestFactory.setConnectTimeout(10000);
            clientHttpRequestFactory.setReadTimeout(10000);
            restTemplate = new RestTemplate(clientHttpRequestFactory);
            pageStr = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            logger.error("获取数据失败，尝试重新获取---", e);
            try {
                Thread.sleep(sleepTime);
                pageStr = restTemplate.getForObject(url, String.class);
            } catch (Exception ex) {
                    logger.error("获取数据失败，再次尝试获取：",e);
                try {
                    Thread.sleep(sleepTime);
                    pageStr = restTemplate.getForObject(url, String.class);
                } catch (Exception el) {
                    logger.error("三次获取数据失败，有可能页面禁止访问，程序进入休眠，放弃当前数据：",el);
                    try{
                        Thread.sleep(forbiddenSleepTime);
                    }catch (Exception ee){

                    }
                }
            }
        }
        return pageStr;
    }

    /**
     * 两个时间相减
     */
    public Long getDisparityTime(String startTime,String endTime){
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date d1 = df.parse("2017-09-02 "+startTime+":00");
            Date d2 = df.parse("2017-09-02 "+endTime+":00");
            long diff = d2.getTime() - d1.getTime();//这样得到的差值是微秒级别
            if(diff>=0){
                long minutes = diff/(1000* 60);
                logger.info("minutes : "+minutes);
                return minutes;
            }else {
                Date d3 = df.parse("2017-09-03 "+endTime+":00");
                long diff1 = d3.getTime() - d1.getTime();
                long minutes = diff1/(1000* 60);
                logger.info("minutes : "+minutes);
                return minutes;
            }
        } catch (Exception e) {
            logger.error("时间相减出错！因为始发站相减出错，本程序捕捉而不需要做处理 ",e);
        }
        return new Long(0);
    }
}
