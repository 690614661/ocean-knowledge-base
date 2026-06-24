package com.ocean.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * .env 环境变量加载器（启动前执行）
 * <p>
 * 在 Spring 解析 application.yml 之前读取 .env 文件并注入 Environment，
 * 确保 ${VAR:default} 能正确解析为 .env 中的值。
 * <p>
 * 这样在 IDEA 中直接启动也能自动加载七牛云等配置。
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        File envFile = findEnvFile();
        if (envFile == null || !envFile.exists()) {
            return;
        }

        Map<String, Object> props = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int eqIdx = line.indexOf('=');
                if (eqIdx <= 0) continue;

                String key = line.substring(0, eqIdx).trim();
                String value = line.substring(eqIdx + 1).trim();

                // 只在不覆盖已有环境变量的情况下设置
                if (environment.getProperty(key) == null) {
                    props.put(key, value);
                }
            }
        } catch (Exception ignored) {
        }

        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(
                    new MapPropertySource("dotenv", props));
        }
    }

    private File findEnvFile() {
        File cwd = new File(System.getProperty("user.dir"));
        File envInCwd = new File(cwd, ".env");
        if (envInCwd.exists()) return envInCwd;

        File parentDir = cwd.getParentFile();
        if (parentDir != null) {
            File envInParent = new File(parentDir, ".env");
            if (envInParent.exists()) return envInParent;
        }
        return null;
    }
}
