package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.diabetes.monitor.common.BizException;
import com.diabetes.monitor.common.ResultCode;
import com.diabetes.monitor.entity.*;
import com.diabetes.monitor.service.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Resource
    private SysUserService sysUserService;
    @Resource
    private HealthRecordBodyService bodyService;
    @Resource
    private HealthRecordBloodSugarService bloodSugarService;
    @Resource
    private HealthRecordDietService dietService;
    @Resource
    private HealthRecordExerciseService exerciseService;

    // 类顶部常量
    private static final String FONT_PATH = "C:/Windows/Fonts/simsun.ttc,0";  // 宋体

    @Resource
    private ThreadPoolTaskExecutor taskExecutor;
    /**
     * 创建 PDF 表格单元格
     */
    private PdfPCell createPdfCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }


    /**
     * 生成健康报告（支持统计区间；startDate/endDate 为 null 时默认最近 7 天）
     */
    @Override
    public ResponseEntity<byte[]> generateReport(Integer userId, LocalDate startDate, LocalDate endDate) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            log.warn("生成报告失败：用户不存在，userId={}", userId);
            return new ResponseEntity<>(new byte[0], HttpStatus.OK);
        }

        // ===== 统计窗口：默认最近 7 天（含今天），左闭右开 =====
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();
        LocalDate start = (startDate != null) ? startDate : end.minusDays(6);
        if (start.isAfter(end)) {
            throw new BizException(ResultCode.BAD_REQUEST, "开始日期不能晚于结束日期");
        }
        if (ChronoUnit.DAYS.between(start, end) > 89) {          // 最多 90 天
            throw new BizException(ResultCode.BAD_REQUEST, "报告统计区间不能超过 90 天");
        }
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime toExclusive = end.plusDays(1).atStartOfDay();

        CompletableFuture<HealthRecordBody> bodyFuture = CompletableFuture.supplyAsync(
                () -> bodyService.getLatest(userId), taskExecutor);

        //血糖
        CompletableFuture<List<HealthRecordBloodSugar>> sugarFuture = CompletableFuture.supplyAsync(
                () -> bloodSugarService.list(new QueryWrapper<HealthRecordBloodSugar>()
                        .eq("user_id", userId)
                        .ge("measure_time", from).lt("measure_time", toExclusive)
                        .orderByAsc("measure_time")), taskExecutor);

        // 饮食
        CompletableFuture<List<HealthRecordDiet>> dietFuture = CompletableFuture.supplyAsync(
                () -> dietService.list(new QueryWrapper<HealthRecordDiet>()
                        .eq("user_id", userId)
                        .ge("eat_time", from).lt("eat_time", toExclusive)
                        .orderByAsc("eat_time")), taskExecutor);

        // 运动
        CompletableFuture<List<HealthRecordExercise>> exerciseFuture = CompletableFuture.supplyAsync(
                () -> exerciseService.list(new QueryWrapper<HealthRecordExercise>()
                        .eq("user_id", userId)
                        .ge("exercise_date", start).le("exercise_date", end)
                        .orderByAsc("exercise_date")), taskExecutor);

        CompletableFuture.allOf(bodyFuture, sugarFuture, dietFuture, exerciseFuture).join();

        HealthRecordBody latestBody = bodyFuture.join();
        List<HealthRecordBloodSugar> sugarList = sugarFuture.join();
        List<HealthRecordDiet> dietList = dietFuture.join();
        List<HealthRecordExercise> exerciseList = exerciseFuture.join();

        //生成pdf
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, bos);
        document.open();

        ReportFonts fonts = createFonts();

        // 构建各章节内容
        buildTitleSection(document, fonts, start, end);
        buildBasicInfoSection(document, user, fonts);
        buildBodySection(document, latestBody, fonts);
        buildBloodSugarSection(document, sugarList, start, end, fonts);
        buildDietSection(document, dietList, start, end, fonts);
        buildExerciseSection(document, exerciseList, start, end, fonts);

        document.close();

        byte[] pdfBytes = bos.toByteArray();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "健康报告.pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    /**
     * 构建标题区块
     */
    private void buildTitleSection(Document document, ReportFonts fonts, LocalDate start, LocalDate end) {
        Paragraph title = new Paragraph("糖尿病健康管理报告", fonts.title());
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        Paragraph subtitle = new Paragraph(
                "统计区间：" + start + " ~ " + end + "　生成日期：" + LocalDate.now(),
                fonts.small());
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);
    }

    /**
     * 构建用户基本信息区块
     */
    private void buildBasicInfoSection(Document document, SysUser user, ReportFonts fonts) {
        addSectionHeader(document, "一、基本信息", fonts);
        document.add(new Paragraph("姓名：" + (user.getRealName() != null ? user.getRealName() : user.getUsername()), fonts.body()));
        document.add(new Paragraph("年龄：" + (user.getAge() != null ? user.getAge() : "未知") + " | 性别：" + (user.getGender() != null && user.getGender() == 1 ? "男" : "女"), fonts.body()));
        document.add(new Paragraph("糖尿病类型：" + (user.getDiabetesType() != null ? (user.getDiabetesType() == 1 ? "1型" : user.getDiabetesType() == 2 ? "2型" : "妊娠") : "未知"), fonts.body()));
        document.add(new Paragraph("确诊日期：" + (user.getDiagnosedDate() != null ? user.getDiagnosedDate().toString() : "未知"), fonts.body()));
        document.add(new Paragraph(" "));
    }

    /**
     * 构建最新体征指标区块
     */
    private void buildBodySection(Document document, HealthRecordBody latestBody, ReportFonts fonts) {
        addSectionHeader(document, "二、最新体征指标", fonts);
        if (latestBody != null) {
            PdfPTable bodyTable = new PdfPTable(4);
            bodyTable.setWidthPercentage(100);
            bodyTable.addCell(createPdfCell("体重(kg)", fonts.body()));
            bodyTable.addCell(createPdfCell("BMI", fonts.body()));
            bodyTable.addCell(createPdfCell("体脂率",fonts.body()));
            bodyTable.addCell(createPdfCell("血压", fonts.body()));
            bodyTable.addCell(createPdfCell(latestBody.getWeight() != null ? String.format("%.1f", latestBody.getWeight()) : "-", fonts.body()));
            bodyTable.addCell(createPdfCell(latestBody.getBmi() != null ? String.format("%.1f", latestBody.getBmi()) : "-", fonts.body()));
            bodyTable.addCell(createPdfCell(latestBody.getBodyFat() != null ? String.format("%.1f", latestBody.getBodyFat()) : "-", fonts.body()));
            bodyTable.addCell(createPdfCell(latestBody.getSystolicPressure() != null ? latestBody.getSystolicPressure() + "/" + latestBody.getDiastolicPressure() : "-", fonts.body()));
            document.add(bodyTable);
        } else {
            addNoData(document, "暂无体征数据", fonts);
        }
        document.add(new Paragraph(" "));

    }

    /**
     * 构建血糖记录区块（统计区间）
     */
    private void buildBloodSugarSection(Document document, List<HealthRecordBloodSugar> sugarList,
                                        LocalDate start, LocalDate end, ReportFonts fonts) {
        addSectionHeader(document, "三、血糖记录（" + start + " ~ " + end + "）", fonts);
        if (sugarList.isEmpty()) {
            addNoData(document, "暂无血糖记录", fonts);
        } else {
            double sum = 0, max = 0, min = 999; int overCount = 0;
            PdfPTable sugarTable = new PdfPTable(4);
            sugarTable.setWidthPercentage(100);
            sugarTable.addCell(createPdfCell("时间", fonts.body()));
            sugarTable.addCell(createPdfCell("类型", fonts.body()));
            sugarTable.addCell(createPdfCell("血糖值", fonts.body()));
            sugarTable.addCell(createPdfCell("状态", fonts.body()));

            // 遍历血糖记录
            for (HealthRecordBloodSugar r : sugarList) {
                double val = r.getBloodSugar();
                sum += val; max = Math.max(max, val); min = Math.min(min, val);
                if (val > 7.8) overCount++;
                // 测量类型字典（与 sql/init.sql 一致）：1 空腹 / 2 餐前 / 3 餐后2h / 4 睡前 / 5 凌晨
                String type = switch (r.getMeasureType() == null ? 0 : r.getMeasureType()) {
                    case 1 -> "空腹";
                    case 2 -> "餐前";
                    case 3 -> "餐后2h";
                    case 4 -> "睡前";
                    case 5 -> "凌晨";
                    default -> "未知";
                };
                String status = val > 7.8 ? "偏高" : val < 3.9 ? "偏低" : "正常";
                sugarTable.addCell(createPdfCell(r.getMeasureTime() != null ? r.getMeasureTime().toString() : "-", fonts.body()));
                sugarTable.addCell(createPdfCell(type, fonts.body()));
                sugarTable.addCell(createPdfCell(String.format("%.1f mmol/L", val), fonts.body()));
                sugarTable.addCell(createPdfCell(status, fonts.body()));
            }
            document.add(sugarTable);
            document.add(new Paragraph(" "));
            double avg = sum / sugarList.size();
            document.add(new Paragraph(String.format("统计：均值%.1f | 最高%.1f | 最低%.1f | 超标%d次", avg, max, min, overCount), fonts.body()));
        }
        document.add(new Paragraph(" "));

    }

    /**
     * 构建饮食汇总区块（统计区间；日均按区间天数算）
     */
    private void buildDietSection(Document document, List<HealthRecordDiet> dietList,
                                  LocalDate start, LocalDate end, ReportFonts fonts) {
        addSectionHeader(document, "四、饮食汇总（" + start + " ~ " + end + "）", fonts);
        if (dietList.isEmpty()) {
            addNoData(document, "暂无饮食记录", fonts);
        } else {
            double totalCal = 0, totalCarb = 0, totalProtein = 0;
            for (HealthRecordDiet r : dietList) {
                totalCal += r.getCalories() != null ? r.getCalories() : 0;
                totalCarb += r.getCarbs() != null ? r.getCarbs() : 0;
                totalProtein += r.getProtein() != null ? r.getProtein() : 0;
            }
            int days = Math.max(1, (int) ChronoUnit.DAYS.between(start, end) + 1);
            document.add(new Paragraph(String.format("日均摄入：%.0fkcal | 碳水%.0fg | 蛋白%.0fg", totalCal/days, totalCarb/days, totalProtein/days), fonts.body()));
            document.add(new Paragraph("糖友建议：1500-1800kcal/日，碳水占比45-55%", fonts.small()));
        }
        document.add(new Paragraph(" "));

    }

    /**
     * 构建运动汇总区块（统计区间）
     */
    private void buildExerciseSection(Document document, List<HealthRecordExercise> exerciseList,
                                      LocalDate start, LocalDate end, ReportFonts fonts) {
        addSectionHeader(document, "五、运动汇总（" + start + " ~ " + end + "）", fonts);
        if (exerciseList.isEmpty()) {
            addNoData(document, "暂无运动记录", fonts);
        } else {
            int totalMin = 0; double totalBurn = 0;
            for (HealthRecordExercise r : exerciseList) {
                totalMin += r.getDurationMinutes() != null ? r.getDurationMinutes() : 0;
                totalBurn += r.getCaloriesBurned() != null ? r.getCaloriesBurned() : 0;
            }
            document.add(new Paragraph(String.format("总运动时长：%d分钟 | 总消耗：%.0fkcal", totalMin, totalBurn), fonts.body()));
            document.add(new Paragraph("建议：每周至少150分钟中等强度运动", fonts.small()));
        }

    }

    /**
     * 添加区块标题
     */
    private void addSectionHeader(Document document, String text, ReportFonts fonts) {
        Paragraph p = new Paragraph(text, fonts.header());
        p.setSpacingBefore(12);
        document.add(p);
    }

    /**
     * 添加"暂无数据"提示
     */
    private void addNoData(Document document, String text, ReportFonts fonts) {
        document.add(new Paragraph(text, fonts.body()));
    }

    private record ReportFonts(Font title, Font header, Font body, Font small) {}

    private ReportFonts createFonts() {
        BaseFont baseFont;
        try {
            baseFont = BaseFont.createFont(FONT_PATH, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (IOException e) {
            log.error("加载中文字体失败", e);
            return new ReportFonts(new Font(), new Font(), new Font(), new Font());
        }
        return new ReportFonts(
                new Font(baseFont, 20, Font.BOLD),
                new Font(baseFont, 14, Font.BOLD),
                new Font(baseFont, 11),
                new Font(baseFont, 9)
        );
    }

}
