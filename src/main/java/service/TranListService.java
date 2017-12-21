package service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import core.Applnit;
import dao.DbManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import util.DbQueryUtil;
import util.HttpClientUtils;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Created by Administrator on 2017/9/6.
 * 用于获取火车列表
 */
public class TranListService {
    public static void initTranList(){
        Applnit.initApp();
        Logger logger = Logger.getLogger(TranListService.class);
        String str = "";
        try{
            //关闭ssl验证
            CloseableHttpClient httpClient = HttpClientUtils.acceptsUntrustedCertsHttpClient();
            HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
            RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);
            str = restTemplate.getForObject("https://kyfw.12306.cn/otn/resources/js/query/train_list.js?scriptVersion=1.0", String.class);
        }catch (Exception e){
            e.printStackTrace();
        }
        logger.info(" 获取到的站点信息 ： " + str);
        //从第16个开始算
        String subStr = str.substring(16, str.length());
        logger.info(" sub之后的站点信息 ： " + subStr);
        JSONObject allObj = JSON.parseObject(subStr);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate nowDate = LocalDate.now();
        LocalDate tomorrow = nowDate.plus(1, ChronoUnit.DAYS);
        String trigger_date= dateTimeFormatter.format(tomorrow);
        JSONObject dateObj = JSON.parseObject(allObj.get(trigger_date).toString());
//        logger.info("明天的列车有 : "+dateObj);
        for(String zimu : dateObj.keySet()){

            JSONArray FObj = dateObj.getJSONArray(zimu);
            logger.info("列车的开头字母有： "+zimu +" 该开头字母有 这些辆车："+FObj.size());
            for(int i=0;i<FObj.size();i++){
                JSONObject obj = FObj.getJSONObject(i);
                logger.info("-----------"+obj);
                String tran_code = obj.getString("station_train_code");
                String repStr1 = tran_code.replace("(", ",");
                String repStr2 = repStr1.replace("-",",");
                String repStr3 = repStr2.replace(")","");
                String[] sa = repStr3.split(",");
                String tran_name = sa[0];
                String starting_station = sa[1];
                String end_station = sa[2];
                String train_no = obj.getString("train_no");
                logger.info("------------"+tran_name+" "+starting_station+" "+end_station);
                DateTimeFormatter dateTimeFormatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime nowDateTime = LocalDateTime.now();
                String trigger_time = dateTimeFormatter2.format(nowDateTime);
                String sql = "insert into tbl_tran_list (tran_name,starting_station,ending_station,train_no,category,trigger_time)" +
                        "values('"+tran_name+"','"+starting_station+"','"+end_station+"','"+train_no+"','"+zimu+"','"+trigger_time+"')" +
                        "on DUPLICATE KEY UPDATE tran_name = '"+tran_name+"', starting_station = '"+starting_station+"',ending_station = '"+end_station+"'," +
                        "category = '"+zimu+"',trigger_time = '"+trigger_time+"'";
                try(Connection con = DbManager.getConnection()){
                    DbQueryUtil.getInstance().insertRow(con,sql,true);
                    logger.info("插入一条数据------- tran_name = "+tran_name);
                    logger.debug("插入一条数据------- tran_name = "+tran_name);
                }catch (Exception e){
                    logger.error("获取连接失败/插入数据库失败,尝试重新插入");
                    try(Connection con = DbManager.getConnection()){
                        DbQueryUtil.getInstance().insertRow(con,sql,true);
                    }catch (Exception ex){
                        logger.error("获取连接失败/插入数据库再次失败,再次尝试"+tran_name);
                        try(Connection con = DbManager.getConnection()){
                            DbQueryUtil.getInstance().insertRow(con,sql,true);
                        }catch (Exception el){
                            logger.error("获取连接失败/插入数据库失败,放弃数据"+tran_name);
                        }
                    }
                }
            }
        }
    }
}
