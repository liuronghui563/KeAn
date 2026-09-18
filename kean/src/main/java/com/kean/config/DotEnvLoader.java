package com.kean.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 启动前加载仓库根目录 / 模块目录的 .env，避免 IDE 或新终端未手动注入环境变量。
 */
public final class DotEnvLoader {

    private DotEnvLoader() {
    }

    public static void load() {
        Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                cwd.resolve(".env"),
                cwd.resolve("kean").resolve(".env"),
                cwd.getParent() != null ? cwd.getParent().resolve(".env") : cwd.resolve(".env")
        );
        for (Path path : candidates) {
            if (path == null || !Files.isRegularFile(path)) {
                continue;
            }
            try {
                apply(path);
            } catch (IOException ignored) {
                // 继续尝试下一个候选文件
            }
        }
    }

    private static void apply(Path path) throws IOException {
        for (String raw : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                continue;
            }
            int split = line.indexOf('=');
            String key = line.substring(0, split).trim();
            String value = line.substring(split + 1).trim();
            if (key.isEmpty()) {
                continue;
            }
            if (System.getenv(key) == null && System.getProperty(key) == null) {
                System.setProperty(key, value);
            }
        }
    }
}
