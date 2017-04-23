package utilities.nlp;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NameCaser {

    private List<String>cases;

    private static final int GENITIVE_CASE = 1;
    private static final int ABLATIVE_CASE = 4;

    public NameCaser(String name) throws IOException {


        Elements elementsByTag = Jsoup.connect("http://namecaselib.com/ua/case-ua/" + name + "/").get()
                .body()
                // "magic zero" is chosen because both lists contain only one element. Otherwise we can not parse it anyway
                .getElementsByClass("namecase").get(0) // fixme selectors should be refactored to avoid such hard-coded statements
                .getElementsByTag("ul").get(0)
                .getElementsByTag("li");
        cases = new ArrayList<>(7);
        for (Element element : elementsByTag){
            cases.add(element.text().substring(3));
        }
    }

    public String toGenitiveCase(){
        return cases.get(GENITIVE_CASE);
    }

    public String toAblativeCase(){
        return cases.get(ABLATIVE_CASE);
    }
}
