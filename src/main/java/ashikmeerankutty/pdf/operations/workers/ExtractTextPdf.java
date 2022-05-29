package ashikmeerankutty.pdf.operations.workers;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.*;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Base64;
import java.util.List;

@Component
public class ExtractTextPdf implements Worker {
    @Override
    public String getTaskDefName() {
        return "extract_text_pdf";
    }


    public String extractPdf(String input) throws IOException, DocumentException {
        PdfReader reader = new PdfReader(Base64.getDecoder().decode(input));
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        TextExtractionStrategy strategy;

        String extractedText = "";
        for (int i = 1; i <= reader.getNumberOfPages(); i++)
        {
            strategy = parser.processContent(i, new SimpleTextExtractionStrategy());
            extractedText = extractedText + strategy.getResultantText();
        }
        reader.close();
        return extractedText;
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        List<String> inputStrings = (List<String>) task.getInputData().get("input_files");
        try {
            String extractedPdf = extractPdf(inputStrings.get(0));
            result.addOutputData("extracted_text", extractedPdf);
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        result.addOutputData("extracted_text", "");
        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }
}
