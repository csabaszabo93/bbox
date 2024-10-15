package hu.bbox.producer.repositories;

import java.util.function.UnaryOperator;
import java.util.random.RandomGeneratorFactory;

public class RandomProductVatStore implements UnaryOperator<String> {
    @Override
    public String apply(String s) {
        int random = RandomGeneratorFactory.getDefault().create().nextInt(50);
        return String.valueOf(random);
    }
}
