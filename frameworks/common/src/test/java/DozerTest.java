import com.github.dozermapper.core.DozerBeanMapperBuilder;

import java.util.Optional;

public class DozerTest {
    public static void main(String[] args) {
        SourceObject source = new SourceObject();
        DestinationObject target = new DestinationObject();

        Optional.ofNullable(source)
                .ifPresent(each -> DozerBeanMapperBuilder.buildDefault().map(each, target));

        System.out.println(target);
    }

    public static class SourceObject {
        private String name = "John";
        private int age = 30;

        // Getters and setters
    }

    public static class DestinationObject {
        private String name;
        private int age;

        // Getters and setters

        @Override
        public String toString() {
            return "DestinationObject{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    '}';
        }
    }
}