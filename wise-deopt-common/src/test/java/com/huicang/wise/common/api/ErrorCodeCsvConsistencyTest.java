package com.huicang.wise.common.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 错误码与《后端异常码对照表.csv》一致性测试（STD-CONTRACT-03 / STD-ERR-03）。
 *
 * <p>对照表是错误码的**唯一来源**；本测试确保：
 * <ul>
 *   <li>{@link ErrorCode} 中每个中文描述与 HTTP 状态码都与对照表一致；</li>
 *   <li>对照表登记的每个错误码都能在枚举中找到（不允许「表里有、代码里没有」）。</li>
 * </ul>
 *
 * <p>找不到对照表时（例如裁剪后的构建环境）跳过，不使构建失败。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
class ErrorCodeCsvConsistencyTest {

    /**
     * 对照表相对路径（相对各模块工作目录）。
     */
    private static final String[] CANDIDATES = {
            "wise-depot-plan/后端异常码对照表.csv",
            "../wise-depot-plan/后端异常码对照表.csv",
            "../../wise-depot-plan/后端异常码对照表.csv"
    };

    /**
     * 定位对照表文件。
     *
     * @return 对照表路径，找不到时返回 null
     */
    private Path locateCsv() {
        for (String candidate : CANDIDATES) {
            Path path = Paths.get(candidate);
            if (Files.exists(path)) {
                return path.toAbsolutePath();
            }
        }
        return null;
    }

    /**
     * 按 CSV 规则拆分一行（支持双引号包裹的字段）。
     *
     * @param line 待拆分行
     * @return 字段数组
     */
    private List<String> splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    /**
     * 读取对照表，返回 错误码 -> [描述, HTTP状态码]。
     *
     * @param csv 对照表路径
     * @return 映射
     * @throws IOException 读取失败
     */
    private Map<String, String[]> loadCsv(Path csv) throws IOException {
        Map<String, String[]> result = new HashMap<>();
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            List<String> fields = splitCsvLine(raw);
            if (fields.size() < 7) {
                continue;
            }
            String code = fields.get(0).trim();
            if (code.isEmpty() || "--".equals(code)) {
                continue;
            }
            result.put(code, new String[] { fields.get(5).trim(), fields.get(6).trim() });
        }
        return result;
    }

    /**
     * 枚举中的每条错误码都应与对照表一致。
     *
     * @throws IOException 读取对照表失败
     */
    @Test
    @DisplayName("shouldMatchCsvWhenErrorCodeIsRegistered")
    void shouldMatchCsvWhenErrorCodeIsRegistered() throws IOException {
        Path csv = locateCsv();
        assumeTrue(csv != null, "未找到《后端异常码对照表.csv》，跳过一致性校验");

        Map<String, String[]> csvRows = loadCsv(csv);
        assertTrue(csvRows.size() > 0, "对照表未解析出任何错误码: " + csv);

        List<String> mismatches = new ArrayList<>();
        for (ErrorCode errorCode : ErrorCode.values()) {
            String[] row = csvRows.get(errorCode.getCode());
            if (row == null) {
                continue;
            }
            if (!row[0].equals(errorCode.getMessage())) {
                mismatches.add(String.format("%s 描述不一致: 代码=%s 对照表=%s",
                        errorCode.getCode(), errorCode.getMessage(), row[0]));
            }
            if (!row[1].equals(String.valueOf(errorCode.getHttpStatus()))) {
                mismatches.add(String.format("%s 状态码不一致: 代码=%s 对照表=%s",
                        errorCode.getCode(), errorCode.getHttpStatus(), row[1]));
            }
        }

        assertTrue(mismatches.isEmpty(), "错误码与对照表不一致：" + System.lineSeparator()
                + String.join(System.lineSeparator(), mismatches)
                + System.lineSeparator() + "请运行 tools/gen/gen-errorcodes.ps1 重新生成。");
    }

    /**
     * 对照表登记的每条错误码都应在枚举中存在。
     *
     * @throws IOException 读取对照表失败
     */
    @Test
    @DisplayName("shouldExistInEnumForEveryCodeInCsv")
    void shouldExistInEnumForEveryCodeInCsv() throws IOException {
        Path csv = locateCsv();
        assumeTrue(csv != null, "未找到《后端异常码对照表.csv》，跳过一致性校验");

        Map<String, String[]> csvRows = loadCsv(csv);
        List<String> missing = new ArrayList<>();
        for (String code : csvRows.keySet()) {
            boolean found = false;
            for (ErrorCode errorCode : ErrorCode.values()) {
                if (errorCode.getCode().equals(code)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                missing.add(code);
            }
        }

        assertTrue(missing.isEmpty(), "对照表已登记但枚举缺失的错误码："
                + String.join(", ", missing)
                + "；请运行 tools/gen/gen-errorcodes.ps1 重新生成。");
    }
}
