package ashikmeerankutty.pdf.operations.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.stereotype.Component;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.*;
import java.util.Base64;
import java.util.List;

@Component
public class MergePdf implements Worker {
    @Override
    public String getTaskDefName() {
        return "merge_pdf";
    }

    private static void mergePdf(List<String> list, ByteArrayOutputStream outputStream) throws DocumentException, IOException
    {
        Document document = new Document();
        PdfWriter pdfWriter = PdfWriter.getInstance(document, outputStream);
        document.open();
        PdfContentByte pdfContentByte = pdfWriter.getDirectContent();

        for (String inputString : list)
        {
            PdfReader pdfReader = new PdfReader(Base64.getDecoder().decode(inputString));
            for (int i = 1; i <= pdfReader.getNumberOfPages(); i++)
            {
                document.newPage();
                PdfImportedPage page = pdfWriter.getImportedPage(pdfReader, i);
                pdfContentByte.addTemplate(page, 0, 0);
            }
        }

        outputStream.flush();
        document.close();
        outputStream.close();
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        List<String> inputStrings = (List<String>) task.getInputData().get("input_files");

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mergePdf(inputStrings, baos);
            String mergedPdf = Base64.getEncoder().encodeToString(baos.toByteArray());
            result.addOutputData("output_file", mergedPdf);
            result.addOutputData("file_name", "Merged.pdf");
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
            result.addOutputData("output_file", "");
            result.addOutputData("file_name", "Merged.pdf");
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        }
    }
}
