package work.pollochang.sqlconsole.drivers;

import java.net.URL;
import java.net.URLClassLoader;

public class ExternalDriverClassLoader extends URLClassLoader {

    public ExternalDriverClassLoader(URL[] urls) {
        // Use Platform ClassLoader as parent to allow access to java.* and javax.* classes (like java.sql.Driver)
        // but prevent access to the application's classpath.
        super(urls, ClassLoader.getPlatformClassLoader());
    }
}
