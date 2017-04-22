package utilities.nlp.def_caser;

import java.util.ArrayList;
import java.util.List;

public class Word {

    public static final int MAN = 1;
    public static final int WOMAN = 2;
    public static final int TYPE_FIRSTNAME = 1;
    public static final int TYPE_FATHERNAME = 2;
    public static final int TYPE_SURNAME = 3;

    // склоняемое слово (в нижнем регистре)
    private String word;
    // маска регистров букв (склоняемое слово)
    private String mask;
    // true, если все слово было в верхнем регистре и false, если не было
    private boolean isUpperCase;
    // содержит все падежи слова, полученые после склонения текущего слова
    private List<String> nameCases = new ArrayList<String>();

    public List<String> getNameCases() {
        return nameCases;
    }

    public Word setNameCases(List<String> nameCases) {
        this.nameCases = nameCases;
        returnMask();
        return this;
    }

    /**
     * Тип текущей записи (Фамилия/Имя/Отчество)
     * - 1 - ім’я
     * - 2 - прізвище
     * - 3 - по-батькові
     */
    private int type = 0;

    public int getType() {
        return type;
    }

    public Word setType(int type) {
        this.type = type;
        return this;
    }

    // пол
    private int gender = 0;

    public int getGender() {
        return gender;
    }

    public Word setGender(int gender) {
        this.gender = gender;
        return this;
    }

    public String getWord() {
        return word;
    }

    public Word setWord(String word) {
        this.word = word;
        return this;
    }

    public Word(String word) {
        this.setWord(word).setMask().setWord(this.word.toLowerCase());
    }

    private Word setMask() {
        String w = this.word;
        StringBuilder mask = new StringBuilder();
        this.isUpperCase = true;
        for (int i = 0, size = w.length(); i < size; i++) {
            String letter = w.substring(i, i + 1);
            if (letter.equals(letter.toLowerCase())) {
                mask.append("x");
                this.isUpperCase = false;
            } else {
                mask.append("X");
            }
        }
        this.mask = mask.toString();
        return this;
    }

    public Word returnMask() {
        List<String> res = new ArrayList<String>();
        if (isUpperCase) {
            for (String name : this.nameCases) {
                res.add(name.toUpperCase());
            }
        } else {
            int maskSize = this.mask.length();
            for (int q = 0; q < this.nameCases.size(); q++) {
                String name = this.nameCases.get(q);
                String letters = "";
                String masks = this.mask;
                int nameSize = name.length();
                int minSize = Math.min(maskSize, nameSize);
                for (int i = 0; i < minSize; i++) {
                    String letter = name.substring(i, i + 1);
                    letters += masks.substring(i, i + 1).equals("X") ?
                            letter.toUpperCase() : letter;
                }
                if (nameSize > maskSize) letters += name.substring(minSize);
                this.nameCases.set(q, letters);
            }
        }
        return this;
    }

    public boolean isUpperCase() {
        return isUpperCase;
    }
}
