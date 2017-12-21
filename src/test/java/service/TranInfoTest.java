package service;

import com.alibaba.fastjson.JSONArray;
import core.Applnit;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by Administrator on 2017/9/5.
 */
public class TranInfoTest {

//    Logger logger = Logger.getLogger(TranInfoTest.class);
    @Before
    public void testInit(){
        Applnit.initApp();
    }
    @Test
    public void testGetTranInfoFromNet(){
        Applnit.initApp();
        TranInfoService tranInfoService = new TranInfoService();
        String tranNo = "G9004";
        String abc = tranInfoService.getTranInfoFromNet(tranNo);
//        logger.info(abc);
        System.out.println(abc);
    }

    /**
     * 测试如果出现错误数据，通过检测
     */
    @Test
    public void testGetTranInfoFromStr(){
        Applnit.initApp();
        TranInfoService tranInfoService = new TranInfoService();
        String tranNo = "1234";
        JSONArray arr = tranInfoService.getTranInfoFromStr(tranNo);
        System.out.println(arr);
    }

    @Test
    public void testGetTranFrom12306(){
        Applnit.initApp();
        TranInfoService tranInfoService = new TranInfoService();
        JSONArray arr = tranInfoService.getEveryTranInfo("380000G37010","NKH","AOH","2017-09-07");
        System.out.println(arr);
    }
}
