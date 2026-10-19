package com.diabetes.monitor.export;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.monitor.entity.HealthRecordBloodSugar;
import com.diabetes.monitor.entity.HealthRecordBody;
import com.diabetes.monitor.entity.HealthRecordDiet;
import com.diabetes.monitor.entity.HealthRecordExercise;
import com.diabetes.monitor.service.HealthRecordBloodSugarService;
import com.diabetes.monitor.service.HealthRecordBodyService;
import com.diabetes.monitor.service.HealthRecordDietService;
import com.diabetes.monitor.service.HealthRecordExerciseService;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Component
public class ExcelExportWriter {
    @Resource
    private HealthRecordBodyService bodyService;
    @Resource
    private HealthRecordBloodSugarService bloodSugarService;
    @Resource
    private HealthRecordDietService dietService;
    @Resource
    private HealthRecordExerciseService exerciseService;
    @Resource
    private ThreadPoolTaskExecutor taskExecutor;

    /**
     * 只负责"把数据写成 xlsx"，不关心输出目标是响应流还是文件
     */
    public void write(Integer userId, LocalDate startDate, LocalDate endDate, OutputStream out) throws IOException {

        // 限制导出数量：防止用户数据量过大导致内存溢出（OOM）
        int maxExportSize = 1000;
        // MyBatis-Plus的.last()方法用于追加SQL片段，这里添加LIMIT子句
        String limitSql = "LIMIT " + maxExportSize;

        // 时间范围统一按【左闭右开】处理：>= 起始日 00:00，< 结束日次日 00:00
        // 这样 endDate 当天的数据也会被包含，又不会把次日 00:00 的数据算进来
        LocalDateTime from = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime toExclusive = endDate == null ? null : endDate.plusDays(1).atStartOfDay();

        // 并行查询四类健康记录
        CompletableFuture<List<HealthRecordBloodSugar>> sugarFuture =
                CompletableFuture.supplyAsync(() -> bloodSugarService.list(
                        new LambdaQueryWrapper<HealthRecordBloodSugar>()
                                .eq(HealthRecordBloodSugar::getUserId, userId)
                                .ge(from != null, HealthRecordBloodSugar::getMeasureTime, from)
                                .lt(toExclusive != null, HealthRecordBloodSugar::getMeasureTime, toExclusive)
                                .orderByDesc(HealthRecordBloodSugar::getMeasureTime)
                                .last(limitSql)), taskExecutor);

        CompletableFuture<List<HealthRecordBody>> bodyFuture =
                CompletableFuture.supplyAsync(() -> bodyService.list(
                        new LambdaQueryWrapper<HealthRecordBody>()
                                .eq(HealthRecordBody::getUserId, userId)
                                .ge(startDate != null, HealthRecordBody::getRecordDate, startDate)
                                .le(endDate != null, HealthRecordBody::getRecordDate, endDate)
                                .orderByDesc(HealthRecordBody::getRecordDate)
                                .last(limitSql)), taskExecutor);

        CompletableFuture<List<HealthRecordDiet>> dietFuture =
                CompletableFuture.supplyAsync(() -> dietService.list(
                        new LambdaQueryWrapper<HealthRecordDiet>()
                                .eq(HealthRecordDiet::getUserId, userId)
                                .ge(from != null, HealthRecordDiet::getEatTime, from)
                                .lt(toExclusive != null, HealthRecordDiet::getEatTime, toExclusive)
                                .orderByDesc(HealthRecordDiet::getEatTime)
                                .last(limitSql)), taskExecutor);

        CompletableFuture<List<HealthRecordExercise>> exerciseFuture =
                CompletableFuture.supplyAsync(() -> exerciseService.list(
                        new LambdaQueryWrapper<HealthRecordExercise>()
                                .eq(HealthRecordExercise::getUserId, userId)
                                .ge(startDate != null, HealthRecordExercise::getExerciseDate, startDate)
                                .le(endDate != null, HealthRecordExercise::getExerciseDate, endDate)
                                .orderByDesc(HealthRecordExercise::getExerciseDate)
                                .last(limitSql)), taskExecutor);

        // 等待所有查询完成
        //allOf()：等待所有CompletableFuture完成
        //join()：阻塞当前线程直到所有任务完成，如果有任务异常则抛出CompletionException
        //这一步确保后续使用的数据都是已完整查询的
        CompletableFuture.allOf(sugarFuture, bodyFuture, dietFuture, exerciseFuture).join();

        List<HealthRecordBloodSugar> sugarList = sugarFuture.join();
        List<HealthRecordBody> bodyList = bodyFuture.join();
        List<HealthRecordDiet> dietList = dietFuture.join();
        List<HealthRecordExercise> exerciseList = exerciseFuture.join();

        // 3. 生成 Excel（try-with-resources 自动关闭）
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        /**
         * 使用try-with-resources自动管理资源
         * XSSFWorkbook：POI的Excel工作簿对象（.xlsx格式）
         * out：由调用方传进来（HTTP 响应流 或 文件流），本方法【不负责关闭它】
         */
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            /**
             * 创建4个Sheet页
             * 每个Sheet对应一类健康记录，使用不同的日期格式化器
             */
            createSugarSheet(workbook, sugarList, dateTimeFmt);
            createBodySheet(workbook, bodyList, dateFmt);
            createDietSheet(workbook, dietList, dateTimeFmt);
            createExerciseSheet(workbook, exerciseList, dateFmt);

            workbook.write(out);
        }


    }

    /**
     * 通用Sheet创建方法
     * 使用泛型和函数式接口，消除重复代码
     *
     * @param <T> 健康记录实体类型（如HealthRecordBloodSugar、HealthRecordBody等）
     * @param workbook Excel工作簿
     * @param sheetName Sheet页名称
     * @param records 记录列表
     * @param headers 表头数组
     * @param dataExtractor 数据提取函数：将每条记录转换为Object数组，用于填充行数据
     */
    private <T> void createSheet(XSSFWorkbook workbook, String sheetName,
                                 List<T> records, String[] headers,
                                 Function<T, Object[]> dataExtractor) {
        // 在工作簿中创建一个Sheet页
        Sheet sheet = workbook.createSheet(sheetName);
        // ===== 创建表头行（第0行） =====
        Row headerRow = sheet.createRow(0);
        // 获取表头样式（灰色背景、加粗、带边框）
        CellStyle headerStyle = createHeaderStyle(workbook);
        // 遍历表头数组，逐个创建单元格并设置样式
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 16 * 256);
        }

        // ===== 填充数据行（从第1行开始） =====
        int rowNum = 1;
        for (T record : records) {
            Row row = sheet.createRow(rowNum++);
            Object[] values = dataExtractor.apply(record);
            for (int i = 0; i < values.length && i < headers.length; i++) {
                Object value = values[i];
                Cell cell = row.createCell(i);
                if (value instanceof Number) {
                    cell.setCellValue(((Number) value).doubleValue());
                } else if (value != null) {
                    cell.setCellValue(value.toString());
                } else {
                    cell.setCellValue("");
                }
            }
        }
    }

    /**
     * 创建血糖记录Sheet
     *
     * @param workbook Excel工作簿
     * @param records 血糖记录列表
     * @param fmt 日期时间格式化器（包含时分秒）
     */
    private void createSugarSheet(XSSFWorkbook workbook, List<HealthRecordBloodSugar> records,
                                  DateTimeFormatter fmt) {
        String[] headers = {"测量时间", "血糖值(mmol/L)", "测量类型", "备注"};
        createSheet(workbook, "血糖记录", records, headers, r -> new Object[]{
                r.getMeasureTime() != null ? r.getMeasureTime().format(fmt) : "",
                r.getBloodSugar(), r.getMeasureType(),
                r.getRemark() != null ? r.getRemark() : ""
        });
    }


    private void createBodySheet(XSSFWorkbook workbook, List<HealthRecordBody> records,
                                 DateTimeFormatter dateFmt) {
        String[] headers = {"记录日期", "体重(kg)", "BMI", "体脂率(%)", "收缩压", "舒张压", "心率", "腰围(cm)", "备注"};
        createSheet(workbook, "身体记录", records, headers, r -> new Object[]{
                r.getRecordDate() != null ? r.getRecordDate().format(dateFmt) : "",
                r.getWeight(), r.getBmi(), r.getBodyFat(),
                r.getSystolicPressure(), r.getDiastolicPressure(), r.getHeartRate(),
                r.getWaistline(), r.getRemark() != null ? r.getRemark() : ""
        });
    }

    private void createDietSheet(XSSFWorkbook workbook, List<HealthRecordDiet> records,
                                 DateTimeFormatter fmt) {
        String[] headers = {"进食时间", "食物名称", "餐类", "热量(kcal)", "碳水(g)", "蛋白质(g)", "脂肪(g)", "膳食纤维(g)", "份量", "备注"};
        createSheet(workbook, "饮食记录", records, headers, r -> new Object[]{
                r.getEatTime() != null ? r.getEatTime().format(fmt) : "",
                r.getFoodName() != null ? r.getFoodName() : "",
                r.getMealType(), r.getCalories(), r.getCarbs(), r.getProtein(),
                r.getFat(), r.getFiber(), r.getPortion(),
                r.getRemark() != null ? r.getRemark() : ""
        });
    }

    private void createExerciseSheet(XSSFWorkbook workbook, List<HealthRecordExercise> records,
                                     DateTimeFormatter dateFmt) {
        String[] headers = {"运动日期", "运动类型ID", "时长(分钟)", "消耗热量(kcal)", "平均心率", "备注"};
        createSheet(workbook, "运动记录", records, headers, r -> new Object[]{
                r.getExerciseDate() != null ? r.getExerciseDate().format(dateFmt) : "",
                r.getExerciseTypeId(), r.getDurationMinutes(), r.getCaloriesBurned(),
                r.getHeartRateAvg(), r.getRemark() != null ? r.getRemark() : ""
        });
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

}
