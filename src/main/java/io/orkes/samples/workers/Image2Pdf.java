package io.orkes.samples.workers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;


@Component
public class Image2Pdf implements Worker {
    @Override
    public String getTaskDefName() {
        return "image_2_pdf";
    }

    public String imagesToPdf(List<String> inputImages) throws IOException, DocumentException {
        Document document = new Document(PageSize.A4, 20.0f, 20.0f, 20.0f, 150.0f);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter pdfWriter = PdfWriter.getInstance(document, baos);
        document.open();
        for (String singleImage : inputImages) {
            byte[] imageBytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(singleImage);
            Image image = Image.getInstance(imageBytes);
            document.setPageSize(image);
            document.newPage();
            image.setAbsolutePosition(0, 0);
            document.add(image);
        }
        document.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        Document document = new Document(PageSize.A4, 20.0f, 20.0f, 20.0f, 150.0f);
        List<String> inputStrings = (List<String>) task.getInputData().get("input_files");

        for (String singleImage : inputStrings) {
            try {
                Image image = Image.getInstance(singleImage);
                document.setPageSize(image);
                document.newPage();
                image.setAbsolutePosition(0, 0);
                document.add(image);
            } catch (BadElementException e) {
                e.printStackTrace();
            } catch (IOException | DocumentException e) {
                e.printStackTrace();
            }
        }
        try {
            String generatedPdf = imagesToPdf(inputStrings);
            result.addOutputData("output_file", generatedPdf);
            result.addOutputData("file_name", "Generated.pdf");
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            result.addOutputData("output_file", "");
            result.addOutputData("file_name", "Generated.pdf");
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        } catch (DocumentException e) {
            e.printStackTrace();
            result.addOutputData("output_file", "");
            result.addOutputData("file_name", "Generated.pdf");
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        }
    }
}
