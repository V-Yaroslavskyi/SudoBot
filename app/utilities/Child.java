package utilities;

/**
 * Created by bohdan on 22.04.17.
 */
public class Child {
    private String name;
    private String bDay;
    private String age;

    public Child(String name, String bDay, String age) {
        this.name = name;
        this.bDay = bDay;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public String getbDay() {
        return bDay;
    }

    public String getAge() {
        return age;
    }
}
