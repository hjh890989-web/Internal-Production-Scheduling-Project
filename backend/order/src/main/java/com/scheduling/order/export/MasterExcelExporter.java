package com.scheduling.order.export;

import com.scheduling.master.api.LineRoutingLookup;
import com.scheduling.master.api.LineTypeSummary;
import com.scheduling.master.api.SettingGroupLookup;
import com.scheduling.master.api.SettingGroupSummary;
import com.scheduling.master.api.VcConstraintLookup;
import com.scheduling.master.api.VcConstraintSummary;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 통합 마스터 → 원본 포맷 XLSX export — TK-12-1-1 (EP-12 ST-12-1, REQ-FUNC-OC-013).
 *
 * <p>SHEET 구성:
 * <ul>
 *   <li>{@code VC_CONSTRAINT} — 47품번 합금형·LP/IC mold·K/L (BR-V14·V15·V16)</li>
 *   <li>{@code LINE_TYPE} — 라우팅 라인 NEW/FORD priority (BR-E08)</li>
 *   <li>{@code SETTING_GROUP} — 압출 1~8 셋팅 그룹 (BR-E06·E07)</li>
 * </ul>
 *
 * <p>POI XSSF 사용. 원본 수식·서식 보존이 아닌 단순 값 export (Sprint 4 baseline).
 */
@Component
@Profile("with-infra")
public class MasterExcelExporter {

    private final VcConstraintLookup constraintLookup;
    private final LineRoutingLookup lineLookup;
    private final SettingGroupLookup settingLookup;

    public MasterExcelExporter(
        VcConstraintLookup constraintLookup,
        LineRoutingLookup lineLookup,
        SettingGroupLookup settingLookup
    ) {
        this.constraintLookup = constraintLookup;
        this.lineLookup = lineLookup;
        this.settingLookup = settingLookup;
    }

    public byte[] exportMaster() {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeVcConstraintSheet(wb);
            writeLineTypeSheet(wb);
            writeSettingGroupSheet(wb);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("MASTER.xlsx 생성 실패", e);
        }
    }

    private void writeVcConstraintSheet(Workbook wb) {
        var sheet = wb.createSheet("VC_CONSTRAINT");
        CellStyle headerStyle = headerStyle(wb);
        Row header = sheet.createRow(0);
        String[] headers = {"hose_id", "composite_count",
            "lp_molds_per_angle", "lp_angle_qty",
            "ic_molds_per_angle", "ic_angle_qty",
            "lp_left_setting", "lp_right_setting"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = header.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }
        List<VcConstraintSummary> rows = constraintLookup.findAll();
        int r = 1;
        for (VcConstraintSummary v : rows) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(v.hoseId());
            row.createCell(1).setCellValue(v.compositeCount());
            setShortOrBlank(row.createCell(2), v.lpMoldsPerAngle());
            setShortOrBlank(row.createCell(3), v.lpAngleQty());
            setShortOrBlank(row.createCell(4), v.icMoldsPerAngle());
            setShortOrBlank(row.createCell(5), v.icAngleQty());
            row.createCell(6).setCellValue(v.lpLeftSetting());
            row.createCell(7).setCellValue(v.lpRightSetting());
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private void writeLineTypeSheet(Workbook wb) {
        var sheet = wb.createSheet("LINE_TYPE");
        CellStyle headerStyle = headerStyle(wb);
        Row header = sheet.createRow(0);
        String[] headers = {"line_id", "line_type", "priority", "description"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = header.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }
        List<LineTypeSummary> rows = lineLookup.findAllActive();
        int r = 1;
        for (LineTypeSummary l : rows) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(l.lineId());
            row.createCell(1).setCellValue(l.lineType());
            row.createCell(2).setCellValue(l.priority());
            row.createCell(3).setCellValue(l.description() == null ? "" : l.description());
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private void writeSettingGroupSheet(Workbook wb) {
        var sheet = wb.createSheet("SETTING_GROUP");
        CellStyle headerStyle = headerStyle(wb);
        Row header = sheet.createRow(0);
        String[] headers = {"group_number", "group_name", "description"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = header.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }
        List<SettingGroupSummary> rows = settingLookup.findAllGroups();
        int r = 1;
        for (SettingGroupSummary g : rows) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(g.groupNumber());
            row.createCell(1).setCellValue(g.groupName());
            row.createCell(2).setCellValue(g.description() == null ? "" : g.description());
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static void setShortOrBlank(Cell cell, Short v) {
        if (v == null) cell.setBlank();
        else cell.setCellValue(v);
    }
}
