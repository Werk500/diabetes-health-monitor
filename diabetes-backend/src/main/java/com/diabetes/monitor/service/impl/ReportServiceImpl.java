package com.diabetes.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import java.time.LocalDateTime;
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
     * 生成健康报告
     */
    @Override
    public ResponseEntity<byte[]> generateReport(Integer userId) {

        //查用户信息 + 全部健康数据（近7天）
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            log.warn("生成报告失败：用户不存在，userId={}", userId);
            return new ResponseEntity<>(new byte[0], HttpStatus.OK);
        }

        // 血糖/饮食/运动近7天...
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        CompletableFuture<HealthRecordBody> bodyFuture = CompletableFuture.supplyAsync(
                () -> bodyService.getLatest(userId), taskExecutor);

        //血糖
        CompletableFuture<List<HealthRecordBloodSugar>> sugarFuture = CompletableFuture.supplyAsync(
                () -> bloodSugarService.list(new QueryWrapper<HealthRecordBloodSugar>()
                        .eq("user_id", userId).ge("measure_time", sevenDaysAgo)
                        .orderByAsc("measure_time")), taskExecutor);

        // 饮食
        CompletableFuture<List<HealthRecordDiet>> dietFuture = CompletableFuture.supplyAsync(
                () -> dietService.list(new QueryWrapper<HealthRecordDiet>()
                        .eq("user_id", userId)
                        .ge("eat_time", sevenDaysAgo)
                        .orderByAsc("eat_time")), taskExecutor);

        // 运动
        CompletableFuture<List<HealthRecordExercise>> exerciseFuture = CompletableFuture.supplyAsync(
                () -> exerciseService.list(new QueryWrapper<HealthRecordExercise>()
                        .eq("user_id", userId)
                        .ge("exercise_date", sevenDaysAgo.toLocalDate().toString())
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
        buildTitleSection(document, fonts);
        buildBasicInfoSection(document, user, fonts);
        buildBodySection(document, latestBody, fonts);
        buildBloodSugarSection(document, sugarList, fonts);
        buildDietSection(document, dietList, sevenDaysAgo, fonts);
        buildExerciseSection(document, exerciseList, fonts);

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
    private void buildTitleSection(Document document, ReportFonts fonts) {
        Paragraph title = new Paragraph("糖尿病健康管理报告", fonts.title());
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        Paragraph subtitle = new Paragraph("生成日期：" + java.time.LocalDate.now(), fonts.small());
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
     * 构建近7天血糖记录区块
     */
    private void buildBloodSugarSection(Document document, List<HealthRecordBloodSugar> sugarList, ReportFonts fonts) {
        addSectionHeader(document, "三、近7天血糖记录", fonts);
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
                String type = r.getMeasureType() != null && r.getMeasureType() == 1 ? "空腹" : r.getMeasureType() != null && r.getMeasureType() == 2 ? "餐后2h" : "随机";
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
     * 构建近7天饮食汇总区块
     */
    private void buildDietSection(Document document, List<HealthRecordDiet> dietList, LocalDateTime sevenDaysAgo, ReportFonts fonts) {
        addSectionHeader(document, "四、近7天饮食汇总", fonts);
        if (dietList.isEmpty()) {
            addNoData(document, "暂无饮食记录", fonts);
        } else {
            double totalCal = 0, totalCarb = 0, totalProtein = 0;
            for (HealthRecordDiet r : dietList) {
                totalCal += r.getCalories() != null ? r.getCalories() : 0;
                totalCarb += r.getCarbs() != null ? r.getCarbs() : 0;
                totalProtein += r.getProtein() != null ? r.getProtein() : 0;
            }
            int days = Math.max(1, (int) java.time.temporal.ChronoUnit.DAYS.between(sevenDaysAgo.toLocalDate(), java.time.LocalDate.now()) + 1);
            document.add(new Paragraph(String.format("日均摄入：%.0fkcal | 碳水%.0fg | 蛋白%.0fg", totalCal/days, totalCarb/days, totalProtein/days), fonts.body()));
            document.add(new Paragraph("糖友建议：1500-1800kcal/日，碳水占比45-55%", fonts.small()));
        }
        document.add(new Paragraph(" "));

    }

    /**
     * 构建近7天运动汇总区块
     */
    private void buildExerciseSection(Document document, List<HealthRecordExercise> exerciseList, ReportFonts fonts) {
        addSectionHeader(document, "五、近7天运动汇总", fonts);
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
