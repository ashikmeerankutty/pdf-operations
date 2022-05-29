package io.orkes.samples.workers;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.parser.PdfImageObject;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;


import java.io.*;
import java.util.Base64;
import java.util.List;


@Component
public class CompressPdf implements Worker {
    @Override
    public String getTaskDefName() {
        return "compress_pdf";
    }

    public static float FACTOR = 0.5f;

    public String resizePdf(String input) throws IOException, DocumentException {
        PdfName key = new PdfName("ITXT_SpecialId");
        PdfName value = new PdfName("123456789");
        // Read the file
        PdfReader reader = new PdfReader(Base64.getDecoder().decode(input));
        int n = reader.getXrefSize();
        PdfObject object;
        PRStream stream;
        // Look for image and manipulate image stream
        for (int i = 0; i < n; i++) {
            object = reader.getPdfObject(i);
            if (object == null || !object.isStream())
                continue;
            stream = (PRStream)object;
            PdfObject pdfsubtype = stream.get(PdfName.SUBTYPE);
            if (pdfsubtype != null && pdfsubtype.toString().equals(PdfName.IMAGE.toString())) {
                PdfImageObject image = new PdfImageObject(stream);
                BufferedImage bi = image.getBufferedImage();
                if (bi == null) continue;
                int width = (int)(bi.getWidth() * FACTOR);
                int height = (int)(bi.getHeight() * FACTOR);
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                AffineTransform at = AffineTransform.getScaleInstance(FACTOR, FACTOR);
                Graphics2D g = img.createGraphics();
                g.drawRenderedImage(bi, at);
                ByteArrayOutputStream imgBytes = new ByteArrayOutputStream();
                ImageIO.write(img, "JPG", imgBytes);
                stream.clear();
                stream.setData(imgBytes.toByteArray(), false, PRStream.BEST_COMPRESSION);
                stream.put(PdfName.TYPE, PdfName.XOBJECT);
                stream.put(PdfName.SUBTYPE, PdfName.IMAGE);
                stream.put(key, value);
                stream.put(PdfName.FILTER, PdfName.DCTDECODE);
                stream.put(PdfName.WIDTH, new PdfNumber(width));
                stream.put(PdfName.HEIGHT, new PdfNumber(height));
                stream.put(PdfName.BITSPERCOMPONENT, new PdfNumber(8));
                stream.put(PdfName.COLORSPACE, PdfName.DEVICERGB);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfStamper stamper = new PdfStamper(reader, baos);
        stamper.close();
        reader.close();
        // Save altered PDF
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        List<String> inputStrings = (List<String>) task.getInputData().get("input_files");
        try {
            String compressedPdf = resizePdf(inputStrings.get(0));
            result.addOutputData("output_file", compressedPdf);
            result.addOutputData("file_name", "Compressed.pdf");
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            result.addOutputData("output_file", "");
            result.addOutputData("file_name", "Compressed.pdf");
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        } catch (DocumentException e) {
            e.printStackTrace();
            result.addOutputData("output_file", "");
            result.addOutputData("file_name", "Compressed.pdf");
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        }
    }
}
