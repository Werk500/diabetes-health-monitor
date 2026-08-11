package com.diabetes.monitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.Result;
import com.diabetes.monitor.entity.*;
import com.diabetes.monitor.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Tag(name = "健康记录", description = "身体指标、血糖、饮食、运动记录管理")
@RestController
@RequestMapping("/api/record")
public class HealthRecordController {

    @Autowired private HealthRecordBodyService bodyService;
    @Autowired private HealthRecordBloodSugarService bloodSugarService;
    @Autowired private HealthRecordDietService dietService;
    @Autowired private HealthRecordExerciseService exerciseService;
    @Autowired private SysExerciseTypeService exerciseTypeService;
    @Resource private ThreadPoolTaskExecutor taskExecutor;

    // ===== 身体指标 =====
    @Operation(summary = "新增身体指标记录")
    @PostMapping("/body")
    public Result addBody(@RequestBody HealthRecordBody record) {
        if (record.getWeight() != null && record.getUserId() != null && record.getRecordDate() == null) {
            record.setRecordDate(LocalDate.now());
        }
        bodyService.save(record);
        return Result.ok("记录成功");
    }

    @Operation(summary = "查询身体指标列表")
    @GetMapping("/body/list/{userId}")
    public Result bodyList(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<HealthRecordBody> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordBody::getUserId, userId)
               .ge(startDate != null && !startDate.isEmpty(), HealthRecordBody::getRecordDate, startDate)
               .le(endDate != null && !endDate.isEmpty(), HealthRecordBody::getRecordDate, endDate)
               .orderByDesc(HealthRecordBody::getRecordDate);
        return Result.ok(bodyService.list(wrapper));
    }

    @Operation(summary = "查询身体指标趋势")
    @GetMapping("/body/trend/{userId}")
    public Result bodyTrend(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        return Result.ok(bodyService.getBodyTrend(userId, startDate, endDate));
    }

    @Operation(summary = "查询最新身体指标")
    @GetMapping("/body/latest/{userId}")
    public Result bodyLatest(@PathVariable Integer userId) {
        return Result.ok(bodyService.getLatest(userId));
    }

    @Operation(summary = "删除身体指标记录")
    @DeleteMapping("/body/{id}")
    public Result deleteBody(@PathVariable Integer id) {
        bodyService.removeById(id);
        return Result.ok();
    }

    // ===== 血糖记录 =====
    @Operation(summary = "新增血糖记录")
    @PostMapping("/bloodSugar")
    public Result addBloodSugar(@RequestBody HealthRecordBloodSugar record) {
        bloodSugarService.save(record);
        return Result.ok("记录成功");
    }

    @Operation(summary = "查询血糖记录列表")
    @GetMapping("/bloodSugar/list/{userId}")
    public Result bloodSugarList(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<HealthRecordBloodSugar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordBloodSugar::getUserId, userId);
        if (startDate != null && !startDate.isEmpty()) wrapper.ge(HealthRecordBloodSugar::getMeasureTime, startDate + " 00:00:00");
        if (endDate != null && !endDate.isEmpty()) wrapper.le(HealthRecordBloodSugar::getMeasureTime, endDate + " 23:59:59");
        wrapper.orderByDesc(HealthRecordBloodSugar::getMeasureTime);
        return Result.ok(bloodSugarService.list(wrapper));
    }

    @Operation(summary = "查询血糖变化趋势")
    @GetMapping("/bloodSugar/trend/{userId}")
    public Result bloodSugarTrend(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        return Result.ok(bloodSugarService.getBloodSugarTrend(userId, startDate, endDate));
    }

    @Operation(summary = "查询最新血糖记录")
    @GetMapping("/bloodSugar/latest/{userId}")
    public Result bloodSugarLatest(@PathVariable Integer userId) {
        return Result.ok(bloodSugarService.getLatest(userId));
    }

    @Operation(summary = "删除血糖记录")
    @DeleteMapping("/bloodSugar/{id}")
    public Result deleteBloodSugar(@PathVariable Integer id) {
        bloodSugarService.removeById(id);
        return Result.ok();
    }

    // ===== 饮食记录 =====
    @Operation(summary = "新增饮食记录")
    @PostMapping("/diet")
    public Result addDiet(@RequestBody HealthRecordDiet record) {
        dietService.save(record);
        return Result.ok("记录成功");
    }

    @Operation(summary = "查询饮食记录列表")
    @GetMapping("/diet/list/{userId}")
    public Result dietList(@PathVariable Integer userId, @RequestParam(required = false) String date) {
        LambdaQueryWrapper<HealthRecordDiet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordDiet::getUserId, userId);
        if (date != null && !date.isEmpty()) {
            wrapper.ge(HealthRecordDiet::getEatTime, date + " 00:00:00")
                   .le(HealthRecordDiet::getEatTime, date + " 23:59:59");
        }
        wrapper.orderByDesc(HealthRecordDiet::getEatTime);
        return Result.ok(dietService.list(wrapper));
    }

    @Operation(summary = "查询饮食营养统计")
    @GetMapping("/diet/stats/{userId}")
    public Result dietStats(@PathVariable Integer userId, @RequestParam(required = false) String date) {
        return Result.ok(dietService.getDietStats(userId, date));
    }

    @Operation(summary = "删除饮食记录")
    @DeleteMapping("/diet/{id}")
    public Result deleteDiet(@PathVariable Integer id) {
        dietService.removeById(id);
        return Result.ok();
    }

    // ===== 运动记录 =====
    @Operation(summary = "新增运动记录")
    @PostMapping("/exercise")
    public Result addExercise(@RequestBody HealthRecordExercise record) {
        if (record.getExerciseDate() == null) record.setExerciseDate(LocalDate.now());
        exerciseService.save(record);
        return Result.ok("记录成功");
    }

    @Operation(summary = "查询运动记录列表")
    @GetMapping("/exercise/list/{userId}")
    public Result exerciseList(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<HealthRecordExercise> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecordExercise::getUserId, userId)
               .ge(startDate != null && !startDate.isEmpty(), HealthRecordExercise::getExerciseDate, startDate)
               .le(endDate != null && !endDate.isEmpty(), HealthRecordExercise::getExerciseDate, endDate)
               .orderByDesc(HealthRecordExercise::getExerciseDate);
        return Result.ok(exerciseService.list(wrapper));
    }

    @Operation(summary = "查询运动消耗趋势")
    @GetMapping("/exercise/trend/{userId}")
    public Result exerciseTrend(@PathVariable Integer userId, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        return Result.ok(exerciseService.getExerciseStats(userId, startDate, endDate));
    }

    @Operation(summary = "删除运动记录")
    @DeleteMapping("/exercise/{id}")
    public Result deleteExercise(@PathVariable Integer id) {
        exerciseService.removeById(id);
        return Result.ok();
    }

    // ===== 运动类型 =====
    @Operation(summary = "获取运动类型列表")
    @GetMapping("/exerciseType/list")
    public Result exerciseTypeList() {
        return Result.ok(exerciseTypeService.list(new LambdaQueryWrapper<SysExerciseType>().eq(SysExerciseType::getStatus, 1)));
    }

    // ===== 数据导出 =====
    @Operation(summary = "导出健康记录Excel")
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        // 1. 获取当前用户ID（类型安全检查）
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Integer)) {
            throw new BizException("未登录或登录已过期");
        }
        Integer userId = (Integer) auth.getPrincipal();

        // 限制导出数量：防止用户数据量过大导致内存溢出（OOM）
        int maxExportSize = 1000;
        // MyBatis-Plus的.last()方法用于追加SQL片段，这里添加LIMIT子句
        String limitSql = "LIMIT " + maxExportSize;

        // 2. 并行查询四类健康记录
        CompletableFuture<List<HealthRecordBloodSugar>> sugarFuture =
                CompletableFuture.supplyAsync(() -> bloodSugarService.list(
                        new LambdaQueryWrapper<HealthRecordBloodSugar>()
                                .eq(HealthRecordBloodSugar::getUserId, userId)
                                .orderByDesc(HealthRecordBloodSugar::getMeasureTime)
                                .last(limitSql)), taskExecutor);

        CompletableFuture<List<HealthRecordBody>> bodyFuture =
                CompletableFuture.supplyAsync(() -> bodyService.list(
                        new LambdaQueryWrapper<HealthRecordBody>()
                                .eq(HealthRecordBody::getUserId, userId)
                                .orderByDesc(HealthRecordBody::getRecordDate)
                                .last(limitSql)), taskExecutor);

        CompletableFuture<List<HealthRecordDiet>> dietFuture =
                CompletableFuture.supplyAsync(() -> dietService.list(
                        new LambdaQueryWrapper<HealthRecordDiet>()
                                .eq(HealthRecordDiet::getUserId, userId)
                                .orderByDesc(HealthRecordDiet::getEatTime)
                                .last(limitSql)), taskExecutor);

        CompletableFuture<List<HealthRecordExercise>> exerciseFuture =
                CompletableFuture.supplyAsync(() -> exerciseService.list(
                        new LambdaQueryWrapper<HealthRecordExercise>()
                                .eq(HealthRecordExercise::getUserId, userId)
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

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=health_records.xlsx");

        /**
         * 使用try-with-resources自动管理资源
         * XSSFWorkbook：POI的Excel工作簿对象（.xlsx格式）
         * OutputStream：响应输出流，用于向客户端写入数据
         * 这两个资源在使用完毕后会自动关闭
         */
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             OutputStream os = response.getOutputStream()) {

            /**
             * 创建4个Sheet页
             * 每个Sheet对应一类健康记录，使用不同的日期格式化器
             */
            createSugarSheet(workbook, sugarList, dateTimeFmt);
            createBodySheet(workbook, bodyList, dateFmt);
            createDietSheet(workbook, dietList, dateTimeFmt);
            createExerciseSheet(workbook, exerciseList, dateFmt);

            workbook.write(os);
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
