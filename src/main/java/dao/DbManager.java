package dao;


import com.mchange.v2.c3p0.DataSources;
import core.ConfigManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by Administrator on 2017/8/10.
 */
public class DbManager {
    private static final String JDBC_DRIVER = "driverClass";
    private static final String JDBC_URL = "jdbcUrl";
    private static DataSource ds;
    /**
     * 初始化连接池代码块
     */
    static{
        initDBSource();
    }

    /**
     * 初始化c3p0连接池
     */
    private static final void initDBSource(){
        Properties c3p0Pro = ConfigManager.getInstance().getmAppProps();
        String driverClass = c3p0Pro.getProperty(JDBC_DRIVER);
        if(driverClass != null){
            try {
                //加载驱动类
                Class.forName(driverClass);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }

        Properties jdbcpropes = new Properties();
        Properties c3propes = new Properties();
        for(Object key:c3p0Pro.keySet()){
            String skey = (String)key;
            if(skey.startsWith("c3p0.")){
                c3propes.put(skey, c3p0Pro.getProperty(skey));
            }else{
                jdbcpropes.put(skey, c3p0Pro.getProperty(skey));
            }
        }

        try {
            //建立连接池
            DataSource unPooled = DataSources.unpooledDataSource(c3p0Pro.getProperty(JDBC_URL),jdbcpropes);
            ds = DataSources.pooledDataSource(unPooled,c3propes);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取数据库连接对象
     * @return 数据连接对象
     * @throws SQLException
     */
    public static synchronized Connection getConnection() throws SQLException{
        final Connection conn = ds.getConnection();
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        return conn;
    }
}
