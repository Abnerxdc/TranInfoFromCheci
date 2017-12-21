package main;


import core.Applnit;
import service.TranInfoService;
import service.TranListService;

/**
 * Created by Administrator on 2017/9/6.
 */
public class MainApp {
    public static void main(String[] args){
        Applnit.initApp();
        TranListService.initTranList();
        TranInfoService tranInfoService = new TranInfoService();
        tranInfoService.getDateFromDb();
    }
}
