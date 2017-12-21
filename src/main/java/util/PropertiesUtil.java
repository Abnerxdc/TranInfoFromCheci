package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Administrator on 2017/6/16.
 */
public class PropertiesUtil {
    /**
     * 读取Properties文件
     *
     * @param iFilePath
     * @return
     * @throws IOException
     */
    public static Properties loadPropsFile(String iFilePath){
        Properties props = new Properties();
        try(InputStream in = new FileInputStream(iFilePath);
            InputStreamReader inputStreamReader = new  InputStreamReader(in,"UTF-8")){
            props.load(inputStreamReader);
        }catch (Exception e){
            e.printStackTrace();
        }
        return props;
    }

    public static Properties getPropsByPrefix(Properties iProps,String iPrefix,boolean iRemovePrefix){
        Properties ret = new Properties();
        Enumeration<String> keys = (Enumeration<String>)iProps.propertyNames();
        int prefixLength = iPrefix.length();
        while (keys.hasMoreElements()){
            String key = keys.nextElement();
            if(key.startsWith(iPrefix)){
                if(iRemovePrefix){
                    ret.put(key.substring(prefixLength),iProps.getProperty(key));
                }else {
                    ret.put(key,iProps.getProperty(key));
                }
            }
        }
        return ret;
    }
    public static Map<String,String> propsToMap(Properties iProps){
        Map<String,String> ret = new HashMap<String, String>();
        Enumeration<String> keys = (Enumeration<String>)iProps.propertyNames();
        while (keys.hasMoreElements()){
            String key = keys.nextElement();
            ret.put(key, iProps.getProperty(key));
        }
        return ret;
    }
}
