package core;


import org.apache.log4j.Logger;
import util.PropertiesUtil;

import java.util.Properties;

/**
 * Created by Administrator on 2017/6/16.
 */
public class ConfigManager {
    private static Logger logger = Logger.getLogger(ConfigManager.class);

    //配置文件全路径
    private static String mConfPath = "";

    //ConfigManager实例对象
    private static ConfigManager mInstance;

    //c3p0.properties配置文件解析
    private Properties mAppProps;

    //application.properties配置文件解析
    private Properties mBppProps;

    /**
     * 获取配置文件路径
     * @return String
     */
    public static void setConfPath(String iConfPath){
        mConfPath = iConfPath;
    }
    /**
     * 获取ConfigManager单例对象
     *
     * @return ConfigManager
     */
    public static ConfigManager getInstance() {
        if (mInstance == null) {
            synchronized (ConfigManager.class) {
                if (mInstance == null) {
                    mInstance = new ConfigManager();
                }
                return mInstance;
            }
        }
        return mInstance;
    }
    /**
     * 构造ConfigManager，解析读取相关properties配置文件
     */
    private ConfigManager(){
        logger.info("start init ConfigManager.");
        try{
            mAppProps = PropertiesUtil.loadPropsFile(mConfPath+"/c3p0.properties");
            mBppProps = PropertiesUtil.loadPropsFile(mConfPath+"/application.properties");
        }catch (Exception e){
            logger.error("init ConfigManager failed! "+e.getMessage());
        }
    }
    public Properties getmAppProps(){
        return mAppProps;
    }
    /**
     * 从配置文件中获取数据 获取单个休眠时间
     */
    public String getSleepTime(){
        String sleepTime = mBppProps.getProperty("sleepTime").toString();
        return sleepTime;
    }

    /**
     * 获取程序被禁止访问的sleep时间
     * @return
     */
    public String getForbiddenSleepTime(){
        String forbiddenSleepTime = mBppProps.getProperty("forbiddenSleepTime").toString();
        return forbiddenSleepTime;
    }

    /**
     *
     * @return
     */
    public String getStartId(){
        String startId = mBppProps.getProperty("startId").toString();
        return startId;
    }

    public String getEndId(){
        String endId = mBppProps.getProperty("endId").toString();
        return endId;
    }
}
