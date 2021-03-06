package org.easyweb;

import org.easyweb.app.deploy.DeployListener;
import org.easyweb.app.monitor.AppMonitor;
import org.easyweb.bean.BeanFactory;
import org.easyweb.groovy.annotation.AnnotationParser;
import org.easyweb.request.RequestProcessor;
import org.easyweb.request.exception.ExceptionHandler;
import org.easyweb.request.exception.ExceptionProcessor;
import org.easyweb.util.EasywebLogger;
import org.easyweb.velocity.VelocityTool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Created by jimmey on 15-7-30.
 */
public class Easyweb {

    private static RequestProcessor processor;

    public static void initialize() {
        initialize(new HashMap<String, Object>());
    }

    private static volatile boolean initialized = false;

    public static void initialize(Map<String, Object> beans) {
        //从外部注入bean
        BeanFactory.registerNativeBeans(beans);
        if (!initialized) {
            initialized = true;
            load(AnnotationParser.class);
            load(DeployListener.class);
            load(VelocityTool.class);
            ExceptionProcessor.loadHandlers();
            processor = new RequestProcessor();
            AppMonitor.getInstance().start();
        }
    }

    public static void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (processor == null) {
            throw new RuntimeException("Easyweb Not Initialized Yet!!");
        }
        processor.process(request, response);
    }

    public static void destroy() {
        AppMonitor.getInstance().stop();
    }

    private static <T> void load(Class<T> clazz) {
        ServiceLoader<T> loader = ServiceLoader.load(clazz);
        Iterator<T> it = loader.iterator();
        while (it.hasNext()) {
            T t = it.next();
            EasywebLogger.debug("[ServiceLoader] Load " + t.toString());
        }
    }

}
