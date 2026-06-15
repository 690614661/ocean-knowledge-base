package com.ocean.interceptor;

import com.ocean.util.XssFilterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@WebFilter(filterName = "xssFilter", urlPatterns = "/*")
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(new XssHttpServletRequestWrapper((HttpServletRequest) request), response);
    }

    static class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

        private byte[] bodyBytes;
        private boolean bodyParsed = false;

        public XssHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return value != null ? XssFilterUtil.stripXss(value) : null;
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            String[] cleaned = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleaned[i] = XssFilterUtil.stripXss(values[i]);
            }
            return cleaned;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            if ("token".equalsIgnoreCase(name)) {
                return value;
            }
            return value != null ? XssFilterUtil.stripXss(value) : null;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (bodyParsed) {
                return new ByteArrayServletInputStream(bodyBytes);
            }

            // 检查是否为 JSON 请求
            String contentType = getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                String body = readBody(super.getInputStream());
                String cleaned = XssFilterUtil.stripXss(body);
                bodyBytes = cleaned.getBytes(StandardCharsets.UTF_8);
                bodyParsed = true;
                return new ByteArrayServletInputStream(bodyBytes);
            }
            return super.getInputStream();
        }

        @Override
        public BufferedReader getReader() throws IOException {
            if (bodyParsed) {
                return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bodyBytes), StandardCharsets.UTF_8));
            }
            return super.getReader();
        }

        private String readBody(InputStream inputStream) throws IOException {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            return sb.toString();
        }
    }

    static class ByteArrayServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream inputStream;

        public ByteArrayServletInputStream(byte[] data) {
            this.inputStream = new ByteArrayInputStream(data);
        }

        @Override
        public int read() {
            return inputStream.read();
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
        }
    }
}
