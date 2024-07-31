package api.banner

import com.ccctc.adaptor.util.MisEnvironment
import groovy.sql.Sql
import org.springframework.core.env.Environment

class BannerConnection {

    static Sql getSession(Environment environment, String  misCode) {
        String url = MisEnvironment.getProperty(environment, misCode, "jdbc.url")
        String user = (MisEnvironment.getProperty(environment, misCode, "sisuser"))
        String password = (MisEnvironment.getProperty(environment, misCode, "sispassword"))
        String driver = MisEnvironment.getProperty(environment, misCode, "jdbc.driver")
        Sql sql = Sql.newInstance(
                url,
                user,
                password,
                driver
        )
        user = null
        password = null
        return sql
    }
}
