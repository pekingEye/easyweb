package org.easyweb.request;

import org.apache.commons.lang.StringUtils;
import org.easyweb.Configuration;
import org.easyweb.app.App;
import org.easyweb.app.AppStatus;
import org.easyweb.app.command.CommandHandler;
import org.easyweb.context.Context;
import org.easyweb.context.ThreadContext;
import org.easyweb.profiler.Profiler;
import org.easyweb.request.assets.AssetsProcessor;
import org.easyweb.request.exception.ExceptionProcessor;
import org.easyweb.request.exception.ExceptionType;
import org.easyweb.request.pipeline.Pipeline;
import org.easyweb.request.render.CodeRender;
import org.easyweb.request.uri.AppUriContainer;
import org.easyweb.request.uri.UriTemplate;
import org.easyweb.util.EasywebLogger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;

public class RequestProcessor {

    private static String defaultCharset = Configuration.getRequestCharset();

    /**
     * 对一个请求做处理
     *
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    public void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
        App app = null;
        try {
            if (CommandHandler.handle(request, response)) {
                return;
            }
            Profiler.start("process HTTP request");
            ThreadContext.init(request, response);
            initMDC();
            Context context = ThreadContext.getContext();
            app = context.getApp();
            if (app == null || app.getStatus() != AppStatus.OK) {
                EasywebLogger.error("[RequestProcessor] App not found or status error, %s", request.getRequestURI());
                ExceptionProcessor.process(ExceptionType.APP_STATUS_ERROR, null, response);
                return;
            }

            if (AssetsProcessor.process(request, response, app)) {
                return;
            }

            /**
             * 先执行pipeline的逻辑代码
             */
            boolean pipeline = true;
            try {
                Profiler.enter("start pipeline");
                Pipeline.invoke();
                pipeline = !context.isBreakPipeline();
            } finally {
                Profiler.release();
            }

            /**
             * 如果pipeline进行了redirect,则直接redirect,后面逻辑不再执行
             */
            String redirect = context.getRedirectTo();
            if (StringUtils.isNotBlank(redirect)) {
                response.sendRedirect(redirect);
                return;
            }

            /**
             * 如果pipeline break了，那么后面的页面渲染也不执行，二是执行
             */
            if (pipeline) {
                try {
                    Profiler.enter("process page");
                    processPage(app, request, response);
                } finally {
                    Profiler.release();
                }
            }
        } finally {
            ThreadContext.clean();
            Profiler.release();
            if (app != null) {
                String requestString = dumpRequest(request);
                long duration = Profiler.getDuration();
                if (duration > app.getProfilerThreshold()) {
                    EasywebLogger.warn(MessageFormat.format("[Profiler] Response of {0} returned in {1,number}ms\n{2}\n", requestString, duration, getDetail()));
                } else {
                    EasywebLogger.debug(MessageFormat.format("[Profiler] Response of {0} returned in {1,number}ms\n{2}\n",
                            requestString, duration, getDetail()));
                }
            }
            Profiler.reset();
        }

    }

    private void processPage(App app, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = request.getRequestURI();
        Context context = ThreadContext.getContext();
        if (context.getForwardTo() != null) {
            uri = context.getForwardTo();
        }
        UriTemplate uriTemplate = AppUriContainer.getUriTemplate(app, uri, request.getMethod());
        if (uriTemplate == null) {
            ExceptionProcessor.process(ExceptionType.PAGE_NOT_FOUND, null, response);
        } else {
            PageMethod pageMethod = uriTemplate.getPageMethod();
            if (StringUtils.isNotBlank(pageMethod.getLayout())) {
                ThreadContext.getContext().setLayout(pageMethod.getLayout());
            }
            try {
                response(response, CodeRender.getInstance().renderPage(uriTemplate));
            } catch (Exception e) {
                ExceptionProcessor.process(ExceptionType.PROCESS_EXCEPTION, e, response);
            }
        }
    }

    private void response(HttpServletResponse response, String content) throws Exception {
        Context context = ThreadContext.getContext();
        String redirect = context.getRedirectTo();
        if (StringUtils.isNotBlank(redirect)) {
            response.sendRedirect(redirect);
        } else if (!context.isDownload()) {
            if (response.getContentType() == null) {
                response.setContentType("text/html;charset=" + defaultCharset);
            }
            byte[] bytes = content.getBytes(defaultCharset);
            write(bytes, response);
        }
    }

    private void write(byte[] bytes, HttpServletResponse response) throws IOException {
        response.setContentLength(bytes.length);
        response.setHeader("Content-Language", "zh-CN");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
        response.getOutputStream().close();
    }

    private void initMDC() {
        Context context = ThreadContext.getContext();
        EasywebLogger.initMDC("requestURI", context.getRequest().getRequestURL().toString());
    }

    private String dumpRequest(HttpServletRequest request) {
        StringBuffer buffer = new StringBuffer(request.getMethod());
        buffer.append(" ").append(request.getRequestURI());
        String queryString = StringUtils.trimToNull(request.getQueryString());
        if (queryString != null) {
            buffer.append("?").append(queryString);
        }
        return buffer.toString();
    }

    private String getDetail() {
        return Profiler.dump("Detail: ", "        ");
    }

}
