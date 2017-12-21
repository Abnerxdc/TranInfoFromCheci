package core;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Created by Administrator on 2017/6/16.
 */
public class Applnit {
    private static Logger logger = Logger.getLogger(Applnit.class);
    public static void initApp(){
        init("./conf");
    }
    public static void init(String iConfPath){
        //设置log4j配置文件
        PropertyConfigurator.configure(iConfPath+ "/log4j.properties");
        ConfigManager.setConfPath(iConfPath);
        //设置配置文件路径
        logger.info("set application config path. path = "+iConfPath);
    }
}
