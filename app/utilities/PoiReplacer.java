package utilities;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

import scala.collection.AbstractSeq;
import scala.collection.JavaConverters;
import scala.collection.immutable.Map;
import utilities.nlp.NameCaser;

public class PoiReplacer {

    private final static String TEMPLATE_FILL_DIR = "generated/";
    private final static String TEMPLATE = "conf/templates/SOC_template.docx";

    //builder method
    public File buildDoc(Map<String, String> prepReplacementMap, scala.collection.immutable.List<Child> children, String UID) throws IOException {

        final File attachTemplate = attachFromTemplate(UID);
        final String templateName = attachTemplate.getName();


        java.util.Map<String, String> allReplacements
                = new HashedMap<>(JavaConverters.mapAsJavaMapConverter(prepReplacementMap).asJava());

        final NameCaser plaintiffNameCaser = new NameCaser(allReplacements.get("@plaintiffName"));
        final String plaintiffNameRv = plaintiffNameCaser.toGenitiveCase();
        allReplacements.put("@rvPlaintiffName", plaintiffNameRv);
        allReplacements.put("@ovPlaintiffName", plaintiffNameCaser.toAblativeCase());

        final NameCaser defendantNameCaser = new NameCaser(allReplacements.get("@defendantName"));
        final String defendantNameRv = defendantNameCaser.toGenitiveCase();
        allReplacements.put("@rvDefendantName", defendantNameRv);
        allReplacements.put("@ovDefendantName", defendantNameCaser.toAblativeCase());

        allReplacements.put("@1childrenList", childrenEnum1(plaintiffNameRv, defendantNameRv, allReplacements.get("@childrenLiveWith"), children));
        allReplacements.put("@2childrenList", childrenEnum2(children));
        allReplacements.put("@numberOfBirthSerts",
                children.isEmpty()? "":"         8. Копія свідоцтва про народження дітей –" +((AbstractSeq) children).size()+ "eкз." // fixme spaces are temp fix
        );
        allReplacements.put("@noRATSReason", children.isEmpty() ? "відсутність згоди другого з подружжя" : "наявність неповнолітніх дітей");
        allReplacements.put("@currDate",  LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        replace(TEMPLATE_FILL_DIR + templateName, allReplacements);

        return attachTemplate;
    }

    private String toRV(String name) {
        return name;// fixme STUB, REPLACE W/ REALIZATION
    }

    private File attachFromTemplate(String uniqueId) throws IOException {

        final File target = setAttachFileName(uniqueId);

        //situation when file wasn't deleted on some prev run is possible, so

        if (target.exists()) {
            target.delete();
        }

        Files.copy(new File(TEMPLATE).toPath(), target.toPath());

        return target;
    }

    private File setAttachFileName(String uniqueId) {         //todo refactor according to final doc template
        return new File(TEMPLATE_FILL_DIR + uniqueId + ".docx");
    }

    private void replace(
            String fileName,
            java.util.Map<String, String> replacements
    ) throws IOException {
        try {

            final XWPFDocument doc = new XWPFDocument(OPCPackage.open(fileName));

            // replacing in plain doc text
            doc.getParagraphs().forEach(p -> replaceInParagraph(p, replacements));

            // replacing in table, walking through all of cells
            // for every table, for its every row, for every cell in row and every paragraph in cell (actually < three)

            doc.getTables().stream()
                    .flatMap(table -> table.getRows().stream()) // getting all rows
                    .flatMap(cell -> cell.getTableCells().stream()) // getting all cells
                    .flatMap(row -> row.getParagraphs().stream()) // getting all paragraphs
                    .forEach(p -> replaceInParagraph(p, replacements)); // replace in all of paragraphs of all cells

            // end of in-table replacement, save document

            try (FileOutputStream fos = new FileOutputStream(new File(fileName), true)) {

                doc.write(fos);
                doc.close();
            }

            // to throw one exception type is enough, it is similar to IOException

        } catch (InvalidFormatException ex) {
            throw new IOException(ex);
        }
    }

    private void replaceInParagraph(XWPFParagraph p, java.util.Map<String, String> replacements) {

        String paragraphText = p.getText();

        for (java.util.Map.Entry<String, String> entry : replacements.entrySet()) {
            final String key = entry.getKey();
            if (paragraphText != null && paragraphText.contains(key)) {

                paragraphText = paragraphText.replace(key, entry.getValue());
                editParagraph(p, paragraphText);
            }
        }
    }

    private String childrenEnum1(String plaintiffNameRV, String defendantNameRV, String childrenLiveWith, scala.collection.immutable.List<Child> children) {

        if (children.isEmpty()) {
            return "";
        } else {
            int numOfChildren = ((AbstractSeq) children).size();
            StringBuilder result = new StringBuilder("За час перебування у шлюбі у ");
            result.append(plaintiffNameRV);
            result.append(" та ");
            result.append(defendantNameRV);
            result.append(" народилося "); // todo think about numbers!!!
            result.append(numOfChildren);
            result.append(numOfChildren > 1 ? " дітей:" : "дитина:");

            for (int i = 0; i < numOfChildren; i++) {
                final Child child = children.apply(i);
                result.append(child.getName()).append("(").append(child.getbDay()).append(")").append(i < numOfChildren - 1 ? ", " : ".");
            }
            result.append(numOfChildren > 1 ? " Діти живуть з " : " Дитина живе з ").append(childrenLiveWith).append(".");
            return result.toString();
        }
    }

    private String childrenEnum2(scala.collection.immutable.List<Child> children) {


        if (children.isEmpty()) {
            return "";
        } else {
            int numOfChildren = ((AbstractSeq) children).size();
            StringBuilder result = new StringBuilder("На час звернення до суду із цим позовом у подружжя є ");
            result.append(numOfChildren);
            result.append(" неповнолітніх дітей: ");

            for (int i = 0; i < numOfChildren; i++) {
                final Child child = children.apply(i);
                result.append(child.getName())
                        .append("(")
                        .append(child.getAge()).append(" років)")
                        .append(i < numOfChildren - 1 ? ", " : ".");
            }
            return result.toString();
        }
    }

    private void editParagraph(XWPFParagraph p, String text) {

        // Set text to first run, delete another
        final List<XWPFRun> runs = p.getRuns();
        runs.get(0).setText(text, 0);

        // Cleanup paragraph
        final int runsSize = runs.size();
        for (int i = 1; i < runsSize; i++) {
            // always delete first element (otherwise index would be displaced in runs array)
            p.removeRun(1);
        }
    }
}