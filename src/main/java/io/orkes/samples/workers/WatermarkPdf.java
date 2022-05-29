package io.orkes.samples.workers;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class WatermarkPdf implements Worker {
    @Override
    public String getTaskDefName() {
        return "watermark_pdf";
    }


    public String watermarkPdf(String input, String watermarkType, String watermarkData) throws IOException, DocumentException {
        PdfReader reader = new PdfReader(Base64.getDecoder().decode(input));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfStamper stamper = new PdfStamper(reader, baos);
        Font f = new Font(Font.FontFamily.HELVETICA, 15);
        int n = reader.getNumberOfPages();
        PdfContentByte over;
        Rectangle pagesize;
        float x, y;
        for (int i = 1; i <= n; i++) {

            // get page size and position
            pagesize = reader.getPageSizeWithRotation(i);
            x = (pagesize.getLeft() + pagesize.getRight()) / 2;
            y = (pagesize.getTop() + pagesize.getBottom()) / 2;
            over = stamper.getOverContent(i);
            over.saveState();

            // set transparency
            PdfGState state = new PdfGState();
            state.setFillOpacity(0.2f);
            over.setGState(state);

            switch (watermarkType) {
                case "IMAGE": {
                    byte[] imageBytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(watermarkData);
                    Image img = Image.getInstance(imageBytes);
                    float w = img.getScaledWidth();
                    float h = img.getScaledHeight();
                    over.addImage(img, w, 0, 0, h, x - (w / 2), y - (h / 2));
                }
                case "TEXT": {
                    Phrase p = new Phrase(watermarkData, f);
                    ColumnText.showTextAligned(over, Element.ALIGN_CENTER, p, x, y, 0);
                }
            }
            over.restoreState();
        }
        stamper.close();
        reader.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        List<String> inputStrings = (List<String>) task.getInputData().get("input_files");
        String watermarkType = (String) task.getInputData().get("watermark_type");
        String watermarkData = (String) task.getInputData().get("watermark_data");
        try {
            String watermarkedPdf = watermarkPdf(inputStrings.get(0), watermarkType, watermarkData);
            result.addOutputData("output_file", watermarkedPdf);
            result.addOutputData("file_name", "Watermarked.pdf");
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
            result.addOutputData("output_file", "");
            result.addOutputData("file_name", "Watermarked.pdf");
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        }
    }
}
